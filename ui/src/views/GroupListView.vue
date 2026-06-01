<template>
  <div>
    <div class="d-flex flex-wrap align-center mb-4 ga-2">
      <v-spacer />
      <v-text-field v-model="dt.search.value" prepend-inner-icon="mdi-magnify" :label="t('common.search')" variant="outlined" density="compact" hide-details class="search-field"
        @update:model-value="onSearch" />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">
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
      @update:options="loadData" @click:row="(_, { item }) => openEdit(item.name)">
      <template #header.scope="{ column }"><span class="d-inline-flex align-center"><v-icon size="small" class="mr-1">mdi-shape-outline</v-icon>{{ column.title }}<v-tooltip activator="parent" location="top" :text="column.title" /></span></template>
      <template #header.count="{ column }"><span class="d-inline-flex align-center"><v-icon size="small" class="mr-1">mdi-account-multiple-outline</v-icon>{{ column.title }}<v-tooltip activator="parent" location="top" :text="column.title" /></span></template>
      <template #header.locked="{ column }"><span class="d-inline-flex align-center"><v-icon size="small" class="mr-1">mdi-shield-lock-outline</v-icon>{{ column.title }}<v-tooltip activator="parent" location="top" :text="column.title" /></span></template>
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
        <!-- View details: group properties (name / scope / parent) are
             effectively read-only once a group exists at the LDAP
             level, so this is an "open the details panel" gesture,
             not an "edit" one. The eye icon + "View" tooltip
             communicate that — and the row click still routes through
             the same handler so the affordance is consistent. -->
        <v-btn icon size="small" variant="text" @click.stop="openEdit(item.name)">
          <v-icon size="small">mdi-eye-outline</v-icon>
          <v-tooltip activator="parent" :text="t('common.view')" location="top" />
        </v-btn>
        <v-btn icon size="small" variant="text" color="error" @click.stop="startDelete(item)">
          <v-icon size="small">mdi-delete</v-icon>
          <v-tooltip activator="parent" :text="t('common.delete')" location="top" />
        </v-btn>
      </template>
    </LigojDataTableServer>

    <!-- Group view / create dialog. NOT `persistent`: viewing an
         existing group is read-only (nothing to lose on accidental
         dismiss) and the create form is small enough that ESC /
         backdrop closing is an acceptable trade for the Escape-to-
         dismiss reflex the user expects. The Panel is
         `v-if`-mounted on `editDialog` so each open is a fresh
         component (clean form state, fresh REST fetch for the target
         group); `:key="editingId ?? 'new'"` belt-and-suspenders for
         the rare in-place target swap. -->
    <v-dialog v-model="editDialog" max-width="700" scrollable>
      <v-card>
        <v-card-title class="d-flex align-center ga-2">
          <v-icon color="primary">{{ editingId ? 'mdi-eye-outline' : 'mdi-account-group' }}</v-icon>
          <span v-if="editingId">{{ t('group.detailsTitle') }}</span>
          <span v-else>{{ t('group.new') }}</span>
          <span v-if="editingId" class="text-primary">{{ editingId }}</span>
          <v-spacer />
          <v-btn icon size="small" variant="text" @click="editDialog = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>
        <GroupEditPanel
          v-if="editDialog"
          :key="editingId ?? 'new'"
          :group-id="editingId"
          @saved="onEditSaved"
          @deleted="onEditDeleted"
          @cancel="editDialog = false"
        />
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card>
        <v-card-title class="d-flex align-center ga-2">
          <v-icon color="primary">{{ TYPE_ICONS.GROUP }}</v-icon>
          <span>{{ t('group.deleteTitle') }}</span>
        </v-card-title>
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
        <v-card-title class="d-flex align-center ga-2">
          <v-icon color="primary">{{ TYPE_ICONS.GROUP }}</v-icon>
          <span>{{ t('common.bulkDeleteTitle') }}</span>
        </v-card-title>
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
import { TYPE_ICONS } from '../composables/delegateTypes.js'
import { useGroupMembersDialog } from '../composables/useGroupMembersDialog.js'
import GroupEditPanel from '../components/GroupEditPanel.vue'

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

/* ---- Create / edit dialog ----------------------------------------
 * Replaces the legacy `router.push('/id/group/<id>')` page navigation
 * with an in-place dialog so the user keeps the list (and any scroll
 * / filter state) when editing. The routed URLs still resolve here
 * (see `index.js` route table) and auto-open the dialog on mount,
 * preserving email / bookmark deep links.
 * ------------------------------------------------------------------ */

const editDialog = ref(false)
const editingId = ref(null)

function openCreate() {
  editingId.value = null
  editDialog.value = true
}

function openEdit(name) {
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

const router = useRouter()
const route = useRoute()
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
  // Deep-link support. `/id/group/new` and `/id/group/<id>` route
  // here (see plugin index.js) and open the dialog over the list so
  // emails / bookmarks pointing at the old per-entity URLs still
  // land the user in the right place — but inside the list context.
  const id = route.params?.id
  if (id === 'new' || route.path?.endsWith('/group/new')) {
    openCreate()
  } else if (id) {
    openEdit(String(id))
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
