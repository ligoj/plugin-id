/*
 * Plugin "id" — Identity management (users, groups, companies, delegates, container-scopes).
 *
 * Contract consumed by the main app's plugin loader:
 *   - id         : stable plugin identifier
 *   - label      : display name
 *   - install    : called once at registration; receives shared context
 *   - feature    : single entry point callable from the app and other plugins
 *                  (action-dispatcher over the plugin's service functions)
 *   - service    : raw service functions (direct ES access)
 *   - meta       : presentation hints (icon, color)
 *
 * This entry is plain ES — no build step required. SFC-based views for this
 * plugin will ship once the per-plugin Vite pipeline is in place.
 */

const REST = '/rest/'

const service = {
  /** Resolves whether the usage agreement dialog must be shown. */
  requireAgreement(userSettings) {
    return !userSettings || !userSettings['security-agreement']
  },

  /** Marks the usage agreement as accepted for the current user. */
  async acceptAgreement(userSettings) {
    const resp = await fetch(REST + 'system/setting/security-agreement/1', {
      method: 'POST',
      credentials: 'include',
    })
    if (!resp.ok) throw new Error('Failed to accept agreement')
    if (userSettings) userSettings['security-agreement'] = true
    return true
  },

  /** Starts polling an import/upload endpoint; returns a handle for cancellation. */
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

// Feature dispatcher: the spec requires each plugin to expose a single `feature`
// function callable from the app and other plugins. Actions map to service calls.
const features = {
  requireAgreement: service.requireAgreement,
  acceptAgreement: service.acceptAgreement,
  scheduleUpload: service.scheduleUpload,
}

export default {
  id: 'id',
  label: 'Identity',
  install(/* ctx */) {
    // No-op for now. Routes for /id/* are hardcoded in the main app's router.
    // When 3b lands, this will register dynamic routes via ctx.router.
  },
  feature(action, ...args) {
    const fn = features[action]
    if (!fn) throw new Error(`Plugin "id" has no feature "${action}"`)
    return fn(...args)
  },
  service,
  meta: { icon: 'mdi-account-group', color: 'blue-darken-3' },
}

// Named export for direct ES imports (tests, cross-plugin calls).
export { service }
