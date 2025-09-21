<script setup lang="ts">
import BasicLayout from './layouts/BasicLayout.vue'
import { healthCheck } from '@/api/healthController.ts'
healthCheck().then((res) => {
  console.log(res)
})
</script>

<template>
  <BasicLayout />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif, 'Apple Color Emoji',
    'Segoe UI Emoji', 'Segoe UI Symbol';
  font-size: 14px;
  line-height: 1.5;
  color: #333;
}

#app {
  width: 100%;
  height: 100vh;
}
</style>
