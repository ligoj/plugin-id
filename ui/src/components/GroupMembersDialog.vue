<template>
  <!-- Globally-mounted dialog (registered as a host header item in
       `install({...})`). Renders nothing visible in the app bar — the
       header-item slot just keeps the component alive across route
       changes; Vuetify's `<v-dialog>` teleports itself to <body> so it
       overlays everything regardless of where this template sits.

       Opened from anywhere via `useGroupMembersDialog().openFor(name)`
       — GroupListView's row action and the host's subscription-row
       buttons (returned by `service.renderFeatures`) both call it. -->
  <LjDialog v-model="open" :title="`${t('id.group.manageTitle')} ${groupName}`" icon="mdi-account-multiple" :max-width="1100" @after-leave="onAfterLeave">
    <!-- :key forces the Panel to remount when the group changes,
         so its internal data-table state (pagination, search,
         selection) starts fresh for each opened group. The
         :v-if-on-open avoids any backend round-trip while the
         dialog is dismissed. -->
    <GroupMembersPanel v-if="open && groupName" :key="groupName" :group-name="groupName" @changed="onChanged" />
  </LjDialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18nStore } from '@ligoj/host'
import GroupMembersPanel from './GroupMembersPanel.vue'
import { useGroupMembersDialog } from '../composables/useGroupMembersDialog.js'
import { LjDialog } from '@ligoj/host'

const { t } = useI18nStore()
const { state, close } = useGroupMembersDialog()

const groupName = computed(() => state.value.group)
const open = computed({
  get: () => state.value.open,
  set: (v) => { if (!v) close() },
})

/**
 * Tracks whether the user added or removed at least one member
 * during the current session. Flipped to true by the Panel's
 * `changed` event; consumed by `onAfterLeave` to decide whether to
 * fire the caller's `onChanged` callback. Reset whenever a new
 * `openFor` lands so a fresh session starts clean.
 */
const dirty = ref(false)

/**
 * Snapshot of `state.onChanged` taken when the dialog opens. The
 * composable's `close()` resets `state.onChanged` synchronously, but
 * Vuetify's `@after-leave` runs AFTER the exit transition — by which
 * time the state reference would already be null. Holding a local
 * reference dodges that race.
 */
let pendingOnChanged = null

watch(() => state.value.open, (isOpen) => {
  if (isOpen) {
    dirty.value = false
    pendingOnChanged = state.value.onChanged
  }
})

function onChanged() {
  dirty.value = true
}

function onAfterLeave() {
  if (dirty.value && typeof pendingOnChanged === 'function') {
    try { pendingOnChanged() }
    // Swallow caller errors — we don't want a bad refresh handler to
    // strand the dialog state.
    catch (err) { console.warn('[plugin-id] group-members onChanged threw', err) }
  }
  dirty.value = false
  pendingOnChanged = null
}
</script>

<!-- All dialog chrome (card, header icon tile, close button, body padding)
     now comes from <LjDialog> + the global `.lj-surface` on its card; this
     wrapper carries no view-specific CSS of its own. -->
