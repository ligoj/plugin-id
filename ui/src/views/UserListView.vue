<template>
  <div>
    <div class="d-flex flex-wrap align-center mb-4 ga-2">
      <v-spacer />
      <v-text-field v-model="dt.search.value" prepend-inner-icon="mdi-magnify" :label="t('common.search')" variant="outlined" density="compact" hide-details class="search-field"
        @update:model-value="onSearch" />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('user.new') }}
      </v-btn>
      <ImportExportBar export-endpoint="service/id/user" import-endpoint="service/id/user/import/csv/full" export-filename="users.csv"
        @imported="dt.load({ page: 1, itemsPerPage: itemsPerPage.value })" />
    </div>

    <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4">
      <v-alert-title>{{ t('user.noProvider') }}</v-alert-title>
      {{ dt.error.value === 'internal' ? t('user.noProviderMsg') : dt.error.value }}
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

    <LigojDataTableServer filename="users.csv" :fetch-all="dt.loadAll" v-if="!dt.error.value" v-show="dt.items.value.length > 0 || !dt.loading.value" v-model="selected"
      v-model:items-per-page="itemsPerPage" :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value" item-value="id" show-select hover
      @update:options="loadData" @click:row="(_, { item }) => openEdit(item.id)">
      <!-- Mails column (chantier D4): render each address as a v-chip, cap
           at 2 visible and fold the rest into a "+N" indicator so rows stay
           on a single line even when a user has many addresses. Pattern
           matches the existing groups-cell rendering below. -->
      <template #item.id="{ item }">
        <div class="d-flex align-center ga-2">
          <v-icon size="small" color="medium-emphasis">mdi-account-circle</v-icon>
          <code>{{ item.id }}</code>
        </div>
      </template>
      <template #header.id="{ column }"><span class="d-inline-flex align-center"><v-icon size="small" class="mr-1">mdi-account</v-icon>{{ column.title }}<v-tooltip activator="parent" location="top" :text="column.title" /></span></template>
      <template #header.company="{ column }"><span class="d-inline-flex align-center"><v-icon size="small" class="mr-1">mdi-domain</v-icon>{{ column.title }}<v-tooltip activator="parent" location="top" :text="column.title" /></span></template>
      <template #header.mails="{ column }"><span class="d-inline-flex align-center"><v-icon size="small" class="mr-1">mdi-email-outline</v-icon>{{ column.title }}<v-tooltip activator="parent" location="top" :text="column.title" /></span></template>
      <template #header.groups="{ column }"><span class="d-inline-flex align-center"><v-icon size="small" class="mr-1">mdi-account-group</v-icon>{{ column.title }}<v-tooltip activator="parent" location="top" :text="column.title" /></span></template>
      <template #header.locked="{ column }">
        <span class="d-inline-flex align-center">
          <v-icon size="small">mdi-shield-account-outline</v-icon>
          <v-tooltip activator="parent" location="top" :text="column.title" />
        </span>
      </template>
      <template #item.mails="{ item }">
        <div class="mails-cell">
          <v-chip v-for="m in (item.mails || []).slice(0, 2)" :key="m" size="small" variant="tonal" class="mr-1">{{ m }}</v-chip>
          <span v-if="(item.mails || []).length > 2" class="text-caption text-medium-emphasis">
            +{{ item.mails.length - 2 }}
          </span>
        </div>
      </template>
      <template #item.groups="{ item }">
        <div class="groups-cell">
          <v-chip v-for="g in (item.groups || []).slice(0, 3)" :key="g.name || g" size="small" class="mr-1">{{ g.name || g }}</v-chip>
          <span v-if="(item.groups || []).length > 3" class="text-caption text-medium-emphasis">
            +{{ item.groups.length - 3 }}
          </span>
        </div>
      </template>
      <template #item.locked="{ item }">
        <div class="text-center">
          <v-tooltip v-if="item.locked" :text="t('user.statusLocked')" location="top">
            <template #activator="{ props: tt }">
              <v-icon v-bind="tt" color="error" size="small">mdi-lock</v-icon>
            </template>
          </v-tooltip>
          <v-tooltip v-else :text="t('user.statusActive')" location="top">
            <template #activator="{ props: tt }">
              <v-icon v-bind="tt" color="success" size="small">mdi-lock-open-variant</v-icon>
            </template>
          </v-tooltip>
        </div>
      </template>
      <template #item.actions="{ item }">
        <!-- Single gear button opening a menu of row actions (Fabrice
             review, chantier I.1). @click.stop on the gear activator
             keeps the click off the row, whose @click:row opens the edit
             dialog. The menu items deliberately carry NO .stop:
             VMenu teleports its content outside the <tr>, so item clicks
             can never reach @click:row anyway — and a .stop there would
             swallow the bubbling click that VMenu's close-on-content-click
             relies on, leaving the menu open behind the dialogs.
             Lock/Isolate labels and icons are contextual: the API omits
             `locked`/`isolated` when null, so a truthy value means the
             user is locked/isolated. -->
        <v-menu>
          <template #activator="{ props }">
            <v-btn icon size="x-small" variant="text" :aria-label="t('user.actions')" v-bind="props" @click.stop>
              <v-icon size="small">mdi-cog</v-icon>
              <v-tooltip activator="parent" :text="t('user.actions')" location="top" />
            </v-btn>
          </template>
          <v-list density="compact" min-width="220">
            <v-list-item prepend-icon="mdi-pencil" :title="t('user.edit')" @click="openEdit(item.id)" />
            <v-list-item prepend-icon="mdi-delete" base-color="error" :title="t('common.delete')" @click="startDelete(item)" />
            <v-divider class="my-1" />
            <v-list-item :prepend-icon="item.locked ? 'mdi-lock-open-variant' : 'mdi-lock'" :title="item.locked ? t('user.unlock') : t('user.lock')"
              @click="startUserAction(item, item.locked ? 'unlock' : 'lock')" />
            <v-list-item :prepend-icon="item.isolated ? 'mdi-account-check' : 'mdi-account-off'" :title="item.isolated ? t('user.restore') : t('user.isolate')"
              @click="startUserAction(item, item.isolated ? 'restore' : 'isolate')" />
            <v-list-item prepend-icon="mdi-lock-reset" :title="t('user.resetPassword')" @click="startUserAction(item, 'resetPassword')" />
          </v-list>
        </v-menu>
      </template>
    </LigojDataTableServer>

    <!-- Single-user delete (chantier D2): name in bold red via the
         default slot of LigojConfirmDialog. -->
    <LigojConfirmDialog v-model="deleteDialog" :title="t('user.deleteTitle')" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting" @confirm="confirmDeleteUser">
      {{ t('user.deleteConfirmBefore') }}<strong class="text-error">{{ deleteTarget?.id }}</strong>{{ t('user.deleteConfirmAfter') }}
    </LigojConfirmDialog>

    <!-- Bulk delete (chantier D2 rattrapage): the selected count is
         rendered in bold red via the default slot, same visual weight
         as a sensitive single-item confirmation. The host's monolithic
         `common.bulkDeleteConfirm` key stays intact; we just use two
         new plugin-local fragments around the count. -->
    <LigojConfirmDialog v-model="bulkDeleteDialog" :title="t('common.bulkDeleteTitle')" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting" @confirm="confirmBulkDelete">
      {{ t('common.bulkDeleteConfirmBefore') }}<strong class="text-error">{{ selected.length }}</strong>{{ t('common.bulkDeleteConfirmAfter') }}
    </LigojConfirmDialog>

    <!-- Confirmation for the sensitive lock/isolate/reset actions
         triggered from the row gear menu (chantier D2): the login is
         now bolded and coloured via two i18n fragments around it
         (user.<action>ConfirmBefore / user.<action>ConfirmAfter). -->
    <LigojConfirmDialog v-model="actionDialog" :title="t('user.' + actionType)" :confirm-label="t('common.confirm')" :loading="actionLoading" @confirm="confirmUserAction">
      {{ t('user.' + actionType + 'ConfirmBefore') }}<strong class="text-error">{{ actionTarget?.id }}</strong>{{ t('user.' + actionType + 'ConfirmAfter') }}
    </LigojConfirmDialog>

    <!-- User create/edit popup (chantier I.2). userId null = create mode. -->
    <UserEditDialog v-model="editDialog" :user-id="editUserId" @saved="onUserSaved" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDataTable, useApi, useAppStore, useErrorStore, useI18nStore, ImportExportBar, LigojDataTableServer, LigojConfirmDialog } from '@ligoj/host'
import UserEditDialog from './UserEditDialog.vue'

const appStore = useAppStore()
const api = useApi()
const errorStore = useErrorStore()
const i18n = useI18nStore()
const t = i18n.t
const DEMO_USERS = [
  { id: 'admin', firstName: 'Admin', lastName: 'User', company: 'Ligoj', mails: ['admin@ligoj.org'], groups: [{ name: 'Engineering' }, { name: 'Management' }], locked: false },
  { id: 'jdupont', firstName: 'Jean', lastName: 'Dupont', company: 'Ligoj', mails: ['jean.dupont@ligoj.org'], groups: [{ name: 'Engineering' }, { name: 'DevOps' }], locked: false },
  { id: 'mmartin', firstName: 'Marie', lastName: 'Martin', company: 'AcmeCorp', mails: ['marie.martin@acme.com'], groups: [{ name: 'Marketing' }], locked: false },
  { id: 'pdurand', firstName: 'Pierre', lastName: 'Durand', company: 'AcmeCorp', mails: ['pierre.durand@acme.com'], groups: [{ name: 'Engineering' }], locked: false },
  { id: 'sleblanc', firstName: 'Sophie', lastName: 'Leblanc', company: 'TechSolutions', mails: ['sophie.leblanc@techsol.com'], groups: [{ name: 'DevOps' }], locked: false },
  { id: 'tmoreau', firstName: 'Thomas', lastName: 'Moreau', company: 'TechSolutions', mails: ['thomas.moreau@techsol.com'], groups: [{ name: 'Sales' }], locked: false },
  { id: 'crichard', firstName: 'Claire', lastName: 'Richard', company: 'Ligoj', mails: ['claire.richard@ligoj.org'], groups: [{ name: 'Management' }], locked: false },
  { id: 'agarcia', firstName: 'Antoine', lastName: 'Garcia', company: 'Ligoj', mails: ['antoine.garcia@ligoj.org'], groups: [{ name: 'Engineering' }], locked: false },
]
const dt = useDataTable('service/id/user', { defaultSort: 'id', demoData: DEMO_USERS })
const itemsPerPage = ref(25)
let searchTimeout = null
let lastOptions = { page: 1, itemsPerPage: 25, sortBy: [] }

const selected = ref([])
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)
const bulkDeleteDialog = ref(false)

// Row gear-menu action state (lock/unlock, isolate/restore, reset).
const actionDialog = ref(false)
const actionType = ref('')
const actionTarget = ref(null)
const actionLoading = ref(false)

// User create/edit dialog state. editUserId null = create mode.
const editDialog = ref(false)
const editUserId = ref(null)

const headers = computed(() => [
  { title: t('user.login'), key: 'id', sortable: true },
  { title: t('user.firstName'), key: 'firstName', sortable: true },
  { title: t('user.lastName'), key: 'lastName', sortable: true },
  { title: t('user.company'), key: 'company', sortable: true },
  { title: t('user.emails'), key: 'mails', sortable: false },
  { title: t('user.groups'), key: 'groups', sortable: false },
  { title: t('common.status'), key: 'locked', sortable: false, width: '80px', align: 'center' },
  { title: '', key: 'actions', sortable: false, width: '120px', align: 'end' },
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

async function confirmDeleteUser() {
  if (dt.demoMode.value) {
    errorStore.push({ message: t('user.demoDelete'), status: 0 })
    deleteDialog.value = false
    return
  }
  deleting.value = true
  await api.del(`rest/service/id/user/${deleteTarget.value.id}`)
  deleting.value = false
  deleteDialog.value = false
  deleteTarget.value = null
  dt.load({ page: 1, itemsPerPage: itemsPerPage.value })
}

function startBulkDelete() {
  bulkDeleteDialog.value = true
}

async function confirmBulkDelete() {
  if (dt.demoMode.value) {
    errorStore.push({ message: t('user.demoDelete'), status: 0 })
    bulkDeleteDialog.value = false
    return
  }
  deleting.value = true
  for (const id of selected.value) {
    await api.del(`rest/service/id/user/${id}`)
  }
  deleting.value = false
  bulkDeleteDialog.value = false
  selected.value = []
  dt.load({ page: 1, itemsPerPage: itemsPerPage.value })
}

function startUserAction(item, type) {
  actionTarget.value = item
  actionType.value = type
  actionDialog.value = true
}

async function confirmUserAction() {
  if (dt.demoMode.value) {
    errorStore.push({ message: t('user.demoAction'), status: 0 })
    actionDialog.value = false
    return
  }
  actionLoading.value = true
  const id = actionTarget.value.id
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
  actionTarget.value = null
  // Reload the current page so the status icon and the contextual
  // lock/isolate menu labels reflect the new state.
  dt.load(lastOptions)
}

function openCreate() {
  editUserId.value = null
  editDialog.value = true
}

function openEdit(id) {
  editUserId.value = id
  editDialog.value = true
}

function onUserSaved() {
  // Reload the current page after a create/edit/delete or an account
  // action performed from the dialog.
  dt.load(lastOptions)
}

onMounted(() => {
  appStore.setBreadcrumbs(
    [
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('user.title') },
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

/* Keep the groups column on a single line so rows stay vertically
   aligned when a user has many groups. Chips overflow horizontally
   instead of wrapping. */
.groups-cell {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  overflow: hidden;
  white-space: nowrap;
}

/* Mails column (chantier D4). Soft-wrap is acceptable here because
   email addresses can be long: stacking onto a 2nd line keeps the
   column readable. The +N indicator at the end of (item.mails || [])
   keeps the visual footprint bounded. */
.mails-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
