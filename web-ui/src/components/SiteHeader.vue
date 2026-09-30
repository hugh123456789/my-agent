<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { BriefcaseBusiness, FileText, House, Menu, Moon, Sun, UserRound, X } from '@lucide/vue'

defineProps({ isDark: Boolean, open: Boolean })
defineEmits(['toggle-theme', 'toggle-menu'])

const navItems = [
  { label: '首页', icon: House },
  { label: '文章', icon: FileText },
  { label: '项目', icon: BriefcaseBusiness },
  { label: '关于', icon: UserRound },
]
const anchors = ['home', 'articles', 'projects', 'about']
const route = useRoute()
const activeIndex = computed(() => {
  const currentAnchor = route.path.split('/')[1] || 'home'
  const index = anchors.indexOf(currentAnchor)
  return index === -1 ? 0 : index
})
const visible = ref(true)
let lastScrollY = 0

function handleScroll() {
  const currentScrollY = window.scrollY
  if (currentScrollY < 24) {
    visible.value = true
  } else if (currentScrollY < lastScrollY - 4) {
    visible.value = true
  } else if (currentScrollY > lastScrollY + 4) {
    visible.value = false
  }
  lastScrollY = currentScrollY
}

onMounted(() => {
  lastScrollY = window.scrollY
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<template>
  <header class="site-header" :class="{ 'is-visible': visible }">
    <nav class="desktop-nav" aria-label="主导航">
      <RouterLink v-for="(item, index) in navItems" :key="item.label" :aria-label="item.label" :to="`/${anchors[index] === 'home' ? '' : anchors[index]}`"><component :is="item.icon" class="nav-icon" :size="16" :stroke-width="1.8" /></RouterLink>
      <span class="nav-indicator" :style="{ '--nav-index': activeIndex }" aria-hidden="true"></span>
    </nav>
    <div class="header-actions">
      <a class="login-link" href="mailto:hello@calicastle.com">联系我</a>
      <button class="theme-button" type="button" aria-label="切换主题" @click="$emit('toggle-theme')"><Sun v-if="isDark" :size="17" :stroke-width="1.8" /><Moon v-else :size="17" :stroke-width="1.8" /></button>
      <button class="menu-button" type="button" aria-label="打开菜单" :aria-expanded="open" @click="$emit('toggle-menu')"><X v-if="open" :size="19" /><Menu v-else :size="19" /></button>
    </div>
  </header>
</template>
