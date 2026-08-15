<template>
  <header class="global-header" :class="{ scrolled }">
    <!-- 顶部渐变流光线条 -->
    <div class="header-topline"></div>

    <div class="header-content">
      <!-- 左侧：Logo 和品牌 -->
      <div class="header-left">
        <router-link to="/" class="logo-section">
          <div class="logo-capsule">
            <div class="logo-icon">
              <img class="logo-img" src="/favicon.ico" alt="AI零代码平台" />
            </div>
            <span class="site-title">AI零代码</span>
          </div>
          <span class="beta-badge">BETA</span>
        </router-link>
      </div>

      <!-- 中间：导航链接 -->
      <div class="header-center">
        <nav class="nav-links">
          <router-link
            v-for="item in navItemsWithIcons"
            :key="item.key"
            :to="item.key"
            class="nav-link"
            :class="{ active: isActive(item.key) }"
          >
            <component :is="item.icon" class="nav-icon" />
            <span>{{ item.label }}</span>
            <div class="nav-underline"></div>
          </router-link>
        </nav>
      </div>

      <!-- 右侧：用户区域 -->
      <div class="header-right">
        <div class="user-section">
        <!-- 已登录状态 -->
        <div v-if="loginUserStore.loginUser.userName" class="user-logged-in">
          <a-dropdown :trigger="['hover']" placement="bottomRight" overlay-class-name="user-dropdown">
            <div class="user-trigger">
              <div class="user-avatar-ring">
                <a-avatar
                  :src="loginUserStore.loginUser.userAvatar"
                  :size="36"
                  class="user-avatar"
                >
                  {{ (loginUserStore.loginUser.userName || 'U').charAt(0).toUpperCase() }}
                </a-avatar>
              </div>
              <span class="user-name">{{ loginUserStore.loginUser.userName }}</span>
              <svg class="dropdown-arrow" viewBox="0 0 12 12" fill="none">
                <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </div>
            <template #overlay>
              <div class="dropdown-menu">
                <div class="dropdown-header">
                  <div class="dropdown-avatar">
                    {{ (loginUserStore.loginUser.userName || 'U').charAt(0).toUpperCase() }}
                  </div>
                  <div class="dropdown-user-info">
                    <div class="dropdown-username">{{ loginUserStore.loginUser.userName }}</div>
                    <div class="dropdown-role">创作者</div>
                  </div>
                </div>
                <div class="dropdown-divider"></div>
                <div class="dropdown-item" @click="handleUserMenuClick('profile')">
                  <svg viewBox="0 0 20 20" fill="none" class="dropdown-icon">
                    <path d="M10 10C12.7614 10 15 7.76142 15 5C15 2.23858 12.7614 0 10 0C7.23858 0 5 2.23858 5 5C5 7.76142 7.23858 10 10 10Z" fill="currentColor"/>
                    <path d="M10 12C5.58172 12 2 14.2386 2 17V20H18V17C18 14.2386 14.4183 12 10 12Z" fill="currentColor"/>
                  </svg>
                  <span>个人信息</span>
                </div>
                <div class="dropdown-divider"></div>
                <div class="dropdown-item" @click="handleUserMenuClick('works')">
                  <svg viewBox="0 0 20 20" fill="none" class="dropdown-icon">
                    <rect x="3" y="3" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/>
                    <rect x="11" y="3" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/>
                    <rect x="3" y="11" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/>
                    <rect x="11" y="11" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/>
                  </svg>
                  <span>我的作品</span>
                </div>
                <div class="dropdown-divider"></div>
                <div class="dropdown-item logout" @click="handleUserMenuClick('logout')">
                  <svg viewBox="0 0 20 20" fill="none" class="dropdown-icon">
                    <path d="M7 17H5C3.89543 17 3 16.1046 3 15V5C3 3.89543 3.89543 3 5 3H7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                    <path d="M13 14L17 10M17 10L13 6M17 10H9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  <span>退出登录</span>
                </div>
              </div>
            </template>
          </a-dropdown>
        </div>

        <!-- 未登录状态 -->
        <button v-else class="login-button" @click="handleLogin">
          <span class="login-text">登录</span>
          <svg viewBox="0 0 20 20" fill="none" class="login-arrow">
            <path d="M7 4L13 10L7 16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        </div>
      </div>
    </div>

    <!-- 底部渐变边框 -->
    <div class="header-border"></div>
  </header>
</template>

<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, ref, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()
const route = useRoute()
const router = useRouter()

/* ---------- 滚动状态 ---------- */
const scrolled = ref(false)

const getScroller = (): HTMLElement | null =>
  document.querySelector('.main-content')

const getScrollY = () => getScroller()?.scrollTop || 0

const handleScroll = () => {
  scrolled.value = getScrollY() > 8
}

onMounted(() => {
  getScroller()?.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()
})

onBeforeUnmount(() => {
  getScroller()?.removeEventListener('scroll', handleScroll)
})

// 导航项配置
const navItems = computed(() => {
  const items = [
    { key: '/', label: '主页', icon: 'HomeIcon' },
    { key: '/user/apps', label: '我的作品', icon: 'GridIcon' },
  ]

  // 管理员菜单
  if (loginUserStore.loginUser.userRole === 'admin') {
    items.push(
      { key: '/admin/userManage', label: '用户管理', icon: 'UserIcon' },
      { key: '/admin/appManage', label: '应用管理', icon: 'AppIcon' },
      { key: '/user/chatHistoryManage', label: '对话历史', icon: 'ChatIcon' }
    )
  }

  return items
})

// 简单图标组件
const HomeIcon = {
  render: () => h('svg', { viewBox: '0 0 20 20', fill: 'none', class: 'nav-icon-svg' }, [
    h('path', { d: 'M3 10L10 3L17 10', stroke: 'currentColor', 'stroke-width': '1.5', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }),
    h('path', { d: 'M5 8.5V17H8V13H12V17H15V8.5', stroke: 'currentColor', 'stroke-width': '1.5', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' })
  ])
}

const GridIcon = {
  render: () => h('svg', { viewBox: '0 0 20 20', fill: 'none', class: 'nav-icon-svg' }, [
    h('rect', { x: '3', y: '3', width: '5.5', height: '5.5', rx: '1.3', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('rect', { x: '11.5', y: '3', width: '5.5', height: '5.5', rx: '1.3', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('rect', { x: '3', y: '11.5', width: '5.5', height: '5.5', rx: '1.3', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('rect', { x: '11.5', y: '11.5', width: '5.5', height: '5.5', rx: '1.3', stroke: 'currentColor', 'stroke-width': '1.5' })
  ])
}

const UserIcon = {
  render: () => h('svg', { viewBox: '0 0 20 20', fill: 'none', class: 'nav-icon-svg' }, [
    h('circle', { cx: '10', cy: '6', r: '3.5', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('path', { d: 'M2 18C2 14.6863 5.58172 12 10 12C14.4183 12 18 14.6863 18 18', stroke: 'currentColor', 'stroke-width': '1.5', 'stroke-linecap': 'round' })
  ])
}

const AppIcon = {
  render: () => h('svg', { viewBox: '0 0 20 20', fill: 'none', class: 'nav-icon-svg' }, [
    h('rect', { x: '2', y: '2', width: '7', height: '7', rx: '1.5', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('rect', { x: '11', y: '2', width: '7', height: '7', rx: '1.5', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('rect', { x: '2', y: '11', width: '7', height: '7', rx: '1.5', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('rect', { x: '11', y: '11', width: '7', height: '7', rx: '1.5', stroke: 'currentColor', 'stroke-width': '1.5' })
  ])
}

const ChatIcon = {
  render: () => h('svg', { viewBox: '0 0 20 20', fill: 'none', class: 'nav-icon-svg' }, [
    h('path', { d: 'M3 5.5C3 4.11929 4.11929 3 5.5 3H14.5C15.8807 3 17 4.11929 17 5.5V11.5C17 12.8807 15.8807 14 14.5 14H9L4.5 17.5V14H5.5C4.11929 14 3 12.8807 3 11.5V5.5Z', stroke: 'currentColor', 'stroke-width': '1.5', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' })
  ])
}

// 动态获取图标组件
const getIconComponent = (iconName: string): Component => {
  const icons: Record<string, Component> = {
    HomeIcon,
    UserIcon,
    AppIcon,
    ChatIcon,
    GridIcon
  }
  return icons[iconName] || HomeIcon
}

// 重新计算导航项带图标
const navItemsWithIcons = computed(() => {
  return navItems.value.map(item => ({
    ...item,
    icon: getIconComponent(item.icon)
  }))
})

// 判断导航项是否激活
const isActive = (key: string) => {
  const path = route.path
  if (key === '/') {
    return path === '/' || path.startsWith('/app/')
  }
  return path === key || path.startsWith(key + '/')
}

// 用户菜单点击处理
const handleUserMenuClick = async (key: string) => {
  switch (key) {
    case 'profile':
      message.info('个人信息功能开发中...')
      break
    case 'works':
      await router.push('/user/apps')
      break
    case 'logout':
      const success = await loginUserStore.logout()
      if (success) {
        message.success('退出登录成功')
        router.push('/')
      } else {
        message.error('退出登录失败')
      }
      break
  }
}

// 登录按钮点击
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
  height: 64px;
  background: linear-gradient(180deg, rgba(12, 12, 14, 0.88), rgba(12, 12, 14, 0.72));
  transition: background 0.3s ease, box-shadow 0.3s ease;
}

.global-header.scrolled {
  background: linear-gradient(180deg, rgba(12, 12, 14, 0.97), rgba(12, 12, 14, 0.94));
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45);
}

/* 顶部渐变流光线条 */
.header-topline {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
  opacity: 0.85;
  pointer-events: none;
}

.header-topline::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 300%;
  height: 100%;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.25), rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0.25));
  animation: toplineShift 8s linear infinite;
}

@keyframes toplineShift {
  from { transform: translateX(0); }
  to { transform: translateX(-33.333%); }
}

.header-content {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  width: 100%;
  padding: 0 24px;
}

/* 左 / 中 / 右 逻辑区块 */
.header-left,
.header-center,
.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.header-right {
  margin-left: auto;
}

/* Logo 区域 */
.logo-section {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  cursor: pointer;
}

.logo-capsule {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 14px 5px 5px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 100px;
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, color 0.3s ease, transform 0.3s ease;
}

.logo-section:hover .logo-capsule {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.28);
  box-shadow:
    0 0 24px rgba(255, 255, 255, 0.08),
    inset 0 0 12px rgba(255, 255, 255, 0.04);
}

.logo-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #27272a 0%, #3f3f46 100%);
  border-radius: 9px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.logo-icon::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 30%, rgba(255, 255, 255, 0.28) 50%, transparent 70%);
  transform: translateX(-100%);
  animation: logoShine 4s ease-in-out infinite;
}

@keyframes logoShine {
  0%, 60% { transform: translateX(-100%); }
  85%, 100% { transform: translateX(100%); }
}

.logo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.site-title {
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.3px;
  background: linear-gradient(90deg, #fff 0%, #a1a1aa 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.beta-badge {
  font-size: 9px;
  font-weight: 600;
  color: #d4d4d8;
  background: rgba(255, 255, 255, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  letter-spacing: 0.5px;
  animation: badgePulse 3s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% { box-shadow: 0 0 0 rgba(255, 255, 255, 0); }
  50% { box-shadow: 0 0 10px rgba(255, 255, 255, 0.18); }
}

/* 导航链接 */
.nav-links {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 15px;
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
  text-decoration: none;
  border-radius: 10px;
  position: relative;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, transform 0.25s ease;
  overflow: hidden;
}

.nav-link::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.06);
  opacity: 0;
  transition: opacity 0.25s ease;
  border-radius: 10px;
}

.nav-link:hover {
  color: #e2e8f0;
}

.nav-link:hover::before {
  opacity: 1;
}

.nav-link.active {
  color: #6ee7b7;
  background: rgba(16, 185, 129, 0.1);
}

.nav-link.active::before {
  opacity: 1;
}

.nav-icon-svg {
  width: 16px;
  height: 16px;
  transition: transform 0.25s ease;
}

.nav-link:hover .nav-icon-svg {
  transform: translateY(-1px);
}

.nav-link.active .nav-icon-svg {
  color: #34d399;
  filter: drop-shadow(0 0 6px rgba(52, 211, 153, 0.5));
}

.nav-underline {
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: calc(100% - 30px);
  height: 2px;
  background: linear-gradient(90deg, rgba(52, 211, 153, 0.9), rgba(16, 185, 129, 0.35));
  border-radius: 1px;
  transition: transform 0.25s ease;
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.4);
}

.nav-link.active .nav-underline {
  transform: translateX(-50%) scaleX(1);
}

/* 用户区域 */
.user-section {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 6px;
  border-radius: 100px;
  cursor: pointer;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, transform 0.25s ease;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.user-trigger:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.user-avatar-ring {
  position: relative;
  padding: 2px;
  background: linear-gradient(135deg, #3f3f46, #27272a);
  border-radius: 50%;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.12);
}

.user-avatar {
  background: #131316;
  color: #a1a1aa;
  font-weight: 600;
  font-size: 13px;
}

.user-avatar-ring::after {
  content: '';
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background: #22c55e;
  border: 2px solid #0a0a0c;
  border-radius: 50%;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #e2e8f0;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-arrow {
  width: 12px;
  height: 12px;
  color: #64748b;
  transition: transform 0.25s ease;
}

.user-trigger:hover .dropdown-arrow {
  transform: rotate(180deg);
  color: #94a3b8;
}

/* 登录按钮 */
.login-button {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #27272a 0%, #3f3f46 100%);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 100px;
  cursor: pointer;
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, color 0.3s ease, transform 0.3s ease;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  overflow: hidden;
}

.login-button::after {
  content: '';
  position: absolute;
  top: 0;
  left: -60%;
  width: 50%;
  height: 100%;
  background: linear-gradient(105deg, transparent, rgba(255, 255, 255, 0.35), transparent);
  transform: skewX(-20deg);
  transition: left 0.55s ease;
}

.login-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.55);
  background: linear-gradient(135deg, #3f3f46 0%, #52525b 100%);
  border-color: rgba(255, 255, 255, 0.3);
}

.login-button:hover::after {
  left: 120%;
}

.login-button:active {
  transform: translateY(0);
}

.login-arrow {
  width: 14px;
  height: 14px;
  transition: transform 0.25s ease;
}

.login-button:hover .login-arrow {
  transform: translateX(2px);
}

/* 底部边框 */
.header-border {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.18) 25%,
    rgba(255, 255, 255, 0.32) 50%,
    rgba(255, 255, 255, 0.18) 75%,
    transparent 100%
  );
  opacity: 0.6;
}

/* 下拉菜单样式 */
:deep(.user-dropdown) {
  padding-top: 8px;
}

.dropdown-menu {
  min-width: 200px;
  background: rgba(19, 19, 22, 0.96);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5);
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.dropdown-avatar {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #27272a, #3f3f46);
  border-radius: 10px;
  font-size: 16px;
  font-weight: 700;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.dropdown-user-info {
  flex: 1;
}

.dropdown-username {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.dropdown-role {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.dropdown-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin: 4px 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  font-size: 14px;
  color: #94a3b8;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.dropdown-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #e2e8f0;
}

.dropdown-item.logout:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #f87171;
}

.dropdown-icon {
  width: 16px;
  height: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .nav-links {
    display: none;
  }

  .site-title {
    font-size: 14px;
  }

  .beta-badge {
    display: none;
  }

  .user-name {
    display: none;
  }

  .login-button {
    padding: 8px 16px;
  }

  .login-text {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .logo-capsule {
    padding: 4px 10px 4px 4px;
  }

  .logo-icon {
    width: 26px;
    height: 26px;
  }
}
</style>
