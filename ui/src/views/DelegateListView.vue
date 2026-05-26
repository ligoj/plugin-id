<template>
  <div>
    <div class="d-flex flex-wrap align-center mb-4 ga-2">
      <v-spacer />
      <v-text-field v-model="dt.search.value" prepend-inner-icon="mdi-magnify" :label="t('common.search')" variant="outlined" density="compact" hide-details class="search-field"
        @update:model-value="onSearch" />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog(null)">
        {{ t('delegate.new') }}
      </v-btn>
    </div>

    <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4">
      {{ dt.error.value }}
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

    <LigojDataTableServer filename="delegates.csv" :fetch-all="dt.loadAll" v-if="!dt.error.value" v-show="dt.items.value.length > 0 || !dt.loading.value" v-model="selected"
      v-model:items-per-page="itemsPerPage" :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value" item-value="id" show-select hover
      @update:options="loadData" @click:row="(_, { item }) => openDialog(item.id)">
      <!-- Receiver column (chantier D5): the receiver's *kind* is rendered
           as a small leading icon (Account / Group / Domain) followed by
           its identifier. The dedicated Type chip column has been removed
           entirely (chantier D6+D8).
           Note: backend stores receiverType in lowercase ("company", …)
           for some rows, so normalize before the TYPE_ICONS lookup —
           same rationale as DelegateEditDialog.loadOnOpen(). -->
      <template #item.receiver="{ item }">
        <v-icon size="small" class="me-1">{{ TYPE_ICONS[item.receiverType?.toUpperCase()] || 'mdi-account' }}</v-icon>
        {{ item.receiver?.name || item.receiver?.id || item.name || '-' }}
      </template>
      <!-- Resource column (chantier D5+D8): fuses the former Type and
           Resource columns. The kind is the leading icon (Account /
           Group / Domain / File-tree); the text is the resource name
           (or DN for TREE delegates). Same lowercase-normalize as above. -->
      <template #item.name="{ item }">
        <v-icon size="small" class="me-1">{{ TYPE_ICONS[item.type?.toUpperCase()] || '' }}</v-icon>
        {{ item.name || '-' }}
      </template>
      <template #item.canAdmin="{ item }">
        <v-icon v-if="item.canAdmin" color="success" size="small">mdi-check</v-icon>
      </template>
      <template #item.canWrite="{ item }">
        <v-icon v-if="item.canWrite" color="success" size="small">mdi-check</v-icon>
      </template>
      <template #item.actions="{ item }">
        <v-btn icon size="small" variant="text" @click.stop="openDialog(item.id)">
          <v-icon size="small">mdi-pencil</v-icon>
        </v-btn>
        <v-btn icon size="small" variant="text" color="error" @click.stop="startDelete(item)">
          <v-icon size="small">mdi-delete</v-icon>
        </v-btn>
      </template>
    </LigojDataTableServer>

    <v-alert v-if="!dt.loading.value && !dt.error.value && dt.totalItems.value === 0" type="info" variant="tonal" class="mt-4">
      {{ t('delegate.empty') }}
    </v-alert>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card>
        <v-card-title>{{ t('delegate.deleteTitle') }}</v-card-title>
        <v-card-text>
          {{ t('delegate.deleteConfirm', { name: deleteTarget?.receiver?.name || deleteTarget?.name || deleteTarget?.id }) }}
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

    <!-- Delegate create/edit popup (chantier D3). editDelegateId null = create mode. -->
    <DelegateEditDialog v-model="editDialog" :delegate-id="editDelegateId" @saved="onDelegateSaved" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDataTable, useApi, useAppStore, useI18nStore, LigojDataTableServer } from '@ligoj/host'
import DelegateEditDialog from './DelegateEditDialog.vue'
import { TYPE_ICONS } from '../composables/delegateTypes.js'

const appStore = useAppStore()
const api = useApi()
const i18n = useI18nStore()
const t = i18n.t

const dt = useDataTable('security/delegate', { defaultSort: 'receiver' })
const itemsPerPage = ref(25)
let searchTimeout = null

const selected = ref([])
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)
const bulkDeleteDialog = ref(false)
let lastOptions = {}

// Delegate create/edit dialog state. editDelegateId null = create mode.
const editDialog = ref(false)
const editDelegateId = ref(null)

// Headers revised per Fabrice's 22-may review (chantiers D5+D6+D8):
//  - Receiver gets a leading kind icon (slot below).
//  - Type and Resource are fused into a single "Resource" column whose
//    text is the resource name and whose leading icon is the type. The
//    Type chip column is gone.
const headers = computed(() => [
  { title: t('delegate.receiver'), key: 'receiver', sortable: true },
  { title: t('delegate.resource'), key: 'name', sortable: false },
  { title: t('delegate.admin'), key: 'canAdmin', sortable: false, width: '80px', tooltip: t('delegate.adminHelp') },
  { title: t('delegate.write'), key: 'canWrite', sortable: false, width: '80px', tooltip: t('delegate.writeHelp') },
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

function openDialog(id = null) {
  editDelegateId.value = id
  editDialog.value = true
}

function onDelegateSaved() {
  // Reload the current page after a create/edit/delete performed from
  // the dialog so the list reflects the new state.
  dt.load(lastOptions)
}

function startDelete(item) {
  deleteTarget.value = item
  deleteDialog.value = true
}

async function confirmDelete() {
  deleting.value = true
  await api.del(`rest/security/delegate/${deleteTarget.value.id}`)
  deleting.value = false
  deleteDialog.value = false
  deleteTarget.value = null
  dt.load(lastOptions)
}

function startBulkDelete() {
  bulkDeleteDialog.value = true
}

async function confirmBulkDelete() {
  deleting.value = true
  for (const id of selected.value) {
    await api.del(`rest/security/delegate/${id}`)
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
      { title: t('delegate.title') },
    ],
    { refresh: () => dt.load(lastOptions) },
  )
})
</script>

<style scoped>
.search-field {
  min-width: 200px;
  max-width: 300px;
  flex: 1 1 200px;
}
</style>
