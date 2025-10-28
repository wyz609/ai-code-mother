import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import '@/access'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 配置 Ant Design Vue，提高 CSS 优先级（移除 :where 选择器）
app.use(Antd, {
  theme: {
    hashed: false, // 禁用 CSS-in-JS 的 hash，提高样式优先级
  },
})

app.mount('#app')
