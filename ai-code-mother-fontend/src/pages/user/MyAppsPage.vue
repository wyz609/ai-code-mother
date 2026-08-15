<template>
  <main class="my-apps-page">
    <!-- 背景装饰（固定层，不随滚动重绘） -->
    <div class="bg-decor" aria-hidden="true">
      <div class="bg-orb orb-1"></div>
      <div class="bg-orb orb-2"></div>
    </div>

    <div class="page-container">
      <!-- ========== 页面头部 ========== -->
      <header class="page-head">
        <button class="back-btn" type="button" title="返回上一页" aria-label="返回上一页" @click="goBack">
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path d="M12.5 4L6.5 10L12.5 16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
        <div class="head-info">
          <h1>
            我的作品
            <span class="count-badge">{{ page.total }}</span>
          </h1>
          <p>管理已生成的网站、部署版本与源码</p>
        </div>
        <div class="head-actions">
          <div class="view-switch" role="tablist" aria-label="视图切换">
            <button
              type="button"
              class="view-btn"
              :class="{ active: viewMode === 'grid' }"
              title="卡片视图"
              aria-label="卡片视图"
              @click="viewMode = 'grid'"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                <rect x="3" y="3" width="7" height="7" rx="1.5"/>
                <rect x="14" y="3" width="7" height="7" rx="1.5"/>
                <rect x="3" y="14" width="7" height="7" rx="1.5"/>
                <rect x="14" y="14" width="7" height="7" rx="1.5"/>
              </svg>
            </button>
            <button
              type="button"
              class="view-btn"
              :class="{ active: viewMode === 'table' }"
              title="列表视图"
              aria-label="列表视图"
              @click="viewMode = 'table'"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                <path d="M8 6h13M8 12h13M8 18h13"/>
                <path d="M3.5 6h.01M3.5 12h.01M3.5 18h.01" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <a-button type="primary" class="create-btn" @click="router.push('/')">
            <template #icon>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="M12 5v14M5 12h14"/>
              </svg>
            </template>
            创建应用
          </a-button>
        </div>
      </header>

      <!-- ========== 卡片视图 ========== -->
      <section v-if="viewMode === 'grid'" class="work-grid">
        <!-- 骨架屏 -->
        <template v-if="loading">
          <div v-for="i in 6" :key="i" class="work-card skeleton-card">
            <div class="sk-cover"></div>
            <div class="card-body">
              <div class="sk-line w-60"></div>
              <div class="sk-line w-90"></div>
              <div class="sk-line w-40"></div>
            </div>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else-if="apps.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" aria-hidden="true">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <line x1="3" y1="9" x2="21" y2="9"/>
              <line x1="9" y1="21" x2="9" y2="9"/>
            </svg>
          </div>
          <p class="empty-title">还没有任何作品</p>
          <p class="empty-desc">去首页输入想法，AI 将为你生成第一个应用</p>
          <a-button type="primary" class="create-btn" @click="router.push('/')">去创建</a-button>
        </div>

        <!-- 作品卡片 -->
        <article
          v-for="(app, index) in apps"
          v-else
          :key="app.id"
          class="work-card"
          :style="{ animationDelay: `${index * 45}ms` }"
          @click="openChat(app)"
        >
          <div class="card-cover">
            <img v-if="app.cover" :src="app.cover" :alt="`${app.appName} 的预览图`" loading="lazy" />
            <div v-else class="cover-placeholder" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2">
                <polyline points="16 18 22 12 16 6"/>
                <polyline points="8 6 2 12 8 18"/>
              </svg>
              <span>{{ app.codeGenType || 'html' }}</span>
            </div>
            <span :class="['deploy-badge', app.deployKey ? 'is-live' : '']">
              <i></i>
              {{ app.deployKey ? '已部署' : '未部署' }}
            </span>
          </div>

          <div class="card-body">
            <div class="card-title-row">
              <h2 class="card-name" :title="app.appName">{{ app.appName || '未命名应用' }}</h2>
              <span class="type-tag">{{ app.codeGenType || 'html' }}</span>
            </div>
            <p class="card-prompt" :title="app.initPrompt">{{ app.initPrompt || '暂无描述' }}</p>
            <div class="card-meta">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                <circle cx="12" cy="12" r="9"/>
                <path d="M12 7v5l3 2"/>
              </svg>
              {{ formatTime(app.createTime) }}
            </div>
            <div class="card-actions">
              <button type="button" class="act-btn" title="继续对话" aria-label="继续对话" @click.stop="openChat(app)">
                <MessageSquare :size="16" />
              </button>
              <button type="button" class="act-btn" title="应用设置" aria-label="应用设置" @click.stop="openSettings(app)">
                <Settings :size="16" />
              </button>
              <button
                type="button"
                class="act-btn"
                title="部署应用"
                aria-label="部署应用"
                :disabled="deployingId === app.id"
                @click.stop="deploy(app)"
              >
                <span v-if="deployingId === app.id" class="mini-spinner"></span>
                <Rocket v-else :size="16" />
              </button>
              <button type="button" class="act-btn" title="下载源码" aria-label="下载源码" @click.stop="download(app)">
                <Download :size="16" />
              </button>
              <button type="button" class="act-btn danger" title="删除应用" aria-label="删除应用" @click.stop="remove(app)">
                <Trash2 :size="16" />
              </button>
            </div>
          </div>
        </article>
      </section>

      <!-- ========== 列表（表格）视图 ========== -->
      <section v-else class="workbench">
        <a-table
          :columns="columns"
          :data-source="apps"
          :loading="loading"
          :pagination="false"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'work'">
              <div class="work-cell">
                <img v-if="record.cover" class="cover" :src="record.cover" :alt="`${record.appName} 的预览图`" loading="lazy" />
                <div v-else class="cover-placeholder cover-sm" aria-hidden="true"></div>
                <div class="work-info">
                  <strong>{{ record.appName || '未命名应用' }}</strong>
                  <span>{{ record.codeGenType || 'html' }}</span>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'prompt'">
              <span class="prompt">{{ record.initPrompt || '暂无描述' }}</span>
            </template>
            <template v-else-if="column.key === 'deploy'">
              <span :class="['deploy-status', record.deployKey ? 'is-live' : '']">
                <i></i>
                {{ record.deployKey ? '已部署' : '未部署' }}
              </span>
            </template>
            <template v-else-if="column.key === 'action'">
              <div class="action-group">
                <a-tooltip title="继续对话"><a-button type="text" aria-label="继续对话" @click="openChat(record)"><MessageSquare :size="17" /></a-button></a-tooltip>
                <a-tooltip title="应用设置"><a-button type="text" aria-label="应用设置" @click="openSettings(record)"><Settings :size="17" /></a-button></a-tooltip>
                <a-tooltip title="部署应用"><a-button type="text" aria-label="部署应用" :loading="deployingId === record.id" @click="deploy(record)"><Rocket :size="17" /></a-button></a-tooltip>
                <a-tooltip title="下载源码"><a-button type="text" aria-label="下载源码" @click="download(record)"><Download :size="17" /></a-button></a-tooltip>
                <a-tooltip title="删除应用"><a-button danger type="text" aria-label="删除应用" @click="remove(record)"><Trash2 :size="17" /></a-button></a-tooltip>
              </div>
            </template>
          </template>
        </a-table>
      </section>

      <!-- ========== 分页 ========== -->
      <div v-if="page.total > page.pageSize" class="pager">
        <a-pagination
          v-model:current="page.current"
          :page-size="page.pageSize"
          :total="page.total"
          :show-size-changer="false"
          :show-total="(total: number) => `共 ${total} 个作品`"
          @change="loadApps"
        />
      </div>
    </div>
  </main>

  <!-- 修改应用弹窗 -->
  <EditAppModal
    v-model:open="editModalOpen"
    :app-id="editingApp?.id"
    :app-name="editingApp?.appName"
    @saved="handleAppEdited"
  />
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { Download, MessageSquare, Rocket, Settings, Trash2 } from 'lucide-vue-next'
import { deleteApp, deployApp, downloadAppCode, listMyAppsByPage } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import EditAppModal from '@/components/EditAppModal.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const apps = ref<API.AppVO[]>([])
const loading = ref(false)
const deployingId = ref<string | number | undefined>()
const viewMode = ref<'grid' | 'table'>('grid')
const page = reactive({ current: 1, pageSize: 12, total: 0 })

const columns = [
  { title: '作品', key: 'work', width: 280 },
  { title: '初始需求', key: 'prompt' },
  { title: '部署状态', key: 'deploy', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 235 },
]

const formatTime = (time?: string) => {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const loadApps = async () => {
  loading.value = true
  try {
    const response = await listMyAppsByPage({
      pageNum: page.current,
      pageSize: page.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })
    if (response.data.code === 0 && response.data.data) {
      apps.value = response.data.data.records || []
      page.total = response.data.data.totalRow || 0
    } else {
      message.error(response.data.message || '加载作品失败')
    }
  } finally {
    loading.value = false
  }
}

const openChat = (app: API.AppVO) => router.push(`/app/chat/${app.id}`)
const editingApp = ref<API.AppVO | null>(null)
const editModalOpen = ref(false)
const openSettings = (app: API.AppVO) => {
  editingApp.value = app
  editModalOpen.value = true
}
const handleAppEdited = () => {
  editingApp.value = null
  loadApps()
}

const deploy = async (app: API.AppVO) => {
  if (!app.id) return
  deployingId.value = app.id
  try {
    const response = await deployApp({ appId: String(app.id) })
    if (response.data.code !== 0 || !response.data.data) {
      message.error(response.data.message || '部署失败')
      return
    }
    message.success('部署完成')
    window.open(response.data.data, '_blank', 'noopener,noreferrer')
    await loadApps()
  } finally {
    deployingId.value = undefined
  }
}

const download = async (app: API.AppVO) => {
  if (!app.id) return
  try {
    const file = await downloadAppCode({ appId: String(app.id) })
    const url = URL.createObjectURL(file.data)
    const link = document.createElement('a')
    link.href = url
    link.download = `${app.appName || app.id}.zip`
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    message.error('下载源码失败')
  }
}

const remove = (app: API.AppVO) => {
  if (!app.id) return
  Modal.confirm({
    title: '删除应用',
    content: `将永久删除“${app.appName || '未命名应用'}”及其对话记录，无法恢复。`,
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      const response = await deleteApp({ id: String(app.id) })
      if (response.data.code !== 0) {
        message.error(response.data.message || '删除失败')
        return
      }
      message.success('应用已删除')
      if (apps.value.length === 1 && page.current > 1) page.current -= 1
      await loadApps()
    },
  })
}

onMounted(async () => {
  if (!loginUserStore.loginUser.id) {
    await router.replace('/user/login')
    return
  }
  await loadApps()
})
</script>

<style scoped>
/* ========== 页面骨架：石墨黑 + 细网格 + 光斑 ========== */
.my-apps-page {
  min-height: calc(100vh - 64px);
  padding: 32px 24px 56px;
  background:
    radial-gradient(900px 460px at 88% -12%, rgba(16, 185, 129, 0.05), transparent 60%),
    radial-gradient(700px 380px at -6% 108%, rgba(20, 184, 166, 0.04), transparent 55%),
    #0a0a0c;
  color: #f4f4f5;
}

.my-apps-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.024) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.024) 1px, transparent 1px);
  background-size: 44px 44px;
  -webkit-mask-image: radial-gradient(ellipse 85% 65% at 50% 15%, #000 10%, transparent 75%);
  mask-image: radial-gradient(ellipse 85% 65% at 50% 15%, #000 10%, transparent 75%);
  pointer-events: none;
  z-index: 0;
}

.my-apps-page > * {
  position: relative;
  z-index: 1;
}

.page-container {
  max-width: 1240px;
  margin: 0 auto;
}

/* ========== 页面头部 ========== */
.page-head {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 28px;
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
  transition: background-color 0.25s ease, border-color 0.25s ease, color 0.25s ease, transform 0.25s ease, box-shadow 0.25s ease;
}

.back-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.09);
  border-color: rgba(16, 185, 129, 0.45);
  transform: translateX(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.45);
}

.back-btn svg {
  width: 19px;
  height: 19px;
}

.head-info {
  flex: 1;
  min-width: 0;
}

.head-info h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.3px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.count-badge {
  font-size: 13px;
  font-weight: 600;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  color: #6ee7b7;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.35);
  padding: 2px 10px;
  border-radius: 100px;
  min-width: 34px;
  text-align: center;
}

.head-info p {
  margin: 8px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* 视图切换 */
.view-switch {
  display: flex;
  gap: 3px;
  padding: 3px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 10px;
}

.view-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.view-btn:hover {
  color: rgba(255, 255, 255, 0.85);
}

.view-btn.active {
  background: rgba(16, 185, 129, 0.16);
  color: #6ee7b7;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.15);
}

.view-btn svg {
  width: 15px;
  height: 15px;
}

/* 创建按钮 */
.create-btn.ant-btn-primary {
  background: linear-gradient(135deg, #10b981, #059669);
  border-color: transparent;
  color: #ffffff;
  font-weight: 600;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.3);
}

.create-btn.ant-btn-primary:hover {
  background: linear-gradient(135deg, #34d399, #10b981);
  border-color: transparent;
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.45);
  transform: translateY(-1px);
}

.create-btn svg {
  width: 14px;
  height: 14px;
}

/* ========== 作品卡片网格 ========== */
.work-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  content-visibility: auto;
  contain-intrinsic-size: auto 340px;
}

.work-card {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  cursor: pointer;
  animation: cardIn 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
  transition: border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease, background-color 0.3s ease;
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.work-card:hover {
  transform: translateY(-4px);
  border-color: rgba(16, 185, 129, 0.4);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03));
  box-shadow:
    0 12px 32px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(16, 185, 129, 0.12),
    0 0 24px rgba(16, 185, 129, 0.08);
}

/* 封面 */
.card-cover {
  position: relative;
  height: 120px;
  background: #0d1117;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.16);
  background:
    radial-gradient(240px 120px at 30% 20%, rgba(16, 185, 129, 0.1), transparent 65%),
    radial-gradient(220px 120px at 80% 85%, rgba(20, 184, 166, 0.08), transparent 60%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  letter-spacing: 1px;
}

.cover-placeholder svg {
  width: 34px;
  height: 34px;
}

.deploy-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  font-size: 11px;
  border-radius: 100px;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  background: rgba(10, 12, 16, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.5);
}

.deploy-badge i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
}

.deploy-badge.is-live {
  color: #6ee7b7;
  border-color: rgba(16, 185, 129, 0.4);
}

.deploy-badge.is-live i {
  background: #34d399;
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.8);
}

/* 卡片主体 */
.card-body {
  padding: 14px 16px 12px;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.card-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #f4f4f5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.type-tag {
  flex-shrink: 0;
  font-size: 10px;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: #fb923c;
  background: rgba(251, 146, 60, 0.12);
  border: 1px solid rgba(251, 146, 60, 0.3);
  padding: 2px 8px;
  border-radius: 6px;
}

.card-prompt {
  margin: 8px 0 10px;
  font-size: 12.5px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.45);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  min-height: 40px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.32);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.card-meta svg {
  width: 13px;
  height: 13px;
}

/* 操作按钮组 */
.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 10px;
}

.act-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.act-btn:hover:not(:disabled) {
  color: #6ee7b7;
  background: rgba(16, 185, 129, 0.12);
  transform: translateY(-1px);
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.15);
}

.act-btn.danger:hover:not(:disabled) {
  color: #f87171;
  background: rgba(248, 113, 113, 0.12);
  box-shadow: 0 0 12px rgba(248, 113, 113, 0.15);
}

.act-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.act-btn:focus-visible {
  outline: 2px solid rgba(16, 185, 129, 0.7);
  outline-offset: 2px;
}

.mini-spinner {
  width: 15px;
  height: 15px;
  border: 2px solid rgba(110, 231, 183, 0.3);
  border-top-color: #6ee7b7;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 骨架屏 */
.skeleton-card {
  cursor: default;
}

.sk-cover {
  height: 120px;
  background: linear-gradient(100deg, rgba(255, 255, 255, 0.03) 30%, rgba(255, 255, 255, 0.07) 50%, rgba(255, 255, 255, 0.03) 70%);
  background-size: 200% 100%;
  animation: shimmer 1.6s linear infinite;
}

.sk-line {
  height: 12px;
  border-radius: 6px;
  margin: 14px 16px 0;
  background: linear-gradient(100deg, rgba(255, 255, 255, 0.04) 30%, rgba(255, 255, 255, 0.08) 50%, rgba(255, 255, 255, 0.04) 70%);
  background-size: 200% 100%;
  animation: shimmer 1.6s linear infinite;
}

.w-60 { width: 60%; }
.w-90 { width: 90%; }
.w-40 { width: 40%; margin-bottom: 18px; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ========== 空状态 ========== */
.empty-state {
  grid-column: 1 / -1;
  padding: 90px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.empty-icon {
  width: 84px;
  height: 84px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.25);
  margin-bottom: 6px;
}

.empty-icon svg {
  width: 38px;
  height: 38px;
}

.empty-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}

.empty-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

/* ========== 表格视图 ========== */
.workbench {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
}

:deep(.ant-table) {
  background: transparent;
}

:deep(.ant-table-thead > tr > th) {
  background: rgba(255, 255, 255, 0.04);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
  font-size: 12.5px;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: rgba(255, 255, 255, 0.03);
}

.work-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.cover,
.cover-placeholder.cover-sm {
  width: 72px;
  height: 46px;
  flex: 0 0 auto;
  border-radius: 6px;
  object-fit: cover;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.work-info {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.work-info strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #f4f4f5;
}

.work-info span,
.prompt {
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
}

.prompt {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.deploy-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 13px;
}

.deploy-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
}

.deploy-status.is-live {
  color: #6ee7b7;
}

.deploy-status.is-live i {
  background: #34d399;
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.7);
}

.action-group {
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}

/* ========== 分页 ========== */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

.pager :deep(.ant-pagination) {
  display: flex;
  align-items: center;
  gap: 2px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 8px 12px;
}

.pager :deep(.ant-pagination .ant-pagination-item) {
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.pager :deep(.ant-pagination .ant-pagination-item a) {
  color: rgba(255, 255, 255, 0.82);
  font-weight: 500;
}

.pager :deep(.ant-pagination .ant-pagination-item:hover) {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.4);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
}

.pager :deep(.ant-pagination .ant-pagination-item:hover a) {
  color: #ffffff;
}

.pager :deep(.ant-pagination .ant-pagination-item-active) {
  background: linear-gradient(135deg, #10b981, #059669);
  border-color: rgba(52, 211, 153, 0.6);
  box-shadow:
    0 4px 14px rgba(16, 185, 129, 0.4),
    0 0 0 1px rgba(16, 185, 129, 0.2);
}

.pager :deep(.ant-pagination .ant-pagination-item-active:hover) {
  background: linear-gradient(135deg, #34d399, #10b981);
  border-color: rgba(52, 211, 153, 0.8);
  transform: translateY(-1px);
}

.pager :deep(.ant-pagination .ant-pagination-item-active a),
.pager :deep(.ant-pagination .ant-pagination-item-active:hover a) {
  color: #ffffff;
  font-weight: 600;
}

.pager :deep(.ant-pagination .ant-pagination-prev .ant-pagination-item-link),
.pager :deep(.ant-pagination .ant-pagination-next .ant-pagination-item-link) {
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.82);
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.pager :deep(.ant-pagination .ant-pagination-prev:hover .ant-pagination-item-link),
.pager :deep(.ant-pagination .ant-pagination-next:hover .ant-pagination-item-link) {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.4);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
}

.pager :deep(.ant-pagination .ant-pagination-disabled .ant-pagination-item-link) {
  opacity: 0.35;
  cursor: not-allowed;
}

.pager :deep(.ant-pagination .ant-pagination-ellipsis) {
  color: rgba(255, 255, 255, 0.4);
}

.pager :deep(.ant-pagination .ant-pagination-item:focus-visible),
.pager :deep(.ant-pagination .ant-pagination-item-link:focus-visible) {
  outline: 2px solid rgba(16, 185, 129, 0.7);
  outline-offset: 1px;
}

.pager :deep(.ant-pagination .ant-pagination-total-text) {
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
}

/* ========== 响应式 ========== */
@media (max-width: 720px) {
  .my-apps-page {
    padding: 24px 14px 48px;
  }

  .page-head {
    flex-wrap: wrap;
    align-items: flex-start;
    gap: 12px;
  }

  .head-info {
    order: 1;
    width: 100%;
  }

  .head-actions {
    order: 2;
    width: 100%;
    justify-content: space-between;
  }

  .create-btn {
    flex: 1;
  }

  .work-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .action-group {
    gap: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .work-card,
  .skeleton-card .sk-cover,
  .skeleton-card .sk-line {
    animation: none;
  }
}
</style>
