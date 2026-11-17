import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { useAuthStore } from '@ligoj/host'
import DelegateEditDialog from '../views/DelegateEditDialog.vue'

function jsonResponse(body) {
  return Promise.resolve({
    ok: true, status: 200,
    headers: { get: (k) => (k === 'content-type' ? 'application/json' : null) },
    json: () => Promise.resolve(body), text: () => Promise.resolve(JSON.stringify(body)),
  })
}
const lastUrl = () => String(globalThis.fetch.mock.calls.at(-1)[0])

describe('DelegateEditDialog — receiver and resource search', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    globalThis.fetch = vi.fn(() => jsonResponse({ data: [{ id: 'fdaugan', firstName: 'Fabrice', lastName: 'Daugan' }], recordsTotal: 1 }))
  })

  it('sends the typed user details as the DataTables search criterion the user API reads', async () => {
    const w = mount(DelegateEditDialog, { shallow: true, props: { modelValue: true, delegateId: null } })
    await flushPromises()
    w.vm.receiverSearch = 'Fabrice'
    await w.vm.loadReceiverItems()
    expect(lastUrl()).toContain('rest/service/id/user?')
    expect(lastUrl()).toContain('search[value]=Fabrice')
    expect(lastUrl()).not.toContain('q=')
    expect(w.vm.receiverItems).toEqual([{ id: 'fdaugan', label: 'fdaugan — Fabrice Daugan' }])
  })

  it('labels the user candidates by the visual identifier, the value stays the login', async () => {
    useAuthStore().session = { applicationSettings: { data: { 'service:id:visual-id-name': 'customAttributes.employeeId' } } }
    globalThis.fetch = vi.fn(() => jsonResponse({ data: [{ id: 'fdaugan', firstName: 'Fabrice', lastName: 'Daugan', customAttributes: { employeeId: 'E-42' } }], recordsTotal: 1 }))
    const w = mount(DelegateEditDialog, { shallow: true, props: { modelValue: true, delegateId: null } })
    await flushPromises()
    await w.vm.loadReceiverItems()
    expect(w.vm.receiverItems).toEqual([{ id: 'fdaugan', label: 'E-42 — Fabrice Daugan' }])
  })

  it('searches the groups and companies with the same criterion', async () => {
    globalThis.fetch = vi.fn(() => jsonResponse({ data: [{ name: 'dev' }], recordsTotal: 1 }))
    const w = mount(DelegateEditDialog, { shallow: true, props: { modelValue: true, delegateId: null } })
    await flushPromises()
    w.vm.resourceSearch = 'de'
    await w.vm.loadResourceItems()
    expect(lastUrl()).toContain('rest/service/id/group?search[value]=de&rows=20')
    w.vm.form.receiverType = 'COMPANY'
    await flushPromises()
    w.vm.receiverSearch = 'ac'
    await w.vm.loadReceiverItems()
    expect(lastUrl()).toContain('rest/service/id/company?search[value]=ac&rows=20')
    expect(w.vm.receiverItems).toEqual([{ id: 'dev', label: 'dev' }])
  })

  it('sends no criterion when nothing is typed', async () => {
    const w = mount(DelegateEditDialog, { shallow: true, props: { modelValue: true, delegateId: null } })
    await flushPromises()
    w.vm.receiverSearch = '   '
    await w.vm.loadReceiverItems()
    expect(lastUrl()).toContain('rest/service/id/user?rows=20')
    expect(lastUrl()).not.toContain('search')
  })

  it('edit: the title badge and the prefilled receiver show the visual identifier, the value stays the login', async () => {
    useAuthStore().session = { applicationSettings: { data: { 'service:id:visual-id-name': 'customAttributes.employeeId' } } }
    // The delegate endpoint gives the receiver as a login only: the dialog looks the user up
    const user = { id: 'u0042', name: 'u0042', firstName: 'Fabrice', lastName: 'Daugan', customAttributes: { employeeId: 'E-42' } }
    globalThis.fetch = vi.fn((url) => jsonResponse(String(url).includes('rest/service/id/user/u0042')
      ? user
      : { id: 7, receiverType: 'user', receiver: 'u0042', type: 'group', name: 'dig' }))
    const w = mount(DelegateEditDialog, { shallow: true, props: { modelValue: false, delegateId: 7 } })
    await w.setProps({ modelValue: true })
    await flushPromises()
    expect(w.vm.form.receiver).toBe('u0042')
    expect(w.vm.receiverBadge).toBe('E-42')
    expect(w.vm.receiverDisplayItems[0]).toEqual({ id: 'u0042', label: 'E-42 — Fabrice Daugan' })
    // Another receiver picked: the badge follows the new value
    w.vm.form.receiver = 'other'
    expect(w.vm.receiverBadge).toBe('other')
  })
})
