<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppsByPage, listFeaturedAppsByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 优化提示词功能已移除

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面
      const appId = res.data.data
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppsByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listFeaturedAppsByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId?: string | number) => {
  if (appId) {
    router.push(`/app/chat/${String(appId)}`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 格式化时间函数已移除，不再需要显示创建时间

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<template>
  <div id="homePage">
    <div class="container">
      <!-- 网站标题和描述 -->
      <div class="hero-section">
        <h1 class="hero-title">根据灵感 生成代码</h1>
        <p class="hero-description">一句话轻松创建网站应用</p>
      </div>

      <!-- 用户提示词输入框 -->
      <div class="input-section">
        <a-textarea
          v-model:value="userPrompt"
          placeholder="帮我创建个人博客网站"
          :rows="4"
          :maxlength="1000"
          class="prompt-input"
        />
        <div class="input-actions">
          <a-button type="primary" size="large" @click="createApp" :loading="creating">
            <template #icon>
              <span>↑</span>
            </template>
          </a-button>
        </div>
      </div>

      <!-- 快捷按钮 -->
      <div class="quick-actions">
        <a-button
          type="default"
          @click="
            setPrompt(
              '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁的设计风格，支持响应式布局，文章支持Markdown格式，首页展示最新文章和热门推荐。',
            )
          "
          >个人博客网站</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面。采用商务风格的设计，包含轮播图、产品展示卡片、团队介绍、客户案例展示，支持多语言切换和在线客服功能。',
            )
          "
          >企业官网</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '构建一个功能完整的在线商城，包含商品展示、购物车、用户注册登录、订单管理、支付结算等功能。设计现代化的商品卡片布局，支持商品搜索筛选、用户评价、优惠券系统和会员积分功能。',
            )
          "
          >在线商城</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '制作一个精美的作品展示网站，适合设计师、摄影师、艺术家等创作者。包含作品画廊、项目详情页、个人简历、联系方式等模块。采用瀑布流或网格布局展示作品，支持图片放大预览和作品分类筛选。',
            )
          "
          >作品展示网站</a-button
        >
      </div>

      <!-- 我的作品 - 只在用户登录时显示 -->
      <div v-if="loginUserStore.loginUser.id" class="section my-works-section">
        <h2 class="section-title">我的作品</h2>
        <div class="app-grid">
          <AppCard
            v-for="app in myApps"
            :key="app.id"
            :app="app"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div v-if="myApps.length === 0" class="empty-state">
          <div class="empty-icon">📝</div>
          <p class="empty-text">还没有创建任何应用</p>
          <p class="empty-subtext">使用上面的输入框开始创建您的第一个应用吧</p>
        </div>
        <div v-else class="pagination-wrapper">
          <a-pagination
            v-model:current="myAppsPage.current"
            v-model:page-size="myAppsPage.pageSize"
            :total="myAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个应用`"
            @change="loadMyApps"
          />
        </div>
      </div>

      <!-- 精选案例 -->
      <div class="section">
        <h2 class="section-title">精选案例</h2>
        <div class="featured-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="app.id"
            :app="app"
            :featured="true"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="featuredAppsPage.current"
            v-model:page-size="featuredAppsPage.pageSize"
            :total="featuredAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个案例`"
            @change="loadFeaturedApps"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background: #0a0a0a;
  position: relative;
  overflow-x: hidden;
}

/* 简洁高性能的网格背景 */
#homePage::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  pointer-events: none;
  z-index: 1;
}

/* 微妙的中心光晕效果 */
#homePage::after {
  content: '';
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 800px;
  height: 800px;
  background: radial-gradient(
    circle,
    rgba(96, 165, 250, 0.08) 0%,
    rgba(96, 165, 250, 0.04) 25%,
    transparent 70%
  );
  pointer-events: none;
  z-index: 1;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
  position: relative;
  z-index: 2;
}

/* 英雄区域 */
.hero-section {
  text-align: center;
  padding: 100px 0 60px;
  margin-bottom: 50px;
  position: relative;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 20px;
  line-height: 1.1;
  color: #ffffff;
  letter-spacing: -1.5px;
}

.hero-description {
  font-size: 20px;
  margin: 0;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 400;
  letter-spacing: 0.3px;
}

/* 输入区域 - 性能优化 */
.input-section {
  position: relative;
  margin: 0 auto 50px;
  max-width: 720px;
}

.prompt-input {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  font-size: 16px;
  padding: 18px 60px 18px 18px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(8px);
  width: 100%;
  color: #ffffff;
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.prompt-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.prompt-input:focus {
  border-color: rgba(96, 165, 250, 0.4);
  background: rgba(255, 255, 255, 0.08);
  outline: none;
  box-shadow: 
    0 8px 24px rgba(96, 165, 250, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.input-actions {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.input-actions .ant-btn {
  background: linear-gradient(135deg, #ffffff 0%, #f0f0f0 100%);
  border: none;
  border-radius: 10px;
  height: 42px;
  width: 42px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0a0a0a;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.input-actions .ant-btn:hover {
  background: linear-gradient(135deg, #ffffff 0%, #e0e0e0 100%);
  box-shadow: 0 6px 16px rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

/* 快捷按钮 - 性能优化 */
.quick-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 60px;
  flex-wrap: wrap;
}

.quick-actions .ant-btn {
  border-radius: 10px;
  padding: 10px 20px;
  height: auto;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.quick-actions .ant-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(96, 165, 250, 0.4);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(96, 165, 250, 0.2);
  transform: translateY(-2px);
}

/* 区域容器 - 玻璃拟态效果 */
.section {
  margin-bottom: 60px;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 36px 32px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

/* 我的作品区域特殊样式 */
.my-works-section {
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 32px;
  color: #ffffff;
  position: relative;
  padding-bottom: 16px;
  text-align: center;
  letter-spacing: -0.5px;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 50px;
  height: 2px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.4;
}

.empty-text {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 600;
  margin: 0 0 8px 0;
}

.empty-subtext {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
  line-height: 1.6;
}

/* 网格布局 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 24px;
  color: #ffffff;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

:deep(.ant-pagination .ant-pagination-item) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.ant-pagination .ant-pagination-item-active) {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
}

:deep(.ant-pagination .ant-pagination-item a) {
  color: rgba(255, 255, 255, 0.7);
}

:deep(.ant-pagination .ant-pagination-item-active a) {
  color: #ffffff;
}

:deep(.ant-pagination .ant-pagination-prev .ant-pagination-item-link),
:deep(.ant-pagination .ant-pagination-next .ant-pagination-item-link) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
}

:deep(.ant-pagination .ant-pagination-prev:hover .ant-pagination-item-link),
:deep(.ant-pagination .ant-pagination-next:hover .ant-pagination-item-link),
:deep(.ant-pagination .ant-pagination-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

:deep(.ant-pagination .ant-pagination-item:hover a) {
  color: #ffffff;
}

/* 卡片样式 - 玻璃拟态效果 */
:deep(.app-card) {
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

:deep(.app-card:hover) {
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
  transform: translateY(-4px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 40px;
  }

  .hero-description {
    font-size: 18px;
  }

  .hero-section {
    padding: 80px 0 50px;
  }

  .section {
    padding: 28px 24px;
    margin-bottom: 40px;
  }

  .app-grid,
  .featured-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .quick-actions {
    gap: 10px;
  }

  .quick-actions .ant-btn {
    font-size: 13px;
    padding: 8px 16px;
  }

  .section-title {
    font-size: 24px;
  }

  .container {
    padding: 16px;
  }

  .prompt-input {
    padding: 16px 52px 16px 16px;
  }

  .empty-state {
    padding: 40px 16px;
  }

  .empty-icon {
    font-size: 40px;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-description {
    font-size: 16px;
  }

  .input-section {
    max-width: 100%;
  }

  .prompt-input {
    font-size: 15px;
    padding: 16px 50px 16px 16px;
  }

  .input-actions .ant-btn {
    width: 38px;
    height: 38px;
  }

  .section {
    padding: 24px 20px;
  }

  .section-title {
    font-size: 22px;
  }
}
</style>
