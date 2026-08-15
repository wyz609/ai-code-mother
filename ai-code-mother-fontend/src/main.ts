import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './style.css'

import '@/access'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 配置 Ant Design Vue
app.use(Antd)

app.mount('#app')
