/*
 * The members table of a group fits its dialog: the emails and groups columns take the room the other columns
 * leave, cut their chips, and give their full values in a tooltip.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { useAuthStore } from '@ligoj/host'
import GroupMembersPanel from '../components/GroupMembersPanel.vue'

const MEMBER = {
  id: 'lara', firstName: 'Lara', lastName: 'Jiménez', company: 'department3',
  mails: ['lara.jimenez@sample.com', 'lj@sample.com', 'lara@other.com'],
  groups: [{ name: 'billing-ops' }, { name: 'dev' }, { name: 'ops' }, { name: 'qa' }],
}
const LjDataTable = {
  props: ['headers', 'items'], emits: ['update:options'],
  mounted() { this.$emit('update:options', { page: 1, itemsPerPage: 25, sortBy: [] }) },
  template: '<div class="vdt"><div v-for="it in items" :key="it.id" class="row">'
    + '<span class="mails-cell"><slot name="cell.mails" :item="it" /></span><span class="groups-cell"><slot name="cell.groups" :item="it" /></span></div></div>',
}
const stubs = {
  LjDataTable,
  'v-tooltip': { template: '<div class="tt"><slot /></div>' },
  'v-chip': { template: '<span class="vchip"><slot /></span>' },
  'v-icon': true,
}

function json(body) {
  return Promise.resolve({ ok: true, status: 200, headers: { get: () => 'application/json' }, json: () => Promise.resolve(body), text: () => Promise.resolve(JSON.stringify(body)) })
}

beforeEach(() => {
  setActivePinia(createPinia())
  useAuthStore().session = { admin: true }
  globalThis.fetch = vi.fn(() => json({ data: [MEMBER], recordsTotal: 1, recordsFiltered: 1 }))
})

async function mountPanel() {
  const w = mount(GroupMembersPanel, { shallow: true, props: { groupName: 'billing-ops' }, global: { stubs } })
  await flushPromises()
  return w
}

describe('GroupMembersPanel members table', () => {
  it('gives the emails and groups columns a share of the room left, never more', async () => {
    const headers = (await mountPanel()).findComponent(LjDataTable).props('headers')
    const width = (key) => headers.find((h) => h.key === key).width
    expect(width('mails')).toMatch(/%$/)
    expect(width('groups')).toMatch(/%$/)
  })

  it('shows every email in the tooltip of the cut cell', async () => {
    const cell = (await mountPanel()).find('.mails-cell')
    expect(cell.find('.cut').exists()).toBe(true)
    expect(cell.findAll('.tt .cell-tip > div').map((d) => d.text())).toEqual(MEMBER.mails)
  })

  it('shows every group in its chip in the tooltip of the cut cell', async () => {
    const cell = (await mountPanel()).find('.groups-cell')
    expect(cell.find('.cut').exists()).toBe(true)
    expect(cell.findAll('.tt .vchip').map((c) => c.text())).toEqual(['billing-ops', 'dev', 'ops', 'qa'])
  })
})
