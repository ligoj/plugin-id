import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@ligoj/host'
import { CONF_KEY, readOnlyAttributes, isReadOnly } from '../readOnlyAttributes.js'

function session(data) {
  useAuthStore().session = { applicationSettings: { data } }
}

describe('readOnlyAttributes', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  it('reads the configured names, comma or space separated, trimmed', () => {
    session({ [CONF_KEY]: ' firstName, lastName customAttributes.badge ,' })
    expect(readOnlyAttributes()).toEqual(['firstName', 'lastName', 'customAttributes.badge'])
    session({})
    expect(readOnlyAttributes()).toEqual([])
  })

  it('locks a listed attribute in edit mode only, custom attributes by their full path', () => {
    session({ [CONF_KEY]: 'firstName customAttributes.badge' })
    expect(isReadOnly('firstName', true)).toBe(true)
    expect(isReadOnly('lastName', true)).toBe(false)
    expect(isReadOnly('customAttributes.badge', true)).toBe(true)
    expect(isReadOnly('customAttributes.other', true)).toBe(false)
    expect(isReadOnly('firstName', false)).toBe(false)
  })
})
