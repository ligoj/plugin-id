<!--
  UsersView — 2026 "Vibrant" Users list (plugin-id), mockup-faithful.

  Renders the validated 2026 mockup language (page header + toolbar +
  VibrantDataTable: tinted header, mailchips, group chips, mono login,
  gear popmenu, "Lignes : N / a–b sur total" footer) while REUSING the
  real logic: useDataTable for fetch/sort/pagination/search, and the core
  CRUD dialogs (UserEditDialog, LigojConfirmDialog) via @ligoj/host.
-->
<template>
  <div class="users lj-surface">
    <LjPageHeader :title="t('user.title')" :subtitle="t('user.subtitle2026')">
      <template #actions>
        <LjButton icon="mdi-plus" @click="openCreate">{{ t('user.new') }}</LjButton>
        <!-- Export/Copy now live in the table's own tools cog (VibrantDataTable
             :fetch-all). Only the CSV Import action remains here. -->
        <LjButton variant="ghost" icon="mdi-upload" :loading="importing" @click="importInput?.click()">
          {{ importing ? (t('common.importing') || 'Import…') : (t('common.import') || 'Importer') }}
        </LjButton>
        <input ref="importInput" type="file" accept=".csv,.tsv,text/csv" hidden @change="onImport" />
      </template>
    </LjPageHeader>

    <!-- Search toolbar. -->
    <div class="toolbar">
      <LjSearch v-model="dt.search.value" :placeholder="t('user.searchPlaceholder') || t('common.search')" @input="onSearch" />
      <span class="tb-sp" />
      <v-slide-x-transition>
        <div v-if="selected.length" class="bulkbar">
          <span class="bulk-count">{{ selected.length }} {{ t('common.selected') }}</span>
          <LjButton variant="danger" icon="mdi-delete" :icon-size="16" @click="startBulkDelete">{{ t('common.delete') }}</LjButton>
        </div>
      </v-slide-x-transition>
    </div>

    <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4" rounded="lg">
      <v-alert-title>{{ t('user.noProvider') }}</v-alert-title>
      {{ dt.error.value === 'internal' ? t('user.noProviderMsg') : dt.error.value }}
    </v-alert>
    <v-alert v-if="dt.demoMode.value" type="info" variant="tonal" density="compact" class="mb-4" rounded="lg">
      {{ t('user.demoMode') }}
    </v-alert>

    <VibrantDataTable v-if="!dt.error.value" :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value" selectable v-model="selected" item-value="id"
      default-sort="id" :fetch-all="dt.loadAll" filename="users.csv" @update:options="loadData" @row-click="(item) => openEdit(item.id)">
      <template #cell.id="{ item }">
        <span class="login"><v-icon size="16" class="login-ic">mdi-account-circle</v-icon><span class="mono">{{ item.id }}</span></span>
      </template>
      <template #cell.mails="{ item }">
        <span class="mails">
          <span v-for="m in (item.mails || []).slice(0, 2)" :key="m" class="mailchip"><v-icon size="12">mdi-email-outline</v-icon>{{ m }}</span>
          <span v-if="(item.mails || []).length > 2" class="more">+{{ item.mails.length - 2 }}</span>
          <span v-if="!(item.mails || []).length" class="dash">—</span>
        </span>
      </template>
      <template #cell.groups="{ item }">
        <span class="groups">
          <span v-for="g in (item.groups || []).slice(0, 3)" :key="g.name || g" class="chip">{{ g.name || g }}</span>
          <span v-if="(item.groups || []).length > 3" class="more">+{{ item.groups.length - 3 }}</span>
          <span v-if="!(item.groups || []).length" class="dash">—</span>
        </span>
      </template>
      <template #cell.locked="{ item }">
        <v-tooltip :text="item.locked ? t('user.statusLocked') : t('user.statusActive')" location="top">
          <template #activator="{ props: tt }">
            <v-icon v-bind="tt" :color="item.locked ? 'error' : 'success'" size="19">{{ item.locked ? 'mdi-lock' : 'mdi-lock-open-variant' }}</v-icon>
          </template>
        </v-tooltip>
      </template>
      <template #actions="{ item }">
        <v-menu location="bottom end">
          <template #activator="{ props }">
            <button class="lj-iconbtn" v-bind="props" :aria-label="t('user.actions')" @click.stop><v-icon size="18">mdi-cog</v-icon></button>
          </template>
          <div class="lj-popmenu">
            <button @click="openEdit(item.id)"><v-icon size="18">mdi-pencil</v-icon>{{ t('user.edit') }}</button>
            <button class="danger" @click="startDelete(item)"><v-icon size="18">mdi-delete</v-icon>{{ t('common.delete') }}</button>
            <div class="sep" />
            <button @click="startUserAction(item, item.locked ? 'unlock' : 'lock')"><v-icon size="18">{{ item.locked ? 'mdi-lock-open-variant' : 'mdi-lock' }}</v-icon>{{ item.locked ? t('user.unlock')
              :
              t('user.lock') }}</button>
            <button @click="startUserAction(item, item.isolated ? 'restore' : 'isolate')"><v-icon size="18">{{ item.isolated ? 'mdi-account-check' : 'mdi-account-off' }}</v-icon>{{ item.isolated ?
              t('user.restore') : t('user.isolate') }}</button>
            <button @click="startUserAction(item, 'resetPassword')"><v-icon size="18">mdi-lock-reset</v-icon>{{ t('user.resetPassword') }}</button>
          </div>
        </v-menu>
      </template>
    </VibrantDataTable>

    <!-- Single-user delete: name in bold red via the default slot. -->
    <LigojConfirmDialog v-model="deleteDialog" :title="t('user.deleteTitle')" :icon="TYPE_ICONS.USER" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting"
      @confirm="confirmDeleteUser">
      {{ t('user.deleteConfirmBefore') }}<strong class="text-error">{{ deleteTarget?.id }}</strong>{{ t('user.deleteConfirmAfter') }}
    </LigojConfirmDialog>
    <LigojConfirmDialog v-model="bulkDeleteDialog" :title="t('common.bulkDeleteTitle')" :icon="TYPE_ICONS.USER" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting"
      @confirm="confirmBulkDelete">
      {{ t('common.bulkDeleteConfirmBefore') }}<strong class="text-error">{{ selected.length }}</strong>{{ t('common.bulkDeleteConfirmAfter') }}
    </LigojConfirmDialog>
    <LigojConfirmDialog v-model="actionDialog" :title="t('user.' + actionType)" :icon="TYPE_ICONS.USER" :confirm-label="t('common.confirm')" :loading="actionLoading" @confirm="confirmUserAction">
      {{ t('user.' + actionType + 'ConfirmBefore') }}<strong class="text-error">{{ actionTarget?.id }}</strong>{{ t('user.' + actionType + 'ConfirmAfter') }}
    </LigojConfirmDialog>

    <!-- User create/edit popup. userId null = create mode. -->
    <UserEditDialog v-model="editDialog" :user-id="editUserId" @saved="onUserSaved" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDataTable, useApi, useAppStore, useErrorStore, useI18nStore } from '@ligoj/host'
import { TYPE_ICONS } from '../composables/delegateTypes.js'
// Shared 2026 chrome: table, confirm dialog (aliased so <LigojConfirmDialog>
// tags need no change), page header, buttons, search — all from the host.
import { VibrantDataTable, VibrantConfirmDialog as LigojConfirmDialog, LjPageHeader, LjButton, LjSearch } from '@ligoj/host'
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
let searchTimeout = null
let lastOptions = { page: 1, itemsPerPage: 25, sortBy: [] }

const selected = ref([])
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)
const bulkDeleteDialog = ref(false)
const actionDialog = ref(false)
const actionType = ref('')
const actionTarget = ref(null)
const actionLoading = ref(false)
const editDialog = ref(false)
const editUserId = ref(null)
const importInput = ref(null)
const importing = ref(false)

// `exportValue` keeps the CSV/clipboard output human-readable (arrays and
// the locked boolean would otherwise serialize as JSON / true|false).
const headers = computed(() => [
  { title: t('user.login'), label: t('user.login'), key: 'id', sortable: true },
  { label: t('user.firstName'), key: 'firstName', sortable: true },
  { label: t('user.lastName'), key: 'lastName', sortable: true },
  { label: t('user.company'), key: 'company', sortable: true },
  { label: t('user.emails'), key: 'mails', exportValue: (r) => (r.mails || []).join(' ') },
  { label: t('user.groups'), key: 'groups', exportValue: (r) => (r.groups || []).map((g) => g.name || g).join(' ') },
  { label: t('common.status'), key: 'locked', align: 'center', width: '90px', exportValue: (r) => (r.locked ? t('user.statusLocked') : t('user.statusActive')) },
])

function loadData(options) {
  lastOptions = options
  dt.load(options)
}

function onSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => dt.load({ page: 1, itemsPerPage: lastOptions.itemsPerPage || 25 }), 300)
}

function startDelete(item) { deleteTarget.value = item; deleteDialog.value = true }

async function confirmDeleteUser() {
  if (dt.demoMode.value) { errorStore.push({ message: t('user.demoDelete'), status: 0 }); deleteDialog.value = false; return }
  deleting.value = true
  await api.del(`rest/service/id/user/${deleteTarget.value.id}`)
  deleting.value = false
  deleteDialog.value = false
  deleteTarget.value = null
  dt.load(lastOptions)
}

function startBulkDelete() { bulkDeleteDialog.value = true }

async function confirmBulkDelete() {
  if (dt.demoMode.value) { errorStore.push({ message: t('user.demoDelete'), status: 0 }); bulkDeleteDialog.value = false; return }
  deleting.value = true
  for (const id of selected.value) await api.del(`rest/service/id/user/${id}`)
  deleting.value = false
  bulkDeleteDialog.value = false
  selected.value = []
  dt.load(lastOptions)
}

function startUserAction(item, type) { actionTarget.value = item; actionType.value = type; actionDialog.value = true }

async function confirmUserAction() {
  if (dt.demoMode.value) { errorStore.push({ message: t('user.demoAction'), status: 0 }); actionDialog.value = false; return }
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
  dt.load(lastOptions)
}

function openCreate() { editUserId.value = null; editDialog.value = true }
function openEdit(id) { editUserId.value = id; editDialog.value = true }
function onUserSaved() { dt.load(lastOptions) }

/* Export / Copy are provided by VibrantDataTable's tools cog (it calls the
   `:fetch-all="dt.loadAll"` we pass to pull every row). No view-local CSV
   code needed. */

async function onImport(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (dt.demoMode.value) { errorStore.push({ message: t('user.demoMode'), status: 0 }); e.target.value = ''; return }
  importing.value = true
  const fd = new FormData()
  fd.append('csv-file', file)
  try {
    // useApi.upload returns null on a non-2xx (the global ErrorSnackbar already
    // surfaced the cause via handleResponse); a non-null result = accepted.
    const res = await api.upload('rest/service/id/user/import/csv/full', fd)
    if (res !== null) {
      errorStore.success(t('common.importSuccess', { file: file.name }))
      dt.load({ page: 1, itemsPerPage: lastOptions.itemsPerPage || 25 })
    }
  } finally {
    importing.value = false
    e.target.value = ''
  }
}

onMounted(() => {
  appStore.setBreadcrumbs(
    [{ title: t('nav.home'), to: '/' }, { title: t('nav.identity') }, { title: t('user.title') }],
    { refresh: () => dt.load(lastOptions) },
  )
})
</script>

<style scoped>
/* Chrome (header, buttons, search, row menu) lives in the shared host
   components + the global `.lj-surface` / `.lj-iconbtn` / `.lj-popmenu`
   classes. This block keeps only the view-specific toolbar layout + table
   cell rendering; the `--mono` / `--ink-*` / `--pill` / `--radius-sm` vars
   it reads are supplied by `.lj-surface` on the root. */
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tb-sp {
  flex: 1;
}

.bulkbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bulk-count {
  font-weight: 700;
  font-size: 13px;
  color: var(--ink-2);
}

/* Cells. */
.login {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.login-ic {
  color: var(--ink-3);
}

.mono {
  font-family: var(--mono);
  font-size: 13px;
  font-weight: 600;
}

.mails {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
}

.mailchip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-2);
  background: var(--pill);
  border: var(--border-w) var(--lj-border-style, solid) var(--border-c);
  border-radius: var(--radius-sm);
  padding: 3px 9px;
}

.mailchip :deep(.v-icon) {
  opacity: .6;
}

.groups {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 5px;
  overflow: hidden;
}

.chip {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-2);
  background: var(--pill);
  border: var(--border-w) var(--lj-border-style, solid) var(--border-c);
  border-radius: var(--lj-radius-sm, 20px);
  padding: 3px 11px;
  white-space: nowrap;
}

.more {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-3);
}

.dash {
  color: var(--ink-3);
}
</style>
