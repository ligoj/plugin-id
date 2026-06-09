<!--
  CompaniesView — 2026 "Vibrant" Companies/Entities list (plugin-id). Same
  recipe as GroupsView (VibrantDataTable + reused CompanyEditPanel in a
  Vibrant dialog + VibrantConfirmDialog), on service/id/company. No members.
-->
<template>
  <div class="companies lj-surface">
    <LjPageHeader :title="t('company.title')" :subtitle="t('company.subtitle2026')">
      <template #actions>
        <LjSearch v-model="dt.search.value" :placeholder="t('company.searchPlaceholder') || t('common.search')" @input="onSearch" />
        <v-slide-x-transition>
          <div v-if="selected.length" class="bulkbar">
            <span class="bulk-count">{{ selected.length }} {{ t('common.selected') }}</span>
            <LjButton variant="danger" icon="mdi-delete" :icon-size="16" @click="startBulkDelete">{{ t('common.delete') }}</LjButton>
          </div>
        </v-slide-x-transition>
        <LjButton icon="mdi-plus" @click="openCreate">{{ t('company.new') }}</LjButton>
      </template>
    </LjPageHeader>

    <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4" rounded="lg">
      <v-alert-title>{{ t('user.noProvider') }}</v-alert-title>
      {{ dt.error.value === 'internal' ? t('company.noProvider') : dt.error.value }}
    </v-alert>
    <v-alert v-if="dt.demoMode.value" type="info" variant="tonal" density="compact" class="mb-4" rounded="lg">
      {{ t('user.demoMode') }}
    </v-alert>

    <VibrantDataTable v-if="!dt.error.value" :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value" selectable v-model="selected" item-value="name"
      default-sort="name" :fetch-all="dt.loadAll" filename="companies.csv" @update:options="loadData" @row-click="(item) => openDetails(item.name)">
      <template #cell.name="{ item }">
        <span class="cname">{{ item.name }}</span>
      </template>
      <template #cell.scope="{ item }">
        <span v-if="item.scope" class="tagdot"><span class="d" :style="{ background: scopeColor(item.scope) }" />{{ item.scope }}</span>
        <span v-else class="dash">—</span>
      </template>
      <template #cell.count="{ item }"><span class="mono">{{ item.count ?? '—' }}</span></template>
      <template #cell.locked="{ item }">
        <LjStatus :status="item.locked ? 'error' : 'ok'"
                  :tooltip="item.locked ? t('user.statusLocked') : t('user.statusActive')" />
      </template>
      <template #actions="{ item }">
        <v-menu location="bottom end">
          <template #activator="{ props }">
            <button class="lj-iconbtn" v-bind="props" :aria-label="t('common.view')" @click.stop><v-icon size="18">mdi-cog</v-icon></button>
          </template>
          <div class="lj-popmenu">
            <button @click="openDetails(item.name)"><v-icon size="18">mdi-eye-outline</v-icon>{{ t('common.view') }}</button>
            <div class="sep" />
            <button class="danger" @click="startDelete(item)"><v-icon size="18">mdi-delete</v-icon>{{ t('common.delete') }}</button>
          </div>
        </v-menu>
      </template>
    </VibrantDataTable>

    <!-- No `v-if` on the panel: the lazy v-dialog owns the mount/unmount so the
         content tears down after the close transition (avoids orphaning the
         header). See GroupListView for the same fix. -->
    <LjDialog v-model="editDialog" :title="editingId ? `${t('company.detailsTitle')} ${editingId}` : t('company.new')" :icon="editingId ? 'mdi-eye-outline' : 'mdi-office-building'" :max-width="600">
      <CompanyEditPanel :key="editingId ?? 'new'" :company-id="editingId" @saved="onEditSaved" @deleted="onEditDeleted" @cancel="editDialog = false" />
    </LjDialog>

    <LigojConfirmDialog v-model="deleteDialog" :title="t('company.deleteTitle')" :icon="TYPE_ICONS.COMPANY" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting"
      @confirm="confirmDelete">
      {{ t('company.deleteConfirmBefore') }}<strong class="text-error">{{ deleteTarget?.name }}</strong>{{ t('company.deleteConfirmAfter') }}
    </LigojConfirmDialog>
    <LigojConfirmDialog v-model="bulkDeleteDialog" :title="t('common.bulkDeleteTitle')" :icon="TYPE_ICONS.COMPANY" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting"
      @confirm="confirmBulkDelete">
      {{ t('common.bulkDeleteConfirmBefore') }}<strong class="text-error">{{ selected.length }}</strong>{{ t('common.bulkDeleteConfirmAfter') }}
    </LigojConfirmDialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useDataTable, useApi, useAppStore, useErrorStore, useI18nStore } from '@ligoj/host'
import { TYPE_ICONS } from '../composables/delegateTypes.js'
import { VibrantDataTable, VibrantConfirmDialog as LigojConfirmDialog, LjPageHeader, LjButton, LjSearch, LjDialog, LjStatus } from '@ligoj/host'
import CompanyEditPanel from '../components/CompanyEditPanel.vue'

const route = useRoute()
const appStore = useAppStore()
const api = useApi()
const errorStore = useErrorStore()
const i18n = useI18nStore()
const t = i18n.t

const DEMO_COMPANIES = [
  { name: 'Ligoj', scope: 'Internal', count: 12, locked: false },
  { name: 'AcmeCorp', scope: 'External', count: 5, locked: false },
  { name: 'TechSolutions', scope: 'External', count: 3, locked: false },
]
const dt = useDataTable('service/id/company', { defaultSort: 'name', demoData: DEMO_COMPANIES })
let searchTimeout = null
let lastOptions = { page: 1, itemsPerPage: 25, sortBy: [] }

const selected = ref([])
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)
const bulkDeleteDialog = ref(false)
const editDialog = ref(false)
const editingId = ref(null)

const headers = computed(() => [
  { title: t('common.name'), label: t('common.name'), key: 'name', sortable: true },
  { label: t('group.scope'), key: 'scope' },
  { label: t('group.members'), key: 'count', align: 'center', width: '110px' },
  { label: t('group.locked'), key: 'locked', align: 'center', width: '90px', exportValue: (r) => (r.locked ? t('user.statusLocked') : t('user.statusActive')) },
])

function scopeColor(scope) { return /intern/i.test(scope) ? '#2563eb' : '#e6a019' }

function loadData(options) { lastOptions = options; dt.load(options) }
function onSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => dt.load({ page: 1, itemsPerPage: lastOptions.itemsPerPage || 25 }), 300)
}

function openCreate() { editingId.value = null; editDialog.value = true }
function openDetails(name) { editingId.value = name; editDialog.value = true }
function onEditSaved() { editDialog.value = false; editingId.value = null; dt.load(lastOptions) }
function onEditDeleted() { editDialog.value = false; editingId.value = null; dt.load(lastOptions) }

function startDelete(item) { deleteTarget.value = item; deleteDialog.value = true }
async function confirmDelete() {
  if (dt.demoMode.value) { errorStore.push({ message: t('company.demoDelete'), status: 0 }); deleteDialog.value = false; return }
  deleting.value = true
  await api.del(`rest/service/id/company/${deleteTarget.value.name}`)
  deleting.value = false; deleteDialog.value = false; deleteTarget.value = null
  dt.load(lastOptions)
}
function startBulkDelete() { bulkDeleteDialog.value = true }
async function confirmBulkDelete() {
  if (dt.demoMode.value) { errorStore.push({ message: t('company.demoDelete'), status: 0 }); bulkDeleteDialog.value = false; return }
  deleting.value = true
  for (const name of selected.value) await api.del(`rest/service/id/company/${name}`)
  deleting.value = false; bulkDeleteDialog.value = false; selected.value = []
  dt.load(lastOptions)
}

onMounted(() => {
  appStore.setBreadcrumbs(
    [{ title: t('nav.home'), to: '/' }, { title: t('nav.identity') }, { title: t('company.title') }],
    { refresh: () => dt.load(lastOptions) },
  )
  const id = route.params?.id
  if (id === 'new' || route.path?.endsWith('/company/new')) openCreate()
  else if (id) openDetails(String(id))
})
</script>

<style scoped>
/* View-specific cells only — all chrome lives in the shared host components
   (LjPageHeader / LjButton / LjSearch / LjDialog) and the global
   `.lj-surface` / `.lj-iconbtn` / `.lj-popmenu` classes. The `--mono` /
   `--ink-*` vars these cells read are supplied by `.lj-surface` on the root. */
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

.cname {
  font-weight: 600;
}

.mono {
  font-family: var(--mono);
  font-size: 13px;
  font-weight: 600;
}

.tagdot {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 500;
}

.tagdot .d {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.dash {
  color: var(--ink-3);
}
</style>