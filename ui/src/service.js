import { h } from 'vue'
import { VBtn, VChip, VIcon, useI18nStore } from '@ligoj/host'

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
