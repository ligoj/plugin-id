/*
 * The row actions of the identity tables open from vertical dots, like the other tables of Ligoj.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@ligoj/host'
import UserListView from '../views/UserListView.vue'
import GroupListView from '../views/GroupListView.vue'
import CompanyListView from '../views/CompanyListView.vue'
import DelegateListView from '../views/DelegateListView.vue'
import ContainerScopeView from '../views/ContainerScopeView.vue'

const ROW = { id: 'r1', name: 'r1', login: 'r1', receiver: { id: 'r1' }, type: 'GROUP' }
const stubs = {
  LjDataTable: {
    props: ['items'], emits: ['update:options'],
    mounted() { this.$emit('update:options', { page: 1, itemsPerPage: 25, sortBy: [] }) },
    template: '<div class="vdt"><div v-for="(it, i) in items" :key="i" class="row"><slot name="actions" :item="it" /></div></div>',
  },
  'v-menu': { template: '<div><slot name="activator" :props="{}" /></div>' },
  'v-icon': { template: '<i class="vicon"><slot /></i>' },
}

function json(body) {
  return Promise.resolve({ ok: true, status: 200, headers: { get: () => 'application/json' }, json: () => Promise.resolve(body), text: () => Promise.resolve(JSON.stringify(body)) })
}

beforeEach(() => {
  setActivePinia(createPinia())
  useAuthStore().session = { admin: true }
  globalThis.fetch = vi.fn(() => json({ data: [ROW], recordsTotal: 1, recordsFiltered: 1 }))
})

describe.each([UserListView, GroupListView, CompanyListView, DelegateListView, ContainerScopeView])('%s', (view) => {
  it('opens the row actions from vertical dots', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
    const w = mount(view, { shallow: true, global: { plugins: [router], stubs } })
    await flushPromises()
    const icons = w.findAll('.row .lj-iconbtn .vicon').map((i) => i.text())
    expect(icons.length).toBeGreaterThan(0)
    expect([...new Set(icons)]).toEqual(['mdi-dots-vertical'])
  })
})
