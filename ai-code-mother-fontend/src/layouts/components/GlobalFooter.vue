<template>
  <footer class="global-footer">
    <!-- 背景光斑 -->
    <div class="footer-bg" aria-hidden="true">
      <div class="fo-orb fo-1"></div>
      <div class="fo-orb fo-2"></div>
    </div>

    <div class="footer-content">
      <!-- 顶部装饰线 -->
      <div class="footer-glow"></div>

      <!-- 主要内容 -->
      <div class="footer-main">
        <!-- 左侧品牌信息 -->
        <div class="footer-brand">
          <div class="brand-logo">
            <div class="brand-icon">
              <img class="brand-img" src="/favicon.ico" alt="AI零代码生成平台" />
            </div>
            <span class="brand-text">AI零代码生成平台</span>
          </div>
          <p class="brand-tagline">让创意触手可及</p>
        </div>

        <!-- 中间链接区域 -->
        <div class="footer-links">
          <a href="https://github.com/wyz609" target="_blank" class="footer-link" title="GitHub">
            <svg viewBox="0 0 24 24" fill="currentColor" class="link-icon">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
            </svg>
          </a>
          <a href="#" class="footer-link" title="使用文档">
            <svg viewBox="0 0 24 24" fill="none" class="link-icon">
              <path d="M12 6.25278V19.2528M12 6.25278C10.8321 5.47686 9.24649 5 7.5 5C5.75351 5 4.16789 5.47686 3 6.25278V19.2528C4.16789 18.4769 5.75351 18 7.5 18C9.24649 18 10.8321 18.4769 12 19.2528M12 6.25278C13.1679 5.47686 14.7535 5 16.5 5C18.2465 5 19.8321 5.47686 21 6.25278V19.2528C19.8321 18.4769 18.2465 18 16.5 18C14.7535 18 13.1679 18.4769 12 19.2528" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </a>
          <a href="#" class="footer-link" title="服务条款">
            <svg viewBox="0 0 24 24" fill="none" class="link-icon">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </a>
        </div>

        <!-- 右侧版权信息 -->
        <div class="footer-copyright">
          <span class="copyright-text">
            by
            <a href="https://github.com/wyz609" target="_blank" class="author-link">程序员阿阳</a>
          </span>
          <span class="copyright-year">© {{ year }}</span>
        </div>
      </div>
    </div>

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
.global-footer {
  position: relative;
  width: 100%;
  margin-top: auto;
  padding: 0;
  overflow: hidden;
  background: linear-gradient(180deg, transparent 0%, rgba(19, 19, 22, 0.6) 30%);
}

/* 背景光斑 */
.footer-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.fo-orb {
  position: absolute;
  border-radius: 50%;
  opacity: 0.18;
  animation: foDrift 14s ease-in-out infinite alternate;
  transform: translateZ(0);
}

.fo-1 {
  width: 380px;
  height: 380px;
  left: -140px;
  bottom: -180px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.16) 0%, rgba(255, 255, 255, 0.07) 40%, transparent 70%);
}

.fo-2 {
  width: 320px;
  height: 320px;
  right: -120px;
  bottom: -160px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.13) 0%, rgba(255, 255, 255, 0.06) 40%, transparent 70%);
  animation-delay: -6s;
}

@keyframes foDrift {
  0% { transform: translate(0, 0); }
  100% { transform: translate(-30px, -24px); }
}

.footer-content {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 顶部装饰线 */
.footer-glow {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 300px;
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.18) 20%,
    rgba(255, 255, 255, 0.4) 50%,
    rgba(255, 255, 255, 0.18) 80%,
    transparent 100%
  );
  animation: glowPulse 3.5s ease-in-out infinite;
}

.footer-glow::before {
  content: '';
  position: absolute;
  top: -5px;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 10px;
  background: radial-gradient(ellipse, rgba(255, 255, 255, 0.16) 0%, transparent 70%);
  animation: glowPulse 3.5s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* 主要内容 */
.footer-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* 品牌区域 */
.footer-brand {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 9px;
}

.brand-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 2px 10px rgba(255, 255, 255, 0.08);
}

.brand-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.brand-text {
  font-size: 13px;
  font-weight: 600;
  color: #d4d4d8;
  letter-spacing: 0.2px;
  background: linear-gradient(90deg, #e4e4e7 0%, #a1a1aa 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-tagline {
  font-size: 11px;
  color: #64748b;
  margin: 0;
  padding-left: 37px;
}

/* 链接区域 */
.footer-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  color: #64748b;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, transform 0.25s ease;
}

.footer-link:hover {
  background: rgba(255, 255, 255, 0.07);
  border-color: rgba(255, 255, 255, 0.25);
  color: #e4e4e7;
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.45);
}

.link-icon {
  width: 16px;
  height: 16px;
}

/* 版权信息 */
.footer-copyright {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.copyright-text {
  font-size: 12px;
  color: #64748b;
}

.author-link {
  color: #a1a1aa;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.author-link:hover {
  color: #ffffff;
}

.copyright-year {
  font-size: 11px;
  color: #475569;
  letter-spacing: 1px;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .footer-content {
    padding: 0 16px;
  }

  .footer-main {
    flex-direction: column;
    gap: 16px;
    padding: 16px 0;
    text-align: center;
  }

  .footer-brand {
    align-items: center;
  }

  .brand-tagline {
    padding-left: 0;
  }

  .footer-links {
    order: -1;
  }

  .footer-copyright {
    align-items: center;
  }

  .back-top {
    right: 18px;
    bottom: 18px;
    width: 40px;
    height: 40px;
  }
}

@media (max-width: 480px) {
  .brand-text {
    font-size: 12px;
  }

  .brand-tagline {
    font-size: 10px;
  }

  .footer-link {
    width: 32px;
    height: 32px;
  }

  .link-icon {
    width: 14px;
    height: 14px;
  }
}
</style>
