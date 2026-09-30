<script setup>
import { ArrowUpRight, Sparkles } from '@lucide/vue'
import SquareCard from './SquareCard.vue'

defineProps({ email: { type: String, required: true }, subscribed: Boolean, emailError: { type: String, default: '' } })
defineEmits(['update:email', 'submit'])
</script>

<template>
  <SquareCard as="section" class="info-card newsletter-card" aria-labelledby="newsletter-title">
    <template #icon><Sparkles :size="17" /></template>
    <span class="section-kicker">STAY IN THE LOOP</span><h2 id="newsletter-title">订阅动态更新</h2><p>偶尔写点东西，分享我的思考、实践和正在做的事。</p>
    <form @submit.prevent="$emit('submit')"><label class="sr-only" for="email">邮箱地址</label><el-input id="email" :model-value="email" type="email" placeholder="你的邮箱地址" :aria-invalid="Boolean(emailError)" :aria-describedby="emailError ? 'email-error' : undefined" @update:model-value="$emit('update:email', $event)" /><el-button native-type="submit" class="newsletter-submit" :class="{ subscribed }">{{ subscribed ? '已订阅' : '订阅' }}<ArrowUpRight :size="15" /></el-button><span v-if="emailError" id="email-error" class="form-error" role="alert">{{ emailError }}</span></form>
  </SquareCard>
</template>
