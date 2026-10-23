/*
 * Unsaved changes of the edit dialogs: each one hands its form to LjDialog as `guard`, the dialog asking before
 * discarding a change (host LjDialog tests), and its Cancel goes through the dialog `close`. A group or a company is
 * created in a panel of the dialog: the panel hands its form to the list view. Its details have nothing to guard.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore, useI18nStore } from '@ligoj/host'
import ContainerScopeView from '../views/ContainerScopeView.vue'
import GroupListView from '../views/GroupListView.vue'
import CompanyListView from '../views/CompanyListView.vue'
import GroupEditPanel from '../components/GroupEditPanel.vue'
import CompanyEditPanel from '../components/CompanyEditPanel.vue'
import enMessages from '../i18n/en.js'

const close = vi.fn()
const stubs = {
  // The guarded dialog: its `close` is the one its content and footer call
  LjDialog: {
    name: 'LjDialog',
    props: ['modelValue', 'guard', 'guardReady'],
    setup: () => ({ close }),
    template: '<div v-if="modelValue" class="ljdialog"><slot :close="close" /><div class="foot"><slot name="footer" :close="close" /></div></div>',
  },
  LjButton: { emits: ['click'], template: '<button class="ljbtn" @click="$emit(\'click\')"><slot /></button>' },
  LjPageHeader: { template: '<div class="hdr"><slot name="actions" /></div>' },
  LjDataTable: true, LjConfirmDialog: true, LigojConfirmDialog: true, LjSearch: true, LjSegmented: true, LjStatus: true,
  LjAvailabilityField: true, LigojTextField: true, LigojCombobox: true, LigojSelect: true, LigojAutocomplete: true,
  CreateAnotherToggle: true, GroupMembersDialog: true,
  'v-tooltip': true, 'v-icon': true, 'v-alert': true, 'v-skeleton-loader': true, 'v-checkbox': true, 'v-divider': true,
  'v-menu': true, 'v-form': { template: '<form><slot /></form>' }, 'v-card-text': { template: '<div><slot /></div>' },
  'v-row': { template: '<div><slot /></div>' }, 'v-col': { template: '<div><slot /></div>' },
}

function json(body) {
  return { ok: true, status: 200, headers: { get: () => 'application/json' }, clone() { return json(body) }, json: async () => body, text: async () => JSON.stringify(body) }
}

beforeEach(() => {
  setActivePinia(createPinia())
  useI18nStore().merge(enMessages, 'en')
  useAuthStore().session = { admin: true }
  close.mockClear()
  globalThis.fetch = vi.fn(async () => json({ data: [], recordsTotal: 0, recordsFiltered: 0 }))
})

async function mountView(view) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  const w = mount(view, { global: { stubs, plugins: [router] } })
  await flushPromises()
  return w
}
const openDialog = (w) => w.findAllComponents({ name: 'LjDialog' }).find((d) => d.props('modelValue'))
const guardOf = (w) => JSON.stringify(openDialog(w).props('guard'))
const button = (w, key) => openDialog(w).findAll('.ljbtn').find((b) => b.text() === useI18nStore().t(key))

// The guarded value: the same while the dialog writes by itself, changed by an edit of the user, Cancel asking
async function expectGuarded(w, edit) {
  expect(openDialog(w).props('guard')).toBeTruthy()
  const unchanged = guardOf(w)
  await flushPromises()
  expect(guardOf(w)).toBe(unchanged)
  edit()
  await nextTick()
  expect(guardOf(w)).not.toBe(unchanged)
  await button(w, 'common.cancel').trigger('click')
  expect(close).toHaveBeenCalledTimes(1)
}

describe('edit dialogs of the identity views', () => {
  it('container scope', async () => {
    const w = await mountView(ContainerScopeView)
    w.vm.openNew()
    await nextTick()
    await expectGuarded(w, () => { w.vm.editForm.name = 'Department' })
  })

  it.each([
    ['group', GroupListView, GroupEditPanel],
    ['company', CompanyListView, CompanyEditPanel],
  ])('%s creation, its form in the panel', async (_, view, panel) => {
    const w = await mountView(view)
    w.vm.openCreate()
    await flushPromises()
    // The unchanged form is the one the panel shows
    expect(openDialog(w).props('guardReady')).toBe(true)
    await expectGuarded(w, () => { w.findComponent(panel).vm.form.name = 'devops' })
  })

  it.each([
    ['group', GroupListView, 'openEdit'],
    ['company', CompanyListView, 'openDetails'],
  ])('%s details: nothing to guard', async (_, view, open) => {
    const w = await mountView(view)
    w.vm[open]('dev')
    await flushPromises()
    expect(openDialog(w).props('guard')).toBeNull()
    await button(w, 'common.close').trigger('click')
    expect(close).toHaveBeenCalledTimes(1)
  })
})
