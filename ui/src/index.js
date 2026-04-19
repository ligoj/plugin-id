/*
 * Plugin "id" — Identity management (users, groups, companies, delegates,
 * container-scopes).
 *
 * Contract consumed by the Ligoj Vue host:
 *   - id         : stable plugin identifier
 *   - label      : display name
 *   - component  : root Vue component (optional; set once views are moved in)
 *   - install    : called once at registration; receives shared context
 *   - feature    : single entry point callable from the app and other plugins
 *                  (action dispatcher over the plugin's service functions)
 *   - service    : raw service functions (direct ES access)
 *   - meta       : presentation hints (icon, color)
 *
 * Authored as source — compiled to `/webjars/id/vue/index.js` by Vite.
 */
import IdPlugin from './IdPlugin.vue'
import service from './service.js'

const features = {
  requireAgreement: service.requireAgreement,
  acceptAgreement: service.acceptAgreement,
  scheduleUpload: service.scheduleUpload,
}

export default {
  id: 'id',
  label: 'Identity',
  component: IdPlugin,
  install(/* ctx */) {
    // No-op. When slice 3b moves routes into the plugin, this will use
    // ctx.router to register dynamic routes for the plugin's views.
  },
  feature(action, ...args) {
    const fn = features[action]
    if (!fn) throw new Error(`Plugin "id" has no feature "${action}"`)
    return fn(...args)
  },
  service,
  meta: { icon: 'mdi-account-group', color: 'blue-darken-3' },
}

export { service }
