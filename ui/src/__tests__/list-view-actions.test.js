/*
 * The identity list views expose their toolbar to plugins through the
 * `actionExtension` feature: each one hands `LjPageHeader` its target and a
 * context supplier (selection + reload) — see the host `useActionExtensions`.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { LjPageHeader } from '@ligoj/host'
import UserListView from '../views/UserListView.vue'
import GroupListView from '../views/GroupListView.vue'
import CompanyListView from '../views/CompanyListView.vue'
import DelegateListView from '../views/DelegateListView.vue'

const VIEWS = [
  { component: UserListView, target: 'user' },
  { component: GroupListView, target: 'group' },
  { component: CompanyListView, target: 'company' },
  { component: DelegateListView, target: 'delegate' },
]

function jsonResponse(body) {
  return Promise.resolve({
    ok: true, status: 200,
    headers: { get: (k) => (k === 'content-type' ? 'application/json' : null) },
    json: () => Promise.resolve(body), text: () => Promise.resolve(JSON.stringify(body)),
  })
}

function mountView(component) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  return mount(component, { shallow: true, global: { plugins: [router] } })
}

describe('identity list views — toolbar plugin actions', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    globalThis.fetch = vi.fn(() => jsonResponse({ data: [], recordsTotal: 0, recordsFiltered: 0 }))
  })

  for (const { component, target } of VIEWS) {
    it(`${component.__name || target} targets '${target}' and supplies selection + reload`, async () => {
      const w = mountView(component)
      await flushPromises()
      const header = w.findComponent(LjPageHeader)
      expect(header.exists()).toBe(true)
      expect(header.props('actionsTarget')).toBe(target)
      const context = header.props('actionsContext')()
      expect(context.selected).toEqual([])
      expect(typeof context.reload).toBe('function')
      // reload re-queries the list
      const before = globalThis.fetch.mock.calls.length
      context.reload()
      await flushPromises()
      expect(globalThis.fetch.mock.calls.length).toBeGreaterThan(before)
    })
  }
})
