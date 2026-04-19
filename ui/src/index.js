/*
 * Plugin "id" — Identity management (users, groups, companies, delegates,
 * container-scopes).
 *
 * Contract consumed by the Ligoj Vue host:
 *   - id         : stable plugin identifier
 *   - label      : display name
 *   - component  : root Vue component (plugin shell)
 *   - install    : called once at registration; receives ctx.router so the
 *                  plugin can register its own routes dynamically
 *   - feature    : single entry point callable from the app and other plugins
 *                  (action dispatcher over the plugin's service functions)
 *   - service    : raw service functions (direct ES access)
 *   - meta       : presentation hints (icon, color)
 *
 * Authored as source — compiled to `/webjars/id/vue/index.js` by Vite.
 * Shared host surface (stores, composables) is imported from `@ligoj/host`,
 * kept external at build so plugin and host share the same instances.
 */
import IdPlugin from './IdPlugin.vue'
import ContainerScopeView from './views/ContainerScopeView.vue'
import service from './service.js'

const features = {
  requireAgreement: service.requireAgreement,
  acceptAgreement: service.acceptAgreement,
  scheduleUpload: service.scheduleUpload,
}

const routes = [
  { path: '/id/container-scope', name: 'id-container-scope', component: ContainerScopeView },
]

export default {
  id: 'id',
  label: 'Identity',
  component: IdPlugin,
  routes,
  install({ router }) {
    for (const route of routes) {
      router.addRoute(route)
    }
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
