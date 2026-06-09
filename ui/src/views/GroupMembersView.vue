<template>
  <!-- Routed `/id/subscription/:id` view — kept for direct links from
       legacy bookmarks / external systems. The day-to-day entry path
       is now the global dialog (opened from the subscription row's
       action button or from `GroupListView`), so this view's role is
       reduced to: bootstrap the subscription → mount the same panel
       that the dialog uses. -->
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

      <GroupMembersPanel v-if="groupName" :group-name="groupName" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useApi, useAppStore, useI18nStore } from '@ligoj/host'
import GroupMembersPanel from '../components/GroupMembersPanel.vue'

const route = useRoute()
const api = useApi()
const appStore = useAppStore()
const { t } = useI18nStore()

const subscription = ref(null)
const loadingSubscription = ref(false)

const groupName = computed(() => subscription.value?.parameters?.['service:id:group'] || '')
const projectId = computed(() => subscription.value?.project?.id ?? subscription.value?.project ?? null)
const projectHref = computed(() =>
  projectId.value ? `/home/project/${projectId.value}` : '/home/project',
)

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
  appStore.setBreadcrumbs(() => [
      { title: t('nav.home'), to: '/' },
      { title: t('nav.projects'), to: '/home/project' },
      ...(projectId.value
        ? [{ title: String(projectId.value), to: projectHref.value }]
        : []),
      { title: groupName.value || t('id.group.unknown') },
    ],
    // No refresh callback — the panel owns its data table; refreshing
    // it from the breadcrumb bar would need the panel to expose a
    // public reload(), which isn't worth the wiring for an
    // increasingly-secondary view.
  )
})

// Re-bootstrap when the route's `:id` changes (navigation between
// subscriptions). Edge case — keeps the SPA snappy.
watch(() => route.params.id, async (id) => {
  if (id) await loadSubscription()
})
</script>
