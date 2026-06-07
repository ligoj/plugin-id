import { ref, watch } from 'vue'

/**
 * Live availability check for a creation form field (user login, group /
 * company / container-scope name). Debounces the typed value and runs
 * `check(value)` which resolves `true` when the resource ALREADY exists.
 *
 * Exposes a `status` ref driving the inline feedback:
 *   idle      → empty / too short / disabled (no check)
 *   checking  → request in flight
 *   available → free to use
 *   taken     → already exists (block the create)
 *   error     → the check itself failed (stay silent in the UI)
 *
 * A monotonic sequence guards against out-of-order responses so a slow early
 * keystroke can't overwrite a fast later one.
 *
 * @param {() => any} source    Getter for the field value (e.g. `() => form.id`).
 * @param {(v: string) => Promise<boolean>} check  Existence check.
 * @param {object} [opts]
 * @param {number} [opts.debounce=350]   Debounce in ms.
 * @param {number} [opts.minLength=1]     Skip the check below this length.
 * @param {() => boolean} [opts.enabled]  Gate (e.g. only on create, not demo).
 * @returns {{ status: import('vue').Ref<string> }}
 */
export function useAvailability(source, check, { debounce = 350, minLength = 1, enabled = () => true } = {}) {
  const status = ref('idle')
  let timer = null
  let seq = 0

  watch(source, (raw) => {
    clearTimeout(timer)
    const value = (raw ?? '').toString().trim()
    if (!enabled() || value.length < minLength) {
      status.value = 'idle'
      return
    }
    status.value = 'checking'
    const mine = ++seq
    timer = setTimeout(async () => {
      try {
        const exists = await check(value)
        if (mine === seq) status.value = exists ? 'taken' : 'available'
      } catch {
        if (mine === seq) status.value = 'error'
      }
    }, debounce)
  })

  return { status }
}

/**
 * Exact, case-insensitive existence check through a DataTable search endpoint.
 * Returns `true` when a returned item's `field` equals the value. Silent: a
 * failed request never raises a toast while the user is typing.
 *
 * @param {object} api       The `useApi()` instance.
 * @param {string} endpoint  REST path without the leading `rest/`.
 * @param {string} value     The exact value to look up.
 * @param {string} [field='name']  Item property to compare.
 */
export async function existsByExact(api, endpoint, value, field = 'name') {
  const url = `rest/${endpoint}?search[value]=${encodeURIComponent(value)}&rows=100&page=1`
  const resp = await api.get(url, { silent: true })
  const items = Array.isArray(resp) ? resp : (resp?.data || [])
  const needle = value.toLowerCase()
  return items.some((it) => String(it?.[field] ?? '').toLowerCase() === needle)
}
