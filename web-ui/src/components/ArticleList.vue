<script setup>
import { ArrowUpRight } from '@lucide/vue'
import SquareCard from './SquareCard.vue'

defineProps({ articles: { type: Array, required: true } })
</script>

<template>
  <section id="articles" class="articles-section" aria-labelledby="articles-title">
    <div class="section-heading"><div><span class="section-kicker">01 / NOTES</span><h2 id="articles-title">近期文章</h2></div><RouterLink to="/articles" class="section-link">查看全部 <ArrowUpRight :size="16" /></RouterLink></div>
    <div class="article-list">
      <SquareCard v-for="article in articles" :key="article.title" as="article" class="article-card">
        <img class="article-image" :src="article.image" :alt="article.imageAlt" width="760" height="240" loading="lazy" />
        <template #header><div class="article-meta"><time>{{ article.date }}</time><span class="meta-dot" aria-hidden="true"></span><span>阅读 6 分钟</span></div></template>
        <h3>{{ article.title }}</h3><p>{{ article.excerpt }}</p>
        <div class="tag-list"><span v-for="tag in article.tags" :key="tag" class="tag">{{ tag }}</span></div>
        <RouterLink to="/articles" class="card-arrow" :aria-label="`阅读文章：${article.title}`"><ArrowUpRight :size="17" /></RouterLink>
      </SquareCard>
    </div>
  </section>
</template>
