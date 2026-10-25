<template>
  <!-- Member-management panel — group-name-driven, no subscription
       dependency. Used by both:
         - `GroupMembersDialog`   (header-mounted, opened from the
           GroupListView action column or the host's subscription-row
           buttons via `useGroupMembersDialog().openFor(group)`).
         - `GroupMembersView`     (routed `/id/subscription/:id` view —
           bootstraps the subscription then mounts this panel).
       Lifting the body out of the routed view is the deduplication
       that made the dialog cheap: backend API contract is "group name
       → members list", no project / subscription context required. -->
  <div class="gmpanel lj-surface">
    <!-- Add-member bar: server-side autocomplete + CTA. -->
    <div v-if="auth.isAllowedApi(USER_API, 'PUT')" class="addbar">
      <LigojAutocomplete v-model="newMember" v-model:search="searchTerm" :label="t('id.group.addPlaceholder')" :items="searchResults" item-title="label" item-value="id" :loading="searching" no-filter
        clearable variant="outlined" density="comfortable" rounded="lg" hide-details autocomplete="off" class="addsel" prepend-inner-icon="mdi-account-search"
        @update:search="onSearch" @update:menu="onSearchMenu" />
      <LjButton icon="mdi-account-plus" :disabled="!newMember || !groupName" :loading="adding" @click="addMember">{{ t('id.group.add') }}</LjButton>
    </div>

    <LjSearch v-model="dt.search.value" :placeholder="t('common.search')" class="member-search" @input="onMemberSearch" />

    <v-alert v-if="dt.error.value" type="warning" variant="tonal" class="mb-4" rounded="lg">
      {{ dt.error.value === 'internal' ? t('user.noProviderMsg') : dt.error.value }}
    </v-alert>

    <LjDataTable v-if="!dt.error.value" :headers="headers" :items="dt.items.value" :items-length="dt.totalItems.value" :loading="dt.loading.value"
      item-value="id" :default-sort="visualIdColumnKey()" :fetch-all="dt.loadAll" filename="members.csv" @update:options="loadData">
      <template #[`cell.${visualIdColumnKey()}`]="{ item }">
        <span class="login"><v-icon size="16" class="login-ic">mdi-account-circle</v-icon><span class="mono">{{ visualIdValue(item) }}</span></span>
      </template>
      <!-- Emails and groups: cut to the room left by the other columns, the full values in the tooltip -->
      <template #cell.mails="{ item }">
        <span class="cut">
          <template v-if="(item.mails || []).length">
            <span v-for="m in item.mails.slice(0, 2)" :key="m" class="mailchip"><v-icon size="12">mdi-email-outline</v-icon><span class="txt">{{ m }}</span></span>
            <span v-if="item.mails.length > 2" class="more">+{{ item.mails.length - 2 }}</span>
            <v-tooltip activator="parent" location="top"><div class="cell-tip"><div v-for="m in item.mails" :key="m">{{ m }}</div></div></v-tooltip>
          </template>
          <span v-else class="dash">—</span>
        </span>
      </template>
      <template #cell.groups="{ item }">
        <span class="cut">
          <template v-if="(item.groups || []).length">
            <span v-for="g in item.groups.slice(0, 3)" :key="g.name || g" class="chip">{{ g.name || g }}</span>
            <span v-if="item.groups.length > 3" class="more">+{{ item.groups.length - 3 }}</span>
            <v-tooltip activator="parent" location="top">
              <div class="cell-tip chips"><v-chip v-for="g in item.groups" :key="g.name || g" size="x-small" variant="tonal" label>{{ g.name || g }}</v-chip></div>
            </v-tooltip>
          </template>
          <span v-else class="dash">—</span>
        </span>
      </template>
      <template #actions="{ item }">
        <button v-if="canRemove(item) && auth.isAllowedApi(USER_API, 'DELETE')" class="lj-iconbtn remove-danger" :aria-label="t('id.group.removeTitle')" @click.stop="startRemove(item)">
          <v-icon size="18">mdi-account-minus</v-icon>
          <v-tooltip activator="parent" :text="t('id.group.removeTitle')" location="top" />
        </button>
        <v-tooltip v-else-if="isTransitive(item)" :text="t('id.group.transitive')" location="top">
          <template #activator="{ props: tt }">
            <v-icon v-bind="tt" size="18" color="info">mdi-information-outline</v-icon>
          </template>
        </v-tooltip>
      </template>
    </LjDataTable>

    <!-- Confirm dialog. Vuetify teleports it to <body>, so being
         nested inside this panel — which itself can be inside a
         <v-dialog> — doesn't cause z-index issues. -->
    <LigojConfirmDialog v-model="removeDialog" :title="t('id.group.removeTitle')" :icon="TYPE_ICONS.USER" :confirm-label="t('common.remove')" confirm-color="error" :loading="removing" @confirm="confirmRemove">
      {{ t('id.group.removeConfirmBefore') }}<strong class="text-error">{{ removeTarget?.id }}</strong>{{ t('id.group.removeConfirmAfter', { group: groupName }) }}
    </LigojConfirmDialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  useApi,
  useAuthStore,
  useDataTable,
  useErrorStore,
  useI18nStore,
} from '@ligoj/host'
import { visualIdColumnKey, visualIdLabel, visualIdValue } from '../visualId.js'
import { TYPE_ICONS } from '../composables/delegateTypes.js'
import { LjConfirmDialog as LigojConfirmDialog, LjDataTable, LjButton, LjSearch, LigojAutocomplete } from '@ligoj/host'

const props = defineProps({
  /**
   * The LDAP group name to manage. Drives every API call:
   *   - GET    rest/service/id/user?group=<name>     (members table)
   *   - PUT    rest/service/id/user/<u>/group/<name> (add)
   *   - DELETE rest/service/id/user/<u>/group/<name> (remove)
   *
   * Reactive: when this changes the panel reloads. (In the dialog
   * we additionally key the whole panel so internal state resets.)
   */
  groupName: { type: String, required: true },
})

/**
 * `changed` fires after every successful add or remove. The dialog
 * wrapper tracks this to flip a `dirty` flag and invokes the caller's
 * `onChanged` callback when the dialog later closes — letting the
 * GroupListView refresh its member-count column, the host's
 * subscription rows refresh their feature chips, etc., without
 * needing to re-fetch on every individual edit.
 */
const emit = defineEmits(['changed'])

const api = useApi()
const auth = useAuthStore()
// API-permission gates (v-if auth.isAllowedApi) of the actions
const USER_API = 'rest/service/id/user'
const errorStore = useErrorStore()
const { t } = useI18nStore()

const groupName = computed(() => props.groupName)

const itemsPerPage = ref(25)
let lastOptions = {}
let searchTimer = null

// `extraParams` is a function (not a one-shot object) so `groupName`
// stays live: every load() picks up the current group, which matters
// in the routed view where the same panel instance can see groupName
// change. The dialog mounts a fresh panel per group (via :key) so the
// reactive aspect doesn't matter there, but the function form costs
// nothing.
const dt = useDataTable('service/id/user', {
  defaultSort: visualIdColumnKey(),
  extraParams: () => ({ group: groupName.value }),
})

const headers = computed(() => [
  // Visual identifier column: configured attribute (or login), key 'visual-id' maps server-side
  { label: visualIdLabel(), key: visualIdColumnKey(), sortable: true, width: '170px' },
  { label: t('user.firstName'), key: 'firstName', sortable: true },
  { label: t('user.lastName'), key: 'lastName', sortable: true },
  { label: t('user.company'), key: 'company', sortable: true },
  // A share of the room the other columns leave: their cells have no width of their own, see `.cut`
  { label: t('user.emails'), key: 'mails', width: '30%', exportValue: (r) => (r.mails || []).join(' ') },
  { label: t('user.groups'), key: 'groups', width: '22%', exportValue: (r) => (r.groups || []).map((g) => g.name || g).join(' ') },
])

function loadData(options) {
  lastOptions = options
  // `extraParams` injects `?group=...` so we don't need to thread it
  // through here. Previously this passed `params: { group }` which
  // useDataTable silently ignored — that's how the URL ended up
  // missing the group filter.
  dt.load(options)
}

function onMemberSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(
    () => dt.load({ page: 1, itemsPerPage: itemsPerPage.value }),
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
  // Backend criteria is the DataTables-style `search[value]` (UserOrgResource#findAll)
  const data = await api.get(`rest/service/id/user?search[value]=${encodeURIComponent(query)}&rows=20&page=1&sidx=${visualIdColumnKey()}&sord=asc`)
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
      emit('changed', { action: 'add', user: newMember.value, group: groupName.value })
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
    emit('changed', { action: 'remove', user: removeTarget.value.id, group: groupName.value })
    removeDialog.value = false
    removeTarget.value = null
    dt.load(lastOptions)
  } finally {
    removing.value = false
  }
}

/* ---- Bootstrap ------------------------------------------------- */
//
// No initial dt.load() here — the LigojDataTableServer fires
// `@update:options` automatically on first mount, which calls
// `loadData` and runs the first fetch. Calling dt.load() in
// onMounted on top of that produced the double GET the user
// reported. The watcher below still covers the routed-view case
// where the same panel instance sees `groupName` change without a
// fresh mount.

watch(() => groupName.value, (g) => {
  if (g) dt.load(lastOptions)
})
</script>

<style scoped>
/* Chrome (add CTA, member search) now comes from the shared host components
   (LjButton, LjSearch) plus the global lj-surface / lj-iconbtn classes,
   which supply the mono/ink/pill/radius tokens these cells read. Only the
   panel-specific add-bar layout, danger remove button hover, and table cell
   rendering remain. */

/* Add-member bar + search spacing. */
.addbar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-bottom: 14px; }
.addsel { min-width: 320px; flex: 1 1 320px; }
.member-search { margin-bottom: 14px; }

/* Danger variant of the global ghost icon button (member removal). */
.remove-danger.lj-iconbtn:hover { background: rgba(var(--v-theme-error), .1); color: rgb(var(--v-theme-error)); }

/* Cells (shared language with UsersView). */
.login { display: inline-flex; align-items: center; gap: 8px; }
.login-ic { color: var(--ink-3); }
.mono { font-family: var(--mono); font-size: 13px; font-weight: 600; }
/* A cut cell: no width of its own (width 0, min-width 100%), so it fills the room its column gets and never widens
   the table; its chips shrink with an ellipsis, the counter stays whole. */
.cut { display: flex; align-items: center; gap: 5px; width: 0; min-width: 100%; overflow: hidden; }
.mailchip { display: inline-flex; flex: 0 1 auto; min-width: 0; align-items: center; gap: 5px; font-size: 12.5px; font-weight: 600; color: rgba(var(--v-theme-on-surface), .72); background: var(--pill); border: var(--border-w) var(--lj-border-style, solid) var(--border-c); border-radius: var(--radius-sm); padding: 3px 9px; }
.mailchip :deep(.v-icon) { opacity: .6; flex: none; }
.mailchip .txt, .chip { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chip { display: block; flex: 0 1 auto; min-width: 0; font-size: 12px; font-weight: 700; color: rgba(var(--v-theme-on-surface), .72); background: var(--pill); border: var(--border-w) var(--lj-border-style, solid) var(--border-c); border-radius: var(--lj-radius-sm, 20px); padding: 3px 11px; }
.more { flex: none; font-size: 12px; font-weight: 700; color: var(--ink-3); }
.cell-tip { display: grid; gap: 2px; font-size: 12.5px; }
.cell-tip.chips { display: flex; flex-wrap: wrap; gap: 4px; max-width: 360px; }
.dash { color: var(--ink-3); }
</style>
