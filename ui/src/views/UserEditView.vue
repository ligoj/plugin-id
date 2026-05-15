<template>
  <div>
    <v-alert v-if="demoMode" type="info" variant="tonal" density="compact" class="mb-4">
      {{ t('user.demoEdit') }}
    </v-alert>

    <v-skeleton-loader v-if="loading" type="card, actions" max-width="700" class="mb-4" />

    <v-card v-if="!loading" class="edit-card">
      <v-card-text>
        <v-form ref="formRef" @submit.prevent="save">
          <v-text-field v-model="form.id" :label="t('user.login')" :rules="[rules.required]" :disabled="isEdit" :hint="isEdit ? '' : t('user.loginHint')" persistent-hint variant="outlined"
            class="mb-2" />
          <v-text-field v-model="form.firstName" :label="t('user.firstName')" :rules="[rules.required]" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.lastName" :label="t('user.lastName')" :rules="[rules.required]" variant="outlined" class="mb-2" />
          <!-- Auto-suggest for company. Queries rest/service/id/company as the
               user types (300 ms debounced). v-model stores the company name
               as a string, matching the payload contract of rest/service/id/user. -->
          <v-autocomplete
            v-model="form.company"
            :items="companyResults"
            :loading="companyLoading"
            :search="companySearchQuery"
            item-title="name"
            item-value="name"
            :label="t('user.company')"
            placeholder="Rechercher une entité…"
            variant="outlined"
            class="mb-2"
            no-filter
            clearable
            @update:search="onCompanySearch"
          >
            <template #item="{ props: itemProps, item }">
              <v-list-item v-bind="itemProps" :title="item?.name || ''">
                <template v-if="item?.scope || item?.count !== undefined" #subtitle>
                  <span v-if="item?.scope" class="text-caption mr-2">{{ item.scope }}</span>
                  <v-chip
                    v-if="item?.count !== undefined"
                    size="x-small"
                    variant="tonal"
                    class="mr-1"
                  >{{ item.count }} {{ t('user.title').toLowerCase() }}</v-chip>
                </template>
              </v-list-item>
            </template>
            <template #no-data>
              <v-list-item>
                <v-list-item-title>
                  {{ companySearchQuery ? 'Aucune entité trouvée' : 'Saisissez des caractères pour rechercher' }}
                </v-list-item-title>
              </v-list-item>
            </template>
          </v-autocomplete>
          <v-text-field v-model="form.mail" :label="t('user.email')" type="email" variant="outlined" class="mb-2" />
          <!-- Auto-suggest for groups (multi-select). Queries
               rest/service/id/group as the user types (300 ms debounced).
               v-model holds an array of group **names** (strings),
               matching the payload contract of rest/service/id/user. -->
          <v-autocomplete
            v-if="isEdit"
            v-model="groups"
            :items="groupResults"
            :loading="groupLoading"
            :search="groupSearchQuery"
            item-title="name"
            item-value="name"
            :label="t('user.groups')"
            placeholder="Ajouter un groupe…"
            variant="outlined"
            class="mb-2"
            multiple
            chips
            closable-chips
            no-filter
            clearable
            @update:search="onGroupSearch"
            @update:model-value="onGroupModelUpdate"
          >
            <template #item="{ props: itemProps, item }">
              <v-list-item v-bind="itemProps" :title="item?.name || ''" />
            </template>
            <template #no-data>
              <v-list-item>
                <v-list-item-title>
                  {{ groupSearchQuery ? 'Aucun groupe trouvé' : 'Saisissez des caractères pour rechercher' }}
                </v-list-item-title>
              </v-list-item>
            </template>
          </v-autocomplete>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-btn v-if="isEdit" color="error" variant="tonal" @click="confirmDelete = true">
          <v-icon start>mdi-delete</v-icon> {{ t('common.delete') }}
        </v-btn>
        <v-spacer />
        <v-btn variant="text" @click="router.push('/id/user')">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" variant="elevated" :loading="saving" @click="save">
          <v-icon start>mdi-content-save</v-icon> {{ t('common.save') }}
        </v-btn>
      </v-card-actions>
    </v-card>

    <v-card v-if="isEdit && !loading" class="edit-card mt-4">
      <v-card-title class="text-h6">{{ t('user.actions') }}</v-card-title>
      <v-card-text>
        <div class="d-flex flex-wrap ga-2">
          <v-btn v-if="!locked" color="warning" variant="tonal" prepend-icon="mdi-lock" @click="startAction('lock')">{{ t('user.lock') }}</v-btn>
          <v-btn v-if="locked" color="success" variant="tonal" prepend-icon="mdi-lock-open-variant" @click="startAction('unlock')">{{ t('user.unlock') }}</v-btn>
          <v-btn v-if="!isolated" color="error" variant="tonal" prepend-icon="mdi-account-off" @click="startAction('isolate')">{{ t('user.isolate') }}</v-btn>
          <v-btn v-if="isolated" color="success" variant="tonal" prepend-icon="mdi-account-check" @click="startAction('restore')">{{ t('user.restore') }}</v-btn>
          <v-btn color="info" variant="tonal" prepend-icon="mdi-lock-reset" @click="startAction('resetPassword')">{{ t('user.resetPassword') }}</v-btn>
        </div>
      </v-card-text>
    </v-card>

    <v-dialog v-model="confirmDelete" max-width="400">
      <v-card>
        <v-card-title>{{ t('user.deleteTitle') }}</v-card-title>
        <v-card-text>
          {{ t('user.deleteConfirm', { id: form.id }) }}
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

    <v-dialog v-model="actionDialog" max-width="400">
      <v-card>
        <v-card-title>{{ t('user.' + actionType) }}</v-card-title>
        <v-card-text>{{ t('user.' + actionType + 'Confirm', { id: form.id }) }}</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="actionDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" variant="elevated" :loading="actionLoading" @click="confirmAction">{{ t('common.confirm') }}</v-btn>
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
const groups = ref([])
const locked = ref(false)
const isolated = ref(false)
const actionDialog = ref(false)
const actionType = ref('')
const actionLoading = ref(false)

// --- Company auto-suggest state ---
const companySearchQuery = ref('')
const companyResults = ref([])
const companyLoading = ref(false)
let companyDebounce = null

// --- Group auto-suggest state (multi-select) ---
const groupSearchQuery = ref('')
const groupResults = ref([])
const groupLoading = ref(false)
let groupDebounce = null

const isEdit = computed(() => !!route.params.id)

const form = ref({
  id: '',
  firstName: '',
  lastName: '',
  company: '',
  mail: '',
})

const { showGuardDialog, confirmLeave, cancelLeave, markClean, init: initGuard } = useFormGuard(form)

const rules = {
  required: v => !!v || t('common.required'),
}

// --- Company auto-suggest logic ---

/** Called on every keystroke in the autocomplete. Debounced 300 ms. */
function onCompanySearch(query) {
  companySearchQuery.value = query || ''
  clearTimeout(companyDebounce)
  companyDebounce = setTimeout(() => searchCompanies(query), 300)
}

async function searchCompanies(query) {
  if (!query || query.length < 1) {
    companyResults.value = []
    return
  }
  companyLoading.value = true
  try {
    // Direct URL with un-encoded brackets — the legacy DataTables backend
    // expects `search[value]=...` literally.
    const url = `rest/service/id/company?search[value]=${encodeURIComponent(query)}&rows=20&page=1&sidx=name&sord=asc`
    const resp = await api.get(url)
    // Defensive: api.get may return the wrapper { data: [...] } or the
    // array directly depending on the endpoint's content-type handling.
    companyResults.value = Array.isArray(resp) ? resp : (Array.isArray(resp?.data) ? resp.data : [])
    // Fallback : if no IAM provider is configured (Demo mode), the
    // backend returns no companies. Surface a small demo list so the
    // pattern can be visually validated. The full backend integration
    // will work once a real LDAP node is configured in Admin → Nodes.
    if (companyResults.value.length === 0 && query) {
      const DEMO = [
        { name: 'Ligoj', scope: 'Company', count: 4 },
        { name: 'AcmeCorp', scope: 'Company', count: 2 },
        { name: 'TechSolutions', scope: 'Company', count: 2 },
      ]
      const q = query.toLowerCase()
      companyResults.value = DEMO.filter(c => c.name.toLowerCase().includes(q))
    }
  } catch (err) {
    console.error('Company search failed:', err)
    companyResults.value = []
  } finally {
    companyLoading.value = false
  }
}

/** When editing an existing user, the company is already set but the
 *  autocomplete's item list is empty — pre-seed with the current value
 *  so v-autocomplete can render its label correctly on open. */
async function ensureCurrentCompanyInResults(name) {
  if (!name) return
  try {
    const url = `rest/service/id/company?search[value]=${encodeURIComponent(name)}&rows=5&page=1&sidx=name&sord=asc`
    const resp = await api.get(url)
    const items = Array.isArray(resp) ? resp : (Array.isArray(resp?.data) ? resp.data : [])
    companyResults.value = items.length ? items : [{ name }]
  } catch {
    companyResults.value = [{ name }]
  }
}

// --- Group auto-suggest logic (multi-select) ---

/** Called on every keystroke in the group autocomplete. Debounced 300 ms. */
function onGroupSearch(query) {
  groupSearchQuery.value = query || ''
  clearTimeout(groupDebounce)
  groupDebounce = setTimeout(() => searchGroups(query), 300)
}

/** After picking or removing a chip, reset the search field so the user
 *  can immediately type the next group name. Vuetify v4 doesn't clear
 *  the inline query automatically in multi-select mode. */
function onGroupModelUpdate() {
  groupSearchQuery.value = ''
  groupResults.value = []
}

async function searchGroups(query) {
  if (!query || query.length < 1) {
    groupResults.value = []
    return
  }
  groupLoading.value = true
  try {
    const url = `rest/service/id/group?search[value]=${encodeURIComponent(query)}&rows=20&page=1&sidx=name&sord=asc`
    const resp = await api.get(url)
    groupResults.value = Array.isArray(resp) ? resp : (Array.isArray(resp?.data) ? resp.data : [])
    // Fallback (same pattern as company) — Demo mode often returns
    // an empty array; surface a small demo list so multi-select can
    // be exercised visually. Replaced by real backend data once an
    // LDAP node is configured.
    if (groupResults.value.length === 0 && query) {
      const DEMO = [
        { name: 'Engineering' },
        { name: 'Management' },
        { name: 'DevOps' },
        { name: 'Marketing' },
        { name: 'Sales' },
      ]
      const q = query.toLowerCase()
      groupResults.value = DEMO.filter(g => g.name.toLowerCase().includes(q))
    }
  } catch (err) {
    console.error('Group search failed:', err)
    groupResults.value = []
  } finally {
    groupLoading.value = false
  }
}

/** Pre-seed groupResults with the user's existing groups so Vuetify can
 *  render their chips on edit without an explicit search. Takes an array
 *  of group **names** (strings). */
function ensureCurrentGroupsInResults(names) {
  if (!Array.isArray(names) || !names.length) return
  groupResults.value = names.map(n => ({ name: n }))
}

// Demo users matching UserListView
const DEMO_USERS = [
  { id: 'admin', firstName: 'Admin', lastName: 'User', company: 'Ligoj', mails: ['admin@ligoj.org'], groups: [{ name: 'Engineering' }, { name: 'Management' }] },
  { id: 'jdupont', firstName: 'Jean', lastName: 'Dupont', company: 'Ligoj', mails: ['jean.dupont@ligoj.org'], groups: [{ name: 'Engineering' }, { name: 'DevOps' }] },
  { id: 'mmartin', firstName: 'Marie', lastName: 'Martin', company: 'AcmeCorp', mails: ['marie.martin@acme.com'], groups: [{ name: 'Marketing' }] },
  { id: 'pdurand', firstName: 'Pierre', lastName: 'Durand', company: 'AcmeCorp', mails: ['pierre.durand@acme.com'], groups: [{ name: 'Engineering' }] },
  { id: 'sleblanc', firstName: 'Sophie', lastName: 'Leblanc', company: 'TechSolutions', mails: ['sophie.leblanc@techsol.com'], groups: [{ name: 'DevOps' }] },
  { id: 'tmoreau', firstName: 'Thomas', lastName: 'Moreau', company: 'TechSolutions', mails: ['thomas.moreau@techsol.com'], groups: [{ name: 'Sales' }] },
  { id: 'crichard', firstName: 'Claire', lastName: 'Richard', company: 'Ligoj', mails: ['claire.richard@ligoj.org'], groups: [{ name: 'Management' }] },
  { id: 'agarcia', firstName: 'Antoine', lastName: 'Garcia', company: 'Ligoj', mails: ['antoine.garcia@ligoj.org'], groups: [{ name: 'Engineering' }] },
]

function loadDemoUser(id) {
  const user = DEMO_USERS.find(u => u.id === id)
  if (user) {
    form.value.id = user.id
    form.value.firstName = user.firstName
    form.value.lastName = user.lastName
    form.value.company = user.company
    form.value.mail = user.mails?.[0] || ''
    // Normalize groups to an array of names (strings) so v-autocomplete
    // with item-value="name" can roundtrip them through v-model.
    groups.value = (user.groups || []).map(g => g.name || g)
    ensureCurrentGroupsInResults(groups.value)
    locked.value = !!user.locked
    isolated.value = !!user.isolated
  }
}

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    const data = await api.get(`rest/service/id/user/${route.params.id}`)
    if (data && !data.code) {
      form.value.id = data.id || ''
      form.value.firstName = data.firstName || ''
      form.value.lastName = data.lastName || ''
      form.value.company = data.company || ''
      form.value.mail = data.mails?.[0] || ''
      // Normalize groups to an array of names (strings) so v-autocomplete
      // with item-value="name" can roundtrip them through v-model.
      groups.value = (data.groups || []).map(g => g.name || g)
      locked.value = !!data.locked
      isolated.value = !!data.isolated
      // Pre-seed the company suggest with the current value so the input
      // displays it correctly without an explicit search.
      await ensureCurrentCompanyInResults(form.value.company)
      // Pre-seed groupResults with stubs so v-autocomplete renders the
      // existing chips immediately (no API roundtrip needed).
      ensureCurrentGroupsInResults(groups.value)
    } else {
      // API unavailable — use demo data
      demoMode.value = true
      errorStore.clear()
      loadDemoUser(route.params.id)
      // Pre-seed in demo mode too, with a stub object.
      if (form.value.company) {
        companyResults.value = [{ name: form.value.company }]
      }
    }
    loading.value = false
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('user.title'), to: '/id/user' },
      { title: form.value.id || t('user.edit') },
    ])
  } else {
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('user.title'), to: '/id/user' },
      { title: t('user.new') },
    ])
    // Check if API is available
    const check = await api.get('rest/service/id/user/admin')
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
    errorStore.push({ message: t('user.demoSave'), status: 0 })
    return
  }

  saving.value = true
  const payload = {
    id: form.value.id,
    firstName: form.value.firstName,
    lastName: form.value.lastName,
    company: form.value.company,
    mail: form.value.mail,
    // groups is an array of names (strings). Defensive `.map(g => g.name || g)`
    // in case any legacy object slipped through. Only sent on edit since the
    // groups field is hidden on New User for this PR.
    ...(isEdit.value ? { groups: groups.value.map(g => g.name || g) } : {}),
  }

  if (isEdit.value) {
    await api.put('rest/service/id/user', payload)
  } else {
    await api.post('rest/service/id/user', payload)
  }
  saving.value = false
  markClean()
  router.push('/id/user')
}

async function remove() {
  if (demoMode.value) {
    errorStore.push({ message: t('user.demoDelete'), status: 0 })
    confirmDelete.value = false
    return
  }

  deleting.value = true
  await api.del(`rest/service/id/user/${route.params.id}`)
  deleting.value = false
  confirmDelete.value = false
  markClean()
  router.push('/id/user')
}

function startAction(type) {
  actionType.value = type
  actionDialog.value = true
}

async function confirmAction() {
  if (demoMode.value) {
    errorStore.push({ message: t('user.demoAction'), status: 0 })
    actionDialog.value = false
    return
  }
  actionLoading.value = true
  const id = form.value.id
  const actions = {
    lock: () => api.del(`rest/service/id/user/${id}/lock`),
    unlock: () => api.put(`rest/service/id/user/${id}/unlock`),
    isolate: () => api.del(`rest/service/id/user/${id}/isolate`),
    restore: () => api.put(`rest/service/id/user/${id}/restore`),
    resetPassword: () => api.put(`rest/service/id/user/${id}/reset`),
  }
  await actions[actionType.value]()
  actionLoading.value = false
  actionDialog.value = false
  // Update local state
  if (actionType.value === 'lock') locked.value = true
  if (actionType.value === 'unlock') locked.value = false
  if (actionType.value === 'isolate') isolated.value = true
  if (actionType.value === 'restore') isolated.value = false
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

<style>
/*
 * Safety net for the ligojLight custom theme: --v-theme-on-surface-variant
 * defaults to a near-white grey, making v-list-item titles/subtitles
 * invisible inside autocomplete dropdowns. We force a readable colour on
 * `.v-autocomplete__content` (always stamped by Vuetify on every
 * v-autocomplete overlay content). `!important` wins over @layer-scoped
 * Vuetify defaults. Non-scoped intentionally — the v-menu content is
 * teleported to <body>, so scoped CSS never reaches it.
 *
 * Note Vuetify 4: in the #item slot scope, `item` is the raw item
 * directly (not a {raw, title, value, props} wrapper as in v3). The
 * wrapper moved to `internalItem`. So access fields via `item.name`,
 * never `item.raw.name`.
 */
.v-autocomplete__content .v-list-item-title {
  color: rgb(var(--v-theme-on-surface)) !important;
}
.v-autocomplete__content .v-list-item-subtitle {
  color: rgb(var(--v-theme-on-surface)) !important;
  opacity: 0.7;
}
</style>
