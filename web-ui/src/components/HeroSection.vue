<script setup>
import { ArrowDownRight, Code2, Mail, MessageCircle } from '@lucide/vue'
import BrandMark from './BrandMark.vue'

</script>

<template>
  <section id="home" class="hero content-width" aria-labelledby="hero-title">
    <div class="hero-avatar-wrap"><BrandMark /></div>
    <div class="hero-copy">
      <p class="eyebrow">HELLO, I'M CALI</p>
      <h1 id="hero-title">开发者，设计师，<br /><span>细节控，创始人。</span></h1>
      <p class="hero-description">用代码和设计，把脑海里的想法变成真实、好用、值得被记住的产品。</p>
      <div class="hero-actions"><a class="hero-link" href="#articles">浏览文章 <ArrowDownRight :size="16" /></a><div class="social-links" aria-label="社交链接"><a href="https://github.com" target="_blank" rel="noreferrer" aria-label="GitHub"><Code2 :size="16" /></a><a href="https://twitter.com" target="_blank" rel="noreferrer" aria-label="Twitter"><MessageCircle :size="16" /></a><a href="mailto:hello@calicastle.com" aria-label="Email"><Mail :size="16" /></a></div></div>
    </div>
    <div class="hero-side-note">PRODUCT / CODE / DETAILS</div>
  </section>
</template>
