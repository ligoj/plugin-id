<template>
  <!-- Shared "Create another" footer toggle for the id create dialogs
       (user / company / group / delegate). Each host renders it ONLY in
       create mode (`v-if="!isEdit"`); when checked, the host keeps the
       dialog open with a fresh form after a successful create so several
       entries can be added in a row. Kept as one component so the label,
       hint tooltip and styling are defined once instead of in all four
       dialogs. -->
  <v-checkbox v-model="proxy" density="compact" hide-details color="primary" class="create-another">
    <template #label>
      <span class="ca-label">{{ t('id.createAnother') }}</span>
      <v-icon size="x-small" color="grey" class="ml-1">mdi-help-circle-outline</v-icon>
      <v-tooltip activator="parent" :text="t('id.createAnotherHint')" location="top" max-width="300" />
    </template>
  </v-checkbox>
</template>

<script setup>
import { computed } from 'vue'
import { useI18nStore } from '@ligoj/host'

const props = defineProps({
  // Two-way bound "create another" flag owned by the host dialog.
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const t = useI18nStore().t
const proxy = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
</script>

<style scoped>
/* Sit inline with the footer buttons without stealing their height. */
.create-another { flex: 0 1 auto; }
.create-another :deep(.v-selection-control) { min-height: 0; }
.create-another :deep(.v-label) { opacity: 1; font-size: 13px; font-weight: 600; }
.ca-label { font-family: var(--font, "Bricolage Grotesque", system-ui, sans-serif); }
</style>
