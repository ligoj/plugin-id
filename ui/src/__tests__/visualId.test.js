import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore, useI18nStore } from '@ligoj/host'
import { visualIdName, isVisualId, visualIdColumnKey, visualIdValue, visualIdLabel, receiverName } from '../visualId.js'

function setConfig(data) {
  // The auth store reads `appSettings` from the session payload
  useAuthStore().session = { applicationSettings: { data } }
}

describe('visualId resolution (service:id:visual-id-name/-label)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useI18nStore().merge({ 'user.login': 'Login', 'user.firstName': 'First Name', 'user.emails': 'Emails' }, 'en')
  })

  it('defaults to the login when unset or unaccepted', () => {
    setConfig({})
    expect(visualIdName()).toBe('id')
    expect(isVisualId()).toBe(false)
    expect(visualIdColumnKey()).toBe('id')
    setConfig({ 'service:id:visual-id-name': 'dn' })
    expect(visualIdName()).toBe('id')
  })

  it('accepts the plain attributes and customAttributes.<property>', () => {
    for (const name of ['firstName', 'lastName', 'mail', 'customAttributes.badge']) {
      setConfig({ 'service:id:visual-id-name': name })
      expect(visualIdName()).toBe(name)
      expect(isVisualId()).toBe(true)
      expect(visualIdColumnKey()).toBe('visual-id')
    }
  })

  it('resolves the value with login fallback', () => {
    const user = { id: 'jdoe', firstName: 'John', mails: ['j@d.io'], customAttributes: { badge: 'B-7' } }
    setConfig({ 'service:id:visual-id-name': 'firstName' })
    expect(visualIdValue(user)).toBe('John')
    setConfig({ 'service:id:visual-id-name': 'mail' })
    expect(visualIdValue(user)).toBe('j@d.io')
    expect(visualIdValue({ id: 'nomail' })).toBe('nomail')
    setConfig({ 'service:id:visual-id-name': 'customAttributes.badge' })
    expect(visualIdValue(user)).toBe('B-7')
    expect(visualIdValue({ id: 'nobadge', customAttributes: {} })).toBe('nobadge')
    expect(visualIdValue(null)).toBe('')
  })

  it('labels: static configuration wins, else localized attribute, else raw property', () => {
    setConfig({ 'service:id:visual-id-name': 'firstName', 'service:id:visual-id-label': 'Matricule' })
    expect(visualIdLabel()).toBe('Matricule')
    setConfig({ 'service:id:visual-id-name': 'firstName' })
    expect(visualIdLabel()).toBe('First Name')
    setConfig({})
    expect(visualIdLabel()).toBe('Login')
    setConfig({ 'service:id:visual-id-name': 'customAttributes.badge' })
    expect(visualIdLabel()).toBe('badge')
  })

  it('delegate receiver: a user shows its visual identifier, a container its name', () => {
    const user = { id: 'jdoe', name: 'jdoe', firstName: 'John', customAttributes: { badge: 'B-7' } }
    setConfig({ 'service:id:visual-id-name': 'customAttributes.badge' })
    expect(receiverName({ receiverType: 'USER', receiver: user })).toBe('B-7')
    expect(receiverName({ receiverType: 'user', receiver: user })).toBe('B-7')
    expect(receiverName({ receiverType: 'GROUP', receiver: { id: 'dig', name: 'DIG', customAttributes: { badge: 'x' } } })).toBe('DIG')
    expect(receiverName({ receiverType: 'COMPANY', receiver: { id: 'ing' } })).toBe('ing')
    setConfig({})
    expect(receiverName({ receiverType: 'USER', receiver: user })).toBe('jdoe')
    expect(receiverName({ receiverType: 'USER' })).toBe('')
    expect(receiverName(null)).toBe('')
  })
})
