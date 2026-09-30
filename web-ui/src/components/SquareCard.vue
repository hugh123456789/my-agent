<script setup>
defineProps({
  as: { type: String, default: 'article' },
})
</script>

<template>
  <component :is="as" class="square-card">
    <div v-if="$slots.header || $slots.icon" class="square-card-header">
      <slot name="header" />
      <span v-if="$slots.icon" class="square-card-icon" aria-hidden="true"><slot name="icon" /></span>
    </div>
    <slot />
  </component>
</template>
