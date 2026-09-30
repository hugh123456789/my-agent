<script setup>
import SquareCard from './SquareCard.vue'

defineProps({ experience: { type: Array, required: true } })
</script>

<template>
  <SquareCard as="section" id="about" class="info-card resume-card" aria-labelledby="resume-title">
    <h2 id="resume-title">工作经历</h2><ul class="experience-list"><li v-for="item in experience" :key="item.company"><span class="experience-marker" aria-hidden="true"></span><div><strong>{{ item.company }}</strong><span>{{ item.role }}</span><time>{{ item.time }}</time></div></li></ul>
  </SquareCard>
</template>
