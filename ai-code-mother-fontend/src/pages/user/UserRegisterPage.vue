<template>
  <a-config-provider :theme="themeConfig">
    <div class="register-page">
      <!-- 背景装饰层 -->
      <div class="bg" aria-hidden="true">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="orb orb-3"></div>
        <div class="grid"></div>
        <span class="float-code f1">&lt;/&gt;</span>
        <span class="float-code f2">{ }</span>
        <span class="float-code f3">=&gt;</span>
        <span class="float-code f4">import ai</span>
        <span class="float-code f5"># AI</span>
      </div>

      <div class="register-wrap">
        <!-- 左侧品牌展示区 -->
        <div class="brand-panel">
          <div class="brand-head">
            <img class="brand-logo" src="/favicon.ico" alt="AI零代码平台" />
            <div class="brand-title">
              <h1>AI Code Mother</h1>
              <p>零代码生成平台</p>
            </div>
          </div>

          <h2 class="slogan">一句话，让 AI<br />帮你生成一个完整网站</h2>

          <ul class="features">
            <li>
              <span class="f-icon"><MessageSquare :size="20" /></span>
              <div><b>对话式生成</b><small>描述你的想法，AI 自动生成应用</small></div>
            </li>
            <li>
              <span class="f-icon"><Eye :size="20" /></span>
              <div><b>实时预览</b><small>边对话边预览生成效果</small></div>
            </li>
            <li>
              <span class="f-icon"><Rocket :size="20" /></span>
              <div><b>一键部署</b><small>生成的应用可直接部署上线</small></div>
            </li>
          </ul>

          <!-- 打字机代码演示 -->
          <div class="code-demo" aria-hidden="true">
            <div class="cd-bar"><span class="cd-dot r"></span><span class="cd-dot y"></span><span class="cd-dot g"></span><span class="cd-name">terminal — ai-code-mother</span></div>
            <div class="cd-body">
              <div v-for="(line, idx) in shownLines" :key="idx" class="cd-line" :class="line.cls">{{ line.text }}</div>
              <span class="cd-cursor"></span>
            </div>
          </div>
        </div>

        <!-- 右侧注册表单 -->
        <div class="register-card">
          <div class="card-inner">
            <h3>创建账号 <Hand :size="20" class="greet-icon" /></h3>
            <p class="sub">注册后即可开始你的创造之旅</p>

            <a-form :model="formState" @finish="handleRegister">
              <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
                <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large" allow-clear>
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  </template>
                </a-input>
              </a-form-item>
              <a-form-item name="userPassword" :rules="[{ required: true, message: '请输入密码' }]">
                <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" size="large">
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  </template>
                </a-input-password>
              </a-form-item>
              <a-form-item name="checkPassword" :rules="[{ required: true, message: '请确认密码' }]">
                <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" size="large">
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/></svg>
                  </template>
                </a-input-password>
              </a-form-item>
              <a-form-item>
                <a-button class="register-btn" type="primary" html-type="submit" size="large" block :loading="loading">
                  注 册
                </a-button>
              </a-form-item>
            </a-form>

            <div class="login-link">
              已有账号？<router-link to="/user/login">立即登录 →</router-link>
            </div>
          </div>
        </div>
      </div>

      <div class="page-footer">AI Code Mother · 让创造更简单</div>
    </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, theme } from 'ant-design-vue'
import { MessageSquare, Eye, Rocket, Hand } from "lucide-vue-next"
import { userRegister } from '@/api/userController'

const router = useRouter()
const loading = ref(false)

const themeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: '#e4e4e7',
    colorBgContainer: 'rgba(255, 255, 255, 0.03)',
    colorBgElevated: '#131316',
    colorBorder: 'rgba(255, 255, 255, 0.12)',
    colorText: 'rgba(255, 255, 255, 0.88)',
    colorTextPlaceholder: 'rgba(255, 255, 255, 0.35)',
    borderRadius: 8,
  },
}

const formState = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const handleRegister = async () => {
  if (formState.userPassword !== formState.checkPassword) {
    message.error('两次密码输入不一致')
    return
  }
  loading.value = true
  try {
    const res = await userRegister(formState)
    if (res.data.code === 0) {
      message.success('注册成功')
      await router.push('/user/login')
    } else {
      message.error('注册失败：' + res.data.message)
    }
  } catch {
    message.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}

/* ---------- 打字机代码演示 ---------- */
const CODE_LINES = [
  { text: '// 输入你的创意，生成完整网站', cls: 'comment' },
  { text: 'const site = await ai.create({', cls: '' },
  { text: "  idea: '我的个人作品集',", cls: 'string' },
  { text: "  style: '极简风',", cls: 'string' },
  { text: '  pages: 3,', cls: 'plain' },
  { text: '});', cls: '' },
  { text: 'site.preview()   // 实时预览', cls: 'ok' },
]

const lineCount = ref(1)
let lineTimer: number | null = null

const shownLines = computed(() => CODE_LINES.slice(0, lineCount.value))

function startTyping() {
  lineCount.value = 1
  lineTimer = window.setInterval(() => {
    if (lineCount.value < CODE_LINES.length) {
      lineCount.value++
    }
  }, 550)
}

onBeforeUnmount(() => {
  if (lineTimer) {
    clearInterval(lineTimer)
  }
})

startTyping()
</script>

<style scoped>
/* ========== 页面骨架 ========== */
.register-page {
  position: relative;
  min-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  overflow: hidden;
  background:
    radial-gradient(1200px 700px at 80% -10%, rgba(255, 255, 255, 0.05), transparent 60%),
    radial-gradient(1000px 600px at 10% 110%, rgba(255, 255, 255, 0.03), transparent 55%),
    #0a0a0c;
}

/* ========== 背景装饰 ========== */
.bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  animation: drift 16s ease-in-out infinite alternate;
}

.orb-1 {
  width: 420px;
  height: 420px;
  left: -120px;
  top: -80px;
  background: rgba(255, 255, 255, 0.14);
}

.orb-2 {
  width: 360px;
  height: 360px;
  right: -100px;
  top: 30%;
  background: rgba(255, 255, 255, 0.1);
  animation-delay: -5s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  left: 40%;
  bottom: -140px;
  background: rgba(255, 255, 255, 0.09);
  animation-delay: -10s;
}

@keyframes drift {
  0% {
    transform: translate(0, 0) scale(1);
  }
  100% {
    transform: translate(60px, -50px) scale(1.15);
  }
}

.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 48px 48px;
  -webkit-mask-image: radial-gradient(ellipse 90% 80% at 50% 40%, #000 30%, transparent 75%);
  mask-image: radial-gradient(ellipse 90% 80% at 50% 40%, #000 30%, transparent 75%);
}

.float-code {
  position: absolute;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.03);
  animation: floatY 8s ease-in-out infinite;
}

.f1 { left: 8%; top: 16%; animation-delay: 0s; }
.f2 { right: 10%; top: 22%; animation-delay: -2s; }
.f3 { left: 14%; bottom: 18%; animation-delay: -4s; }
.f4 { right: 18%; bottom: 12%; animation-delay: -6s; }
.f5 { left: 46%; top: 8%; animation-delay: -3s; }

@keyframes floatY {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-22px) rotate(2deg); }
}

/* ========== 主体布局 ========== */
.register-wrap {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1040px;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 28px;
  align-items: center;
}

/* ========== 左侧品牌区 ========== */
.brand-panel {
  padding: 42px 40px;
  border-radius: 20px;
  background: #0b0b0e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.4);
  animation: slideLeft 0.8s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.25),
    0 0 28px rgba(255, 255, 255, 0.16);
  animation: logoPulse 3.5s ease-in-out infinite;
}

@keyframes logoPulse {
  0%, 100% { box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.25), 0 0 22px rgba(255, 255, 255, 0.14); }
  50% { box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.45), 0 0 40px rgba(255, 255, 255, 0.25); }
}

.brand-title h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5px;
  background: linear-gradient(90deg, #fff 0%, #a1a1aa 60%, #52525b 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-title p {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 2px;
}

.slogan {
  margin: 34px 0 26px;
  font-size: clamp(22px, 3vw, 30px);
  line-height: 1.45;
  font-weight: 600;
  color: #fff;
}

.features {
  list-style: none;
  margin: 0 0 30px;
  padding: 0;
  display: grid;
  gap: 12px;
}

.features li {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.07);
  transition: all 0.25s ease;
}

.features li:hover {
  transform: translateX(6px);
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.05);
}

.f-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.75);
}

.greet-icon {
  vertical-align: -4px;
  margin-left: 2px;
  color: #4ade80;
}

.features b {
  display: block;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
}

.features small {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

/* 打字机代码演示 */
.code-demo {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: #0a0a0c;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.4);
}

.cd-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(255, 255, 255, 0.03);
}

.cd-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
}

.cd-dot.r { opacity: 1; }
.cd-dot.y { opacity: 0.75; }
.cd-dot.g { opacity: 0.5; }

.cd-name {
  margin-left: 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.cd-body {
  padding: 16px 18px;
  min-height: 158px;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.9;
}

.cd-line {
  white-space: pre;
  color: #d4d4d8;
  animation: lineIn 0.3s ease both;
}

@keyframes lineIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: none; }
}

.cd-line.comment { color: #52525b; }
.cd-line.string { color: #a1a1aa; }
.cd-line.plain { color: #d4d4d8; }
.cd-line.ok { color: #f4f4f5; text-shadow: 0 0 12px rgba(255, 255, 255, 0.35); }

.cd-cursor {
  display: inline-block;
  width: 8px;
  height: 15px;
  margin-left: 3px;
  vertical-align: -2px;
  background: #e4e4e7;
  animation: blink 1s steps(1) infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

/* ========== 右侧注册卡片 ========== */
.register-card {
  width: 100%;
  max-width: 420px;
  height: auto;
  padding: 48px 40px;
  border-radius: 20px;
  background: #0b0b0e;
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
  animation: slideRight 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.1s both;
}

.card-inner {
  width: 100%;
  padding: 0;
}

.card-inner h3 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #fff;
}

.card-inner .sub {
  margin: 8px 0 30px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.input-icon {
  width: 16px;
  height: 16px;
  color: rgba(255, 255, 255, 0.35);
}

.register-btn {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 6px;
  border: none;
  background: linear-gradient(135deg, #27272a 0%, #3f3f46 100%);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.4);
  transition: all 0.25s ease;
}

.register-btn:hover {
  background: linear-gradient(135deg, #3f3f46 0%, #52525b 100%);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.55);
  transform: translateY(-2px);
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-input-affix-wrapper) {
  height: 46px;
  border-radius: 10px;
  transition: all 0.25s ease;
}

:deep(.ant-input-affix-wrapper-focused) {
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.35);
}

:deep(.ant-input) {
  font-size: 14px;
}

:deep(.ant-input-prefix) {
  margin-inline-end: 8px;
}

:deep(.ant-form-item-explain-error) {
  font-size: 12px;
}

.login-link {
  text-align: center;
  margin-top: 4px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.login-link a {
  color: #a1a1aa;
  text-decoration: none;
  transition: all 0.2s;
}

.login-link a:hover {
  color: #ffffff;
  text-shadow: 0 0 12px rgba(255, 255, 255, 0.5);
}

.page-footer {
  position: relative;
  z-index: 1;
  margin-top: 30px;
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.3);
  animation: fadeIn 1s 0.5s both;
}

/* ========== 入场动画 ========== */
@keyframes slideLeft {
  from { opacity: 0; transform: translateX(-46px); }
  to { opacity: 1; transform: none; }
}

@keyframes slideRight {
  from { opacity: 0; transform: translateX(46px); }
  to { opacity: 1; transform: none; }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ========== 响应式 ========== */
@media (max-width: 900px) {
  .register-wrap {
    grid-template-columns: 1fr;
    max-width: 460px;
  }

  .brand-panel {
    display: none;
  }

  .register-card {
    animation: slideUp 0.8s cubic-bezier(0.22, 1, 0.36, 1) both;
  }

  @keyframes slideUp {
    from { opacity: 0; transform: translateY(36px); }
    to { opacity: 1; transform: none; }
  }
}

@media (prefers-reduced-motion: reduce) {
  .orb,
  .float-code,
  .brand-logo,
  .register-card,
  .brand-panel {
    animation: none !important;
  }
}
</style>
