<template>
  <footer class="global-footer">
    <!-- 顶部渐变边框（与 Header 底部边框呼应） -->
    <div class="footer-border"></div>

    <div class="footer-content">
      <!-- 左侧：品牌（胶囊样式与 Header 一致） -->
      <div class="footer-left">
        <a href="/" class="logo-section">
          <div class="logo-capsule">
            <div class="logo-icon">
              <img class="logo-img" src="/favicon.ico" alt="AI零代码平台" />
            </div>
            <span class="site-title">AI零代码</span>
          </div>
        </a>
        <span class="copyright-badge">© {{ year }}</span>
      </div>

      <!-- 中间：链接（胶囊样式与 Header 导航一致） -->
      <div class="footer-center">
        <nav class="footer-nav">
          <a
            v-for="link in footerLinks"
            :key="link.label"
            :href="link.href"
            target="_blank"
            rel="noopener noreferrer"
            class="footer-nav-link"
            :title="link.label"
          >
            <svg viewBox="0 0 24 24" class="footer-nav-icon" aria-hidden="true">
              <path v-if="link.icon === 'github'" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" fill="currentColor"/>
              <path v-else-if="link.icon === 'doc'" d="M12 6.25278V19.2528M12 6.25278C10.8321 5.47686 9.24649 5 7.5 5C5.75351 5 4.16789 5.47686 3 6.25278V19.2528C4.16789 18.4769 5.75351 18 7.5 18C9.24649 18 10.8321 18.4769 12 19.2528M12 6.25278C13.1679 5.47686 14.7535 5 16.5 5C18.2465 5 19.8321 5.47686 21 6.25278V19.2528C19.8321 18.4769 18.2465 18 16.5 18C14.7535 18 13.1679 18.4769 12 19.2528" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path v-else d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>{{ link.label }}</span>
            <div class="footer-nav-underline"></div>
          </a>
        </nav>
      </div>

      <!-- 右侧：版权（胶囊样式与 Header 用户区一致） -->
      <div class="footer-right">
        <div class="author-capsule">
          <div class="author-avatar">阳</div>
          <div class="author-info">
            <span class="author-by">Powered by</span>
            <a href="https://github.com/wyz609" target="_blank" rel="noopener noreferrer" class="author-link">程序员阿阳</a>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部渐变流光线条（与 Header 顶部流光对称） -->
    <div class="footer-flowline"></div>

    <!-- 回到顶部 -->
    <transition name="backtop">
      <button v-if="showTop" class="back-top" @click="scrollToTop" title="回到顶部">
        <svg viewBox="0 0 20 20" fill="none" class="back-top-icon">
          <path d="M5 12L10 7L15 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M5 17L10 12L15 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.4"/>
        </svg>
      </button>
    </transition>
  </footer>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const year = new Date().getFullYear()

const footerLinks = [
  { label: 'GitHub', href: 'https://github.com/wyz609', icon: 'github' },
  { label: '使用文档', href: '#', icon: 'doc' },
  { label: '服务条款', href: '#', icon: 'terms' },
]

/* ---------- 回到顶部 ---------- */
const showTop = ref(false)

const getScroller = (): HTMLElement | null =>
  document.querySelector('.main-content')

const getScrollY = () => getScroller()?.scrollTop || 0

const handleScroll = () => {
  showTop.value = getScrollY() > 400
}

onMounted(() => {
  getScroller()?.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()
})

onBeforeUnmount(() => {
  getScroller()?.removeEventListener('scroll', handleScroll)
})

const scrollToTop = () => {
  if (getScrollY() === 0) return
  const opts: ScrollToOptions = { top: 0, behavior: 'smooth' }
  getScroller()?.scrollTo(opts)
}
</script>

<style scoped>
/* 与 Header 一致的深色渐变玻璃背景（上下对称） */
.global-footer {
  position: relative;
  width: 100%;
  padding: 0;
  overflow: hidden;
  background: linear-gradient(0deg, rgba(12, 12, 14, 0.92), rgba(12, 12, 14, 0.6));
}

/* 顶部渐变边框（呼应 Header 底部边框） */
.footer-border {
  position: absolute;
  top: 0;
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

/* 底部渐变流光线条（对应 Header 顶部流光，动画方向对称） */
.footer-flowline {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
  opacity: 0.85;
  pointer-events: none;
}

.footer-flowline::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 300%;
  height: 100%;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.25), rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0.25));
  animation: flowlineShift 8s linear infinite;
}

@keyframes flowlineShift {
  from { transform: translateX(0); }
  to { transform: translateX(-33.333%); }
}

/* 与 Header 相同的三栏布局 */
.footer-content {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  width: 100%;
  padding: 0 24px;
}

.footer-left,
.footer-center,
.footer-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.footer-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.footer-right {
  margin-left: auto;
}

/* 品牌：胶囊样式（与 Header Logo 一致） */
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

/* 版权徽章（与 Header BETA 徽章一致） */
.copyright-badge {
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

/* 链接：胶囊样式（与 Header 导航一致） */
.footer-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.footer-nav-link {
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

.footer-nav-link::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.06);
  opacity: 0;
  transition: opacity 0.25s ease;
  border-radius: 10px;
}

.footer-nav-link:hover {
  color: #e2e8f0;
}

.footer-nav-link:hover::before {
  opacity: 1;
}

.footer-nav-icon {
  width: 16px;
  height: 16px;
  transition: transform 0.25s ease;
}

.footer-nav-link:hover .footer-nav-icon {
  transform: translateY(-1px);
  color: #34d399;
  filter: drop-shadow(0 0 6px rgba(52, 211, 153, 0.5));
}

.footer-nav-underline {
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

.footer-nav-link:hover .footer-nav-underline {
  transform: translateX(-50%) scaleX(1);
}

/* 版权：胶囊样式（与 Header 用户区一致） */
.author-capsule {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 6px;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, transform 0.25s ease;
}

.author-capsule:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.author-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #27272a, #3f3f46);
  color: #a1a1aa;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

.author-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.author-by {
  font-size: 10px;
  color: #64748b;
  letter-spacing: 0.4px;
  text-transform: uppercase;
}

.author-link {
  font-size: 13px;
  font-weight: 500;
  color: #e2e8f0;
  text-decoration: none;
  transition: color 0.25s ease;
}

.author-capsule:hover .author-link {
  color: #6ee7b7;
}

/* 回到顶部按钮 */
.back-top {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 999;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #ffffff;
  background: linear-gradient(135deg, #27272a 0%, #3f3f46 100%);
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, transform 0.25s ease;
}

.back-top:hover {
  transform: translateY(-3px);
  background: linear-gradient(135deg, #3f3f46 0%, #52525b 100%);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.55);
}

.back-top:active {
  transform: translateY(0);
}

.back-top-icon {
  width: 18px;
  height: 18px;
}

.backtop-enter-active,
.backtop-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.backtop-enter-from,
.backtop-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

/* 响应式设计（与 Header 一致） */
@media (max-width: 768px) {
  .footer-content {
    padding: 0 16px;
  }

  .footer-nav {
    display: none;
  }

  .copyright-badge {
    display: none;
  }

  .site-title {
    font-size: 14px;
  }

  .author-by {
    display: none;
  }

  .back-top {
    right: 18px;
    bottom: 18px;
    width: 40px;
    height: 40px;
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

  .author-avatar {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }
}
</style>
