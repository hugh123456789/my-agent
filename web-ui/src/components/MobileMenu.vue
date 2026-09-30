<script setup>
defineProps({ open: Boolean })
defineEmits(['close'])

const navItems = ['首页', '文章', '项目', '关于']
const anchors = ['home', 'articles', 'projects', 'about']
</script>

<template>
  <div v-if="open" class="mobile-menu">
    <RouterLink v-for="(item, index) in navItems" :key="item" :to="`/${anchors[index] === 'home' ? '' : anchors[index]}`" @click="$emit('close')">{{ item }}</RouterLink>
    <a href="mailto:hello@calicastle.com" @click="$emit('close')">联系我</a>
  </div>
</template>
