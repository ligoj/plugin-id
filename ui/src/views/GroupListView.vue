<template>
  <div>
    <div class="d-flex flex-wrap align-center mb-4 ga-2">
      <v-spacer />
      <v-text-field v-model="dt.search.value" prepend-inner-icon="mdi-magnify" :label="t('common.search')" variant="outlined" density="compact" hide-details class="search-field"
        @update:model-value="onSearch" />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="router.push('/id/group/new')">
        {{ t('group.new') }}
      </v-btn>
    </div>

    <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4">
      <v-alert-title>{{ t('user.noProvider') }}</v-alert-title>
      {{ dt.error.value === 'internal' ? t('group.noProvider') : dt.error.value }}
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

    <LigojDataTableServer filename="groups.csv" :fetch-all="dt.loadAll" v-if="!dt.error.value" v-show="dt.items.value.length > 0 || !dt.loading.value" v-model="selected"
      v-model:items-per-page="itemsPerPage" :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value" item-value="name" show-select hover
      @update:options="loadData" @click:row="(_, { item }) => router.push('/id/group/' + item.name)">
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
        <!-- Manage members: opens the global GroupMembersDialog
             scoped to this row's group. Dialog is header-mounted
             via `install()` so we just push state to it here. The
             `onChanged` callback fires once on dialog close IF the
             user added/removed at least one member — refreshing the
             groups table so the member-count column reflects the
             new total. -->
        <v-btn icon size="small" variant="text" @click.stop="onManageMembers(item.name)">
          <v-icon size="small">mdi-account-multiple</v-icon>
          <v-tooltip activator="parent" :text="t('id.group.manage')" location="top" />
        </v-btn>
        <v-btn icon size="small" variant="text" @click.stop="router.push('/id/group/' + item.name)">
          <v-icon size="small">mdi-pencil</v-icon>
          <v-tooltip activator="parent" :text="t('common.edit')" location="top" />
        </v-btn>
        <v-btn icon size="small" variant="text" color="error" @click.stop="startDelete(item)">
          <v-icon size="small">mdi-delete</v-icon>
          <v-tooltip activator="parent" :text="t('common.delete')" location="top" />
        </v-btn>
      </template>
    </LigojDataTableServer>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card>
        <v-card-title>{{ t('group.deleteTitle') }}</v-card-title>
        <v-card-text>
          {{ t('group.deleteConfirmBefore') }}<strong class="text-error">{{ deleteTarget?.name }}</strong>{{ t('group.deleteConfirmAfter') }}
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
import { useRouter } from 'vue-router'
import { useDataTable, useApi, useAppStore, useErrorStore, useI18nStore, LigojDataTableServer } from '@ligoj/host'
import { useGroupMembersDialog } from '../composables/useGroupMembersDialog.js'

const membersDialog = useGroupMembersDialog()

/**
 * Click handler for the Manage-members action. Opens the global
 * dialog and registers an `onChanged` callback the dialog will
 * invoke ONCE on close if the user added/removed at least one
 * member — refreshing the table so this row's member-count column
 * shows the new total.
 */
function onManageMembers(name) {
  membersDialog.openFor(name, {
    onChanged: () => dt.load(lastOptions),
  })
}

const router = useRouter()
const appStore = useAppStore()
const api = useApi()
const errorStore = useErrorStore()
const i18n = useI18nStore()
const t = i18n.t

const DEMO_GROUPS = [
  { name: 'Engineering', scope: 'Group', count: 4, locked: false },
  { name: 'Marketing', scope: 'Group', count: 1, locked: false },
  { name: 'DevOps', scope: 'Group', count: 2, locked: false },
  { name: 'Management', scope: 'Group', count: 2, locked: false },
  { name: 'Sales', scope: 'Group', count: 1, locked: false },
]
const dt = useDataTable('service/id/group', { defaultSort: 'name', demoData: DEMO_GROUPS })
const itemsPerPage = ref(25)
let searchTimeout = null

const selected = ref([])
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)
const bulkDeleteDialog = ref(false)
let lastOptions = {}

const headers = computed(() => [
  { title: t('common.name'), key: 'name', sortable: true },
  { title: t('group.scope'), key: 'scope', sortable: false },
  { title: t('group.members'), key: 'count', sortable: false, width: '100px', align: 'center' },
  { title: t('group.locked'), key: 'locked', sortable: false, width: '80px', align: 'center' },
  { title: '', key: 'actions', sortable: false, width: '160px', align: 'center' },
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
    errorStore.push({ message: t('group.demoDelete'), status: 0 })
    deleteDialog.value = false
    return
  }
  deleting.value = true
  await api.del(`rest/service/id/group/${deleteTarget.value.name}`)
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
    errorStore.push({ message: t('group.demoDelete'), status: 0 })
    bulkDeleteDialog.value = false
    return
  }
  deleting.value = true
  for (const name of selected.value) {
    await api.del(`rest/service/id/group/${name}`)
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
      { title: t('group.title') },
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
