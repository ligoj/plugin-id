// Load the sibling index.css at runtime. Vite's library build emits it as
// a separate file but does NOT add `import './index.css'` to the JS entry
// — so when the host dynamic-imports this bundle the stylesheet never
// loads. Injecting a <link rel="stylesheet"> resolved against
// import.meta.url keeps the approach path-agnostic.
if (typeof document !== 'undefined') {
  const id = 'ligoj-plugin-id-css'
  if (!document.getElementById(id)) {
    const link = document.createElement('link')
    link.id = id
    link.rel = 'stylesheet'
    link.href = new URL(/* @vite-ignore */ './index.css', import.meta.url).href
    document.head.appendChild(link)
  }
}

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
import UserListView from './views/UserListView.vue'
import UserEditView from './views/UserEditView.vue'
import GroupListView from './views/GroupListView.vue'
import GroupEditView from './views/GroupEditView.vue'
import CompanyListView from './views/CompanyListView.vue'
import CompanyEditView from './views/CompanyEditView.vue'
import DelegateListView from './views/DelegateListView.vue'
import DelegateEditView from './views/DelegateEditView.vue'
import ContainerScopeView from './views/ContainerScopeView.vue'
import service from './service.js'

const features = {
  requireAgreement: service.requireAgreement,
  acceptAgreement: service.acceptAgreement,
  scheduleUpload: service.scheduleUpload,
}

const routes = [
  { path: '/id/user', name: 'id-user', component: UserListView },
  { path: '/id/user/new', name: 'id-user-new', component: UserEditView },
  { path: '/id/user/:id', name: 'id-user-edit', component: UserEditView },
  { path: '/id/group', name: 'id-group', component: GroupListView },
  { path: '/id/group/new', name: 'id-group-new', component: GroupEditView },
  { path: '/id/group/:id', name: 'id-group-edit', component: GroupEditView },
  { path: '/id/company', name: 'id-company', component: CompanyListView },
  { path: '/id/company/new', name: 'id-company-new', component: CompanyEditView },
  { path: '/id/company/:id', name: 'id-company-edit', component: CompanyEditView },
  { path: '/id/delegate', name: 'id-delegate', component: DelegateListView },
  { path: '/id/delegate/new', name: 'id-delegate-new', component: DelegateEditView },
  { path: '/id/delegate/:id', name: 'id-delegate-edit', component: DelegateEditView },
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
