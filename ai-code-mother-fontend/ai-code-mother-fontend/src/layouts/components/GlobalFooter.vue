<template>
  <a-layout-footer class="global-footer">
    <div class="footer-content">AI零代码生成平台 by 程序员阿阳</div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外逻辑
</script>

<style scoped>
.global-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: #f0f2f5;
  border-top: 1px solid #d9d9d9;
  padding: 0;
  height: 48px;
  line-height: 48px;
}

.footer-content {
  text-align: center;
  color: #666;
  font-size: 14px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer-content {
    padding: 0 16px;
    font-size: 12px;
  }
}
</style>
