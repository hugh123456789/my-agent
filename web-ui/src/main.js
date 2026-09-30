import { createApp } from 'vue'
import '@fontsource/maple-mono/400.css'
import '@fontsource/maple-mono/500.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router/index.js'

createApp(App).use(router).use(ElementPlus).mount('#app')
