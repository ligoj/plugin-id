import { h } from 'vue'
import { VBtn, VChip, VIcon, useI18nStore } from '@ligoj/host'

const REST = '/rest/'

const service = {
  /**
   * Plugin-contributed buttons rendered next to the host's unsubscribe
   * icon on the ProjectDetailView subscription rows. Returns an array of
   * VNodes — the host mounts them as-is without any HTML interpretation,
   * mirroring the legacy `renderFeatures` convention.
   *
   * For an identity subscription the action exposed today is "Manage
   * group members" — opens the plugin's group list with the subscription
   * pre-selected.
   */
  renderFeatures(subscription) {
    const { t } = useI18nStore()
    return [
      h(
        VBtn,
        {
          icon: true,
          size: 'small',
          variant: 'text',
          title: t('id.renderFeatures.manage'),
          to: `/id/group?subscription=${subscription?.id ?? ''}`,
        },
        () => h(VIcon, { size: 'small' }, () => 'mdi-account-multiple'),
      ),
    ]
  },

  /**
   * Plugin-rendered subscription details for the "Details" column. For an
   * identity subscription we surface the member count when the backend
   * populates `subscription.data.members`. Returns null when there is
   * nothing to show — the column degrades cleanly.
   */
  renderDetailsKey(subscription) {
    const { t } = useI18nStore()
    const count = subscription?.data?.members
    if (count == null) return null
    return h(
      VChip,
      { size: 'small', variant: 'tonal', title: t('id.renderDetailsKey.members') },
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
