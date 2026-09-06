/*
 * Custom attributes of the users (primary identity provider).
 *
 * The provider declares the attribute names (LDAP: `service:id:ldap:people-custom-attributes`);
 * `UserOrgResource` forwards them, comma separated, in the session data under
 * `service:id:custom-attributes`. The user dialog renders one field per name so the
 * attributes are editable at creation and edition.
 */
import { useAuthStore } from '@ligoj/host'

export const CONF_KEY = 'service:id:custom-attributes'

/**
 * Names of the custom attributes to edit: the configured ones (session data) merged, in order,
 * with the ones present on the given user (an attribute the provider returned is always shown).
 */
export function customAttributeNames(user) {
  const configured = String(useAuthStore().appSettings?.data?.[CONF_KEY] || '')
    .split(',').map((s) => s.trim()).filter(Boolean)
  const fromUser = Object.keys(user?.customAttributes || {})
  return [...new Set([...configured, ...fromUser])]
}

/**
 * Payload map of the custom attributes: every known name is present, a blank value becomes `null`
 * so the provider removes the attribute instead of storing an empty string.
 */
export function toCustomAttributesPayload(names, values) {
  const payload = {}
  for (const name of names) {
    const value = values?.[name]
    const text = value == null ? '' : String(value).trim()
    payload[name] = text === '' ? null : text
  }
  return payload
}
