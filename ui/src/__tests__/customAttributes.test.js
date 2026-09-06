import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@ligoj/host'
import { CONF_KEY, customAttributeNames, toCustomAttributesPayload } from '../customAttributes.js'

function session(data) {
  useAuthStore().session = { applicationSettings: { data } }
}

describe('customAttributes', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  it('merges the configured names with the ones present on the user, configured first, without duplicates', () => {
    session({ [CONF_KEY]: 'badge, uidFonctionnel ,' })
    expect(customAttributeNames({ customAttributes: { uidFonctionnel: 'x', legacy: 'y' } })).toEqual(['badge', 'uidFonctionnel', 'legacy'])
  })

  it('falls back to the user attributes when nothing is configured, and to nothing at all', () => {
    session({})
    expect(customAttributeNames({ customAttributes: { legacy: 'y' } })).toEqual(['legacy'])
    expect(customAttributeNames(null)).toEqual([])
    expect(customAttributeNames({})).toEqual([])
  })

  it('builds the payload with every name, trimmed values and null for blanks', () => {
    expect(toCustomAttributesPayload(['a', 'b', 'c'], { a: ' v ', b: '   ', d: 'ignored' })).toEqual({ a: 'v', b: null, c: null })
    expect(toCustomAttributesPayload([], null)).toEqual({})
  })
})
