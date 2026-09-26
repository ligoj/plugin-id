/*
 * Group field in CREATE mode: the computed group is `<organization>-<simple name>` and the server
 * requires it to start with both the organization and the project key. When the project key does
 * not start with the organization, no name can satisfy both: say it upfront.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { useI18nStore } from '@ligoj/host'
import IdGroupField from '../fields/IdGroupField.vue'
import enMessages from '../i18n/en.js'

const FieldStub = { props: ['modelValue', 'errorMessages', 'label', 'readonly'], emits: ['update:modelValue'], template: '<input :data-error="(errorMessages || []).join(\'|\')" :data-label="label" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' }

function mountField(formValues, project) {
  return mount(IdGroupField, {
    props: { parameter: { id: 'service:id:group', mandatory: true }, modelValue: '', mode: 'create', formValues, project },
    global: { stubs: { LigojTextField: FieldStub, LigojAutocomplete: true } },
  })
}

describe('IdGroupField — organization / project key consistency', () => {
  beforeEach(() => {
    setActivePinia(createPinia()); useI18nStore().merge(enMessages, 'en')
    globalThis.fetch = vi.fn(() => Promise.resolve({ ok: false, status: 404, headers: { get: () => null }, json: () => Promise.resolve(null), text: () => Promise.resolve('') }))
  })

  it('explains that the project key must start with the organization', async () => {
    const w = mountField({ 'service:id:ou': 'project' }, { pkey: 'demo-2' })
    await w.findAll('input')[0].setValue('azert')
    await flushPromises()
    expect(w.findAll('input')[0].attributes('data-error')).toContain('The project key "demo-2" must start with the organization "project-"')
  })

  it('accepts a project key built with the organization and computes the group name', async () => {
    const w = mountField({ 'service:id:ou': 'demo' }, { pkey: 'demo-2' })
    await w.findAll('input')[0].setValue('2-azert')
    await flushPromises()
    expect(w.findAll('input')[0].attributes('data-error')).toBe('')
    expect(w.emitted('update:modelValue').at(-1)).toEqual(['demo-2-azert'])
  })
})
