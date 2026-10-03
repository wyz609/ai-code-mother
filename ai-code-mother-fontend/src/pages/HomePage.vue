<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { FilePenLine, Building2, ShoppingCart, Palette, MessageSquare, Eye, Rocket, FolderOpen, Star } from 'lucide-vue-next'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppsByPage, listFeaturedAppsByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 快捷灵感模板
const CHIPS = [
  {
    text: '创建一个面向技术写作与个人品牌的现代化博客网站。核心页面：① 首页——顶部固定导航栏（Logo、栏目、搜索入口、深色/浅色主题切换），首屏大图横幅展示最新置顶文章（标题、摘要、发布时间、阅读时长），下方双栏布局：左侧文章流（卡片式列表，含封面缩略图、分类标签、摘要、点赞数和阅读数），右侧边栏（关于我简介卡片、热门文章 Top5、分类标签云）；② 文章详情页——Markdown 正文渲染，支持代码块语法高亮与一键复制、行内代码、引用块、表格，文章顶部显示面包屑与作者信息，左侧悬浮目录锚点导航随滚动高亮，文末展示上一篇/下一篇导航、分享按钮和点赞按钮；③ 分类归档页与标签聚合页——按分类/标签筛选文章，显示文章数量统计；④ 全站搜索页——支持按标题、标签、正文关键词实时搜索，结果高亮关键词；⑤ 评论系统——文章底部评论区，支持昵称、邮箱、网址输入，楼层回复与点赞，提交后即时显示；⑥ 关于我页——个人头像、技术栈图标展示、职业经历时间线、开源项目与联系方式。视觉设计：极简风格，白色背景搭配柔和灰色，使用系统字体与等宽字体渲染代码，主色调为靛蓝色，卡片圆角 8px、轻微阴影，内容区最大宽度 960px 居中，全局响应式布局，移动端导航折叠为汉堡菜单，页面切换带淡入过渡动画。',
    label: '个人博客网站',
    icon: FilePenLine,
  },
  {
    text: '设计一个大型科技公司的专业企业官网，整体采用高端商务风格。核心页面：① 首页——固定顶部导航（Logo、产品、解决方案、客户案例、关于我们、新闻中心、联系我们，右侧多语言切换与搜索图标），首屏全宽轮播 Banner（3 张高清大图轮播，支持左右箭头与圆点指示器、自动播放，每张配大标题、副标语与行动按钮），下方依次为：核心数据展示区（服务企业数、行业覆盖率、专利数量等滚动数字动画）、产品服务卡片区（6 张卡片，每张含图标、标题、简介与详情链接）、行业解决方案区（按金融/制造/医疗等行业分类的选项卡切换）、客户案例展示区（Logo 墙 + 精选案例卡片）、新闻资讯区（最新 4 条新闻列表带日期与标签）、底部 CTA 横幅与页脚（多列链接、备案信息、社交图标）；② 产品服务页——按产品线分类展示，每个产品含功能亮点列表、截图与试用申请按钮；③ 新闻资讯页——新闻列表分页，详情页含正文、发布时间、浏览量与相关新闻推荐；④ 联系我们页——公司地址、电话邮箱信息卡、百度地图占位、在线留言表单（姓名、公司、电话、需求描述，带表单校验与提交成功提示）；⑤ 关于我们页——企业简介、发展历程时间线、核心团队（头像、姓名、职位）、企业文化与荣誉资质。视觉设计：深蓝为主色、白色背景、金色点缀，专业稳重的商务配色，无衬线字体，圆角按钮带悬停阴影过渡，各区块间距统一，全局响应式适配，页面滚动时导航栏吸顶并添加阴影。',
    label: '企业官网',
    icon: Building2,
  },
  {
    text: '构建一个功能完整的 B2C 在线商城网站。核心功能模块：① 首页——顶部搜索栏（支持关键词联想与热门搜索词标签）、左侧商品分类导航菜单（多级分类，悬停展开）、首屏轮播活动 Banner、限时秒杀倒计时区块（展示商品图片、原价、秒杀价与剩余库存进度条）、新品上架与热卖推荐商品瀑布流；② 商品列表页——支持按分类、价格区间、品牌、销量多条件筛选，支持综合/销量/价格/上新排序，商品卡片展示图片、名称、价格（红色现价+灰色划线原价）、销量与收藏按钮，支持分页与加载更多；③ 商品详情页——大图轮播与缩略图切换、SKU 规格选择（颜色/尺寸按钮组，选中态高亮，无库存置灰）、数量加减器、加入购物车与立即购买按钮、优惠信息展示、商品参数表格、图文详情与用户评价区（含评分星级、晒图与追评）；④ 购物车页——商品列表支持勾选、数量修改、删除，实时计算选中商品总价，支持全选与结算按钮；⑤ 订单结算与支付页——收货地址管理（新增/编辑/设为默认）、配送方式选择、订单金额明细、模拟支付流程（支付成功后显示订单完成页与订单号）；⑥ 用户中心——登录/注册页（账号密码+验证码，表单校验）、我的订单（按待付款/待发货/待收货/已完成分标签页）、收藏夹、优惠券中心与会员积分记录。视觉设计：现代电商风格，主色橙色、辅助深灰，白色卡片背景，商品卡片圆角、悬停上浮阴影，图标统一使用线性图标，全局响应式适配移动端（商品列表两列网格）。',
    label: '在线商城',
    icon: ShoppingCart,
  },
  {
    text: '制作一个面向设计师/摄影师/艺术家的个人作品集展示网站，强调视觉冲击力与艺术感。核心页面：① 首页——全屏首屏展示个人品牌：超大号姓名标题（粗体大字排版）、一句个人定位 Slogan、向下滚动提示箭头，下方为作品画廊：支持「全部/平面设计/品牌设计/UI设计/插画/摄影」分类筛选按钮组（选中态高亮），作品采用等宽网格布局（桌面端 3 列、平板 2 列、手机 1 列），每件作品卡片展示封面图、作品名与分类，鼠标悬停时封面缩放、叠加半透明遮罩并显示查看详情按钮，点击进入详情页；② 作品详情页——大图展示区（支持点击放大为全屏灯箱，带左右切换与键盘方向键操作、底部缩略图导航），右侧或下方展示作品标题、完成年份、所属分类、项目背景、设计思路与创作过程文字说明，底部提供「上一件/下一件」导航；③ 关于我页——个人照片、简介段落、专业技能列表（带熟练度进度条）、工作与参展经历时间线、获奖荣誉列表、合作过的品牌 Logo 墙；④ 联系方式页——联系表单（姓名、邮箱、项目类型下拉选择、预算区间、需求描述，提交后显示感谢提示）、邮箱与社交平台（微博/微信/Instagram/Behance）图标链接、页脚版权信息。视觉设计：极简黑白灰基调、大量留白，强调字体排版对比（标题使用粗黑体），图片带圆角与细边框，页面元素滚动进入时使用淡入上移动画，全站响应式适配。',
    label: '作品展示',
    icon: Palette,
  },
]

const activeChip = ref(-1)

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
const setPrompt = (prompt: string, idx: number) => {
  userPrompt.value = prompt
  activeChip.value = idx
}

watch(userPrompt, v => {
  if (activeChip.value >= 0 && v !== CHIPS[activeChip.value].text) {
    activeChip.value = -1
  }
})

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
      // 跳转到对话页面，带上初始提示词
      const appId = res.data.data
      const prompt = encodeURIComponent(userPrompt.value.trim())
      await router.push(`/app/chat/${appId}?initPrompt=${prompt}`)
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

// 查看对话历史
const viewHistory = (appId?: string | number) => {
  if (appId) {
    router.push(`/user/chatHistoryManage?appId=${String(appId)}`)
  }
}

const viewMyApps = () => {
  router.push('/user/apps')
}

// 查看作品（打开部署后的线上版本）
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 滚动渐显动画
let observer: IntersectionObserver | null = null

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()

  observer = new IntersectionObserver(
    entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('in-view')
          observer?.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.12 },
  )

  requestAnimationFrame(() => {
    document.querySelectorAll('#homePage .reveal').forEach(el => observer?.observe(el))
  })
})

onBeforeUnmount(() => {
  observer?.disconnect()
})
</script>

<template>
  <div id="homePage">
    <!-- 背景装饰 -->
    <div class="bg-decor" aria-hidden="true">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <span class="float-code f1">&lt;/&gt;</span>
      <span class="float-code f2">{ }</span>
      <span class="float-code f3">=&gt;</span>
    </div>

    <div class="container">
      <!-- 网站标题和描述 -->
      <section class="hero-section">
        <div class="hero-badge">
          <span class="hb-dot"></span>
          AI 零代码生成平台 · 让创意触手可及
        </div>
        <h1 class="hero-title">
          根据灵感
          <span class="grad-text">生成代码</span>
        </h1>
        <p class="hero-description">一句话轻松创建网站应用，所见即所得</p>

        <!-- 三个特性小点 -->
        <div class="hero-tags">
          <span class="hero-tag"><MessageSquare :size="13" /> 对话式生成</span>
          <span class="hero-tag"><Eye :size="13" /> 实时预览</span>
          <span class="hero-tag"><Rocket :size="13" /> 一键部署</span>
        </div>
      </section>

      <!-- 用户提示词输入框 -->
      <section class="input-section reveal">
        <div class="prompt-box">
          <a-textarea
            v-model:value="userPrompt"
            placeholder="帮我创建个人博客网站"
            :rows="4"
            :maxlength="1000"
            class="prompt-input"
            @keydown.enter.exact.prevent="createApp"
          />
          <div class="prompt-footer">
            <span class="char-count" :class="{ warn: userPrompt.length > 900 }">
              {{ userPrompt.length }} / 1000
            </span>
            <button class="send-btn" type="button" @click="createApp" :disabled="creating">
              <svg viewBox="0 0 20 20" fill="none" class="send-icon">
                <path d="M10 16V4M10 4L5 9M10 4L15 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>{{ creating ? '生成中…' : '生成应用' }}</span>
            </button>
          </div>
        </div>
      </section>

      <!-- 快捷按钮 -->
      <section class="quick-section reveal">
        <p class="quick-label">试试这些灵感 ↓</p>
        <div class="quick-actions">
          <button
            v-for="(chip, idx) in CHIPS"
            :key="idx"
            type="button"
            class="chip-btn"
            :class="{ active: activeChip === idx }"
            @click="setPrompt(chip.text, idx)"
          >
            <span class="chip-icon"><component :is="chip.icon" :size="15" /></span>
            {{ chip.label }}
          </button>
        </div>
      </section>

      <!-- 我的作品 - 只在用户登录时显示 -->
      <section v-if="loginUserStore.loginUser.id" class="section my-works-section reveal">
        <div class="section-head">
          <h2 class="section-title">
            <span class="st-icon"><FolderOpen :size="20" /></span>
            我的作品
          </h2>
          <div class="section-actions">
            <span class="section-count">{{ myAppsPage.total }} 个</span>
            <button class="section-link" type="button" @click="viewMyApps">查看全部</button>
          </div>
        </div>
        <div class="app-grid">
          <AppCard
            v-for="app in myApps"
            :key="app.id"
            :app="app"
            clickable
            @view-chat="viewChat"
            @view-work="viewWork"
            @view-history="viewHistory"
          />
        </div>
        <div v-if="myApps.length === 0" class="empty-state">
          <div class="empty-icon"><FilePenLine :size="40" /></div>
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
      </section>

      <!-- 精选案例 -->
      <section class="section reveal">
        <div class="section-head">
          <h2 class="section-title">
            <span class="st-icon"><Star :size="20" /></span>
            精选案例
          </h2>
          <span class="section-count">{{ featuredAppsPage.total }} 个</span>
        </div>
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
      </section>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: calc(100vh - 64px);
  position: relative;
  overflow-x: hidden;
  background:
    radial-gradient(1100px 500px at 85% -10%, rgba(255, 255, 255, 0.05), transparent 60%),
    radial-gradient(900px 460px at -10% 110%, rgba(255, 255, 255, 0.03), transparent 55%),
    #0a0a0c;
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
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
  -webkit-mask-image: radial-gradient(ellipse 90% 70% at 50% 30%, #000 20%, transparent 75%);
  mask-image: radial-gradient(ellipse 90% 70% at 50% 30%, #000 20%, transparent 75%);
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
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 25%,
    transparent 70%
  );
  pointer-events: none;
  z-index: 1;
}

/* 背景光斑与悬浮代码 */
.bg-decor {
  /* fixed：装饰层不随滚动移动，滚动时零重绘 */
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.orb {
  position: absolute;
  border-radius: 50%;
  opacity: 0.35;
  animation: drift 16s ease-in-out infinite alternate;
  transform: translateZ(0);
}

.orb-1 {
  width: 380px;
  height: 380px;
  left: -140px;
  top: 120px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0.06) 40%, transparent 70%);
}

.orb-2 {
  width: 320px;
  height: 320px;
  right: -120px;
  top: 45%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0.05) 40%, transparent 70%);
  animation-delay: -6s;
}

.orb-3 {
  width: 260px;
  height: 260px;
  left: 45%;
  bottom: -120px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.04) 40%, transparent 70%);
  animation-delay: -11s;
}

@keyframes drift {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(50px, -40px) scale(1.12); }
}

.float-code {
  position: absolute;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 6px;
  padding: 5px 9px;
  background: rgba(255, 255, 255, 0.02);
  animation: floatY 8s ease-in-out infinite;
}

.f1 { left: 10%; top: 22%; }
.f2 { right: 12%; top: 30%; animation-delay: -2.5s; }
.f3 { left: 18%; top: 60%; animation-delay: -5s; }

@keyframes floatY {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-18px) rotate(2deg); }
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
  padding: 90px 0 50px;
  margin-bottom: 40px;
  position: relative;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 100px;
  margin-bottom: 24px;
  animation: fadeUp 0.8s 0.1s both;
}

.hb-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #e4e4e7;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.8);
  animation: dotPulse 2s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 20px;
  line-height: 1.1;
  color: #ffffff;
  letter-spacing: -1.5px;
  animation: fadeUp 0.8s 0.2s both;
}

.grad-text {
  background: linear-gradient(90deg, #ffffff 0%, #a1a1aa 55%, #71717a 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-description {
  font-size: 20px;
  margin: 0;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 400;
  letter-spacing: 0.3px;
  animation: fadeUp 0.8s 0.3s both;
}

.hero-tags {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 26px;
  flex-wrap: wrap;
  animation: fadeUp 0.8s 0.4s both;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  padding: 6px 14px;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, transform 0.25s ease;
}

.hero-tag:hover {
  color: #f4f4f5;
  border-color: rgba(255, 255, 255, 0.35);
  box-shadow: 0 0 18px rgba(255, 255, 255, 0.08);
  transform: translateY(-2px);
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: none; }
}

/* 输入区域 */
.input-section {
  margin: 0 auto 40px;
  max-width: 720px;
}

.prompt-box {
  position: relative;
  border-radius: 16px;
  padding: 8px;
  background: #0b0b0e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, color 0.3s ease, transform 0.3s ease;
}

.prompt-box:focus-within {
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow:
    0 0 0 3px rgba(255, 255, 255, 0.06),
    0 12px 40px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.prompt-input {
  border-radius: 10px;
  border: none;
  font-size: 16px;
  padding: 14px 16px;
  background: transparent;
  width: 100%;
  color: #ffffff;
  box-shadow: none;
  transition: none;
}

.prompt-input::placeholder {
  color: rgba(255, 255, 255, 0.35);
}

.prompt-input:focus {
  background: transparent;
  outline: none;
  box-shadow: none;
}

:deep(.prompt-input textarea) {
  resize: none;
}

.prompt-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 8px 4px;
}

.char-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  letter-spacing: 0.5px;
  transition: color 0.2s ease;
}

.char-count.warn {
  color: #fbbf24;
}

.send-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #27272a 0%, #3f3f46 100%);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, transform 0.25s ease;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  position: relative;
  overflow: hidden;
}

.send-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -60%;
  width: 50%;
  height: 100%;
  background: linear-gradient(105deg, transparent, rgba(255, 255, 255, 0.22), transparent);
  transform: skewX(-20deg);
  transition: left 0.55s ease;
}

.send-btn:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #3f3f46 0%, #52525b 100%);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.55);
}

.send-btn:hover::after {
  left: 120%;
}

.send-btn:active {
  transform: translateY(0);
}

.send-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.send-icon {
  width: 16px;
  height: 16px;
}

/* 快捷灵感 */
.quick-section {
  text-align: center;
  margin-bottom: 60px;
}

.quick-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 1px;
  margin: 0 0 14px;
}

.quick-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.chip-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.75);
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, transform 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chip-icon {
  font-size: 15px;
  line-height: 1;
}

.chip-btn:hover {
  background: rgba(255, 255, 255, 0.09);
  border-color: rgba(255, 255, 255, 0.3);
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
}

.chip-btn.active {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.4);
  color: #ffffff;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.08), 0 6px 16px rgba(0, 0, 0, 0.45);
}

/* 区域容器 - 石墨黑卡片 */
.section {
  margin-bottom: 60px;
  background: #0b0b0e;
  border-radius: 16px;
  padding: 36px 32px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

/* 我的作品区域特殊样式 */
.my-works-section {
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: #0c0c10;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-bottom: 30px;
}

.section-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  padding: 3px 10px;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-link {
  border: 0;
  padding: 4px 0;
  color: rgba(255, 255, 255, 0.72);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
}

.section-link:hover {
  color: #ffffff;
}

.section-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0;
  color: #ffffff;
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.5px;
}

.st-icon {
  font-size: 22px;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 56px;
  height: 2px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0.15));
  border-radius: 2px;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.25);
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 46px;
  margin-bottom: 16px;
  opacity: 0.5;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  animation: floatY 4s ease-in-out infinite;
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
.app-grid,
.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
  /* 视口外的卡片跳过渲染，降低滚动负担 */
  content-visibility: auto;
  contain-intrinsic-size: auto 1000px;
}

/* 卡片样式 */
:deep(.app-card) {
  border-radius: 14px;
  overflow: hidden;
  background: #0b0b0e;
  border: 1px solid rgba(255, 255, 255, 0.07);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, color 0.3s ease, transform 0.3s ease;
  position: relative;
}

:deep(.app-card::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.55), rgba(255, 255, 255, 0.1));
  opacity: 0;
  transition: opacity 0.3s ease;
}

:deep(.app-card:hover) {
  border-color: rgba(255, 255, 255, 0.2);
  background: #0e0e12;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.5);
  transform: translateY(-5px);
}

:deep(.app-card:hover::before) {
  opacity: 1;
}

/* 卡片入场动画 */
.section.in-view .app-card {
  animation: cardIn 0.55s ease both;
}

.section.in-view .app-card:nth-child(2) { animation-delay: 0.07s; }
.section.in-view .app-card:nth-child(3) { animation-delay: 0.14s; }
.section.in-view .app-card:nth-child(4) { animation-delay: 0.21s; }
.section.in-view .app-card:nth-child(5) { animation-delay: 0.28s; }
.section.in-view .app-card:nth-child(6) { animation-delay: 0.35s; }

@keyframes cardIn {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: none; }
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

:deep(.ant-pagination) {
  display: flex;
  align-items: center;
  gap: 2px;
}

:deep(.ant-pagination .ant-pagination-item) {
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

:deep(.ant-pagination .ant-pagination-item a) {
  color: rgba(255, 255, 255, 0.82);
  font-weight: 500;
}

:deep(.ant-pagination .ant-pagination-item:hover) {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.4);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
}

:deep(.ant-pagination .ant-pagination-item:hover a) {
  color: #ffffff;
}

:deep(.ant-pagination .ant-pagination-item-active) {
  background: linear-gradient(135deg, #10b981, #059669);
  border-color: rgba(52, 211, 153, 0.6);
  box-shadow:
    0 4px 14px rgba(16, 185, 129, 0.4),
    0 0 0 1px rgba(16, 185, 129, 0.2);
}

:deep(.ant-pagination .ant-pagination-item-active:hover) {
  background: linear-gradient(135deg, #34d399, #10b981);
  border-color: rgba(52, 211, 153, 0.8);
  transform: translateY(-1px);
}

:deep(.ant-pagination .ant-pagination-item-active a),
:deep(.ant-pagination .ant-pagination-item-active:hover a) {
  color: #ffffff;
  font-weight: 600;
}

:deep(.ant-pagination .ant-pagination-prev .ant-pagination-item-link),
:deep(.ant-pagination .ant-pagination-next .ant-pagination-item-link) {
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.82);
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

:deep(.ant-pagination .ant-pagination-prev:hover .ant-pagination-item-link),
:deep(.ant-pagination .ant-pagination-next:hover .ant-pagination-item-link) {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.4);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
}

:deep(.ant-pagination .ant-pagination-disabled .ant-pagination-item-link) {
  opacity: 0.35;
  cursor: not-allowed;
}

:deep(.ant-pagination .ant-pagination-ellipsis) {
  color: rgba(255, 255, 255, 0.4);
}

:deep(.ant-pagination .ant-pagination-item:focus-visible),
:deep(.ant-pagination .ant-pagination-item-link:focus-visible) {
  outline: 2px solid rgba(16, 185, 129, 0.7);
  outline-offset: 1px;
}

:deep(.ant-pagination .ant-pagination-total-text) {
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
}

/* 滚动渐显 */
.reveal {
  opacity: 0;
  transform: translateY(28px);
  transition: opacity 0.7s ease, transform 0.7s ease;
}

.reveal.in-view {
  opacity: 1;
  transform: none;
}

@media (prefers-reduced-motion: reduce) {
  .orb,
  .float-code,
  .empty-icon,
  .hero-badge,
  .hero-title,
  .hero-description,
  .hero-tags,
  .send-btn::after,
  .grad-text {
    animation: none !important;
  }

  .reveal {
    opacity: 1;
    transform: none;
    transition: none;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 38px;
  }

  .hero-description {
    font-size: 17px;
  }

  .hero-section {
    padding: 70px 0 40px;
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

  .chip-btn {
    font-size: 13px;
    padding: 8px 15px;
  }

  .section-title {
    font-size: 22px;
  }

  .container {
    padding: 16px;
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
    font-size: 30px;
  }

  .hero-description {
    font-size: 15px;
  }

  .input-section {
    max-width: 100%;
  }

  .prompt-input {
    font-size: 15px;
  }

  .send-btn {
    padding: 9px 16px;
    font-size: 13px;
  }

  .section {
    padding: 24px 20px;
  }

  .section-title {
    font-size: 20px;
  }
}
</style>
