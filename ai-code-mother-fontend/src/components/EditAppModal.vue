<template>
  <Teleport to="body">
    <Transition name="edit-fade">
      <div
        v-if="open"
        class="edit-mask"
        role="presentation"
        @mousedown.self="handleClose"
      >
        <div class="edit-modal" role="dialog" aria-modal="true" :aria-label="`修改应用：${appName || ''}`">
          <!-- 装饰层：细网格 + 四角括号 + 底部扫描线 -->
          <div class="modal-grid" aria-hidden="true"></div>
          <span class="corner corner-tl" aria-hidden="true"></span>
          <span class="corner corner-tr" aria-hidden="true"></span>
          <span class="corner corner-bl" aria-hidden="true"></span>
          <span class="corner corner-br" aria-hidden="true"></span>
          <span class="modal-scan" aria-hidden="true"></span>

          <div class="modal-head">
            <div class="modal-title">
              <span class="modal-kicker">// EDIT_APPLICATION</span>
              <h2 class="modal-h2">修改应用</h2>
            </div>
            <button type="button" class="modal-close" aria-label="关闭弹窗" title="关闭 (Esc)" @click="handleClose">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <div class="modal-body">
            <p class="modal-tip">修改作品名称，不会影响已生成的代码和部署版本。</p>
            <form class="edit-form" novalidate @submit.prevent="handleSave">
              <label class="field-label" for="edit-app-name">应用名称</label>
              <input
                id="edit-app-name"
                ref="nameInput"
                v-model="name"
                class="modal-input"
                type="text"
                maxlength="100"
                placeholder="请输入应用名称"
                autocomplete="off"
              />
              <div class="field-meta">
                <span v-if="nameError" class="field-error">{{ nameError }}</span>
                <span v-else class="field-hint">PS：更新立即生效</span>
                <span class="field-count">{{ name.trim().length }} / 100</span>
              </div>

              <div class="form-actions">
                <button type="button" class="modal-cancel" :disabled="saving" @click="handleClose">取消</button>
                <button
                  type="submit"
                  class="modal-save"
                  :disabled="saving || !name.trim()"
                >
                  <span v-if="saving" class="save-spinner" aria-hidden="true"></span>
                  <template v-else>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                      <path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/>
                      <polyline points="17 21 17 13 7 13 7 21"/>
                      <polyline points="7 3 7 8 15 8"/>
                    </svg>
                    保存更改
                  </template>
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { updateApp } from '@/api/appController'

const props = defineProps<{
  open: boolean
  appId?: string | number
  appName?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', open: boolean): void
  (e: 'saved', appName: string): void
}>()

const name = ref('')
const nameInput = ref<HTMLInputElement | null>(null)
const saving = ref(false)

const nameError = computed(() => {
  const value = name.value.trim()
  if (!value) return '请输入应用名称'
  if (value.length > 100) return '应用名称不能超过 100 个字符'
  return ''
})

const handleClose = () => {
  if (saving.value) return
  emit('update:open', false)
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') handleClose()
}

const handleSave = async () => {
  const value = name.value.trim()
  if (!value || nameError.value || !props.appId) return
  saving.value = true
  try {
    const response = await updateApp({ id: String(props.appId), appName: value })
    if (response.data.code !== 0) {
      message.error(response.data.message || '保存失败')
      return
    }
    message.success('应用名称已更新')
    emit('saved', value)
    emit('update:open', false)
  } finally {
    saving.value = false
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      name.value = props.appName || ''
      document.addEventListener('keydown', handleKeydown)
      nextTick(() => nameInput.value?.focus())
    } else {
      document.removeEventListener('keydown', handleKeydown)
    }
  },
)

watch(
  () => props.appName,
  (appName) => {
    if (props.open) name.value = appName || ''
  },
)

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.edit-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(2, 8, 10, 0.72);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.edit-modal {
  position: relative;
  width: 100%;
  max-width: 480px;
  overflow: hidden;
  border-radius: 16px;
  border: 1px solid rgba(110, 231, 183, 0.24);
  background:
    linear-gradient(160deg, rgba(21, 33, 37, 0.97), rgba(13, 17, 23, 0.98) 55%, rgba(16, 24, 33, 0.98));
  box-shadow:
    0 0 0 1px rgba(52, 211, 153, 0.06),
    0 24px 64px rgba(0, 0, 0, 0.65),
    0 0 42px rgba(16, 185, 129, 0.13);
}

/* ---- 装饰层 ---- */
.modal-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.35;
  background-image:
    linear-gradient(rgba(110, 231, 183, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(110, 231, 183, 0.06) 1px, transparent 1px);
  background-size: 26px 26px;
  mask-image: radial-gradient(ellipse at 50% 0%, black 30%, transparent 78%);
  -webkit-mask-image: radial-gradient(ellipse at 50% 0%, black 30%, transparent 78%);
}

.corner {
  position: absolute;
  width: 18px;
  height: 18px;
  pointer-events: none;
  border-color: rgba(52, 211, 153, 0.65);
}

.corner-tl { top: 10px; left: 10px; border-top: 1px solid; border-left: 1px solid; border-top-left-radius: 4px; }
.corner-tr { top: 10px; right: 10px; border-top: 1px solid; border-right: 1px solid; border-top-right-radius: 4px; }
.corner-bl { bottom: 10px; left: 10px; border-bottom: 1px solid; border-left: 1px solid; border-bottom-left-radius: 4px; }
.corner-br { bottom: 10px; right: 10px; border-bottom: 1px solid; border-right: 1px solid; border-bottom-right-radius: 4px; }

.modal-scan {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(52, 211, 153, 0.8), transparent);
  opacity: 0.6;
}

/* ---- 头部 ---- */
.modal-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px 26px 0;
}

.modal-kicker {
  display: block;
  margin-bottom: 6px;
  font-family: 'SF Mono', 'Cascadia Code', Menlo, Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: #34d399;
  opacity: 0.75;
}

.modal-h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 650;
  letter-spacing: 0.3px;
  color: #f4f4f5;
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  transition: background-color 0.25s ease, border-color 0.25s ease, color 0.25s ease, transform 0.25s ease, box-shadow 0.25s ease;
}

.modal-close svg { width: 15px; height: 15px; }

.modal-close:hover {
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.1);
  color: #f87171;
  transform: rotate(90deg);
}

/* ---- 主体 ---- */
.modal-body {
  position: relative;
  padding: 18px 26px 26px;
}

.modal-tip {
  margin: 0 0 18px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.5);
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.55);
}

.modal-input {
  width: 100%;
  box-sizing: border-box;
  height: 44px;
  padding: 0 14px;
  font-size: 14px;
  font-family: 'SF Mono', 'Cascadia Code', Menlo, Consolas, monospace;
  color: #e7f6f0;
  background: rgba(4, 12, 14, 0.85);
  border: 1px solid rgba(110, 231, 183, 0.18);
  border-radius: 10px;
  outline: none;
  transition: border-color 0.25s ease, box-shadow 0.25s ease, background-color 0.25s ease;
}

.modal-input::placeholder {
  color: rgba(255, 255, 255, 0.28);
}

.modal-input:focus {
  border-color: #34d399;
  background: rgba(5, 14, 16, 0.95);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.13), 0 0 20px rgba(16, 185, 129, 0.16);
}

.field-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  min-height: 18px;
}

.field-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
}

.field-error {
  font-size: 12px;
  color: #f87171;
}

.field-count {
  margin-left: auto;
  font-family: 'SF Mono', 'Cascadia Code', Menlo, Consolas, monospace;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
}

.modal-cancel {
  height: 38px;
  padding: 0 18px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.25s ease, color 0.25s ease, background-color 0.25s ease;
}

.modal-cancel:hover:not(:disabled) {
  border-color: rgba(255, 255, 255, 0.24);
  color: #f4f4f5;
  background: rgba(255, 255, 255, 0.07);
}

.modal-save {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 20px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #03110c;
  background: linear-gradient(135deg, #10b981, #34d399);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: box-shadow 0.25s ease, transform 0.25s ease, opacity 0.25s ease, filter 0.25s ease;
}

.modal-save svg { width: 14px; height: 14px; }

.modal-save:hover:not(:disabled) {
  box-shadow: 0 0 22px rgba(16, 185, 129, 0.42), 0 4px 14px rgba(16, 185, 129, 0.3);
  transform: translateY(-1px);
  filter: brightness(1.06);
}

.modal-save:active:not(:disabled) {
  transform: translateY(0);
}

.modal-save:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.modal-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.save-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(3, 17, 12, 0.25);
  border-top-color: #03110c;
  border-radius: 50%;
  animation: edit-spin 0.7s linear infinite;
}

@keyframes edit-spin {
  to { transform: rotate(360deg); }
}

/* ---- 过渡动画 ---- */
.edit-fade-enter-active {
  transition: opacity 0.28s ease;
}

.edit-fade-enter-active .edit-modal {
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.28s ease;
}

.edit-fade-leave-active {
  transition: opacity 0.2s ease;
}

.edit-fade-leave-active .edit-modal {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.edit-fade-enter-from,
.edit-fade-leave-to {
  opacity: 0;
}

.edit-fade-enter-from .edit-modal {
  transform: translateY(18px) scale(0.96);
  opacity: 0;
}

.edit-fade-leave-to .edit-modal {
  transform: translateY(10px) scale(0.97);
  opacity: 0;
}
</style>
