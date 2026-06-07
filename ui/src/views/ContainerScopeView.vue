<!--
  ScopesView — 2026 "Vibrant" Container scopes (plugin-id ContainerScopeView).
  Tabs (group / company), small client-side dataset fed to VibrantDataTable,
  inline create/edit dialog + VibrantConfirmDialog. Endpoint:
  rest/service/id/container-scope/{group|company}.

  Chrome (header, tabs, search, primary button, dialog, row menu) comes from
  the shared host components (LjPageHeader / LjSegmented / LjSearch / LjButton
  / LjDialog) + the `.lj-surface` token class — so this view carries only its
  own cell styling, not the repeated 2026 boilerplate.
-->
<template>
  <div class="scopes lj-surface">
    <LjPageHeader :title="t('containerScope.title')" :subtitle="t('containerScope.subtitle2026')">
      <template #actions>
        <LjButton icon="mdi-plus" @click="openNew">{{ t('containerScope.new') }}</LjButton>
      </template>
    </LjPageHeader>

    <div class="toolbar">
      <LjSegmented v-model="activeTab" :options="tabs" />
      <LjSearch v-model="search" :placeholder="t('common.search')" />
    </div>

    <v-alert v-if="error" type="warning" variant="tonal" class="mb-4" rounded="lg">{{ t('containerScope.noProvider') }}</v-alert>
    <v-alert v-if="demoMode" type="info" variant="tonal" density="compact" class="mb-4" rounded="lg">{{ t('containerScope.demoMode') }}</v-alert>

    <VibrantDataTable v-if="!error" :headers="headers" :items="filteredItems" :items-length="filteredItems.length" :loading="loading"
      item-value="id" filename="container-scopes.csv" @row-click="openEdit">
      <template #cell.name="{ item }">
        <span class="sname"><v-icon size="16" class="sname-ic">mdi-file-tree-outline</v-icon><span>{{ item.name }}</span></span>
      </template>
      <template #cell.dn="{ item }"><code class="dn">{{ item.dn || '—' }}</code></template>
      <template #cell.locked="{ item }">
        <v-tooltip v-if="item.locked" :text="t('user.statusLocked')" location="top">
          <template #activator="{ props: tt }"><v-icon v-bind="tt" color="warning" size="19">mdi-lock</v-icon></template>
        </v-tooltip>
        <span v-else class="dash">—</span>
      </template>
      <template #actions="{ item }">
        <v-menu location="bottom end">
          <template #activator="{ props }">
            <button class="lj-iconbtn" v-bind="props" :aria-label="t('common.edit')" @click.stop><v-icon size="18">mdi-cog</v-icon></button>
          </template>
          <div class="lj-popmenu">
            <button @click="openEdit(item)"><v-icon size="18">mdi-pencil</v-icon>{{ t('common.edit') }}</button>
            <div class="sep" />
            <button class="danger" :disabled="item.locked" @click="!item.locked && startDelete(item)"><v-icon size="18">mdi-delete</v-icon>{{ t('common.delete') }}</button>
          </div>
        </v-menu>
      </template>
    </VibrantDataTable>

    <!-- Create / edit dialog (shared chrome). -->
    <LjDialog v-model="editDialog" :title="editTarget?.id ? t('containerScope.edit') : t('containerScope.new')" :icon="TYPE_ICONS.SCOPE" :max-width="520">
      <v-form ref="formRef" @submit.prevent="save">
        <v-text-field v-model="editForm.name" prepend-inner-icon="mdi-form-textbox" :label="t('common.name')" :rules="[rules.required]" variant="outlined" class="mb-2" autofocus
          :error-messages="nameStatus === 'taken' ? t('id.availability.taken') : ''">
          <template #append-inner>
            <v-progress-circular v-if="nameStatus === 'checking'" size="18" width="2" indeterminate />
            <v-icon v-else-if="nameStatus === 'available'" color="success">mdi-check-circle</v-icon>
            <v-icon v-else-if="nameStatus === 'taken'" color="error">mdi-alert-circle</v-icon>
          </template>
        </v-text-field>
        <v-text-field v-if="editTarget?.id" v-model="editForm.dn" prepend-inner-icon="mdi-file-tree-outline" :label="t('containerScope.dn')" variant="outlined" disabled />
      </v-form>
      <template #footer>
        <LjButton variant="ghost" @click="editDialog = false">{{ t('common.cancel') }}</LjButton>
        <LjButton icon="mdi-content-save" :loading="saving" @click="save">{{ t('common.save') }}</LjButton>
      </template>
    </LjDialog>

    <LigojConfirmDialog v-model="deleteDialog" :title="t('containerScope.deleteTitle')" :icon="TYPE_ICONS.SCOPE" :confirm-label="t('common.delete')" confirm-color="error" :loading="deleting" @confirm="confirmDelete">
      {{ t('containerScope.deleteConfirmBefore') }}<strong class="text-error">{{ deleteTarget?.name }}</strong>{{ t('containerScope.deleteConfirmAfter') }}
    </LigojConfirmDialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useApi, useAppStore, useErrorStore, useI18nStore } from '@ligoj/host'
import { TYPE_ICONS } from '../composables/delegateTypes.js'
import { useAvailability, existsByExact } from '../composables/useAvailability.js'
import { VibrantDataTable, VibrantConfirmDialog as LigojConfirmDialog, LjPageHeader, LjButton, LjSearch, LjSegmented, LjDialog } from '@ligoj/host'

const api = useApi()
const appStore = useAppStore()
const errorStore = useErrorStore()
const i18n = useI18nStore()
const t = i18n.t

const activeTab = ref('group')
const tabs = computed(() => [
  { value: 'group', icon: 'mdi-account-group', label: t('nav.groups') },
  { value: 'company', icon: 'mdi-domain', label: t('nav.companies') },
])

const DEMO_GROUP_SCOPES = [
  { id: 1, name: 'Department', dn: 'ou=Department,dc=demo,dc=com', locked: false },
  { id: 2, name: 'Team', dn: 'ou=Team,dc=demo,dc=com', locked: false },
  { id: 3, name: 'Project', dn: 'ou=Project,dc=demo,dc=com', locked: true },
]
const DEMO_COMPANY_SCOPES = [
  { id: 1, name: 'Organization', dn: 'ou=Organization,dc=demo,dc=com', locked: false },
  { id: 2, name: 'Business Unit', dn: 'ou=BusinessUnit,dc=demo,dc=com', locked: true },
]

const items = ref([])
const loading = ref(false)
const error = ref(null)
const demoMode = ref(false)
const search = ref('')

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((s) => (s.name || '').toLowerCase().includes(q) || (s.dn || '').toLowerCase().includes(q))
})

const headers = computed(() => [
  { label: t('common.name'), key: 'name' },
  { label: t('containerScope.dn'), key: 'dn' },
  { label: t('common.status'), key: 'locked', align: 'center', width: '90px', exportValue: (r) => (r.locked ? t('user.statusLocked') : t('user.statusActive')) },
])

const formRef = ref(null)
const editDialog = ref(false)
const editTarget = ref(null)
const editForm = ref({ name: '', dn: '' })
const saving = ref(false)
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)

const rules = { required: (v) => !!v || t('common.required') }

// Live scope-name availability check (create mode only), against the active
// tab's container-scope list.
const { status: nameStatus } = useAvailability(
  () => editForm.value.name,
  (v) => existsByExact(api, `service/id/container-scope/${activeTab.value}`, v, 'name'),
  { enabled: () => !editTarget.value?.id && !demoMode.value },
)

async function loadData() {
  loading.value = true
  error.value = null
  try {
    const data = await api.get(`rest/service/id/container-scope/${activeTab.value}`)
    if (data && !data.code) {
      items.value = Array.isArray(data) ? data : (data.data || [])
      demoMode.value = false
    } else {
      demoMode.value = true
      errorStore.clear()
      items.value = activeTab.value === 'group' ? DEMO_GROUP_SCOPES : DEMO_COMPANY_SCOPES
    }
  } catch {
    demoMode.value = true
    errorStore.clear()
    items.value = activeTab.value === 'group' ? DEMO_GROUP_SCOPES : DEMO_COMPANY_SCOPES
  }
  loading.value = false
}

watch(activeTab, () => { search.value = ''; loadData() })

function openNew() { editTarget.value = null; editForm.value = { name: '', dn: '' }; editDialog.value = true }
function openEdit(item) { editTarget.value = item; editForm.value = { name: item.name, dn: item.dn || '' }; editDialog.value = true }
function startDelete(item) { deleteTarget.value = item; deleteDialog.value = true }

async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  if (nameStatus.value === 'taken') return
  if (demoMode.value) { errorStore.push({ message: t('containerScope.demoSave'), status: 0 }); editDialog.value = false; return }
  saving.value = true
  const payload = { name: editForm.value.name }
  if (editTarget.value?.id) {
    await api.put(`rest/service/id/container-scope/${activeTab.value}`, { id: editTarget.value.id, ...payload })
  } else {
    await api.post(`rest/service/id/container-scope/${activeTab.value}`, payload)
  }
  saving.value = false
  editDialog.value = false
  loadData()
}

async function confirmDelete() {
  if (demoMode.value) { errorStore.push({ message: t('containerScope.demoDelete'), status: 0 }); deleteDialog.value = false; return }
  deleting.value = true
  await api.del(`rest/service/id/container-scope/${activeTab.value}/${deleteTarget.value.id}`)
  deleting.value = false
  deleteDialog.value = false
  loadData()
}

onMounted(() => {
  appStore.setBreadcrumbs(
    [{ title: t('nav.home'), to: '/' }, { title: t('nav.identity') }, { title: t('containerScope.title') }],
    { refresh: loadData },
  )
  loadData()
})
</script>

<style scoped>
/* View-specific cells only — all chrome lives in the shared host components
   (LjPageHeader / LjSegmented / LjSearch / LjButton / LjDialog) and the
   global `.lj-surface` / `.lj-iconbtn` / `.lj-popmenu` classes. */
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.sname { display: inline-flex; align-items: center; gap: 8px; font-weight: 600; }
.sname-ic { color: var(--ink-3); }
.dn { font-family: var(--mono); font-size: 12.5px; color: var(--ink-2); }
.dash { color: var(--ink-3); }
</style>
