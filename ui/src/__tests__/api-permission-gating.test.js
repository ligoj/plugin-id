/*
 * API-permission gating: every action firing a write call is hidden when the
 * session's `apiAuthorizations` do not grant its path and verb
 * (`useAuthStore().isAllowedApi`). Each action is checked twice: hidden for a
 * session without any API permission, shown when only its own permission is
 * granted (which also pins the path + verb it is gated by).
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore, useI18nStore } from '@ligoj/host'
import ContainerScopeView from '../views/ContainerScopeView.vue'
import GroupListView from '../views/GroupListView.vue'
import UserListView from '../views/UserListView.vue'
import CompanyListView from '../views/CompanyListView.vue'
import DelegateListView from '../views/DelegateListView.vue'
import GroupMembersPanel from '../components/GroupMembersPanel.vue'
import CompanyEditPanel from '../components/CompanyEditPanel.vue'
import GroupEditPanel from '../components/GroupEditPanel.vue'
import UserEditDialog from '../views/UserEditDialog.vue'
import DelegateEditDialog from '../views/DelegateEditDialog.vue'
import enMessages from '../i18n/en.js'

const dataTables = (rows) => ({ data: rows, recordsTotal: rows.length, recordsFiltered: rows.length })

// Response of each GET, by path prefix (the most specific first).
const RESPONSES = [
  ['rest/service/id/container-scope/', [{ id: 1, name: 'Department', dn: 'ou=department,dc=sample,dc=com', locked: false }]],
  ['rest/service/id/group/dev', { name: 'dev', scope: 'Department', locked: false }],
  ['rest/service/id/group', dataTables([{ name: 'dev', scope: 'Department', count: 1 }])],
  ['rest/service/id/company/acme', { name: 'acme', scope: 'Organization', locked: false }],
  ['rest/service/id/company', dataTables([{ name: 'acme', scope: 'Organization', count: 1 }])],
  ['rest/service/id/user/jdoe', { id: 'jdoe', firstName: 'John', lastName: 'Doe', company: 'acme', mails: [], groups: [] }],
  ['rest/service/id/user', dataTables([{ id: 'jdoe', firstName: 'John', lastName: 'Doe', canWriteGroups: true, groups: [{ name: 'dev' }] }])],
  ['rest/security/delegate/1', { id: 1, receiver: { id: 'jdoe' }, receiverType: 'USER', type: 'GROUP', name: 'dev', canAdmin: false, canWrite: false }],
  ['rest/security/delegate', dataTables([{ id: 1, receiver: { id: 'jdoe' }, receiverType: 'USER', type: 'GROUP', name: 'dev' }])],
]

function jsonResponse(body) {
  return {
    ok: true, status: 200,
    headers: { get: () => 'application/json' },
    clone() { return jsonResponse(body) },
    json: async () => body,
    text: async () => JSON.stringify(body),
  }
}

// Render-the-slot stubs: header actions, rows (+ their actions menu), dialogs
// (+ footer). The table stub requests its first page on mount and turns a row
// click into `row-click`.
const stubs = {
  LjPageHeader: { template: '<div class="hdr"><slot name="actions" /></div>' },
  LjButton: { emits: ['click'], template: '<button class="ljbtn" @click="$emit(\'click\')"><slot /></button>' },
  LjDialog: { props: ['modelValue'], template: '<div v-if="modelValue" class="ljdialog"><slot /><div class="foot"><slot name="footer" /></div></div>' },
  LjConfirmDialog: { props: ['modelValue'], template: '<div v-if="modelValue" class="confirm"><slot /></div>' },
  LjDataTable: {
    props: ['items'],
    emits: ['update:options', 'row-click'],
    mounted() { this.$emit('update:options', { page: 1, itemsPerPage: 25, sortBy: [] }) },
    template: '<div class="vdt"><div v-for="(it, i) in items" :key="i" class="row" @click="$emit(\'row-click\', it)"><slot name="actions" :item="it" /></div></div>',
  },
  'v-menu': { template: '<div class="menu"><slot name="activator" :props="{}" /><slot /></div>' },
  'v-list-item': { props: ['title'], template: '<div class="li">{{ title }}</div>' },
  'v-list': { template: '<div class="list"><slot /></div>' },
  'v-form': { template: '<form><slot /></form>' },
  LjSearch: true, LjSegmented: true, LjStatus: true, LjAvailabilityField: true, LigojTextField: true, LigojCombobox: true, LigojSelect: true,
  LigojAutocomplete: true, CreateAnotherToggle: true, GroupEditPanel: true, CompanyEditPanel: true, GroupMembersDialog: true,
  'v-tooltip': true, 'v-icon': true, 'v-alert': true, 'v-skeleton-loader': true, 'v-checkbox': true, 'v-divider': true, 'v-row': { template: '<div><slot /></div>' },
  'v-col': { template: '<div><slot /></div>' },
}

let t
function grant(...permissions) {
  useAuthStore().session = { roles: ['USER'], apiAuthorizations: permissions.map(([method, pattern]) => ({ method, pattern })) }
}
async function mountIt(component, props) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  const w = mount(component, { props, global: { stubs, plugins: [router] } })
  await flushPromises()
  return w
}
const texts = (w, selector) => w.findAll(selector).map((b) => b.text().trim())

beforeEach(() => {
  setActivePinia(createPinia())
  useI18nStore().merge(enMessages, 'en')
  t = useI18nStore().t
  globalThis.fetch = vi.fn((url, opts) => {
    if ((opts?.method || 'GET') !== 'GET') return Promise.resolve(jsonResponse({}))
    const match = RESPONSES.find(([prefix]) => String(url).includes(prefix))
    return Promise.resolve(jsonResponse(match ? match[1] : {}))
  })
})

describe('API-permission gating of the container scopes', () => {
  const SCOPE = '^rest/service/id/container-scope'
  for (const [key, selector, method] of [
    ['containerScope.new', '.hdr .ljbtn', 'POST'],
    ['common.edit', '.row .menu button', 'PUT'],
    ['common.delete', '.row .menu button', 'DELETE'],
  ]) {
    it(`'${key}' needs ${method} ${SCOPE}`, async () => {
      grant()
      expect(texts(await mountIt(ContainerScopeView), selector)).not.toContain(t(key))
      grant([method, SCOPE])
      expect(texts(await mountIt(ContainerScopeView), selector)).toContain(t(key))
    })
  }

  it('the row menu is hidden when none of its actions is allowed', async () => {
    grant()
    const w = await mountIt(ContainerScopeView)
    expect(w.findAll('.row').length).toBe(1)
    expect(w.find('.row .menu').exists()).toBe(false)
  })

  it('without PUT, the row opens the scope as a read-only view', async () => {
    grant()
    let w = await mountIt(ContainerScopeView)
    await w.find('.row').trigger('click')
    expect(texts(w, '.ljdialog .foot .ljbtn')).toEqual([t('common.close')])
    grant(['PUT', SCOPE])
    w = await mountIt(ContainerScopeView)
    await w.find('.row').trigger('click')
    expect(texts(w, '.ljdialog .foot .ljbtn')).toEqual([t('common.cancel'), t('common.save')])
  })
})

describe('API-permission gating of the list views', () => {
  for (const view of [UserListView, GroupListView, CompanyListView, DelegateListView]) {
    it(`${view.__name}: the row menu is hidden when none of its actions is allowed`, async () => {
      grant()
      const w = await mountIt(view)
      expect(w.findAll('.row').length).toBe(1)
      expect(w.find('.row .menu').exists()).toBe(false)
    })
  }
})

describe('API-permission gating of the group members', () => {
  it('managing the members needs PUT on the user memberships, not on the group', async () => {
    grant(['PUT', '^rest/service/id/group'])
    expect(texts(await mountIt(GroupListView), '.row .menu button')).not.toContain(t('id.group.manage'))
    grant(['PUT', '^rest/service/id/user'])
    expect(texts(await mountIt(GroupListView), '.row .menu button')).toContain(t('id.group.manage'))
  })

  it('adding a member needs PUT rest/service/id/user', async () => {
    grant()
    expect((await mountIt(GroupMembersPanel, { groupName: 'dev' })).find('.addbar').exists()).toBe(false)
    grant(['PUT', '^rest/service/id/user'])
    expect(texts(await mountIt(GroupMembersPanel, { groupName: 'dev' }), '.addbar .ljbtn')).toEqual([t('id.group.add')])
  })

  it('removing a member needs DELETE rest/service/id/user', async () => {
    grant()
    let w = await mountIt(GroupMembersPanel, { groupName: 'dev' })
    expect(w.findAll('.row').length).toBe(1)
    expect(w.find('.row .remove-danger').exists()).toBe(false)
    grant(['DELETE', '^rest/service/id/user'])
    w = await mountIt(GroupMembersPanel, { groupName: 'dev' })
    expect(w.find('.row .remove-danger').exists()).toBe(true)
  })
})

describe('API-permission gating of the edit panels and dialogs', () => {
  for (const [component, props, pattern] of [
    [CompanyEditPanel, { companyId: 'acme' }, '^rest/service/id/company'],
    [GroupEditPanel, { groupId: 'dev' }, '^rest/service/id/group'],
  ]) {
    it(`${component.__name}: deleting needs DELETE ${pattern}`, async () => {
      grant()
      expect(texts(await mountIt(component, props), '.ljbtn')).not.toContain(t('common.delete'))
      grant(['DELETE', pattern])
      expect(texts(await mountIt(component, props), '.ljbtn')).toContain(t('common.delete'))
    })
  }

  it('UserEditDialog: the user actions and the save follow the user API permissions', async () => {
    const props = { modelValue: true, userId: 'jdoe' }
    const LOCK = t('user.lock')
    const ISOLATE = t('user.isolate')
    const RESET = t('user.resetPassword')
    grant()
    let w = await mountIt(UserEditDialog, props)
    expect(w.find('.ljdialog').exists()).toBe(true)
    expect(w.find('.actions-label').exists()).toBe(false)
    expect(texts(w, '.li')).toEqual([])
    expect(texts(w, '.foot .ljbtn')).toEqual([t('common.cancel')])

    // lock + isolate are DELETE calls
    grant(['DELETE', '^rest/service/id/user'])
    w = await mountIt(UserEditDialog, props)
    expect(texts(w, '.li')).toEqual([LOCK, ISOLATE])
    expect(texts(w, '.foot .ljbtn')).toEqual([t('common.delete'), t('common.cancel')])

    // the password reset and the save are PUT calls
    grant(['PUT', '^rest/service/id/user'])
    w = await mountIt(UserEditDialog, props)
    expect(texts(w, '.li')).toEqual([RESET])
    expect(texts(w, '.foot .ljbtn')).toEqual([t('common.cancel'), t('common.save')])
  })

  it('DelegateEditDialog: saving a delegate needs PUT rest/security/delegate', async () => {
    const props = { modelValue: true, delegateId: 1 }
    grant()
    expect(texts(await mountIt(DelegateEditDialog, props), '.foot .ljbtn')).toEqual([t('common.cancel')])
    grant(['PUT', '^rest/security/delegate'])
    expect(texts(await mountIt(DelegateEditDialog, props), '.foot .ljbtn')).toEqual([t('common.cancel'), t('common.save')])
  })
})
