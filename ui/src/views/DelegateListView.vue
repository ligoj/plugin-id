<!--
  DelegatesView — 2026 "Vibrant" Delegations list (plugin-id). Same recipe:
  VibrantDataTable + reused DelegateEditDialog (Vibrant) + VibrantConfirmDialog,
  on the security/delegate endpoint. Columns: receiver, resource, admin, write.
-->
<template>
  <div class="delegates lj-surface">
    <LjPageHeader :title="t('delegate.title')" :subtitle="t('delegate.subtitle2026')">
      <template #actions>
        <LjButton icon="mdi-plus" @click="openDialog(null)">{{ t('delegate.new') }}</LjButton>
      </template>
    </LjPageHeader>

    <div class="toolbar">
      <LjSearch v-model="dt.search.value" :placeholder="t('delegate.searchPlaceholder') || t('common.search')" @input="onSearch" />
      <span class="tb-sp" />
      <v-slide-x-transition>
        <div v-if="selected.length" class="bulkbar">
          <span class="bulk-count">{{ selected.length }} {{ t('common.selected') }}</span>
          <LjButton variant="danger" icon="mdi-delete" :icon-size="16" @click="startBulkDelete">{{ t('common.delete') }}</LjButton>
        </div>
      </v-slide-x-transition>
    </div>

    <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4" rounded="lg">{{ dt.error.value }}</v-alert>

    <VibrantDataTable v-if="!dt.error.value" :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value" selectable v-model="selected" item-value="id"
      default-sort="receiver" :fetch-all="dt.loadAll" filename="delegates.csv" @update:options="loadData" @row-click="(item) => openDialog(item.id)">
      <template #cell.receiver="{ item }">
        <span class="rcv">
          <v-tooltip :text="t('delegate.type.' + (item.receiverType || '').toLowerCase())" location="top">
            <template #activator="{ props: tt }"><v-icon v-bind="tt" size="16" class="rcv-ic">{{ TYPE_ICONS[item.receiverType?.toUpperCase()] || 'mdi-account' }}</v-icon></template>
          </v-tooltip>
          <span class="rcv-name">{{ item.receiver?.name || item.receiver?.id || item.name || '—' }}</span>
        </span>
      </template>
      <template #cell.name="{ item }">
        <span class="res">
          <v-tooltip :text="t('delegate.type.' + (item.type || '').toLowerCase())" location="top">
            <template #activator="{ props: tt }"><v-icon v-bind="tt" size="16" class="res-ic">{{ TYPE_ICONS[item.type?.toUpperCase()] || 'mdi-shield-key-outline' }}</v-icon></template>
          </v-tooltip>
          <span>{{ item.name || '—' }}</span>
        </span>
      </template>
      <template #cell.canAdmin="{ item }">
        <span class="bdot" :class="{ on: item.canAdmin }">
          <v-tooltip activator="parent" :text="item.canAdmin ? t('delegate.adminGranted') : t('delegate.adminNotGranted')" location="top" />
        </span>
      </template>
      <template #cell.canWrite="{ item }">
        <span class="bdot" :class="{ on: item.canWrite }">
          <v-tooltip activator="parent" :text="item.canWrite ? t('delegate.writeGranted') : t('delegate.writeNotGranted')" location="top" />
        </span>
      </template>
      <template #actions="{ item }">
        <v-menu location="bottom end">
          <template #activator="{ props }">
            <button class="lj-iconbtn" v-bind="props" :aria-label="t('common.edit')" @click.stop><v-icon size="18">mdi-cog</v-icon></button>
          </template>
          <div class="lj-popmenu">
            <button @click="openDialog(item.id)"><v-icon size="18">mdi-pencil</v-icon>{{ t('common.edit') }}</button>
            <div class="sep" />
            <button class="danger" @click="startDelete(item)"><v-icon size="18">mdi-delete</v-icon>{{ t('common.delete') }}</button>
          </div>
        </v-menu>
      </template>
    </VibrantDataTable>

    <LigojConfirmDialog v-model="deleteDialog" :title="t('delegate.deleteTitle')" :icon="TYPE_ICONS.DELEGATE" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting"
      @confirm="confirmDelete">
      {{ t('delegate.deleteConfirmBefore') }}<strong class="text-error">{{ deleteTarget?.receiver?.name || deleteTarget?.name || deleteTarget?.id }}</strong>{{ t('delegate.deleteConfirmAfter') }}
    </LigojConfirmDialog>
    <LigojConfirmDialog v-model="bulkDeleteDialog" :title="t('common.bulkDeleteTitle')" :icon="TYPE_ICONS.DELEGATE" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting"
      @confirm="confirmBulkDelete">
      {{ t('common.bulkDeleteConfirmBefore') }}<strong class="text-error">{{ selected.length }}</strong>{{ t('common.bulkDeleteConfirmAfter') }}
    </LigojConfirmDialog>

    <DelegateEditDialog v-model="editDialog" :delegate-id="editDelegateId" @saved="onDelegateSaved" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDataTable, useApi, useAppStore, useI18nStore } from '@ligoj/host'
import { TYPE_ICONS } from '../composables/delegateTypes.js'
import { VibrantDataTable, VibrantConfirmDialog as LigojConfirmDialog, LjPageHeader, LjButton, LjSearch } from '@ligoj/host'
import DelegateEditDialog from './DelegateEditDialog.vue'

const appStore = useAppStore()
const api = useApi()
const i18n = useI18nStore()
const t = i18n.t

const dt = useDataTable('security/delegate', { defaultSort: 'receiver' })
let searchTimeout = null
let lastOptions = { page: 1, itemsPerPage: 25, sortBy: [] }

const selected = ref([])
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)
const bulkDeleteDialog = ref(false)
const editDialog = ref(false)
const editDelegateId = ref(null)

const headers = computed(() => [
  { label: t('delegate.receiver'), key: 'receiver', sortable: true, exportValue: (r) => r.receiver?.name || r.receiver?.id || r.name || '' },
  { label: t('delegate.resource'), key: 'name', exportValue: (r) => r.name || '' },
  { label: t('delegate.admin'), key: 'canAdmin', align: 'center', width: '90px', exportValue: (r) => (r.canAdmin ? t('delegate.adminGranted') : '') },
  { label: t('delegate.write'), key: 'canWrite', align: 'center', width: '90px', exportValue: (r) => (r.canWrite ? t('delegate.writeGranted') : '') },
])

function loadData(options) { lastOptions = options; dt.load(options) }
function onSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => dt.load({ page: 1, itemsPerPage: lastOptions.itemsPerPage || 25 }), 300)
}

function openDialog(id = null) { editDelegateId.value = id; editDialog.value = true }
function onDelegateSaved() { dt.load(lastOptions) }

function startDelete(item) { deleteTarget.value = item; deleteDialog.value = true }
async function confirmDelete() {
  deleting.value = true
  await api.del(`rest/security/delegate/${deleteTarget.value.id}`)
  deleting.value = false; deleteDialog.value = false; deleteTarget.value = null
  dt.load(lastOptions)
}
function startBulkDelete() { bulkDeleteDialog.value = true }
async function confirmBulkDelete() {
  deleting.value = true
  for (const id of selected.value) await api.del(`rest/security/delegate/${id}`)
  deleting.value = false; bulkDeleteDialog.value = false; selected.value = []
  dt.load(lastOptions)
}

onMounted(() => {
  appStore.setBreadcrumbs(
    [{ title: t('nav.home'), to: '/' }, { title: t('nav.identity') }, { title: t('delegate.title') }],
    { refresh: () => dt.load(lastOptions) },
  )
})
</script>

<style scoped>
/* View-specific cells only — all chrome lives in the shared host components
   (LjPageHeader / LjButton / LjSearch) and the global `.lj-surface` /
   `.lj-iconbtn` / `.lj-popmenu` classes. The `--ink-*` vars these cells read
   are supplied by `.lj-surface` on the root. */
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

.rcv,
.res {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.rcv-ic,
.res-ic {
  color: var(--ink-3);
}

.rcv-name {
  font-weight: 600;
}

/* Status dot: muted when off, vivid green with a glow when on. */
.bdot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(var(--v-theme-on-surface), .2);
  transition: background .15s, box-shadow .15s;
}

.bdot.on {
  background: #1d9d63;
  box-shadow: 0 0 0 3px rgba(29, 157, 99, .18), 0 0 10px 1px rgba(29, 157, 99, .6);
}
</style>