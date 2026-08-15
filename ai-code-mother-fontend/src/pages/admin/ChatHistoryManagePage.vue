<template>
  <a-config-provider :theme="themeConfig">
    <div class="history-page">
      <!-- 顶部：返回 + 标题 -->
      <div class="page-head">
        <button class="back-btn" type="button" title="返回上一页" @click="goBack">
          <svg viewBox="0 0 20 20" fill="none" class="back-icon">
            <path d="M12.5 4L6.5 10L12.5 16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
        <div class="head-text">
          <h1>{{ pageTitle }}</h1>
          <span v-if="appId" class="app-filter-tag">应用 {{ appId }}</span>
        </div>
      </div>

      <a-empty v-if="!appId && !isAdmin" description="请从首页「我的作品」卡片进入查看对话历史" />

      <!-- ========== 项目实时预览（nginx 提供 vue 工程运行效果） ========== -->
      <div v-if="appId" class="preview-card">
        <div class="preview-bar">
          <span class="p-dots" aria-hidden="true">
            <i class="p-dot p1"></i>
            <i class="p-dot p2"></i>
            <i class="p-dot p3"></i>
          </span>
          <span class="p-title">VUE 实时预览</span>
          <span v-if="previewReady" class="p-status is-on">
            <i></i>
            {{ previewLoading ? '加载中…' : '运行中' }}
          </span>
          <span v-else class="p-status is-off">未就绪</span>
          <div class="p-actions">
            <button
              class="p-btn"
              type="button"
              :disabled="!previewReady || previewLoading"
              title="在新窗口打开预览"
              @click="openPreview"
            >
              <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                <path d="M4.5 13.5L13.5 4.5M13.5 4.5H6.5M13.5 4.5V11.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              新窗口打开
            </button>
            <button class="p-btn" type="button" title="跳转到 AI 对话继续生成" @click="goChat">
              <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                <path d="M10 17.5C14.1421 17.5 17.5 14.1421 17.5 10C17.5 5.85786 14.1421 2.5 10 2.5C5.85786 2.5 2.5 5.85786 2.5 10C2.5 11.7471 3.1517 13.3426 4.23688 14.5531L3.5 17L6.18218 16.3636C7.28025 16.9026 8.56112 17.5 10 17.5Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
              </svg>
              AI 对话
            </button>
          </div>
        </div>
        <div class="preview-body">
          <iframe
            v-if="previewReady"
            :src="previewUrl"
            class="preview-frame"
            title="应用实时预览"
            sandbox="allow-scripts"
          ></iframe>
          <div v-else class="preview-state">
            <div class="preview-orb">
              <div class="orb-ring"></div>
              <div class="orb-core"></div>
            </div>
            <p class="preview-state-title">等待项目运行预览</p>
            <p class="preview-state-desc">
              {{ previewLoading ? '正在连接项目服务…' : '项目可能尚未构建完成，可到 AI 对话界面重新生成' }}
            </p>
            <button v-if="!previewLoading" class="state-btn" type="button" @click="goChat">
              去 AI 对话生成
            </button>
          </div>
        </div>
      </div>

      <div v-if="appId || isAdmin" class="timeline-wrap">
        <!-- ========== 对话时间线 ========== -->
        <ul
          v-if="chatHistory.length > 0"
          :key="listKey"
          class="timeline"
          :class="{ 'is-loading': loading }"
        >
          <li
            v-for="(record, index) in chatHistory"
            :key="record.id"
            class="msg-card"
            :class="record.messageType === 'user' ? 'is-user' : 'is-ai'"
            :style="{ animationDelay: `${index * 45}ms` }"
          >
            <!-- 左侧标识 -->
            <div class="card-mark" aria-hidden="true">
              <template v-if="record.messageType === 'user'">
                <svg viewBox="0 0 20 20" fill="none" class="mark-icon">
                  <circle cx="10" cy="6" r="3.5" stroke="currentColor" stroke-width="1.4" />
                  <path d="M2.5 17.5C2.5 14.4624 5.85786 12 10 12C14.1421 12 17.5 14.4624 17.5 17.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
                </svg>
              </template>
              <template v-else>
                <span class="ai-mark">AI</span>
              </template>
            </div>

            <!-- 卡片主体 -->
            <div class="card-body">
              <!-- 代码块风格对话框 -->
              <div
                class="bubble"
                :class="{ expanded: expandedIds.has(String(record.id)) }"
                @click="toggleExpand(record.id)"
              >
                <div class="bubble-bar">
                  <span class="b-dots" aria-hidden="true">
                    <i class="dot dot-1"></i>
                    <i class="dot dot-2"></i>
                    <i class="dot dot-3"></i>
                  </span>
                  <span class="b-label">{{ record.messageType === 'user' ? 'USER / PROMPT' : 'AI / RESPONSE' }}</span>
                  <span v-if="isClamped(record)" class="b-toggle">{{ expandedIds.has(String(record.id)) ? '收起' : '展开' }}</span>
                  <button
                    class="b-copy"
                    type="button"
                    title="复制内容"
                    @click.stop="copyText(record.message)"
                  >
                    <svg viewBox="0 0 20 20" fill="none" class="act-icon">
                      <rect x="6.5" y="6.5" width="10" height="10" rx="1.5" stroke="currentColor" stroke-width="1.4" />
                      <path d="M13.5 6.5V5C13.5 3.89543 12.6046 3 11.5 3H5C3.89543 3 3 3.89543 3 5V11.5C3 12.6046 3.89543 13.5 5 13.5H6.5" stroke="currentColor" stroke-width="1.4" />
                    </svg>
                  </button>
                </div>
                <div class="bubble-body">{{ record.message }}</div>
              </div>

              <!-- 底部元数据 -->
              <div class="card-bottom">
                <div class="meta-ids">
                  <span class="meta-id" :title="record.id">ID: {{ formatId(record.id) }}</span>
                  <span class="meta-id" :title="record.appId">APP: {{ formatId(record.appId) }}</span>
                  <span v-if="isAdmin && !appId" class="meta-id" :title="record.userId">
                    USER: {{ formatId(record.userId) }}
                  </span>
                </div>
                <span class="time">{{ formatTime(record.createTime) }}</span>
              </div>
            </div>
          </li>
        </ul>

        <!-- 空状态 -->
        <div v-else-if="!loading" class="timeline-empty">
          <div class="empty-icon">⌁</div>
          <p>暂无对话记录</p>
        </div>

        <!-- 底部操作 -->
        <div v-if="!isAdmin && chatHistory.length > 0" class="timeline-foot">
          <span class="foot-count">共 {{ chatHistory.length }} 条对话记录</span>
          <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? '加载中…' : '加载更多' }}
            <svg viewBox="0 0 20 20" fill="none" class="foot-arrow">
              <path d="M10 4V16M10 16L5.5 11.5M10 16L14.5 11.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
        </div>

        <!-- 管理员分页 -->
        <div v-if="isAdmin" class="timeline-pagination">
          <a-pagination
            v-model:current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 条`"
            @change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, theme } from 'ant-design-vue'
import {
  listAppChatHistory,
  listAllChatHistoryByPageForAdmin,
} from '@/api/chatHistoryController'
import { createPreviewToken } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const themeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: '#e2e8f0',
    colorBgContainer: 'rgba(255, 255, 255, 0.03)',
    colorBgElevated: '#131316',
    colorBorder: 'rgba(255, 255, 255, 0.1)',
    colorText: 'rgba(255, 255, 255, 0.85)',
    colorTextSecondary: 'rgba(255, 255, 255, 0.5)',
    borderRadius: 10,
  },
}

const loading = ref(false)
const loadingMore = ref(false)
const chatHistory = ref<API.ChatHistory[]>([])
const lastCreateTime = ref<string | undefined>(undefined)
const hasMore = ref(false)
const expandedIds = ref<Set<string>>(new Set())

/* ---------- 项目实时预览（nginx 提供 vue 工程运行效果） ---------- */
const previewUrl = ref('')
const previewReady = ref(false)
const previewLoading = ref(false)

const loadPreview = async () => {
  if (!appId.value || previewReady.value) return
  previewLoading.value = true
  try {
    // 检查 nginx 预览服务是否已就绪（未构建完成时不会返回 text/html）
    const check = await fetch(`/api/app/preview/${appId.value}/?t=${Date.now()}`, {
      credentials: 'include',
      cache: 'no-store',
    })
    const contentType = check.headers.get('content-type') || ''
    if (!check.ok || !contentType.includes('text/html')) return

    const tokenRes = await createPreviewToken({ appId: appId.value })
    if (tokenRes.data.code === 0 && tokenRes.data.data) {
      previewUrl.value = `/api/app/preview/${appId.value}/${tokenRes.data.data}/?t=${Date.now()}`
      previewReady.value = true
    }
  } catch {
    // 忽略，保持「未就绪」状态
  } finally {
    previewLoading.value = false
  }
}

const openPreview = () => {
  if (!previewUrl.value) return
  window.open(previewUrl.value, '_blank', 'noopener,noreferrer')
}

const goChat = () => {
  router.push(`/app/chat/${appId.value}`)
}

const appId = computed(() => (route.query.appId as string) || '')
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const pageTitle = computed(() => {
  if (appId.value) return '对话历史'
  return isAdmin.value ? '对话历史管理' : '对话历史'
})
const listKey = computed(() => (isAdmin.value ? `p-${pagination.current}` : 'app'))

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

/* ---------- 返回上一页 ---------- */
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

/* ---------- 格式化 ---------- */
const formatId = (id?: string) => {
  if (!id) return '-'
  const s = String(id)
  if (s.length <= 10) return s
  return s.slice(0, 4) + '…' + s.slice(-4)
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

/* 是否超过 3 行需要展开按钮 */
const CLAMP_LINES = 3
const isClamped = (record: API.ChatHistory) => {
  const text = record.message || ''
  if (text.length <= 60) return false
  return text.split('\n').length > CLAMP_LINES || text.length > 120
}

/* ---------- 交互 ---------- */
const toggleExpand = (id?: string) => {
  if (!id) return
  const key = String(id)
  const next = new Set(expandedIds.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  expandedIds.value = next
}

const copyText = async (text?: string) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    message.success('内容已复制')
  } catch {
    message.error('复制失败，请手动选择')
  }
}

/* ---------- 数据加载 ---------- */
const loadAppHistory = async (reset = true) => {
  if (!appId.value) return
  if (reset) {
    loading.value = true
    chatHistory.value = []
    lastCreateTime.value = undefined
  } else {
    loadingMore.value = true
  }
  try {
    const res = await listAppChatHistory({
      appId: appId.value,
      pageSize: 20,
      lastCreateTime: reset ? undefined : lastCreateTime.value,
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      chatHistory.value = reset ? records : [...chatHistory.value, ...records]
      lastCreateTime.value = records.length > 0 ? records[records.length - 1].createTime : undefined
      hasMore.value = records.length >= 20
    } else {
      message.error('加载失败：' + res.data.message)
    }
  } catch {
    message.error('加载对话历史失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  loadAppHistory(false)
}

const loadAdminHistory = async () => {
  loading.value = true
  try {
    const res = await listAllChatHistoryByPageForAdmin({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      appId: appId.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      chatHistory.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    } else {
      message.error('加载失败：' + res.data.message)
    }
  } catch {
    message.error('加载对话历史失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = () => {
  expandedIds.value = new Set()
  loadAdminHistory()
}

onMounted(async () => {
  if (!loginUserStore.loginUser.id) {
    await loginUserStore.fetchLoginUser()
  }
  if (appId.value) {
    loadAppHistory()
    loadPreview()
  } else if (isAdmin.value) {
    loadAdminHistory()
  }
})
</script>

<style scoped>
/* ========== 页面骨架：石墨黑 ========== */
.history-page {
  min-height: calc(100vh - 64px);
  padding: 32px 20px 48px;
  background:
    radial-gradient(800px 420px at 80% -10%, rgba(255, 255, 255, 0.045), transparent 60%),
    radial-gradient(600px 360px at 8% 108%, rgba(255, 255, 255, 0.03), transparent 55%),
    #0a0a0c;
}

/* 细网格质感 */
.history-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 44px 44px;
  -webkit-mask-image: radial-gradient(ellipse 90% 70% at 50% 20%, #000 15%, transparent 75%);
  mask-image: radial-gradient(ellipse 90% 70% at 50% 20%, #000 15%, transparent 75%);
  pointer-events: none;
  z-index: 0;
}

.history-page > * {
  position: relative;
  z-index: 1;
}

/* ========== 顶部：返回按钮 + 标题 ========== */
.page-head {
  max-width: 900px;
  margin: 0 auto 26px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  transition: all 0.25s ease;
}

.back-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.09);
  border-color: rgba(255, 255, 255, 0.22);
  transform: translateX(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.45);
}

.back-btn:active {
  transform: translateX(-2px) scale(0.96);
}

.back-icon {
  width: 19px;
  height: 19px;
}

.head-text {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.page-head h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #f4f4f5;
}

.app-filter-tag {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 3px 10px;
  border-radius: 100px;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

/* ========== 项目实时预览卡片 ========== */
.preview-card {
  max-width: 900px;
  margin: 0 auto 26px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #0b0b0e;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

.preview-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.04);
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.p-dots {
  display: flex;
  gap: 6px;
}

.p-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.p1 { background: #ff5f57; }
.p2 { background: #febc2e; }
.p3 { background: #28c840; }

.p-title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1.2px;
  color: rgba(255, 255, 255, 0.6);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.p-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  padding: 3px 10px;
  border-radius: 100px;
}

.p-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.p-status.is-on {
  color: #6ee7b7;
  background: rgba(52, 211, 153, 0.12);
  border: 1px solid rgba(52, 211, 153, 0.3);
}

.p-status.is-on i {
  background: #34d399;
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.8);
  animation: statusPulse 1.4s ease-in-out infinite;
}

.p-status.is-off {
  color: rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.p-status.is-off i {
  background: rgba(255, 255, 255, 0.35);
}

@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

.p-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.p-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.p-btn:hover:not(:disabled) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.28);
  box-shadow: 0 0 16px rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

.p-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.p-btn svg {
  width: 14px;
  height: 14px;
}

.preview-body {
  height: 420px;
  background: #0d0d10;
  position: relative;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: #ffffff;
}

/* 未就绪状态 */
.preview-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
  padding: 24px;
}

.preview-orb {
  position: relative;
  width: 56px;
  height: 56px;
  margin-bottom: 8px;
}

.orb-ring {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  animation: orbPulse 2s ease-in-out infinite;
}

.orb-core {
  position: absolute;
  inset: 14px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0.12));
  border-radius: 50%;
}

@keyframes orbPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.25); opacity: 0.4; }
}

.preview-state-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}

.preview-state-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  line-height: 1.6;
}

.state-btn {
  margin-top: 8px;
  padding: 8px 20px;
  font-size: 13px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 100px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.state-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.1);
}

@media (prefers-reduced-motion: reduce) {
  .orb-ring,
  .p-status.is-on i {
    animation: none !important;
  }
}

/* ========== 居中阅读区 ========== */
.timeline-wrap {
  max-width: 900px;
  margin: 0 auto;
}

/* ========== 时间线列表 ========== */
.timeline {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline.is-loading {
  opacity: 0.5;
  pointer-events: none;
}

/* ========== 消息卡片 ========== */
.msg-card {
  display: flex;
  gap: 14px;
  padding: 6px 0;
  animation: fadeInUp 0.55s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 左侧标识 ========== */
.card-mark {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}

.msg-card.is-ai .card-mark {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 0 14px rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.msg-card.is-user .card-mark {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.5);
}

.ai-mark {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.mark-icon {
  width: 19px;
  height: 19px;
}

/* ========== 卡片主体 ========== */
.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ========== 代码块风格对话框 ========== */
.bubble {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  background: #0b0b0e;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.bubble:hover {
  border-color: rgba(255, 255, 255, 0.16);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.45);
}

/* AI 气泡：顶部细银线 + 微亮 */
.msg-card.is-ai .bubble {
  border-top: 2px solid rgba(255, 255, 255, 0.5);
}

/* 标题栏（代码块头） */
.bubble-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  user-select: none;
}

.b-dots {
  display: flex;
  gap: 5px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
}

.dot-1 { opacity: 1; }
.dot-2 { opacity: 0.75; }
.dot-3 { opacity: 0.5; }

.b-label {
  flex: 1;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1.2px;
  color: rgba(255, 255, 255, 0.45);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.b-toggle {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  transition: color 0.2s ease;
}

.bubble:hover .b-toggle {
  color: rgba(255, 255, 255, 0.7);
}

.b-copy {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
}

.bubble:hover .b-copy {
  opacity: 1;
}

.b-copy:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
}

.act-icon {
  width: 14px;
  height: 14px;
}

/* 内容区：终端式等宽文本 */
.bubble-body {
  padding: 14px 16px;
  font-size: 13px;
  line-height: 1.7;
  color: #d4d4d8;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  transition: background-color 0.25s ease;
}

.msg-card.is-ai .bubble-body {
  color: #e4e4e7;
}

.bubble.expanded .bubble-body {
  -webkit-line-clamp: unset;
  overflow: visible;
}

/* ========== 底部元数据 ========== */
.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 6px;
}

.meta-ids {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.meta-id {
  font-size: 12px;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  color: rgba(255, 255, 255, 0.25);
  white-space: nowrap;
}

.time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.32);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  white-space: nowrap;
}

/* ========== 空状态 ========== */
.timeline-empty {
  padding: 90px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 14px;
}

.timeline-empty .empty-icon {
  font-size: 40px;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.2);
}

/* ========== 底部加载更多 ========== */
.timeline-foot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-top: 22px;
}

.foot-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.28);
}

.load-more-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 24px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 100px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.load-more-btn:hover:not(:disabled) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.08);
}

.load-more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.foot-arrow {
  width: 14px;
  height: 14px;
}

/* ========== 管理员分页 ========== */
.timeline-pagination {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

:deep(.ant-pagination) {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 10px 14px;
}

@media (max-width: 640px) {
  .preview-body {
    height: 300px;
  }

  .p-btn {
    padding: 5px 9px;
  }

  .p-btn svg {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .msg-card {
    animation: none;
  }
}
</style>
