<template>
  <div
    class="ctx-card group relative flex items-start gap-2.5 rounded-[10px] border border-emerald-500/30 bg-[#0D1117]/80 px-3 py-2.5 backdrop-blur-md transition-all duration-200 hover:border-emerald-500/60 hover:shadow-[0_0_15px_rgba(16,185,129,0.15)]"
  >
    <!-- 呼吸灯：AI 监听中 -->
    <span class="ctx-pulse" aria-hidden="true"></span>

    <!-- 左侧图标区：深色包裹 + 荧光绿 -->
    <span
      class="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-emerald-500/20 bg-emerald-900/20 text-[15px] text-emerald-400"
    >
      <CodeOutlined />
    </span>

    <!-- 文字区 -->
    <div class="flex min-w-0 flex-1 flex-col gap-1">
      <span class="text-[11px] font-semibold uppercase tracking-[0.2em] text-gray-400">
        {{ title }}
      </span>
      <span
        v-if="content"
        class="line-clamp-1 break-all text-[13px] leading-relaxed text-white/90"
      >
        {{ content }}
      </span>
      <code v-if="codeSnippet" class="ctx-code">{{ codeSnippet }}</code>
    </div>

    <!-- 右侧删除按钮 -->
    <button
      class="ctx-remove"
      type="button"
      aria-label="移除上下文"
      title="移除"
      @click="$emit('remove')"
    >
      <CloseOutlined />
    </button>
  </div>
</template>

<script setup lang="ts">
import { CodeOutlined, CloseOutlined } from '@ant-design/icons-vue'

withDefaults(
  defineProps<{
    title?: string
    content?: string
    codeSnippet?: string
  }>(),
  {
    title: 'AI 工作区',
    content: '',
    codeSnippet: '',
  }
)

defineEmits<{
  (e: 'remove'): void
}>()
</script>

<style scoped>
/* ========== 代码片段：等宽编程字体 + 深色底 + 绿色高亮 ========== */
.ctx-code {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.55;
  color: #6ee7b7;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 6px;
  padding: 6px 9px;
  white-space: pre-wrap;
  word-break: break-all;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

/* ========== 呼吸灯 ========== */
.ctx-pulse {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.8);
  animation: ctxPulse 2.4s ease-in-out infinite;
}

@keyframes ctxPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.8);
  }
}

/* ========== 进入动画：滑入 + 淡入 ========== */
.ctx-card {
  animation: ctxSlideIn 0.35s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes ctxSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 删除按钮：默认微隐，悬浮全亮 + 红色 + 缩放 ========== */
.ctx-remove {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-top: 4px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  opacity: 0.3;
  transition: opacity 0.2s ease, color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.ctx-card:hover .ctx-remove {
  opacity: 1;
}

.ctx-remove:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.12);
  transform: scale(1.1);
}

.ctx-remove:focus-visible {
  outline: 2px solid rgba(16, 185, 129, 0.7);
  outline-offset: 1px;
}

.ctx-remove svg {
  width: 11px;
  height: 11px;
}

@media (prefers-reduced-motion: reduce) {
  .ctx-card {
    animation: none;
  }

  .ctx-pulse {
    animation: none;
  }
}
</style>
