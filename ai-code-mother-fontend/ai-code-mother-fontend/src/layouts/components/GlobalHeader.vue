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
          :selectedKeys="selectedKeys"
          mode="horizontal"
          class="header-menu"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </div>

      <!-- 右侧：用户信息 -->
      <div class="user-login-status">
        <div v-if="loginUserStore.loginUser.userName" class="user-info">
          <a-dropdown placement="bottomRight" :trigger="['hover']">
            <div class="user-avatar-section">
              <a-space>
                <a-avatar
                  :src="loginUserStore.loginUser.userAvatar"
                  :alt="loginUserStore.loginUser.userName || '无名'"
                  class="user-avatar"
                >
                  {{ (loginUserStore.loginUser.userName || '无名').charAt(0) }}
                </a-avatar>
                <span class="user-name">{{ loginUserStore.loginUser.userName || '无名' }}</span>
              </a-space>
            </div>
            <template #overlay>
              <a-menu @click="handleUserMenuClick">
                <a-menu-item key="profile">
                  <UserOutlined /> 个人信息
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else class="login-section">
          <a-button type="primary" @click="handleLogin">登录</a-button>
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LogoutOutlined, HomeOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
import type { MenuProps } from 'ant-design-vue'
// JS 中引入 Store
import { useLoginUserStore } from '@/stores/loginUser.ts'
const loginUserStore = useLoginUserStore()
const route = useRoute()

const router = useRouter()

// 根据当前路由动态设置选中的菜单项
const selectedKeys = computed(() => {
  const path = route.path
  // 直接返回当前路径作为选中的菜单项
  if (path === '/' || path === '/admin/userManage' || path === '/admin/appManage') return [path]
  if (path === '/about') return ['/about']
  // 如果是登录或注册页面，不选中任何菜单项
  if (path === '/user/login' || path === '/user/register') return []
  // 检查是否是管理员页面
  if (path.startsWith('/admin/')) return [path]
  // 检查是否是应用相关页面，选中首页
  if (path.startsWith('/app/')) return ['/']
  return ['/'] // 默认选中首页
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    icon: () => h(UserOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    icon: () => h(AppstoreOutlined),
    label: '应用管理',
    title: '应用管理',
  }
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 菜单点击处理
const handleMenuClick = ({ key }: { key: string }) => {
  console.log('菜单点击:', key)
  // 根据菜单项跳转路由
  if (key && key !== route.path) {
    router.push(key)
  }
}

// 用户菜单点击处理
const handleUserMenuClick = async ({ key }: { key: string }) => {
  switch (key) {
    case 'profile':
      // TODO: 跳转到个人信息页面
      message.info('个人信息功能开发中...')
      break
    case 'logout':
      const success = await loginUserStore.logout()
      if (success) {
        message.success('退出登录成功')
        // 跳转到首页
        router.push('/')
      } else {
        message.error('退出登录失败')
      }
      break
  }
}

// 登录按钮点击处理
const handleLogin = () => {
  router.push('/user/login')
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

.user-login-status {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar-section {
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.user-avatar-section:hover {
  background-color: #f5f5f5;
}

.user-avatar {
  transition: all 0.2s ease;
}

.user-avatar-section:hover .user-avatar {
  transform: scale(1.05);
}

.user-name {
  color: #333;
  font-weight: 500;
  transition: all 0.2s ease;
}

.user-avatar-section:hover .user-name {
  color: #1890ff;
}

.login-section {
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
