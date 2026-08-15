<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域（局部滚动容器） -->
    <a-layout-content class="main-content">
      <router-view />
      <!-- 底部版权信息 -->
      <GlobalFooter />
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/layouts/components/GlobalHeader.vue'
import GlobalFooter from '@/layouts/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #0a0a0c;
}

/* 局部滚动容器：只滚动内容区，避免整页滚动造成的卡顿 */
.main-content {
  flex: 1;
  min-height: 0;
  width: 100%;
  padding: 0;
  margin: 0;
  background: transparent;
  /* 为固定的 Header 留出空间 */
  padding-top: 64px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
}
</style>
