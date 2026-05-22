<template>
  <div>
    <v-skeleton-loader v-if="loading" type="card, actions" max-width="700" class="mb-4" />

    <v-card v-if="!loading" class="edit-card">
      <v-card-text>
        <v-form ref="formRef" @submit.prevent="save">
          <!-- Receiver: pick the kind first (drives the autocomplete
               endpoint for the identifier), then the identifier itself.
               Rendered side-by-side on >= sm to keep the dependency
               visible. -->
          <v-row dense>
            <v-col cols="12" sm="5">
              <v-select v-model="form.receiverType" :label="t('delegate.receiverType')" :items="receiverTypes" :item-title="typeTitle" item-value="value" :prepend-inner-icon="receiverIcon" :rules="[rules.required]" variant="outlined" class="mb-2">
                <template #item="{ props: itemProps, item }">
                  <v-list-item v-bind="itemProps">
                    <template #prepend>
                      <v-icon :icon="TYPE_ICONS[item?.value] || ''" />
                    </template>
                  </v-list-item>
                </template>
              </v-select>
            </v-col>
            <v-col cols="12" sm="7">
              <v-autocomplete
                v-model="form.receiver"
                v-model:search="receiverSearch"
                :label="t('delegate.receiver')"
                :items="receiverDisplayItems"
                item-title="label"
                item-value="id"
                :loading="receiverLoading"
                :rules="[rules.required]"
                no-filter
                clearable
                auto-select-first
                variant="outlined"
                class="mb-2"
                @update:search="onReceiverSearch"
                @update:menu="onReceiverMenu"
              />
            </v-col>
          </v-row>

          <!-- Resource: same pattern — the type drives the
               autocomplete endpoint (USER id, GROUP / TREE / COMPANY
               name). -->
          <v-row dense>
            <v-col cols="12" sm="5">
              <v-select v-model="form.type" :label="t('delegate.type')" :items="resourceTypes" :item-title="typeTitle" item-value="value" :prepend-inner-icon="typeIcon" :rules="[rules.required]" variant="outlined" class="mb-2">
                <template #item="{ props: itemProps, item }">
                  <v-list-item v-bind="itemProps">
                    <template #prepend>
                      <v-icon :icon="TYPE_ICONS[item?.value] || ''" />
                    </template>
                  </v-list-item>
                </template>
              </v-select>
            </v-col>
            <v-col cols="12" sm="7">
              <v-autocomplete
                v-model="form.name"
                v-model:search="resourceSearch"
                :label="t('delegate.resource')"
                :items="resourceDisplayItems"
                item-title="label"
                item-value="id"
                :loading="resourceLoading"
                :rules="[rules.required]"
                :hint="t('delegate.resourceHint')"
                persistent-hint
                no-filter
                clearable
                auto-select-first
                variant="outlined"
                class="mb-2"
                @update:search="onResourceSearch"
                @update:menu="onResourceMenu"
              />
            </v-col>
          </v-row>

          <v-checkbox v-model="form.canAdmin" :label="t('delegate.admin')" hide-details class="mb-2" />
          <v-checkbox v-model="form.canWrite" :label="t('delegate.write')" hide-details class="mb-2" />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-btn v-if="isEdit" color="error" variant="tonal" @click="confirmDelete = true">
          <v-icon start>mdi-delete</v-icon> {{ t('common.delete') }}
        </v-btn>
        <v-spacer />
        <v-btn variant="text" @click="router.push('/id/delegate')">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" variant="elevated" :loading="saving" @click="save">
          <v-icon start>mdi-content-save</v-icon> {{ t('common.save') }}
        </v-btn>
      </v-card-actions>
    </v-card>

    <LigojConfirmDialog
      v-model="confirmDelete"
      :title="t('delegate.deleteTitle')"
      :message="t('delegate.deleteConfirm', { name: form.receiver })"
      :confirm-label="t('common.delete')"
      confirm-color="error"
      :loading="deleting"
      @confirm="remove"
    />

    <LigojConfirmDialog
      v-model="showGuardDialog"
      :title="t('common.unsavedTitle')"
      :message="t('common.unsavedMsg')"
      :confirm-label="t('common.discard')"
      confirm-color="warning"
      @confirm="confirmLeave"
      @cancel="cancelLeave"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useApi, useFormGuard, useAppStore, useI18nStore, LigojConfirmDialog } from '@ligoj/host'

const route = useRoute()
const router = useRouter()
const api = useApi()
const appStore = useAppStore()
const i18n = useI18nStore()
const t = i18n.t

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const confirmDelete = ref(false)

const isEdit = computed(() => !!route.params.id)

// Static icon map keyed by the raw enum value. Const at module scope so
// it is never re-created reactively — referenced from both the
// prepend-inner-icon computed (selected value) and the #item slot
// (dropdown rows).
const TYPE_ICONS = {
  USER: 'mdi-account',
  GROUP: 'mdi-account-group',
  COMPANY: 'mdi-domain',
  TREE: 'mdi-file-tree',
}

// Items as plain objects with the raw enum value + an i18n key. v-model
// still holds the value, item-value="value" wires it back. The earlier
// attempt at adding icons via the #selection slot (with item.raw.icon)
// triggered "Maximum recursive updates exceeded" inside v-select — this
// version keeps only the #item slot and renders the selected icon via
// prepend-inner-icon, which sidesteps that loop.
const receiverTypes = [
  { value: 'USER', titleKey: 'delegate.type.user' },
  { value: 'GROUP', titleKey: 'delegate.type.group' },
  { value: 'COMPANY', titleKey: 'delegate.type.company' },
]
const resourceTypes = [
  { value: 'USER', titleKey: 'delegate.type.user' },
  { value: 'GROUP', titleKey: 'delegate.type.group' },
  { value: 'COMPANY', titleKey: 'delegate.type.company' },
  { value: 'TREE', titleKey: 'delegate.type.tree' },
]

/** v-select item-title callback: resolves the i18n key from the item object. */
function typeTitle(item) {
  return t(item.titleKey)
}

const form = ref({
  receiver: '',
  receiverType: 'USER',
  name: '',
  type: 'GROUP',
  canAdmin: false,
  canWrite: false,
})

// Icon shown inside the field, driven by the currently selected value.
const receiverIcon = computed(() => TYPE_ICONS[form.value.receiverType] || '')
const typeIcon = computed(() => TYPE_ICONS[form.value.type] || '')

/* -------------------------------------------------------------------------
 *  Autocomplete: receiver / resource
 *
 *  The two text inputs in section "receiver" and "resource" are dynamic
 *  v-autocompletes that hit the matching identity endpoint:
 *    receiverType=USER     → rest/service/id/user
 *    receiverType=GROUP    → rest/service/id/group
 *    receiverType=COMPANY  → rest/service/id/company
 *    type=USER/GROUP/COMPANY → same as above
 *    type=TREE             → groups (TREE delegates scope on a group)
 *
 *  Server-side filtering only (the autocomplete passes `?q=`), so we
 *  set `no-filter` on the components to disable Vuetify's local filter.
 *  ------------------------------------------------------------------- */

const TYPE_TO_ENDPOINT = {
  USER:    'service/id/user',
  GROUP:   'service/id/group',
  COMPANY: 'service/id/company',
  TREE:    'service/id/group',
}

/** Normalize a backend row to `{ id, label }` regardless of the entity
 *  kind. Users get `id — First Last` so the dropdown is scannable;
 *  groups/companies are identified by name only. */
function normalizeEntity(row, kind) {
  if (!row) return null
  if (kind === 'USER') {
    const full = [row.firstName, row.lastName].filter(Boolean).join(' ')
    return { id: row.id, label: full ? `${row.id} — ${full}` : row.id }
  }
  return { id: row.name, label: row.name }
}

/** Fetch the first page (rows=20) for the kind. An empty query is
 *  allowed so the dropdown can be populated before the user types —
 *  see `loadReceiverItems` / `loadResourceItems`. */
async function fetchEntities(kind, query) {
  const endpoint = TYPE_TO_ENDPOINT[kind]
  if (!endpoint) return []
  const q = (query || '').trim()
  const qp = q ? `q=${encodeURIComponent(q)}&` : ''
  const data = await api.get(`rest/${endpoint}?${qp}rows=20`)
  const rows = Array.isArray(data) ? data : (data?.data || [])
  return rows.map((r) => normalizeEntity(r, kind)).filter(Boolean)
}

const receiverItems = ref([])
const receiverSearch = ref('')
const receiverLoading = ref(false)
let receiverTimer = null

const resourceItems = ref([])
const resourceSearch = ref('')
const resourceLoading = ref(false)
let resourceTimer = null

/** Keep the currently-selected value visible in the dropdown even
 *  before the user has typed anything (e.g. on edit-mode initial
 *  load): pre-pend a synthetic item with `id = current value`. */
const receiverDisplayItems = computed(() => {
  const cur = form.value.receiver
  const items = receiverItems.value
  if (cur && !items.find((i) => i.id === cur)) {
    return [{ id: cur, label: cur }, ...items]
  }
  return items
})
const resourceDisplayItems = computed(() => {
  const cur = form.value.name
  const items = resourceItems.value
  if (cur && !items.find((i) => i.id === cur)) {
    return [{ id: cur, label: cur }, ...items]
  }
  return items
})

async function loadReceiverItems() {
  receiverLoading.value = true
  try { receiverItems.value = await fetchEntities(form.value.receiverType, receiverSearch.value) }
  finally { receiverLoading.value = false }
}
async function loadResourceItems() {
  resourceLoading.value = true
  try { resourceItems.value = await fetchEntities(form.value.type, resourceSearch.value) }
  finally { resourceLoading.value = false }
}

function onReceiverSearch(q) {
  // Vuetify mirrors the picked item's title into the search input;
  // that fires `update:search` with a label that matches an existing
  // item. Skip the round-trip in that case — otherwise we'd refetch
  // with the label and lose the row the user just picked from.
  if (receiverDisplayItems.value.some((i) => i.label === q)) return
  clearTimeout(receiverTimer)
  receiverTimer = setTimeout(loadReceiverItems, 250)
}

function onResourceSearch(q) {
  if (resourceDisplayItems.value.some((i) => i.label === q)) return
  clearTimeout(resourceTimer)
  resourceTimer = setTimeout(loadResourceItems, 250)
}

// Selecting a new type invalidates the corresponding identifier and
// drops the cached items. We deliberately don't refetch here — the
// next dropdown-open (or first keystroke) will lazy-load the first
// page for the new kind. This keeps the form quiet when the user
// doesn't actually interact with these selects.
watch(() => form.value.receiverType, () => {
  form.value.receiver = ''
  receiverSearch.value = ''
  receiverItems.value = []
})
watch(() => form.value.type, () => {
  form.value.name = ''
  resourceSearch.value = ''
  resourceItems.value = []
})

/** Fires when v-autocomplete opens/closes its dropdown. We fetch the
 *  first page only when the menu opens AND no items have been
 *  fetched for the current kind yet — so a user who never opens the
 *  dropdown (or types) doesn't trigger any network call. */
function onReceiverMenu(open) {
  if (open && receiverItems.value.length === 0) loadReceiverItems()
}
function onResourceMenu(open) {
  if (open && resourceItems.value.length === 0) loadResourceItems()
}

const { showGuardDialog, confirmLeave, cancelLeave, markClean, init: initGuard } = useFormGuard(form)

const rules = {
  required: v => !!v || t('common.required'),
}

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    const data = await api.get(`rest/security/delegate/${route.params.id}`)
    if (data) {
      form.value.receiver = data.receiver?.id || data.receiver || ''
      // Normalize to the uppercase enum form used by the v-select items.
      // The backend stores some delegates with lowercase values ("company",
      // "tree", …) and v-model would otherwise mismatch every item, locking
      // the select in a "Maximum recursive updates exceeded" loop.
      form.value.receiverType = (data.receiverType || 'USER').toUpperCase()
      form.value.name = data.name || ''
      form.value.type = (data.type || 'GROUP').toUpperCase()
      form.value.canAdmin = !!data.canAdmin
      form.value.canWrite = !!data.canWrite
    }
    loading.value = false
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('delegate.title'), to: '/id/delegate' },
      { title: form.value.receiver || t('delegate.edit') },
    ])
  } else {
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('delegate.title'), to: '/id/delegate' },
      { title: t('delegate.new') },
    ])
  }
  initGuard()
})

async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  saving.value = true
  const payload = {
    receiver: form.value.receiver,
    receiverType: form.value.receiverType,
    name: form.value.name,
    type: form.value.type,
    canAdmin: form.value.canAdmin,
    canWrite: form.value.canWrite,
  }

  if (isEdit.value) {
    await api.put('rest/security/delegate', { id: Number(route.params.id), ...payload })
  } else {
    await api.post('rest/security/delegate', payload)
  }
  saving.value = false
  markClean()
  router.push('/id/delegate')
}

async function remove() {
  deleting.value = true
  await api.del(`rest/security/delegate/${route.params.id}`)
  deleting.value = false
  confirmDelete.value = false
  markClean()
  router.push('/id/delegate')
}
</script>

<style scoped>
.edit-card {
  max-width: 700px;
}

@media (max-width: 600px) {
  .edit-card {
    max-width: 100%;
  }
}
</style>
