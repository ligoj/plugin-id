<template>
  <div>
    <v-alert v-if="demoMode" type="info" variant="tonal" density="compact" class="mb-4">
      {{ t('group.demoEdit') }}
    </v-alert>

    <v-skeleton-loader v-if="loading" type="card, actions" max-width="700" class="mb-4" />

    <v-card v-if="!loading" class="edit-card">
      <v-card-text>
        <v-form ref="formRef" @submit.prevent="save">
          <v-text-field v-model="form.name" :label="t('common.name')" :rules="[rules.required]" :disabled="isEdit" variant="outlined" class="mb-2" />
          <v-autocomplete v-model="form.scope" :label="t('group.scope')" :items="availableScopes" :loading="scopesLoading" clearable variant="outlined" class="mb-2" />
          <!-- Parent group: lazy server-backed autosuggest. No groups are
               loaded until the dropdown opens (chantier H, Fabrice review) —
               the former mount-time bulk GET doesn't scale to 100k+ groups. -->
          <v-autocomplete
            v-model="form.parent"
            :items="parentResults"
            :loading="parentLoading"
            :search="parentSearchQuery"
            item-title="name"
            item-value="name"
            :label="t('group.parent')"
            :hint="t('group.parentHint')"
            persistent-hint
            variant="outlined"
            class="mb-2"
            no-filter
            clearable
            @update:menu="onParentMenu"
            @update:search="onParentSearch"
          >
            <template #item="{ props: itemProps, item }">
              <v-list-item v-bind="itemProps" :title="item?.name || ''" />
            </template>
          </v-autocomplete>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-btn v-if="isEdit" color="error" variant="tonal" @click="confirmDelete = true">
          <v-icon start>mdi-delete</v-icon> {{ t('common.delete') }}
        </v-btn>
        <v-spacer />
        <v-btn variant="text" @click="router.push('/id/group')">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" variant="elevated" :loading="saving" @click="save">
          <v-icon start>mdi-content-save</v-icon> {{ t('common.save') }}
        </v-btn>
      </v-card-actions>
    </v-card>

    <v-dialog v-model="confirmDelete" max-width="400">
      <v-card>
        <v-card-title>{{ t('group.deleteTitle') }}</v-card-title>
        <v-card-text>
          {{ t('group.deleteConfirmBefore') }}<strong class="text-error">{{ form.name }}</strong>{{ t('group.deleteConfirmAfter') }}
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="confirmDelete = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" variant="elevated" :loading="deleting" @click="remove">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="showGuardDialog" max-width="400">
      <v-card>
        <v-card-title>{{ t('common.unsavedTitle') }}</v-card-title>
        <v-card-text>{{ t('common.unsavedMsg') }}</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="cancelLeave">{{ t('common.cancel') }}</v-btn>
          <v-btn color="warning" variant="elevated" @click="confirmLeave">{{ t('common.discard') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useApi, useFormGuard, useAppStore, useErrorStore, useI18nStore } from '@ligoj/host'

const route = useRoute()
const router = useRouter()
const api = useApi()
const appStore = useAppStore()
const errorStore = useErrorStore()
const i18n = useI18nStore()
const t = i18n.t

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const confirmDelete = ref(false)
const demoMode = ref(false)
const availableScopes = ref([])
// Parent group autosuggest — server-backed, loaded lazily on dropdown open.
const parentResults = ref([])
const parentLoading = ref(false)
const parentSearchQuery = ref('')
const parentLoaded = ref(false)
let parentDebounce = null
// Full scope objects ({id, name, ...}) — used at save() to resolve the
// Integer ID expected by the backend from the name held in form.value.scope.
const scopeAll = ref([])
const scopesLoading = ref(false)

const isEdit = computed(() => route.params.id && route.params.id !== 'new')

const form = ref({
  name: '',
  scope: '',
  parent: '',
})

const { showGuardDialog, confirmLeave, cancelLeave, markClean, init: initGuard } = useFormGuard(form)

const rules = {
  required: v => !!v || t('common.required'),
}

const DEMO_GROUPS = [
  { name: 'Engineering', scope: 'Group' },
  { name: 'Marketing', scope: 'Group' },
  { name: 'DevOps', scope: 'Group' },
  { name: 'Management', scope: 'Group' },
  { name: 'Sales', scope: 'Group' },
]

/**
 * Pull the list of group container scopes (matches what
 * /id/container-scope shows under its "Groups" tab) so the Scope field
 * surfaces an autocomplete instead of a free-text input. The endpoint
 * may return either a bare array or a paginated `{data: [...]}` envelope
 * depending on the identity provider — handle both shapes, fall back to
 * a static list when the IDP isn't reachable.
 */
async function loadGroupScopes() {
  scopesLoading.value = true
  try {
    const data = await api.get('rest/service/id/container-scope/group')
    const rows = Array.isArray(data) ? data : (Array.isArray(data?.data) ? data.data : null)
    if (rows) {
      scopeAll.value = rows
      availableScopes.value = rows.map((s) => s.name).filter(Boolean)
    } else {
      scopeAll.value = []
      availableScopes.value = ['Group', 'Department', 'Team', 'Project']
    }
  } catch {
    scopeAll.value = []
    availableScopes.value = ['Group', 'Department', 'Team', 'Project']
  } finally {
    scopesLoading.value = false
  }
}

// --- Parent group autosuggest (lazy, server-backed) ---

/** First-batch load when the Parent dropdown opens. Nothing is fetched
 *  before this — the former mount-time bulk GET of every group does not
 *  scale (100k+ groups at DGFIP). */
function onParentMenu(open) {
  if (open && !parentLoaded.value) loadParentGroups('')
}

/** Debounced (300 ms) search as the user types in the Parent field. */
function onParentSearch(query) {
  parentSearchQuery.value = query || ''
  clearTimeout(parentDebounce)
  parentDebounce = setTimeout(() => loadParentGroups(query), 300)
}

/** Fetch one page of groups (20) matching `query`; an empty query returns
 *  the first page. Mirrors the Company/Group autosuggests of UserEditView. */
async function loadParentGroups(query) {
  parentLoaded.value = true
  parentLoading.value = true
  try {
    // Direct URL with un-encoded brackets — the legacy DataTables backend
    // expects `search[value]=...` literally.
    const url = `rest/service/id/group?search[value]=${encodeURIComponent(query || '')}&rows=20&page=1&sidx=name&sord=asc`
    const resp = await api.get(url)
    let rows = Array.isArray(resp) ? resp : (Array.isArray(resp?.data) ? resp.data : [])
    // Dev-only fallback, gated behind import.meta.env.DEV so demo data
    // never leaks to production.
    if (import.meta.env.DEV && rows.length === 0) {
      const q = (query || '').toLowerCase()
      rows = DEMO_GROUPS.filter(g => g.name.toLowerCase().includes(q))
    }
    // Always keep the currently selected parent in the list so the
    // autocomplete can render its label, even when it is off this page.
    if (form.value.parent && !rows.some(g => (g.name || g) === form.value.parent)) {
      rows = [{ name: form.value.parent }, ...rows]
    }
    parentResults.value = rows
  } catch (err) {
    console.error('Parent group search failed:', err)
    parentResults.value = form.value.parent ? [{ name: form.value.parent }] : []
  } finally {
    parentLoading.value = false
  }
}

onMounted(async () => {
  loadGroupScopes()

  if (isEdit.value) {
    loading.value = true
    const data = await api.get(`rest/service/id/group/${route.params.id}`)
    if (data && !data.code) {
      form.value.name = data.name || ''
      form.value.scope = data.scope || ''
      form.value.parent = data.parent || ''
      // Pre-seed only the current parent so the field renders its label
      // without loading the whole group list (chantier H).
      if (form.value.parent) parentResults.value = [{ name: form.value.parent }]
    } else {
      demoMode.value = true
      errorStore.clear()
      const demo = DEMO_GROUPS.find(g => g.name === route.params.id)
      if (demo) {
        form.value.name = demo.name
        form.value.scope = demo.scope
        form.value.parent = ''
      }
    }
    loading.value = false
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('group.title'), to: '/id/group' },
      { title: form.value.name || t('group.edit') },
    ])
  } else {
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('group.title'), to: '/id/group' },
      { title: t('group.new') },
    ])
    // Check if API is available. Use a list probe (rows=1) so the check doesn't
    // depend on a specific hardcoded entity that may or may not exist in the
    // real LDAP backend. The list endpoint returns 200 with an empty array when
    // nothing matches, and `code` is only set on real API errors.
    const check = await api.get('rest/service/id/group?rows=1&page=1')
    if (!check || check.code) {
      demoMode.value = true
      errorStore.clear()
    }
  }
  initGuard()
})

async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  if (demoMode.value) {
    errorStore.push({ message: t('group.demoSave'), status: 0 })
    return
  }

  // Resolve the scope ID from its name. The backend expects an Integer
  // ID (container scope), not the string name. scopeAll is preloaded at
  // mount() from rest/service/id/container-scope/group.
  const scopeEntry = scopeAll.value.find(s => s.name === form.value.scope)
  if (!scopeEntry) {
    errorStore.push({ message: `Unknown scope: ${form.value.scope}`, status: 0 })
    return
  }

  saving.value = true
  const payload = { name: form.value.name, scope: scopeEntry.id, parent: form.value.parent || null }

  if (isEdit.value) {
    await api.put('rest/service/id/group', payload)
  } else {
    await api.post('rest/service/id/group', payload)
  }
  saving.value = false
  markClean()
  router.push('/id/group')
}

async function remove() {
  if (demoMode.value) {
    errorStore.push({ message: t('group.demoDelete'), status: 0 })
    confirmDelete.value = false
    return
  }
  deleting.value = true
  await api.del(`rest/service/id/group/${route.params.id}`)
  deleting.value = false
  confirmDelete.value = false
  markClean()
  router.push('/id/group')
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
