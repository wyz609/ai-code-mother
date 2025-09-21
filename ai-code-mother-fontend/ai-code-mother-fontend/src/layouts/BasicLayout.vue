<template>
  <a-layout class="basic-layout">
    <!-- 全局头部 -->
    <GlobalHeader />

    <!-- 主体内容区域 -->
    <a-layout-content class="main-content">
      <div class="content-wrapper">
        <router-view />
      </div>
    </a-layout-content>

    <!-- 全局底部 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from './components/GlobalHeader.vue'
import GlobalFooter from './components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: #f0f2f5;
}

.main-content {
  margin-top: 64px; /* 头部高度 */
  margin-bottom: 48px; /* 底部高度 */
  padding: 24px;
  background: #f0f2f5;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
  min-height: calc(100vh - 64px - 48px - 48px); /* 全屏高度减去头部、底部和内边距 */
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 16px;
  }

  .content-wrapper {
    padding: 16px;
    border-radius: 4px;
  }
}

@media (max-width: 480px) {
  .main-content {
    padding: 12px;
  }

  .content-wrapper {
    padding: 12px;
    min-height: calc(100vh - 64px - 48px - 24px);
  }
}
</style>
