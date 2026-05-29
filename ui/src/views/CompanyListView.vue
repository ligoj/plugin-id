<template>
  <div>
    <div class="d-flex flex-wrap align-center mb-4 ga-2">
      <v-spacer />
      <v-text-field v-model="dt.search.value" prepend-inner-icon="mdi-magnify" :label="t('common.search')" variant="outlined" density="compact" hide-details class="search-field"
        @update:model-value="onSearch" />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('company.new') }}
      </v-btn>
    </div>

    <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4">
      <v-alert-title>{{ t('user.noProvider') }}</v-alert-title>
      {{ dt.error.value === 'internal' ? t('company.noProvider') : dt.error.value }}
    </v-alert>

    <v-alert v-if="dt.demoMode.value" type="info" variant="tonal" density="compact" class="mb-4">
      {{ t('user.demoMode') }}
    </v-alert>

    <v-slide-y-transition>
      <v-toolbar v-if="selected.length" density="compact" color="primary" rounded class="mb-4">
        <v-toolbar-title>{{ selected.length }} {{ t('common.selected') }}</v-toolbar-title>
        <v-spacer />
        <v-btn variant="elevated" color="error" prepend-icon="mdi-delete" @click="startBulkDelete">
          {{ t('common.delete') }}
        </v-btn>
      </v-toolbar>
    </v-slide-y-transition>

    <v-skeleton-loader v-if="dt.loading.value && dt.items.value.length === 0" type="table-heading, table-row@5" class="mb-4" />

    <LigojDataTableServer filename="companies.csv" :fetch-all="dt.loadAll" v-if="!dt.error.value" v-show="dt.items.value.length > 0 || !dt.loading.value" v-model="selected"
      v-model:items-per-page="itemsPerPage" :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value" item-value="name" show-select hover
      @update:options="loadData" @click:row="(_, { item }) => openDetails(item.name)">
      <template #item.locked="{ item }">
        <div class="text-center">
          <v-tooltip v-if="item.locked" :text="t('user.statusLocked')" location="top">
            <template #activator="{ props: tt }">
              <v-icon v-bind="tt" color="error" size="small">mdi-lock</v-icon>
            </template>
          </v-tooltip>
        </div>
      </template>
      <template #item.actions="{ item }">
        <!-- View details: company properties (name / scope / lock /
             member count) are LDAP-managed and effectively immutable
             from this UI, so this is "open the details panel" — not
             "edit". The eye icon + "View" tooltip communicate that. -->
        <v-btn icon size="small" variant="text" @click.stop="openDetails(item.name)">
          <v-icon size="small">mdi-eye-outline</v-icon>
          <v-tooltip activator="parent" :text="t('common.view')" location="top" />
        </v-btn>
        <v-btn icon size="small" variant="text" color="error" @click.stop="startDelete(item)">
          <v-icon size="small">mdi-delete</v-icon>
          <v-tooltip activator="parent" :text="t('common.delete')" location="top" />
        </v-btn>
      </template>
    </LigojDataTableServer>

    <!-- Company view / create dialog. View mode renders the form with
         every field disabled (LDAP companies are immutable from this
         UI) PLUS extra read-only rows for lock status + member count
         that the form alone can't show. Create mode renders the same
         form editable, with the usual Save/Cancel pair. NOT
         `persistent` — ESC and backdrop click both close it. Panel
         is `v-if`-mounted on `editDialog` so each open is a fresh
         component (clean form state, fresh GET); `:key` covers the
         rare in-place target swap. -->
    <v-dialog v-model="editDialog" max-width="700" scrollable>
      <v-card>
        <v-card-title class="d-flex align-center ga-2">
          <v-icon color="primary">{{ editingId ? 'mdi-eye-outline' : 'mdi-domain' }}</v-icon>
          <span v-if="editingId">{{ t('company.detailsTitle') }}</span>
          <span v-else>{{ t('company.new') }}</span>
          <span v-if="editingId" class="text-primary">{{ editingId }}</span>
          <v-spacer />
          <v-btn icon size="small" variant="text" @click="editDialog = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>
        <CompanyEditPanel
          v-if="editDialog"
          :key="editingId ?? 'new'"
          :company-id="editingId"
          @saved="onEditSaved"
          @deleted="onEditDeleted"
          @cancel="editDialog = false"
        />
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card>
        <v-card-title>{{ t('company.deleteTitle') }}</v-card-title>
        <v-card-text>
          {{ t('company.deleteConfirmBefore') }}<strong class="text-error">{{ deleteTarget?.name }}</strong>{{ t('company.deleteConfirmAfter') }}
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" variant="elevated" :loading="deleting" @click="confirmDelete">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="bulkDeleteDialog" max-width="400">
      <v-card>
        <v-card-title>{{ t('common.bulkDeleteTitle') }}</v-card-title>
        <v-card-text>{{ t('common.bulkDeleteConfirm', { count: selected.length }) }}</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="bulkDeleteDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" variant="elevated" :loading="deleting" @click="confirmBulkDelete">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDataTable, useApi, useAppStore, useErrorStore, useI18nStore, LigojDataTableServer } from '@ligoj/host'
import CompanyEditPanel from '../components/CompanyEditPanel.vue'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const api = useApi()
const errorStore = useErrorStore()
const i18n = useI18nStore()
const t = i18n.t

const DEMO_COMPANIES = [
  { name: 'Ligoj', scope: 'Company', count: 4, locked: false },
  { name: 'AcmeCorp', scope: 'Company', count: 2, locked: false },
  { name: 'TechSolutions', scope: 'Company', count: 2, locked: false },
]
const dt = useDataTable('service/id/company', { defaultSort: 'name', demoData: DEMO_COMPANIES })
const itemsPerPage = ref(25)
let searchTimeout = null

const selected = ref([])
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)
const bulkDeleteDialog = ref(false)
let lastOptions = {}

/* ---- Create / view dialog ---------------------------------------
 * Replaces the legacy `router.push('/id/company/<id>')` and
 * `router.push('/id/company/new')` navigations. The routed URLs
 * still resolve here (see plugin index.js) and auto-open the dialog
 * in the appropriate mode so email / bookmark deep links keep
 * working. The Panel itself decides between view (disabled fields,
 * Close+Delete) and create (editable, Save+Cancel) based on its
 * `companyId` prop.
 * ----------------------------------------------------------------- */

const editDialog = ref(false)
const editingId = ref(null)

function openCreate() {
  editingId.value = null
  editDialog.value = true
}

function openDetails(name) {
  editingId.value = name
  editDialog.value = true
}

function onEditSaved() {
  editDialog.value = false
  editingId.value = null
  dt.load(lastOptions)
}

function onEditDeleted() {
  editDialog.value = false
  editingId.value = null
  dt.load(lastOptions)
}

const headers = computed(() => [
  { title: t('common.name'), key: 'name', sortable: true },
  { title: t('group.scope'), key: 'scope', sortable: false },
  { title: t('group.members'), key: 'count', sortable: false, width: '100px', align: 'center' },
  { title: t('group.locked'), key: 'locked', sortable: false, width: '80px', align: 'center' },
  { title: '', key: 'actions', sortable: false, width: '120px', align: 'center' },
])

function loadData(options) {
  lastOptions = options
  dt.load(options)
}

function onSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => dt.load({ page: 1, itemsPerPage: itemsPerPage.value }), 300)
}

function startDelete(item) {
  deleteTarget.value = item
  deleteDialog.value = true
}

async function confirmDelete() {
  if (dt.demoMode.value) {
    errorStore.push({ message: t('company.demoDelete'), status: 0 })
    deleteDialog.value = false
    return
  }
  deleting.value = true
  await api.del(`rest/service/id/company/${deleteTarget.value.name}`)
  deleting.value = false
  deleteDialog.value = false
  deleteTarget.value = null
  dt.load(lastOptions)
}

function startBulkDelete() {
  bulkDeleteDialog.value = true
}

async function confirmBulkDelete() {
  if (dt.demoMode.value) {
    errorStore.push({ message: t('company.demoDelete'), status: 0 })
    bulkDeleteDialog.value = false
    return
  }
  deleting.value = true
  for (const name of selected.value) {
    await api.del(`rest/service/id/company/${name}`)
  }
  deleting.value = false
  bulkDeleteDialog.value = false
  selected.value = []
  dt.load(lastOptions)
}

onMounted(() => {
  appStore.setBreadcrumbs(
    [
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('company.title') },
    ],
    { refresh: () => dt.load(lastOptions) },
  )
  // Deep-link support. `/id/company/new` and `/id/company/<name>`
  // both resolve here (see plugin index.js) and auto-open the dialog
  // in the right mode so emails / bookmarks pointing at the legacy
  // per-entity URLs still land in the list context.
  const id = route.params?.id
  if (id === 'new' || route.path?.endsWith('/company/new')) {
    openCreate()
  } else if (id) {
    openDetails(String(id))
  }
})
</script>

<style scoped>
.search-field {
  min-width: 200px;
  max-width: 300px;
  flex: 1 1 200px;
}
</style>
