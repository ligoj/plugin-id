<template>
  <div>
    <v-skeleton-loader v-if="loading" type="card, actions" max-width="700" class="mb-4" />

    <v-card v-if="!loading" class="edit-card">
      <v-card-text>
        <v-form ref="formRef" @submit.prevent="save">
          <v-text-field v-model="form.receiver" :label="t('delegate.receiver')" :rules="[rules.required]" variant="outlined" class="mb-2" />
          <v-select v-model="form.receiverType" :label="t('delegate.receiverType')" :items="receiverTypes" :item-title="typeTitle" item-value="value" :prepend-inner-icon="receiverIcon" :rules="[rules.required]" variant="outlined" class="mb-2">
            <template #item="{ props: itemProps, item }">
              <v-list-item v-bind="itemProps">
                <template #prepend>
                  <v-icon :icon="TYPE_ICONS[item?.value] || ''" />
                </template>
              </v-list-item>
            </template>
          </v-select>
          <v-text-field v-model="form.name" :label="t('delegate.resource')" :rules="[rules.required]" :hint="t('delegate.resourceHint')" persistent-hint variant="outlined" class="mb-2" />
          <v-select v-model="form.type" :label="t('delegate.type')" :items="resourceTypes" :item-title="typeTitle" item-value="value" :prepend-inner-icon="typeIcon" :rules="[rules.required]" variant="outlined" class="mb-2">
            <template #item="{ props: itemProps, item }">
              <v-list-item v-bind="itemProps">
                <template #prepend>
                  <v-icon :icon="TYPE_ICONS[item?.value] || ''" />
                </template>
              </v-list-item>
            </template>
          </v-select>
          <v-checkbox v-model="form.canAdmin" :label="t('delegate.admin')" hide-details class="mb-2" />
          <v-checkbox v-model="form.canWrite" :label="t('delegate.write')" hide-details class="mb-2" />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-btn v-if="isEdit" color="error" variant="tonal" @click="confirmDelete = true">
          <v-icon start>mdi-delete</v-icon> {{ t('common.delete') }}
        </v-btn>
        <v-spacer />
        <v-btn variant="text" @click="router.push('/id/delegate')">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" variant="elevated" :loading="saving" @click="save">
          <v-icon start>mdi-content-save</v-icon> {{ t('common.save') }}
        </v-btn>
      </v-card-actions>
    </v-card>

    <v-dialog v-model="confirmDelete" max-width="400">
      <v-card>
        <v-card-title>{{ t('delegate.deleteTitle') }}</v-card-title>
        <v-card-text>
          {{ t('delegate.deleteConfirm', { name: form.receiver }) }}
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="confirmDelete = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" variant="elevated" :loading="deleting" @click="remove">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="showGuardDialog" max-width="400">
      <v-card>
        <v-card-title>{{ t('common.unsavedTitle') }}</v-card-title>
        <v-card-text>{{ t('common.unsavedMsg') }}</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="cancelLeave">{{ t('common.cancel') }}</v-btn>
          <v-btn color="warning" variant="elevated" @click="confirmLeave">{{ t('common.discard') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useApi, useFormGuard, useAppStore, useI18nStore } from '@ligoj/host'

const route = useRoute()
const router = useRouter()
const api = useApi()
const appStore = useAppStore()
const i18n = useI18nStore()
const t = i18n.t

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const confirmDelete = ref(false)

const isEdit = computed(() => !!route.params.id)

// Static icon map keyed by the raw enum value. Const at module scope so
// it is never re-created reactively — referenced from both the
// prepend-inner-icon computed (selected value) and the #item slot
// (dropdown rows).
const TYPE_ICONS = {
  USER: 'mdi-account',
  GROUP: 'mdi-account-group',
  COMPANY: 'mdi-domain',
  TREE: 'mdi-file-tree',
}

// Items as plain objects with the raw enum value + an i18n key. v-model
// still holds the value, item-value="value" wires it back. The earlier
// attempt at adding icons via the #selection slot (with item.raw.icon)
// triggered "Maximum recursive updates exceeded" inside v-select — this
// version keeps only the #item slot and renders the selected icon via
// prepend-inner-icon, which sidesteps that loop.
const receiverTypes = [
  { value: 'USER', titleKey: 'delegate.type.user' },
  { value: 'GROUP', titleKey: 'delegate.type.group' },
  { value: 'COMPANY', titleKey: 'delegate.type.company' },
]
const resourceTypes = [
  { value: 'USER', titleKey: 'delegate.type.user' },
  { value: 'GROUP', titleKey: 'delegate.type.group' },
  { value: 'COMPANY', titleKey: 'delegate.type.company' },
  { value: 'TREE', titleKey: 'delegate.type.tree' },
]

/** v-select item-title callback: resolves the i18n key from the item object. */
function typeTitle(item) {
  return t(item.titleKey)
}

const form = ref({
  receiver: '',
  receiverType: 'USER',
  name: '',
  type: 'GROUP',
  canAdmin: false,
  canWrite: false,
})

// Icon shown inside the field, driven by the currently selected value.
const receiverIcon = computed(() => TYPE_ICONS[form.value.receiverType] || '')
const typeIcon = computed(() => TYPE_ICONS[form.value.type] || '')

const { showGuardDialog, confirmLeave, cancelLeave, markClean, init: initGuard } = useFormGuard(form)

const rules = {
  required: v => !!v || t('common.required'),
}

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    const data = await api.get(`rest/security/delegate/${route.params.id}`)
    if (data) {
      form.value.receiver = data.receiver?.id || data.receiver || ''
      // Normalize to the uppercase enum form used by the v-select items.
      // The backend stores some delegates with lowercase values ("company",
      // "tree", …) and v-model would otherwise mismatch every item, locking
      // the select in a "Maximum recursive updates exceeded" loop.
      form.value.receiverType = (data.receiverType || 'USER').toUpperCase()
      form.value.name = data.name || ''
      form.value.type = (data.type || 'GROUP').toUpperCase()
      form.value.canAdmin = !!data.canAdmin
      form.value.canWrite = !!data.canWrite
    }
    loading.value = false
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('delegate.title'), to: '/id/delegate' },
      { title: form.value.receiver || t('delegate.edit') },
    ])
  } else {
    appStore.setBreadcrumbs([
      { title: t('nav.home'), to: '/' },
      { title: t('nav.identity') },
      { title: t('delegate.title'), to: '/id/delegate' },
      { title: t('delegate.new') },
    ])
  }
  initGuard()
})

async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  saving.value = true
  const payload = {
    receiver: form.value.receiver,
    receiverType: form.value.receiverType,
    name: form.value.name,
    type: form.value.type,
    canAdmin: form.value.canAdmin,
    canWrite: form.value.canWrite,
  }

  if (isEdit.value) {
    await api.put('rest/security/delegate', { id: Number(route.params.id), ...payload })
  } else {
    await api.post('rest/security/delegate', payload)
  }
  saving.value = false
  markClean()
  router.push('/id/delegate')
}

async function remove() {
  deleting.value = true
  await api.del(`rest/security/delegate/${route.params.id}`)
  deleting.value = false
  confirmDelete.value = false
  markClean()
  router.push('/id/delegate')
}
</script>

<style scoped>
.edit-card {
  max-width: 700px;
}

@media (max-width: 600px) {
  .edit-card {
    max-width: 100%;
  }
}
</style>
