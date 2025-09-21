<template>
  <a-layout-header class="global-header">
    <div class="header-content">
      <!-- 左侧：Logo和网站标题 -->
      <div class="header-left">
        <div class="logo-section">
          <img src="@/assets/logo.png" alt="Logo" class="logo" />
          <span class="site-title">AI零代码生成平台</span>
        </div>

        <!-- 导航菜单 -->
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          class="header-menu"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </div>

      <!-- 右侧：用户信息 -->
      <div class="header-right">
        <a-button type="primary" @click="handleLogin"> 登录 </a-button>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'

const router = useRouter()
const selectedKeys = ref<string[]>(['home'])

// 菜单配置
const menuItems: MenuProps['items'] = [
  {
    key: 'home',
    label: '首页',
  },
  {
    key: 'about',
    label: '关于',
  },
]

// 菜单点击处理
const handleMenuClick = ({ key }: { key: string }) => {
  selectedKeys.value = [key]

  // 根据菜单项跳转路由
  switch (key) {
    case 'home':
      router.push('/')
      break
    case 'about':
      router.push('/about')
      break
  }
}

// 登录按钮点击处理
const handleLogin = () => {
  console.log('登录功能待实现')
}
</script>

<style scoped>
.global-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
  height: 64px;
  line-height: 64px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.logo-section {
  display: flex;
  align-items: center;
  margin-right: 40px;
}

.logo {
  width: 32px;
  height: 32px;
  margin-right: 12px;
}

.site-title {
  font-size: 20px;
  font-weight: 600;
  color: #1890ff;
  white-space: nowrap;
}

.header-menu {
  flex: 1;
  border-bottom: none;
  background: transparent;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .logo-section {
    margin-right: 20px;
  }

  .site-title {
    font-size: 16px;
  }

  .header-menu {
    display: none;
  }
}

@media (max-width: 480px) {
  .site-title {
    display: none;
  }

  .logo-section {
    margin-right: 16px;
  }
}
</style>
