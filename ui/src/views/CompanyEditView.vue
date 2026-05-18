<template>
  <div>
    <v-alert v-if="demoMode" type="info" variant="tonal" density="compact" class="mb-4">
      {{ t('company.demoEdit') }}
    </v-alert>

    <v-skeleton-loader v-if="loading" type="card, actions" max-width="700" class="mb-4" />

    <v-card v-if="!loading" class="edit-card">
      <v-card-text>
        <v-form ref="formRef" @submit.prevent="save">
          <v-text-field v-model="form.name" :label="t('common.name')" :rules="[rules.required]" :disabled="isEdit" variant="outlined" class="mb-2" />
          <!-- Auto-suggest for scope. The full list of valid scopes for
               companies is fetched once on mount from container-scope/COMPANY
               (small dataset, ~5-10 entries). Filtering is local on every
               keystroke, debounced 300 ms for consistency with the other
               autosuggests in plugin-id. v-model holds the scope **name**
               as a string, matching the existing payload contract. -->
          <v-autocomplete
            v-model="form.scope"
            :items="scopeResults"
            :loading="scopeLoading"
            :search="scopeSearchQuery"
            item-title="name"
            item-value="name"
            :label="t('group.scope')"
            placeholder="Sélectionner un scope…"
            variant="outlined"
            class="mb-2"
            no-filter
            clearable
            @update:search="onScopeSearch"
          >
            <template #item="{ props: itemProps, item }">
              <v-list-item v-bind="itemProps" :title="item?.name || ''" />
            </template>
            <template #no-data>
              <v-list-item>
                <v-list-item-title>
                  {{ scopeSearchQuery ? 'Aucun scope trouvé' : 'Saisissez des caractères pour rechercher' }}
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
        <v-btn variant="text" @click="router.push('/id/company')">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" variant="elevated" :loading="saving" @click="save">
          <v-icon start>mdi-content-save</v-icon> {{ t('common.save') }}
        </v-btn>
      </v-card-actions>
    </v-card>

    <v-dialog v-model="confirmDelete" max-width="400">
      <v-card>
        <v-card-title>{{ t('company.deleteTitle') }}</v-card-title>
        <v-card-text>
          {{ t('company.deleteConfirm', { name: form.name }) }}
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
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

// --- Scope auto-suggest state ---
const scopeSearchQuery = ref('')
const scopeResults = ref([])
const scopeAll = ref([])
const scopeLoading = ref(false)
let scopeDebounce = null

const isEdit = computed(() => route.params.id && route.params.id !== 'new')

const form = ref({
  name: '',
  scope: '',
})

const { showGuardDialog, confirmLeave, cancelLeave, markClean, init: initGuard } = useFormGuard(form)

const rules = {
  required: v => !!v || t('common.required'),
}

const DEMO_COMPANIES = [
  { name: 'Ligoj', scope: 'Company' },
  { name: 'AcmeCorp', scope: 'Company' },
  { name: 'TechSolutions', scope: 'Company' },
]

// --- Scope auto-suggest logic ---

/** Preload all available scopes once. The container-scope endpoint
 *  doesn't accept a query param — we fetch the full list (small dataset)
 *  and filter locally on every keystroke. Called from onMounted. */
async function loadAllScopes() {
  scopeLoading.value = true
  try {
    const resp = await api.get('rest/service/id/container-scope/COMPANY')
    scopeAll.value = Array.isArray(resp) ? resp : (Array.isArray(resp?.data) ? resp.data : [])
    scopeResults.value = scopeAll.value
  } catch (err) {
    console.error('Scope preload failed:', err)
    // Pre-seed with the current value (if any) so the input still
    // renders its label correctly when editing offline. The DEV
    // fallback below kicks in on the next user keystroke.
    if (form.value.scope) {
      scopeAll.value = [{ name: form.value.scope }]
      scopeResults.value = scopeAll.value
    } else {
      scopeAll.value = []
      scopeResults.value = []
    }
  } finally {
    scopeLoading.value = false
  }
}

/** Called on every keystroke. Debounced 300 ms — matches the
 *  Company/Group autosuggest pattern even though the filter is local. */
function onScopeSearch(query) {
  scopeSearchQuery.value = query || ''
  clearTimeout(scopeDebounce)
  scopeDebounce = setTimeout(() => filterScopes(query), 300)
}

function filterScopes(query) {
  if (!query) {
    scopeResults.value = scopeAll.value
    return
  }
  const q = query.toLowerCase()
  scopeResults.value = scopeAll.value.filter(s => (s.name || '').toLowerCase().includes(q))
  // Dev-only fallback: gated behind import.meta.env.DEV so demo
  // data NEVER leaks to production (per Fabrice's review pattern on
  // PR #20). When the container-scope endpoint isn't available in
  // dev, surface a small demo list so the autosuggest can be
  // visually validated.
  if (import.meta.env.DEV && scopeResults.value.length === 0 && query) {
    const DEMO = [
      { name: 'Functional' },
      { name: 'Project' },
      { name: 'Enterprise' },
    ]
    scopeResults.value = DEMO.filter(s => s.name.toLowerCase().includes(q))
  }
}

onBeforeUnmount(() => clearTimeout(scopeDebounce))

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    const data = await api.get(`rest/service/id/company/${route.params.id}`)
    if (data && !data.code) {
      form.value.name = data.name || ''
      form.value.scope = data.scope || ''
    } else {
      demoMode.value = true
      errorStore.clear()
      const demo = DEMO_COMPANIES.find(c => c.name === route.params.id)
      if (demo) {
        form.value.name = demo.name
        form.value.scope = demo.scope
      }
    }
    // Preload the scope list so the dropdown is ready on first open
    // and renders the existing scope as a proper label (not raw text).
    await loadAllScopes()
    loading.value = false
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('company.title'), to: '/id/company' },
      { title: form.value.name || t('company.edit') },
    ])
  } else {
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('company.title'), to: '/id/company' },
      { title: t('company.new') },
    ])
    const check = await api.get('rest/service/id/company/Ligoj')
    if (!check || check.code) {
      demoMode.value = true
      errorStore.clear()
    }
    await loadAllScopes()
  }
  initGuard()
})

async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  if (demoMode.value) {
    errorStore.push({ message: t('company.demoSave'), status: 0 })
    return
  }

  saving.value = true
  const payload = { name: form.value.name, scope: form.value.scope }

  if (isEdit.value) {
    await api.put('rest/service/id/company', payload)
  } else {
    await api.post('rest/service/id/company', payload)
  }
  saving.value = false
  markClean()
  router.push('/id/company')
}

async function remove() {
  if (demoMode.value) {
    errorStore.push({ message: t('company.demoDelete'), status: 0 })
    confirmDelete.value = false
    return
  }
  deleting.value = true
  await api.del(`rest/service/id/company/${route.params.id}`)
  deleting.value = false
  confirmDelete.value = false
  markClean()
  router.push('/id/company')
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
