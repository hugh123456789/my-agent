<script setup>
import { ref } from 'vue'
import { validateEmail } from '../homeState.js'
import { articles } from '../articleData.js'
import ArticleList from '../components/ArticleList.vue'
import HeroSection from '../components/HeroSection.vue'
import NewsletterCard from '../components/NewsletterCard.vue'
import ResumeCard from '../components/ResumeCard.vue'

const experience = [
  { company: '独立工作室', role: '创始人 / 产品设计师', time: '2023 - 至今' },
  { company: '远方科技', role: '高级前端工程师', time: '2020 - 2023' },
  { company: '像素工坊', role: '前端工程师', time: '2018 - 2020' },
]
const email = ref('')
const subscribed = ref(false)
const emailError = ref('')

function submitNewsletter() {
  if (!validateEmail(email.value)) {
    emailError.value = '请输入有效的邮箱地址'
    return
  }
  emailError.value = ''
  subscribed.value = true
}
</script>

<template>
  <main>
    <HeroSection />
    <section class="content-grid content-width">
      <ArticleList :articles="articles" />
      <aside class="aside-column">
        <NewsletterCard v-model:email="email" :subscribed="subscribed" :email-error="emailError" @submit="submitNewsletter" />
        <ResumeCard :experience="experience" />
      </aside>
    </section>
  </main>
</template>
