<template>
  <div>
    <v-skeleton-loader v-if="loadingSubscription && !subscription" type="card, table" class="mb-4" />

    <template v-if="subscription">
      <!-- Header. Shows project + tool + selected group like the legacy
           `id.html` fieldset legend, plus a back button to ProjectDetail. -->
      <div class="d-flex align-start flex-wrap ga-2 mb-4">
        <div>
          <h1 class="text-h5 d-flex align-center ga-2">
            <v-icon>mdi-account-group</v-icon>
            <span>{{ groupName || t('id.group.unknown') }}</span>
          </h1>
          <p class="text-body-2 text-medium-emphasis mt-1">
            {{ t('id.group.subtitle') }}
            <code v-if="subscription.node?.id" class="ml-1">{{ subscription.node.id }}</code>
          </p>
        </div>
        <v-spacer />
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="projectHref">
          {{ t('common.cancel') }}
        </v-btn>
      </div>

      <!-- Add-member toolbar. Server-side autocomplete; submit (PUT)
           reloads the table. -->
      <v-card variant="tonal" class="mb-4">
        <v-card-text class="d-flex flex-wrap align-center ga-2">
          <v-autocomplete v-model="newMember" v-model:search="searchTerm" :label="t('id.group.addPlaceholder')" :items="searchResults" item-title="label" item-value="id" :loading="searching" no-filter
            clearable return-object="false" variant="outlined" density="compact" hide-details style="min-width: 320px; flex: 1 1 320px" @update:search="onSearch" @update:menu="onSearchMenu" />
          <v-btn color="primary" prepend-icon="mdi-account-plus" :disabled="!newMember || !groupName" :loading="adding" @click="addMember">
            {{ t('id.group.add') }}
          </v-btn>
        </v-card-text>
      </v-card>

      <v-text-field v-model="dt.search.value" prepend-inner-icon="mdi-magnify" :label="t('common.search')" variant="outlined" density="compact" hide-details class="search-field mb-4"
        @update:model-value="onMemberSearch" />

      <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4">
        {{ dt.error.value === 'internal' ? t('user.noProviderMsg') : dt.error.value }}
      </v-alert>

      <v-skeleton-loader v-if="dt.loading.value && dt.items.value.length === 0" type="table-heading, table-row@5" class="mb-4" />

      <LigojDataTableServer v-if="!dt.error.value" v-show="dt.items.value.length > 0 || !dt.loading.value" filename="members.csv" :fetch-all="dt.loadAll" v-model:items-per-page="itemsPerPage"
        :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value" item-value="id" hover @update:options="loadData">
        <template #item.mails="{ item }">
          {{ item.mails?.[0] || '' }}
        </template>
        <template #item.groups="{ item }">
          <div class="groups-cell">
            <v-chip v-for="g in (item.groups || []).slice(0, 3)" :key="g.name || g" size="small" class="mr-1">
              {{ g.name || g }}
            </v-chip>
            <span v-if="(item.groups || []).length > 3" class="text-caption text-medium-emphasis">
              +{{ item.groups.length - 3 }}
            </span>
          </div>
        </template>
        <template #item.actions="{ item }">
          <v-btn v-if="canRemove(item)" icon size="small" variant="text" color="error" :title="t('id.group.removeTitle')" @click.stop="startRemove(item)">
            <v-icon size="small">mdi-account-minus</v-icon>
          </v-btn>
          <v-tooltip v-else-if="isTransitive(item)" :text="t('id.group.transitive')" location="top">
            <template #activator="{ props: tt }">
              <v-icon v-bind="tt" size="small" color="info">mdi-information-outline</v-icon>
            </template>
          </v-tooltip>
        </template>
      </LigojDataTableServer>
    </template>

    <!-- Chantier D2: the user being removed is rendered in bold red
         via the default slot so the action carries the same visual
         weight as the delete dialogs across the rest of the screen. -->
    <LigojConfirmDialog v-model="removeDialog" :title="t('id.group.removeTitle')" :confirm-label="t('common.remove')" confirm-color="error" :loading="removing" @confirm="confirmRemove">
      {{ t('id.group.removeConfirmBefore') }}<strong class="text-error">{{ removeTarget?.id }}</strong>{{ t('id.group.removeConfirmAfter', { group: groupName }) }}
    </LigojConfirmDialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  useApi,
  useAppStore,
  useDataTable,
  useErrorStore,
  useI18nStore,
  LigojDataTableServer,
  LigojConfirmDialog,
} from '@ligoj/host'

const route = useRoute()
const api = useApi()
const appStore = useAppStore()
const errorStore = useErrorStore()
const { t } = useI18nStore()

const subscription = ref(null)
const loadingSubscription = ref(false)

const groupName = computed(() => subscription.value?.parameters?.['service:id:group'] || '')
const projectId = computed(() => subscription.value?.project?.id ?? subscription.value?.project ?? null)
const projectHref = computed(() =>
  projectId.value ? `/home/project/${projectId.value}` : '/home/project',
)

const itemsPerPage = ref(25)
let lastOptions = {}
let searchTimer = null

// Subscription-scoped dataset: every fetch carries the current group
// as an extra query parameter so the backend filters server-side
// (mirrors legacy `?group=...`). Plain useDataTable can't inject
// per-call query, so we do it via `extraParams` if the helper supports
// it — otherwise via dt.load(options) augmentation below.
const dt = useDataTable('service/id/user', { defaultSort: 'id' })

const headers = computed(() => [
  { title: t('user.login'), key: 'id', sortable: true, width: '160px' },
  { title: t('user.firstName'), key: 'firstName', sortable: true },
  { title: t('user.lastName'), key: 'lastName', sortable: true },
  { title: t('user.company'), key: 'company', sortable: true },
  { title: t('user.emails'), key: 'mails', sortable: false },
  { title: t('user.groups'), key: 'groups', sortable: false },
  { title: '', key: 'actions', sortable: false, width: '60px', align: 'end' },
])

function loadData(options) {
  lastOptions = options
  // useDataTable forwards `options` to the search endpoint. We inject
  // the group as a Vuetify-friendly `filter` field so the request URL
  // ends with `?group=<group>` — matches the legacy
  // `service/id/user?group=...` shape on the backend.
  dt.load({ ...options, params: { group: groupName.value } })
}

function onMemberSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(
    () => dt.load({ page: 1, itemsPerPage: itemsPerPage.value, params: { group: groupName.value } }),
    300,
  )
}

/* ---- Add member: autocomplete ----------------------------------- */

const newMember = ref(null)
const searchTerm = ref('')
const searchResults = ref([])
const searching = ref(false)
let userSearchTimer = null

async function fetchUsers(q) {
  const query = (q || '').trim()
  // Skip the round-trip when the field is empty AND we already have
  // items — keeps things quiet on focus toggles after a pick.
  const qp = query ? `q=${encodeURIComponent(query)}&` : ''
  const data = await api.get(`rest/service/id/user?${qp}rows=20`)
  const rows = Array.isArray(data) ? data : (data?.data || [])
  return rows.map((r) => ({
    id: r.id,
    label: [r.id, [r.firstName, r.lastName].filter(Boolean).join(' ')].filter(Boolean).join(' — '),
  }))
}

async function loadUserSuggestions() {
  searching.value = true
  try { searchResults.value = await fetchUsers(searchTerm.value) }
  finally { searching.value = false }
}

function onSearch(q) {
  // Ignore the echo Vuetify emits after a pick mirrors the label into
  // the search input.
  if (searchResults.value.some((i) => i.label === q)) return
  clearTimeout(userSearchTimer)
  userSearchTimer = setTimeout(loadUserSuggestions, 250)
}

function onSearchMenu(open) {
  if (open && searchResults.value.length === 0) loadUserSuggestions()
}

const adding = ref(false)
async function addMember() {
  if (!newMember.value || !groupName.value) return
  adding.value = true
  try {
    const ok = await api.put(
      `rest/service/id/user/${encodeURIComponent(newMember.value)}/group/${encodeURIComponent(groupName.value)}`,
    )
    if (ok !== false) {
      errorStore.success(t('id.group.addedToast', { user: newMember.value, group: groupName.value }))
      newMember.value = null
      searchTerm.value = ''
      dt.load(lastOptions)
    }
  } finally {
    adding.value = false
  }
}

/* ---- Remove member --------------------------------------------- */

const removeDialog = ref(false)
const removeTarget = ref(null)
const removing = ref(false)

function canRemove(item) {
  if (!item?.canWriteGroups) return false
  // Direct membership — the user IS in the current group, not via a
  // transitive ownership only.
  return (item.groups || []).some(
    (g) => (g.name || g)?.toLowerCase?.() === groupName.value.toLowerCase(),
  )
}

function isTransitive(item) {
  return !!item?.canWriteGroups && !canRemove(item)
}

function startRemove(item) {
  removeTarget.value = item
  removeDialog.value = true
}

async function confirmRemove() {
  if (!removeTarget.value) return
  removing.value = true
  try {
    await api.del(
      `rest/service/id/user/${encodeURIComponent(removeTarget.value.id)}/group/${encodeURIComponent(groupName.value)}`,
    )
    errorStore.success(t('id.group.removedToast', { user: removeTarget.value.id, group: groupName.value }))
    removeDialog.value = false
    removeTarget.value = null
    dt.load(lastOptions)
  } finally {
    removing.value = false
  }
}

/* ---- Bootstrap ------------------------------------------------- */

async function loadSubscription() {
  loadingSubscription.value = true
  try {
    const data = await api.get(`rest/subscription/${encodeURIComponent(route.params.id)}`)
    subscription.value = data || null
  } finally {
    loadingSubscription.value = false
  }
}

onMounted(async () => {
  await loadSubscription()
  appStore.setBreadcrumbs(
    [
      { title: t('nav.home'), to: '/' },
      { title: t('nav.projects'), to: '/home/project' },
      ...(projectId.value
        ? [{ title: String(projectId.value), to: projectHref.value }]
        : []),
      { title: groupName.value || t('id.group.unknown') },
    ],
    { refresh: () => dt.load(lastOptions) },
  )
  // Defer initial load until the group is known.
  if (groupName.value) {
    dt.load({ page: 1, itemsPerPage: itemsPerPage.value, params: { group: groupName.value } })
  }
})

// If the route's `:id` changes (navigation between subscriptions),
// re-bootstrap. Edge case — keeps the SPA snappy.
watch(() => route.params.id, async (id) => {
  if (id) {
    await loadSubscription()
    if (groupName.value) {
      dt.load({ page: 1, itemsPerPage: itemsPerPage.value, params: { group: groupName.value } })
    }
  }
})
</script>

<style scoped>
.search-field {
  max-width: 320px;
}

.groups-cell {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
