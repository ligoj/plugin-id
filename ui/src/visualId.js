/*
 * Visual identifier of the users in the UI (`service:id:visual-id-name` /
 * `service:id:visual-id-label` configuration values, forwarded by the backend
 * session decoration — see UserOrgResource#decorate).
 *
 * `visual-id-name` selects the attribute displayed as the user's identifier
 * (table first column, implicit sort): `id` (default), `firstName`,
 * `lastName`, `mail` (first one) or `customAttributes.<property>`. The
 * `visual-id-label` is a STATIC (non localizable) header label; when absent,
 * the localized name of the selected attribute is used.
 *
 * Every resolution falls back to the login (`id`).
 */
import { useAuthStore, useI18nStore } from '@ligoj/host'

const NAME_KEY = 'service:id:visual-id-name'
const LABEL_KEY = 'service:id:visual-id-label'

/** i18n key of each accepted plain attribute, for the default label. */
const NAME_LABEL_KEYS = {
  id: 'user.login',
  firstName: 'user.firstName',
  lastName: 'user.lastName',
  mail: 'user.emails',
}

/** The configured attribute name, validated — unaccepted values collapse to 'id'. */
export function visualIdName() {
  const name = useAuthStore().appSettings?.data?.[NAME_KEY] || 'id'
  return (NAME_LABEL_KEYS[name] || name.startsWith('customAttributes.')) ? name : 'id'
}

/** True when a non-default visual identifier is configured. */
export function isVisualId() {
  return visualIdName() !== 'id'
}

/**
 * Table column/sort key: 'visual-id' (mapped server-side by UserOrgResource)
 * when configured, plain 'id' otherwise.
 */
export function visualIdColumnKey() {
  return isVisualId() ? 'visual-id' : 'id'
}

/** The user's visual identifier value, login fallback. */
export function visualIdValue(user) {
  if (!user) return ''
  const name = visualIdName()
  if (name === 'mail') return user.mails?.[0] || user.id
  if (name.startsWith('customAttributes.')) return user.customAttributes?.[name.substring(17)] || user.id
  return user[name] || user.id
}

/** The column header label: configured static label, else localized attribute name. */
export function visualIdLabel() {
  const { t } = useI18nStore()
  const label = useAuthStore().appSettings?.data?.[LABEL_KEY]
  if (label) return label
  const name = visualIdName()
  return NAME_LABEL_KEYS[name] ? t(NAME_LABEL_KEYS[name]) : name.substring(17)
}
