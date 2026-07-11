import { h } from 'vue'
import { renderServiceLink, delegateFeature, useI18nStore, VChip, VIcon } from '@ligoj/host'
import { useGroupMembersDialog } from './composables/useGroupMembersDialog.js'

const REST = '/rest/'

/** Pull the group identifier out of a subscription. Mirrors the legacy
 *  `subscription.parameters['service:id:group']` lookup. */
function groupOf(subscription) {
  return subscription?.parameters?.['service:id:group'] ?? null
}

/**
 * Open the globally-mounted GroupMembersDialog scoped to a row's group. On
 * change it dispatches a window-level event so the mounting host view
 * (typically plugin-ui's ProjectDetailView) can refresh just the affected
 * row — without plugin-id depending on plugin-ui code.
 */
function openGroupMembers(subscription, group) {
  useGroupMembersDialog().openFor(group, {
    onChanged: () => window.dispatchEvent(new CustomEvent('ligoj:subscription-data-changed', {
      detail: { subscriptionId: subscription?.id, group },
    })),
  })
}

const service = {
  /**
   * Sidebar contribution: the top-level "Identity" menu (Users / Groups /
   * Companies / Delegates / Container scopes). Consumed by the host's
   * `mergeNav` engine via the `renderNav` feature — this section used to be
   * hardcoded in the host's BASE_NAV and now travels with the plugin that owns
   * it. Declarative data: the host resolves each `labelKey` against the shared
   * i18n store (the `nav.*` keys are host-owned sidebar vocabulary) and drops
   * any entry the user can't reach per its REST `auth` path. Positioned
   * `before: 'nav.projects'` so Identity keeps its historic slot between Home
   * and Projects.
   */
  renderNav() {
    return {
      id: 'identity',
      labelKey: 'nav.identity',
      icon: 'mdi-account-group',
      match: '/id',
      before: 'nav.projects',
      children: [
        { id: 'id-users', labelKey: 'nav.users', icon: 'mdi-account', route: '/id/user', match: '/id/user', auth: 'id/user' },
        { id: 'id-groups', labelKey: 'nav.groups', icon: 'mdi-account-group', route: '/id/group', match: '/id/group', auth: 'id/container/group' },
        { id: 'id-companies', labelKey: 'nav.companies', icon: 'mdi-domain', route: '/id/company', match: '/id/company', auth: 'id/container/company' },
        { id: 'id-delegates', labelKey: 'nav.delegates', icon: 'mdi-account-arrow-right', route: '/id/delegate', match: '/id/delegate', auth: 'id/delegate' },
        { id: 'id-scopes', labelKey: 'nav.containerScopes', icon: 'mdi-file-tree', route: '/id/scope', match: '/id/scope', auth: 'id/container-scope' },
      ],
    }
  },

  /**
   * Plugin-contributed buttons next to the host's unsubscribe icon on
   * ProjectDetailView's subscription rows. Group management is no longer a
   * dedicated button here — it moved onto the clickable group chip in
   * `renderDetailsKey`. What remains is the optional "Help" external link
   * plus any id-<tool> sub-plugin actions.
   */
  renderFeatures(subscription) {
    const { t } = useI18nStore()
    const help = subscription?.parameters?.['service:id:help']
    const buttons = []
    if (help) {
      buttons.push(renderServiceLink({ icon: 'mdi-help-circle-outline', title: t('id.renderFeatures.help'), href: help }))
    }
    // Append tool-specific actions contributed by an id-<tool> sub-plugin
    // (e.g. `plugin-id-ldap` injects activity-export buttons here).
    buttons.push(...delegateFeature(subscription, 'renderFeatures', 'id'))
    return buttons
  },

  /**
   * Plugin-rendered subscription "key": a single CLICKABLE chip merging the
   * group name (`service:id:group`) and its live member count
   * (`subscription.data.members`, populated by status/refresh). Clicking the
   * chip opens the group-members dialog (replacing the former dedicated
   * "Manage group members" button); the multi-line `title:` is promoted to a
   * v-tooltip by the host. Returns null when no group parameter is set.
   */
  renderDetailsKey(subscription) {
    const { t } = useI18nStore()
    const group = groupOf(subscription)
    if (!group) return null
    const count = subscription?.data?.members
    const tip = [
      `${t('id.renderDetailsKey.group')}: ${group}`,
      count != null ? `${t('id.renderDetailsFeatures.members')}: ${count}` : null,
      t('id.renderFeatures.manage'),
    ].filter(Boolean).join('\n')
    return h(
      VChip,
      {
        size: 'small',
        variant: 'tonal',
        color: 'primary',
        class: 'mr-1',
        style: 'cursor: pointer',
        role: 'button',
        title: tip,
        onClick: (e) => { e?.stopPropagation?.(); openGroupMembers(subscription, group) },
      },
      () => {
        const children = [h(VIcon, { start: true, size: 'small' }, () => 'mdi-account-group'), ' ', String(group)]
        if (count != null) {
          // Member count as a distinct INTERNAL chip (solid badge) — no second
          // person icon, so the group glyph isn't duplicated.
          children.push(h(
            VChip,
            { size: 'x-small', variant: 'flat', color: 'primary', label: true, class: 'ml-2' },
            () => String(count),
          ))
        }
        return children
      },
    )
  },

  /**
   * The member count is now merged into the clickable group chip in
   * `renderDetailsKey`, so there is no separate features chip anymore.
   */
  renderDetailsFeatures() {
    return null
  },

  requireAgreement(userSettings) {
    return !userSettings || !userSettings['security-agreement']
  },

  async acceptAgreement(userSettings) {
    const resp = await fetch(REST + 'system/setting/security-agreement/1', {
      method: 'POST',
      credentials: 'include',
    })
    if (!resp.ok) throw new Error('Failed to accept agreement')
    if (userSettings) userSettings['security-agreement'] = true
    return true
  },

  scheduleUpload(url, id, onDone, onPartial) {
    const handle = setInterval(
      () => service._syncUpload(url, id, onDone, onPartial, handle),
      1000,
    )
    return handle
  },

  async _syncUpload(url, id, onDone, onPartial, handle) {
    try {
      const resp = await fetch(REST + url + '/' + id + '/status', { credentials: 'include' })
      if (!resp.ok) return
      const data = await resp.json()
      onPartial?.(data)
      if (data.end) {
        clearInterval(handle)
        await service._finishUpload(url, id, onDone, onPartial)
      }
    } catch (err) {
      console.error('[plugin:id] upload sync error', err)
    }
  },

  async _finishUpload(url, id, onDone, onPartial) {
    try {
      const resp = await fetch(REST + url + '/' + id, { credentials: 'include' })
      if (!resp.ok) return
      const data = await resp.json()
      onPartial?.({ ...data.status, finished: true, errors: data.entries })
      onDone?.(data)
    } catch (err) {
      console.error('[plugin:id] upload result error', err)
    }
  },
}

export default service
