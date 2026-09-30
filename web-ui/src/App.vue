<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { nextTheme } from './homeState.js'
import MobileMenu from './components/MobileMenu.vue'
import SiteFooter from './components/SiteFooter.vue'
import SiteHeader from './components/SiteHeader.vue'

const isDark = ref(false)
const menuOpen = ref(false)
const theme = computed(() => (isDark.value ? 'dark' : 'light'))
function toggleTheme() {
  isDark.value = nextTheme(theme.value) === 'dark'
}

function closeMenuOnEscape(event) {
  if (event.key === 'Escape') menuOpen.value = false
}

onMounted(() => window.addEventListener('keydown', closeMenuOnEscape))
onUnmounted(() => window.removeEventListener('keydown', closeMenuOnEscape))
</script>

<template>
  <div class="site-shell" :class="{ 'theme-dark': isDark }">
    <SiteHeader :is-dark="isDark" :open="menuOpen" @toggle-theme="toggleTheme" @toggle-menu="menuOpen = !menuOpen" />
    <MobileMenu :open="menuOpen" @close="menuOpen = false" />
    <RouterView />
    <SiteFooter />
  </div>
</template>
