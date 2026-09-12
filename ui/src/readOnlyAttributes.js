/*
 * Attributes locked after the user creation (`service:id:read-only-attributes`,
 * a parameter of the primary identity node, forwarded by `UserOrgResource#decorate`
 * in the session data). The user dialog shows them read-only in edit mode; the
 * API refuses an update changing them.
 */
import { useAuthStore } from '@ligoj/host'

export const CONF_KEY = 'service:id:read-only-attributes'

/** The configured attribute names: `firstName`, `mail`, `customAttributes.<name>`... */
export function readOnlyAttributes() {
  return String(useAuthStore().appSettings?.data?.[CONF_KEY] || '')
    .split(/[,\s]+/).map((s) => s.trim()).filter(Boolean)
}

/** True when the attribute is locked: listed, and the dialog edits an existing user. */
export function isReadOnly(name, isEdit) {
  return !!isEdit && readOnlyAttributes().includes(name)
}
