import { renderServiceLink, renderDetailsChip, delegateFeature, useI18nStore } from '@ligoj/host'
import { useGroupMembersDialog } from './composables/useGroupMembersDialog.js'

const REST = '/rest/'

/** Pull the group identifier out of a subscription. Mirrors the legacy
 *  `subscription.parameters['service:id:group']` lookup. */
function groupOf(subscription) {
  return subscription?.parameters?.['service:id:group'] ?? null
}

const service = {
  /**
   * Plugin-contributed buttons next to the host's unsubscribe icon on
   * ProjectDetailView's subscription rows. Returns VNodes directly —
   * the host mounts them as-is without HTML interpretation, mirroring
   * the legacy `renderFeatures` convention.
   *
   * For an identity subscription we expose two actions:
   *   - "Manage group members" → subscription-scoped `GroupMembersView`
   *   - "Help" → external link from the subscription parameters
   */
  renderFeatures(subscription) {
    const { t } = useI18nStore()
    const help = subscription?.parameters?.['service:id:help']
    const group = groupOf(subscription)
    const buttons = [
      // "Manage group members" — opens the globally-mounted
      // GroupMembersDialog scoped to this row's group (no navigation away
      // from the project). Disabled when the subscription carries no group
      // parameter (defensive — shouldn't happen). The `onChanged` callback
      // fires once on dialog close if the user added/removed a member,
      // dispatching a window-level event so the mounting host view
      // (typically plugin-ui's `ProjectDetailView`) can refresh just the
      // affected row — without plugin-id depending on plugin-ui code.
      renderServiceLink({
        icon: 'mdi-account-multiple',
        title: t('id.renderFeatures.manage'),
        disabled: !group,
        onClick: () => group && useGroupMembersDialog().openFor(group, {
          onChanged: () => window.dispatchEvent(new CustomEvent('ligoj:subscription-data-changed', {
            detail: { subscriptionId: subscription?.id, group },
          })),
        }),
      }),
    ]
    if (help) {
      buttons.push(renderServiceLink({ icon: 'mdi-help-circle-outline', title: t('id.renderFeatures.help'), href: help }))
    }
    // Append tool-specific actions contributed by an id-<tool> sub-plugin
    // (e.g. `plugin-id-ldap` injects activity-export buttons here).
    buttons.push(...delegateFeature(subscription, 'renderFeatures', 'id'))
    return buttons
  },

  /**
   * Plugin-rendered subscription "key" — the resource identifier, in
   * this case the LDAP group name pulled from the subscription
   * parameters. Mirrors the legacy `renderDetailsKey` /
   * `renderKey('service:id:group')` chain.
   */
  renderDetailsKey(subscription) {
    const { t } = useI18nStore()
    const group = groupOf(subscription)
    if (!group) return null
    return renderDetailsChip({ icon: 'mdi-account-group', text: group, title: t('id.renderDetailsKey.group') })
  },

  /**
   * Plugin-rendered supplementary detail — the member count chip the
   * legacy UI showed under "renderDetailsFeatures". Backend populates
   * `subscription.data.members` for identity subscriptions.
   */
  renderDetailsFeatures(subscription) {
    const { t } = useI18nStore()
    const count = subscription?.data?.members
    if (count == null) return null
    return renderDetailsChip({ icon: 'mdi-account-multiple', text: count, title: t('id.renderDetailsFeatures.members'), color: 'primary' })
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
