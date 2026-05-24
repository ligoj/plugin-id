import { h } from 'vue'
import { VBtn, VChip, VIcon, pluginRegistry, useI18nStore } from '@ligoj/host'

const REST = '/rest/'

/** Pull the group identifier out of a subscription. Mirrors the legacy
 *  `subscription.parameters['service:id:group']` lookup. */
function groupOf(subscription) {
  return subscription?.parameters?.['service:id:group'] ?? null
}

/**
 * Extract the tool segment from a node id (`service:id:<tool>[:<instance>]`)
 * and return the sub-plugin name the host expects to find in the registry
 * for that tool — e.g. `service:id:ldap:local` → `id-ldap`. Mirrors the
 * legacy `current.$super(...)` inheritance: identity-tool plugins extended
 * `plugin-id` and contributed their own subscription row actions.
 */
function subPluginIdFor(subscription) {
  const id = subscription?.node?.id || ''
  const parts = id.split(':').filter(Boolean)
  // Expect at least `service:id:<tool>` to delegate; pure `service:id:<x>`
  // with two segments means we have no tool, nothing to delegate to.
  if (parts.length < 3) return null
  return `id-${parts[2]}`
}

/**
 * Calls `feature(action, ...args)` on the loaded sub-plugin for the
 * subscription's tool, returning its VNodes (or an empty array). Falls
 * back to `[]` when nothing is registered, when the plugin doesn't
 * implement the action, or when the call itself throws — sub-plugin
 * failures must never break the parent's rendering.
 */
function delegateToToolPlugin(subscription, action) {
  const subId = subPluginIdFor(subscription)
  if (!subId) return []
  const plugin = pluginRegistry.get(subId)
  if (typeof plugin?.feature !== 'function') return []
  try {
    const result = plugin.feature(action, subscription)
    if (result == null) return []
    return Array.isArray(result) ? result : [result]
  } catch (err) {
    // Unknown actions are expected (plugin chose not to implement). Real
    // errors get surfaced so they don't disappear into the console-void.
    if (!new RegExp(`no feature ["']${action}["']`).test(err?.message || '')) {
      console.warn(`[plugin:id] delegate to ${subId}.${action} threw`, err)
    }
    return []
  }
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
    const buttons = [
      h(
        VBtn,
        {
          icon: true,
          size: 'small',
          variant: 'text',
          title: t('id.renderFeatures.manage'),
          to: `/id/subscription/${subscription?.id ?? ''}`,
        },
        () => h(VIcon, { size: 'small' }, () => 'mdi-account-multiple'),
      ),
    ]
    if (help) {
      buttons.push(
        h(
          VBtn,
          {
            icon: true,
            size: 'small',
            variant: 'text',
            title: t('id.renderFeatures.help'),
            href: help,
            target: '_blank',
            rel: 'noopener noreferrer',
          },
          () => h(VIcon, { size: 'small' }, () => 'mdi-help-circle-outline'),
        ),
      )
    }
    // Append tool-specific actions contributed by an id-<tool> sub-plugin
    // (e.g. `plugin-id-ldap` injects activity-export buttons here).
    buttons.push(...delegateToToolPlugin(subscription, 'renderFeatures'))
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
    return h(
      VChip,
      {
        size: 'small',
        variant: 'tonal',
        title: t('id.renderDetailsKey.group'),
        class: 'mr-1',
      },
      () => [
        h(VIcon, { start: true, size: 'small' }, () => 'mdi-account-group'),
        ' ',
        group,
      ],
    )
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
    return h(
      VChip,
      {
        size: 'small',
        variant: 'tonal',
        color: 'primary',
        title: t('id.renderDetailsFeatures.members'),
        class: 'mr-1',
      },
      () => [
        h(VIcon, { start: true, size: 'small' }, () => 'mdi-account-multiple'),
        ' ',
        String(count),
      ],
    )
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
