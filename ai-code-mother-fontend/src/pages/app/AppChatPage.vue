<template>
  <div class="ai-code-page">
    <!-- 左侧：AI 对话区 -->
    <div class="left-panel">
      <!-- 当前生成任务 Header -->
      <div class="panel-header task-header">
        <div class="header-title">
          <div class="title-icon" aria-hidden="true">
            <CodeOutlined />
            <span class="title-icon-dot"></span>
          </div>
          <div class="header-copy">
            <span class="header-kicker">AI 工作区</span>
            <span class="header-prompt" :title="currentPrompt || appInfo?.appName || 'AI 代码生成'">
              {{ currentPrompt || appInfo?.appName || 'AI 代码生成' }}
            </span>
          </div>
        </div>
        <div class="header-actions">
          <span v-if="generating" class="gen-indicator"><i></i>生成中</span>
          <button
            class="open-window-btn edit-app-btn"
            aria-label="修改应用"
            title="修改应用名称"
            @click="editModalOpen = true"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 11-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 110-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 112.83-2.83l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 114 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 112.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 110 4h-.09a1.65 1.65 0 00-1.51 1z"/>
            </svg>
            修改应用
          </button>
          <button class="btn-icon clear-btn" aria-label="清空对话" @click="clearMessages" title="清空对话">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2m3 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6h14z"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="messages-container" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-illustration">
            <div class="empty-orb">
              <div class="orb-ring"></div>
              <div class="orb-core"></div>
            </div>
            <div class="empty-particles">
              <span class="particle"></span>
              <span class="particle"></span>
              <span class="particle"></span>
            </div>
          </div>
          <p class="empty-title">开始创建你的应用</p>
          <p class="empty-desc">描述你想要的功能，AI 将为你生成代码</p>
          <div class="empty-chips">
            <span class="empty-chip">💡 输入描述即可开始</span>
            <span class="empty-chip">⚡ 支持多轮对话</span>
            <span class="empty-chip">🎨 右侧实时预览</span>
          </div>
        </div>

        <template v-else>
          <div
            v-for="(msg, index) in messages"
            :key="msg.id"
            class="message"
            :class="msg.role === 'user' ? 'user-message' : 'assistant-message'"
            :style="{ '--i': index }"
          >
            <!-- 用户消息（右侧） -->
            <template v-if="msg.role === 'user'">
              <div class="message-bubble user-bubble">
                <div class="bubble-content">
                  <div class="markdown-body" v-html="getRenderedMarkdown(msg.id)" @click="onMarkdownClick"></div>
                </div>
                <button
                  class="bubble-copy-btn"
                  type="button"
                  aria-label="复制消息"
                  title="复制消息"
                  @click="copyMessage(msg.content)"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="9" y="9" width="13" height="13" rx="2"/>
                    <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
                  </svg>
                </button>
              </div>
              <div class="message-avatar user-avatar">
                <svg class="avatar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 18V5"/>
                  <path d="M15 13a4.17 4.17 0 0 1-3-4 4.17 4.17 0 0 1-3 4"/>
                  <path d="M17.598 6.5A3 3 0 1 0 12 5a3 3 0 1 0-5.598 1.5"/>
                  <path d="M17.997 5.125a4 4 0 0 1 2.526 5.77"/>
                  <path d="M18 18a4 4 0 0 0 2-7.464"/>
                  <path d="M19.967 17.483A4 4 0 1 1 12 18a4 4 0 1 1-7.967-.517"/>
                  <path d="M6 18a4 4 0 0 1-2-7.464"/>
                  <path d="M6.003 5.125a4 4 0 0 0-2.526 5.77"/>
                </svg>
              </div>
            </template>

            <!-- AI 消息（左侧） -->
            <template v-else>
              <div class="message-avatar assistant-avatar">
                <svg class="avatar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 6V2H8"/>
                  <path d="M15 11v2"/>
                  <path d="M2 12h2"/>
                  <path d="M20 12h2"/>
                  <path d="M20 16a2 2 0 0 1-2 2H8.828a2 2 0 0 0-1.414.586l-2.202 2.202A.71.71 0 0 1 4 20.286V8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2z"/>
                  <path d="M9 11v2"/>
                </svg>
              </div>
              <div class="message-bubble assistant-bubble">
                <div class="bubble-content">
                  <template v-for="(tok, tokIdx) in getParsedTokens(msg.id)" :key="tokIdx">
                    <!-- 文本段落（Markdown） -->
                    <div
                      v-if="tok.type === 'md'"
                      class="markdown-body"
                      v-html="tok.html"
                      @click="onMarkdownClick"
                    ></div>
                    <!-- 工具调用状态块 -->
                    <div v-else class="tool-call" :class="{ done: tok.done, 'is-fail': tok.done && !tok.success }">
                      <span class="tc-icon" :class="{ running: !tok.done }">
                        <component :is="toolIcon(tok.name)" />
                      </span>
                      <span class="tc-name">{{ tok.name }}</span>
                      <span v-if="tok.file" class="tc-file">{{ tok.file }}</span>
                      <span class="tc-tag">
                        {{ tok.done ? (tok.success ? '完成' : '失败') : '执行中' }}
                      </span>
                    </div>
                  </template>
                  <div v-if="msg.isTyping && !msg.content" class="assistant-thinking" aria-live="polite">
                    <span class="thinking-icon" aria-hidden="true"><LoadingOutlined /></span>
                    <span>正在分析需求，准备生成响应</span>
                    <span class="thinking-dots" aria-hidden="true"><i></i><i></i><i></i></span>
                  </div>
                </div>
                <!-- 底部操作栏 -->
                <div class="msg-actions">
                  <button
                    class="ma-btn"
                    type="button"
                    title="复制全部内容"
                    aria-label="复制全部内容"
                    @click="copyMessage(msg.content)"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <rect x="9" y="9" width="13" height="13" rx="2"/>
                      <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
                    </svg>
                    复制
                  </button>
                  <button
                    class="ma-btn"
                    type="button"
                    title="重新生成"
                    aria-label="重新生成"
                    @click="regenerateMessage(msg)"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M3 12a9 9 0 019-9 9.75 9.75 0 016.74 2.74L21 8"/>
                      <path d="M21 3v5h-5"/>
                      <path d="M21 12a9 9 0 01-9 9 9.75 9.75 0 01-6.74-2.74L3 16"/>
                      <path d="M3 21v-5h5"/>
                    </svg>
                    重新生成
                  </button>
                  <span class="ma-divider" aria-hidden="true"></span>
                  <button
                    class="ma-btn"
                    type="button"
                    title="有帮助"
                    aria-label="有帮助"
                    :class="{ liked: msg.feedback === 'up' }"
                    @click="feedback(msg, 'up')"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M7 10v12"/>
                      <path d="M15 5.88L14 10h5.83a2 2 0 011.92 2.56l-2.33 8A2 2 0 0117.5 22H4a2 2 0 01-2-2v-8a2 2 0 012-2h2.76a2 2 0 001.79-1.11L12 2a3.13 3.13 0 013 3.88Z"/>
                    </svg>
                  </button>
                  <button
                    class="ma-btn"
                    type="button"
                    title="无帮助"
                    aria-label="无帮助"
                    :class="{ disliked: msg.feedback === 'down' }"
                    @click="feedback(msg, 'down')"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M17 14V2"/>
                      <path d="M9 18.12L10 14H4.17a2 2 0 01-1.92-2.56l2.33-8A2 2 0 016.5 2H20a2 2 0 012 2v8a2 2 0 01-2 2h-2.76a2 2 0 00-1.79 1.11L12 22a3.13 3.13 0 01-3-3.88Z"/>
                    </svg>
                  </button>
                </div>
              </div>
            </template>
          </div>
        </template>
      </div>

      <!-- 回到底部按钮（用户上滑后出现） -->
      <transition name="scroll-fade">
        <button
          v-if="showScrollTopBtn"
          class="scroll-bottom-btn"
          type="button"
          title="回到底部"
          aria-label="回到底部"
          @click="jumpToBottom"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M12 5v14M5 12l7 7 7-7"/>
          </svg>
        </button>
      </transition>

      <!-- 输入区域（拖拽放置目标） -->
      <div
        ref="inputAreaRef"
        class="input-area"
        :class="{ 'drag-active': isDragOver }"
      >
        <!-- 拖拽吸附遮罩 -->
        <transition name="drop-fade">
          <div v-if="isDragOver" class="drop-overlay">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path d="M4 10h9v9M13 10L4 19" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>松开鼠标，将此元素添加为修改上下文</span>
          </div>
        </transition>

        <!-- 引用上下文卡片 -->
        <div v-if="referenceElements.length > 0" class="context-chips">
          <transition-group name="chip-pop">
            <ContextReferenceCard
              v-for="(refEl, index) in referenceElements"
              :key="refEl.elementId"
              title="AI 工作区"
              :content="`引用元素：&lt;${refEl.tagName.toLowerCase()}&gt;${refEl.classList.length ? ' .' + refEl.classList[0] : ''}`"
              :code-snippet="refEl.outerHTML"
              @remove="removeReference(index)"
            />
          </transition-group>
        </div>

        <div class="input-wrapper">
          <div class="input-glow"></div>
          <textarea
            v-model="inputMessage"
            placeholder="描述你想要的功能，例如：添加一个导航栏…"
            :disabled="generating"
            @keydown="handleKeydown"
            rows="1"
            ref="textareaRef"
            name="chat-input"
            aria-label="输入消息"
            autocomplete="off"
          ></textarea>
          <button
            class="send-btn"
            :class="{ active: canSend, sending: generating }"
            :disabled="!canSend"
            @click="sendMessage()"
            aria-label="发送"
          >
            <svg v-if="!generating" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span v-else class="loading-spinner" role="status" aria-label="生成中"></span>
          </button>
        </div>
        <div class="input-hint">
          <span class="hint-item">
            <kbd>Enter</kbd> 发送
          </span>
          <span class="hint-divider"></span>
          <span class="hint-item">
            <kbd>Shift</kbd> + <kbd>Enter</kbd> 换行
          </span>
        </div>
      </div>
    </div>

    <!-- 右侧：预览区 -->
    <div class="right-panel">
      <div class="panel-header">
        <div class="tabs">
          <button
            class="tab"
            :class="{ active: activeTab === 'preview' }"
            @click="activeTab = 'preview'"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            实时预览
          </button>
          <button
            class="tab"
            :class="{ active: activeTab === 'code' }"
            @click="activeTab = 'code'"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <polyline points="16 18 22 12 16 6"/>
              <polyline points="8 6 2 12 8 18"/>
            </svg>
            查看代码
          </button>
        </div>
        <div class="header-actions">
          <button
            class="pick-switch"
            :class="{ on: pickMode }"
            :disabled="!canOpenPreview"
            @click="togglePickMode"
            title="开启后可在预览页面中选择元素拖拽到输入框"
          >
            <span class="switch-track"><i class="switch-knob"></i></span>
            拖拽修改
          </button>
          <button
            class="open-window-btn"
            :disabled="!canOpenPreview"
            @click="openPreviewWindow"
            title="在新窗口打开构建好的网站预览"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path d="M15 3h6v6M10 14L21 3M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6"/>
            </svg>
            新窗口打开
          </button>
          <button class="btn-icon" @click="refreshPreview" aria-label="刷新预览" :disabled="generating" title="刷新预览">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path d="M23 4v6h-6M1 20v-6h6"/>
              <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 预览内容 -->
      <div class="preview-container">
        <!-- 预览 Tab -->
        <div v-show="activeTab === 'preview'" class="preview-content">
          <!-- 预览 iframe（精简样式）：srcdoc 注入脚本常驻，拾取模式通过 postMessage 启停，避免重建闪烁 -->
          <div v-if="canShowPreview" class="browser-frame">
            <iframe
              v-if="previewMode === 'srcdoc'"
              ref="previewIframe"
              class="preview-iframe"
              :srcdoc="previewDoc"
              title="应用实时预览"
              sandbox="allow-scripts"
              @load="syncPickEnabled"
            ></iframe>
            <iframe
              v-else
              ref="previewIframe"
              class="preview-iframe"
              :src="previewUrl"
              title="应用实时预览"
              sandbox="allow-scripts"
              @load="syncPickEnabled"
            ></iframe>
          </div>

          <!-- 流式实时渲染（生成中/构建完成前动态展示） -->
          <div v-else-if="livePreviewHtml" class="browser-frame">
            <iframe
              class="preview-iframe"
              :srcdoc="livePreviewHtml"
              title="实时预览（生成中）"
              sandbox="allow-scripts"
            ></iframe>
          </div>

          <!-- 无代码状态 -->
          <div v-else class="preview-empty">
            <div class="empty-code-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <line x1="3" y1="9" x2="21" y2="9"/>
                <line x1="9" y1="21" x2="9" y2="9"/>
              </svg>
            </div>
            <p class="empty-title">等待生成代码</p>
            <p class="empty-desc">在左侧输入需求，AI 将生成代码并在此预览</p>
          </div>
        </div>

        <!-- 代码 Tab -->
        <div v-show="activeTab === 'code'" class="code-content">
          <div v-if="codeFiles.length === 0" class="preview-empty">
            <div class="empty-code-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <polyline points="16 18 22 12 16 6"/>
                <polyline points="8 6 2 12 8 18"/>
              </svg>
            </div>
            <p class="empty-title">暂无代码</p>
            <p class="empty-desc">开始对话后，生成的代码将在此显示</p>
          </div>

          <div v-else class="code-files-container">
            <!-- 文件列表头部 -->
            <div class="files-header">
              <span class="files-count">{{ codeFiles.length }} 个文件</span>
              <div class="files-actions">
                <button class="action-btn" @click="expandAllFiles" title="展开全部" aria-label="展开全部">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    <polyline points="7 13 12 18 17 13"/>
                    <polyline points="7 6 12 11 17 6"/>
                  </svg>
                </button>
                <button class="action-btn" @click="collapseAllFiles" title="折叠全部" aria-label="折叠全部">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    <polyline points="17 11 12 6 7 11"/>
                    <polyline points="17 18 12 13 7 18"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- 文件列表 -->
            <div class="files-list">
              <div
                v-for="(file, index) in codeFiles"
                :key="index"
                class="file-item"
                :class="{ expanded: file.expanded }"
              >
                <div class="file-header" @click="toggleFileExpand(index)">
                  <div class="file-info">
                    <svg class="expand-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline v-if="file.expanded" points="6 9 12 15 18 9"/>
                      <polyline v-else points="9 6 15 12 9 18"/>
                    </svg>
                    <span class="file-icon" :class="file.language">
                      <svg v-if="file.language === 'html'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M4 4l2 16 6 2 6-2 2-16H4z"/>
                        <path d="M8 8l1 8 3 1 3-1 1-8"/>
                      </svg>
                      <svg v-else-if="file.language === 'css'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <circle cx="12" cy="12" r="10"/>
                        <path d="M8 12h8M12 8v8"/>
                      </svg>
                      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                      </svg>
                    </span>
                    <span class="file-name">{{ file.filename }}</span>
                    <span class="file-lang">{{ file.language.toUpperCase() }}</span>
                  </div>
                  <button class="copy-file-btn" @click.stop="copyFileCode(file)" title="复制代码" aria-label="复制代码">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="9" y="9" width="13" height="13" rx="2"/>
                      <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
                    </svg>
                  </button>
                </div>
                <div class="file-content" :class="{ open: file.expanded }">
                  <div class="file-content-inner">
                    <div class="code-viewport">
                      <div class="code-gutter" aria-hidden="true">
                        <span v-for="n in codeLineCount(file)" :key="n" class="code-line-no">{{ n }}</span>
                      </div>
                      <pre class="code-area"><code v-html="getHighlightedCode(file)"></code></pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 修改应用弹窗 -->
  <EditAppModal
    v-model:open="editModalOpen"
    :app-id="appId"
    :app-name="appInfo?.appName"
    @saved="handleAppEdited"
  />
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick, onUnmounted, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message as antMessage } from 'ant-design-vue'
import { CodeOutlined, FolderOpenOutlined, FileTextOutlined, EditOutlined, LoadingOutlined, ToolOutlined } from '@ant-design/icons-vue'
import { createPreviewToken, getAppById, getProjectFiles } from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { buildPreviewDoc } from '@/utils/previewPicker'
import ContextReferenceCard from '@/components/ContextReferenceCard.vue'
import EditAppModal from '@/components/EditAppModal.vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'

// 配置 DOMPurify 支持 target="_blank"
DOMPurify.addHook('afterSanitizeAttributes', function (node) {
  if ('target' in node) {
    node.setAttribute('target', '_blank');
    node.setAttribute('rel', 'noopener noreferrer');
  }
});

// Markdown 渲染器
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch { /* ignore */ }
    }
    return hljs.highlightAuto(str).value
  }
})

// 链接统一新窗口打开并加安全 rel
const defaultLinkRender = md.renderer.rules.link_open ||
  ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))
md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer')
  return defaultLinkRender(tokens, idx, options, env, self)
}

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  isTyping?: boolean  // 是否正在打字
  feedback?: 'up' | 'down'  // 用户反馈
}

// 拖拽引用的目标元素（HTML 上下文）
interface ReferenceElement {
  elementId: string
  tagName: string
  classList: string[]
  outerHTML: string
  xpath?: string
}

const route = useRoute()
const router = useRouter()
const appId = route.params.id as string
const generatedPreviewOrigin = (
  import.meta.env.VITE_GENERATED_PREVIEW_ORIGIN || 'http://localhost:8088'
).replace(/\/+$/, '')

// 状态
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const generating = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const inputAreaRef = ref<HTMLElement | null>(null)
const previewIframe = ref<HTMLIFrameElement | null>(null)
const appInfo = ref<API.AppVO | null>(null)
const activeTab = ref<'preview' | 'code'>('preview')
const extractedCode = ref('')
const codeComplete = ref(false)
const previewUrl = ref('')
const previewReady = ref(false)
// 注入拾取脚本后的预览文档（srcdoc 模式）；为空时回退为 :src 直连
const previewDoc = ref('')
const previewMode = ref<'src' | 'srcdoc'>('src')

// ========== 拖拽引用（右侧预览元素 → 左侧输入框上下文） ==========
const referenceElements = ref<ReferenceElement[]>([])
const isDragOver = ref(false)

// 尝试拉取预览 HTML 并注入拾取脚本；失败返回空串（回退 :src 直连）
const tryBuildInjectedDoc = async (url: string, baseHref: string): Promise<string> => {
  try {
    const resp = await fetch(url, { credentials: 'include', cache: 'no-store' })
    if (!resp.ok) return ''
    const html = await resp.text()
    if (!html.trim()) return ''
    return buildPreviewDoc(html, baseHref)
  } catch (error) {
    console.warn('注入拾取脚本失败，回退直连预览', error)
    return ''
  }
}

// 自动滚动跟随状态
const autoScrollEnabled = ref(true)   // 是否跟随新内容滚动到底部
const showScrollTopBtn = ref(false)   // 是否显示「回到底部」按钮
let lastAutoScrollTime = 0            // 上次自动滚动的时间戳（节流）

// 当前生成任务：取最近一条用户消息作为 Header 中展示的提示词
const currentPrompt = computed(() => {
  const lastUserMsg = [...messages.value].reverse().find(m => m.role === 'user')
  return lastUserMsg?.content || ''
})

// 流式响应缓冲。按小批次刷新，维持连续输出且避免重型 Markdown 渲染阻塞页面。
const typewriterQueue = ref('')
const currentTypingMessage = ref<ChatMessage | null>(null)
const typewriterTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const streamCompleted = ref(false)
let pendingCompletion: (() => void) | null = null
const STREAM_BATCH_SIZE = 120
const STREAM_BATCH_DELAY = 28
const MARKDOWN_RENDER_INTERVAL_MS = 120
let lastMarkdownRenderAt = 0
const PREVIEW_POLL_INTERVAL_MS = 1000
const PREVIEW_POLL_TIMEOUT_MS = 8 * 60 * 1000

// 自动滚动节流间隔（ms）：内容刷新太快时限制滚动频率，避免滚动条反复跳动
const SCROLL_THROTTLE_MS = 100
// 距离底部多少像素以内视为「在底部」，才自动跟随滚动
const FOLLOW_THRESHOLD = 140

// 多文件代码支持
interface CodeFile {
  filename: string
  language: string
  code: string
  expanded: boolean
}
const codeFiles = ref<CodeFile[]>([])

// 缓存渲染后的 Markdown（用户消息用）
const renderedMarkdownCache = new Map<string, string>()

// ========== AI 消息结构化解析：文本段落 + 工具调用状态块 ==========
interface ToolCallToken {
  type: 'tool'
  name: string
  file?: string
  done: boolean
  success: boolean
}
interface MdToken {
  type: 'md'
  html: string
}
type ContentToken = MdToken | ToolCallToken

// 按工具名称映射 Ant Design 图标
const toolIcon = (name: string): Component => {
  const n = name.toLowerCase()
  if (/目录|dir|folder|listdir/.test(n)) return FolderOpenOutlined
  if (/读取文件|readfile|read.?file/.test(n)) return FileTextOutlined
  if (/修改|写入|write|edit|update/.test(n)) return EditOutlined
  return ToolOutlined
}

// 代码块头部装饰（语言名 + 复制按钮）
const enhanceCodeBlocks = (html: string): string =>
  html
    .replace(/<pre><code class="language-(\w+)">/g, (_, lang: string) => {
      return `<div class="code-block"><div class="cb-head"><span class="cb-lang">${lang}</span><button type="button" class="cb-copy-btn" aria-label="复制代码" title="复制代码"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg><span>复制</span></button></div><pre><code class="language-${lang}">`
    })
    .replace(/<pre><code>/g, '<div class="code-block"><div class="cb-head"><span class="cb-lang">code</span><button type="button" class="cb-copy-btn" aria-label="复制代码" title="复制代码"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg><span>复制</span></button></div><pre><code>')
    .replace(/<\/code><\/pre>/g, '</code></pre></div>')

// 将 AI 消息文本解析为「文本段落 + 工具调用块」token 序列
const parseAiContent = (content: string, messageFinished = false): ContentToken[] => {
  const tokens: ContentToken[] = []
  const buffer: string[] = []
  let pendingTool: ToolCallToken | null = null

  const flushText = () => {
    const text = buffer.join('\n').trim()
    buffer.length = 0
    if (text) {
      tokens.push({ type: 'md', html: DOMPurify.sanitize(enhanceCodeBlocks(md.render(text))) })
    }
  }

  for (const line of content.split('\n')) {
    const toolMatch = line.match(/^\[工具调用\]\s*(\S+)(?:\s+(\S+\.\w+))?/)
    const doneMatch = line.match(/^✓\s*(.+)$/)
    if (toolMatch) {
      flushText()
      pendingTool = {
        type: 'tool',
        name: toolMatch[1],
        file: toolMatch[2],
        done: false,
        success: false,
      }
      tokens.push(pendingTool)
    } else if (doneMatch) {
      flushText()
      if (pendingTool) {
        pendingTool.done = true
        pendingTool.success = true
        pendingTool.file = pendingTool.file || doneMatch[1].trim()
        pendingTool = null
      } else {
        tokens.push({ type: 'md', html: DOMPurify.sanitize(md.render(doneMatch[1].trim())) })
      }
    } else {
      buffer.push(line)
    }
  }
  flushText()
  // 消息已结束（非流式中）：未收到完成标记的工具视为已完成
  if (messageFinished) {
    tokens.forEach(t => {
      if (t.type === 'tool' && !t.done) {
        t.done = true
        t.success = true
      }
    })
  }
  return tokens
}

const parsedTokensCache = new Map<string, ContentToken[]>()

// 获取 AI 消息的渲染 token（带缓存，流式刷新时失效）
const getParsedTokens = (msgId: string): ContentToken[] => {
  const msg = messages.value.find(m => m.id === msgId)
  if (!msg) return []
  if (parsedTokensCache.has(msgId)) return parsedTokensCache.get(msgId)!
  const tokens = parseAiContent(msg.content, !msg.isTyping)
  parsedTokensCache.set(msgId, tokens)
  return tokens
}

// 构建进度动画
const buildProgress = ref(0)
let buildProgressTimer: ReturnType<typeof setInterval> | null = null

// SSE 连接
let eventSource: EventSource | null = null

// 计算属性
const canSend = computed(() => {
  return inputMessage.value.trim().length > 0 && !generating.value
})

// 流式生成中的实时预览内容（仅 html 类应用；vue 工程需 nginx 构建后才有可渲染产物）
const livePreviewHtml = ref('')
let lastLiveExtract = 0

// 从流式内容中实时提取可预览的 HTML（节流，避免每批渲染都做正则扫描）
const extractLivePreview = (content: string) => {
  if (appInfo.value?.codeGenType === 'vue_project') return
  const now = Date.now()
  if (now - lastLiveExtract < 150) return
  lastLiveExtract = now

  // 优先取工具写入的 html 文件代码块
  const toolRegex = /\[工具调用\][\s\S]*?(\S+\.\w+)[\s\S]*?```(?:html)?\s*([\s\S]*?)```/g
  let best: string | null = null
  let m: RegExpExecArray | null
  while ((m = toolRegex.exec(content))) {
    const filename = m[1]
    const code = m[2]
    if (filename.endsWith('.html')) best = code
  }
  // 兜底：取最后一个完整 html 代码块
  if (!best) {
    const blockRegex = /```html\s*([\s\S]*?)```/g
    while ((m = blockRegex.exec(content))) best = m[1]
  }
  if (best && best.trim().length > 30) {
    livePreviewHtml.value = best
  }
}

// 是否可打开新窗口预览
const canOpenPreview = computed(() => {
  return previewReady.value && codeComplete.value && !generating.value
})

const canShowPreview = computed(() => {
  return previewReady.value && codeComplete.value && !generating.value
})

// 获取渲染后的 Markdown（带缓存 + 工具调用增强 + 代码块头部与复制按钮）
const getRenderedMarkdown = (msgId: string): string => {
  const msg = messages.value.find(m => m.id === msgId)
  if (!msg) return ''

  if (renderedMarkdownCache.has(msgId)) {
    return renderedMarkdownCache.get(msgId)!
  }

  // 用户消息：弱化「修改需求/HTML上下文」前缀标签，让真实需求与代码成为视觉中心
  let enhanced = msg.content
  if (msg.role === 'user') {
    enhanced = enhanced
      .replace(/^用户的修改需求：/m, '<span class="ctx-label">用户的修改需求：</span>')
      .replace(/^用户指定的当前修改目标元素（HTML上下文）：/m, '<span class="ctx-label">用户指定的当前修改目标元素（HTML上下文）：</span>')
  }

  const rendered = DOMPurify.sanitize(enhanceCodeBlocks(md.render(enhanced)))
  renderedMarkdownCache.set(msgId, rendered)
  return rendered
}

// 点击事件委托：处理 Markdown 内的复制按钮
const onMarkdownClick = async (e: Event) => {
  const target = (e.target as HTMLElement).closest('.cb-copy-btn')
  if (!target) return
  e.preventDefault()

  const block = target.closest('.code-block')
  const codeEl = block?.querySelector('pre code')
  if (!codeEl) return
  const code = codeEl.textContent || ''

  try {
    await navigator.clipboard.writeText(code)
    const btn = target as HTMLButtonElement
    const span = btn.querySelector('span')
    const label = span ? span.textContent : '复制'
    if (span) span.textContent = '已复制'
    btn.classList.add('copied')
    antMessage.success('代码已复制到剪贴板')
    setTimeout(() => {
      btn.classList.remove('copied')
      if (span) span.textContent = label
    }, 2000)
  } catch {
    antMessage.error('复制失败')
  }
}

// 开始构建进度动画
const startBuildProgress = () => {
  buildProgress.value = 0
  if (buildProgressTimer) clearInterval(buildProgressTimer)
  buildProgressTimer = setInterval(() => {
    if (generating.value && buildProgress.value < 90) {
      buildProgress.value += Math.random() * 2
    }
  }, 200)
}

// 停止构建进度动画
const stopBuildProgress = () => {
  if (buildProgressTimer) {
    clearInterval(buildProgressTimer)
    buildProgressTimer = null
  }
  buildProgress.value = 100
  setTimeout(() => {
    buildProgress.value = 0
  }, 500)
}

// 批量刷新流式内容，避免大项目生成时出现数十分钟的逐字追赶。
const processTypewriter = () => {
  if (!currentTypingMessage.value) {
    typewriterTimer.value = null
    return
  }

  if (typewriterQueue.value.length === 0) {
    typewriterTimer.value = null
    // 分片之间可能存在间隔，必须保留当前消息，等待后续 SSE 分片。
    if (!streamCompleted.value) return

    currentTypingMessage.value.isTyping = false
    parsedTokensCache.delete(currentTypingMessage.value.id)
    currentTypingMessage.value = null
    if (pendingCompletion) pendingCompletion()
    return
  }

  const batch = typewriterQueue.value.slice(0, STREAM_BATCH_SIZE)
  typewriterQueue.value = typewriterQueue.value.slice(batch.length)
  currentTypingMessage.value.content += batch
  // MarkdownIt、语法高亮和 DOMPurify 都会遍历整条消息，限制重渲染频率防止长内容卡顿。
  const now = Date.now()
  if (typewriterQueue.value.length === 0 || now - lastMarkdownRenderAt >= MARKDOWN_RENDER_INTERVAL_MS) {
    renderedMarkdownCache.delete(currentTypingMessage.value.id)
    parsedTokensCache.delete(currentTypingMessage.value.id)
    lastMarkdownRenderAt = now
  }
  // 流式过程中实时提取可预览内容
  extractLivePreview(currentTypingMessage.value.content)
  scrollToBottom()

  typewriterTimer.value = setTimeout(processTypewriter, STREAM_BATCH_DELAY)
}

// 添加内容到流式缓冲
const addToTypewriter = (text: string) => {
  typewriterQueue.value += text
}

// 停止打字机
const stopTypewriter = () => {
  if (typewriterTimer.value) {
    clearTimeout(typewriterTimer.value)
    typewriterTimer.value = null
  }
  streamCompleted.value = false
  pendingCompletion = null
  // 将缓冲剩余内容一次性添加
  if (currentTypingMessage.value && typewriterQueue.value.length > 0) {
    currentTypingMessage.value.content += typewriterQueue.value
    renderedMarkdownCache.delete(currentTypingMessage.value.id)
    parsedTokensCache.delete(currentTypingMessage.value.id)
    typewriterQueue.value = ''
  }
  if (currentTypingMessage.value) {
    currentTypingMessage.value.isTyping = false
    parsedTokensCache.delete(currentTypingMessage.value.id)
    currentTypingMessage.value = null
  }
}

// 提取所有代码文件
const extractAllCodeFiles = (content: string) => {
  codeFiles.value = []
  extractedCode.value = ''

  // 使用非贪婪并匹配跨行的方式，更健壮地解析代码块
  const toolCallRegex = /\[工具调用\][\s\S]*?(\S+\.\w+)[\s\S]*?```(\w+)?\s*([\s\S]*?)```/g
  const toolMatches = [...content.matchAll(toolCallRegex)]

  if (toolMatches.length > 0) {
    toolMatches.forEach((match, index) => {
      const filename = match[1]
      const lang = match[2] || getFileExtension(filename)
      const code = match[3].trim()

      codeFiles.value.push({
        filename,
        language: lang.toLowerCase(),
        code,
        expanded: index === 0
      })
    })

    const entryFile = codeFiles.value.find(f =>
      f.filename.endsWith('index.html') ||
      f.filename.endsWith('App.vue') ||
      f.filename === 'index.html'
    )

    if (entryFile) {
      extractedCode.value = entryFile.code
    } else if (codeFiles.value.length > 0) {
      extractedCode.value = codeFiles.value[0].code
    }
    return
  }

  const codeBlockRegex = /```(\w+)?\s*([\s\S]*?)```/g
  const matches = [...content.matchAll(codeBlockRegex)]

  if (matches.length === 0) {
    if (content.includes('<!DOCTYPE') || content.includes('<html') || content.includes('<body')) {
      codeFiles.value = [{
        filename: 'index.html',
        language: 'html',
        code: content.trim(),
        expanded: true
      }]
      extractedCode.value = content.trim()
    }
    return
  }

  const getFilename = (lang: string, index: number): string => {
    const langLower = lang.toLowerCase()
    switch (langLower) {
      case 'html':
      case 'vue':
        return 'index.html'
      case 'css':
        return 'style.css'
      case 'javascript':
      case 'js':
        return 'script.js'
      case 'typescript':
      case 'ts':
        return 'script.ts'
      case 'json':
        return 'data.json'
      default:
        return `file${index + 1}.${langLower}`
    }
  }

  matches.forEach((match, index) => {
    const lang = (match[1] || 'html').toLowerCase()
    const code = match[2].trim()

    let filename = getFilename(lang, index)
    const existingCount = codeFiles.value.filter(f => f.filename === filename).length
    if (existingCount > 0) {
      const baseName = filename.split('.')[0]
      const ext = filename.split('.')[1]
      filename = `${baseName}_${existingCount + 1}.${ext}`
    }

    codeFiles.value.push({
      filename,
      language: lang,
      code,
      expanded: index === 0
    })
  })

  const htmlFile = codeFiles.value.find(f => f.language === 'html')
  const cssFile = codeFiles.value.find(f => f.language === 'css')
  const jsFile = codeFiles.value.find(f => f.language === 'javascript' || f.language === 'js')

  if (htmlFile && (cssFile || jsFile)) {
    let combinedHtml = htmlFile.code

    if (cssFile) {
      const styleTag = `\n<style>\n${cssFile.code}\n</style>\n`
      if (combinedHtml.includes('</head>')) {
        combinedHtml = combinedHtml.replace('</head>', `${styleTag}</head>`)
      } else if (combinedHtml.includes('<head>')) {
        combinedHtml = combinedHtml.replace('<head>', `<head>${styleTag}`)
      } else {
        combinedHtml = `<style>${cssFile.code}</style>\n${combinedHtml}`
      }
    }

    if (jsFile) {
      const scriptTag = '\n<script>\n' + jsFile.code + '\n<\/script>\n'
      if (combinedHtml.includes('</body>')) {
        combinedHtml = combinedHtml.replace('</body>', `${scriptTag}</body>`)
      } else {
        combinedHtml = `${combinedHtml}\n<script>${jsFile.code}<\/script>`
      }
    }

    extractedCode.value = combinedHtml
  } else if (htmlFile) {
    extractedCode.value = htmlFile.code
  } else if (codeFiles.value.length > 0) {
    extractedCode.value = codeFiles.value[0].code
  }

}

// 根据文件扩展名获取语言类型
const getFileExtension = (filename: string): string => {
  const ext = filename.split('.').pop()?.toLowerCase() || ''
  const langMap: Record<string, string> = {
    'vue': 'vue',
    'html': 'html',
    'css': 'css',
    'js': 'javascript',
    'ts': 'typescript',
    'json': 'json',
    'md': 'markdown'
  }
  return langMap[ext] || ext
}

// 切换文件展开状态
const toggleFileExpand = (index: number) => {
  codeFiles.value[index].expanded = !codeFiles.value[index].expanded
}

// 展开所有文件
const expandAllFiles = () => {
  codeFiles.value.forEach(f => f.expanded = true)
}

// 折叠所有文件
const collapseAllFiles = () => {
  codeFiles.value.forEach(f => f.expanded = false)
}

// 代码高亮缓存
const highlightedCodeCache = new WeakMap<CodeFile, string>()

// 获取高亮后的代码
const getHighlightedCode = (file: CodeFile): string => {
  if (highlightedCodeCache.has(file)) {
    return highlightedCodeCache.get(file)!
  }
  let highlighted = ''
  try {
    highlighted = hljs.highlight(file.code, { language: file.language }).value
  } catch {
    highlighted = hljs.highlightAuto(file.code).value
  }
  highlightedCodeCache.set(file, highlighted)
  return highlighted
}

// 计算代码行数（用于显示行号）
const codeLineCount = (file: CodeFile): number => {
  if (!file.code) return 0
  return file.code.split('\n').length
}

// 复制单个文件代码
const copyFileCode = async (file: CodeFile) => {
  try {
    await navigator.clipboard.writeText(file.code)
    antMessage.success(`已复制 ${file.filename}`)
  } catch {
    antMessage.error('复制失败')
  }
}

// 复制用户消息
const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    antMessage.success('内容已复制')
  } catch {
    antMessage.error('复制失败')
  }
}

// 重新生成：截断到该 AI 消息，用其前一条用户消息重新发送
const regenerateMessage = (msg: ChatMessage) => {
  if (generating.value) return
  const idx = messages.value.findIndex(m => m.id === msg.id)
  if (idx < 0) return
  let prompt = ''
  for (let i = idx - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user') {
      prompt = messages.value[i].content
      break
    }
  }
  if (!prompt) return
  messages.value = messages.value.slice(0, idx)
  inputMessage.value = prompt
  nextTick(() => sendMessage(prompt))
}

// 赞/踩反馈（本地状态）
const feedback = (msg: ChatMessage, type: 'up' | 'down') => {
  msg.feedback = msg.feedback === type ? undefined : type
}

/* ---------- 拖拽引用：右侧预览元素 → 输入框上下文（postMessage 手动拖拽） ---------- */
const pickMode = ref(false)
let pickPayload: ReferenceElement | null = null

const togglePickMode = () => {
  if (!canOpenPreview.value) return
  pickMode.value = !pickMode.value
  // 同步拾取脚本启停状态（iframe 常驻不重建，避免切换闪烁）
  syncPickEnabled()
  if (!pickMode.value) {
    pickPayload = null
    isDragOver.value = false
    window.removeEventListener('mousemove', handlePickMove)
    window.removeEventListener('mouseup', handlePickUp)
    // 关闭拾取时通知 iframe 复位并解锁文本选择
    previewIframe.value?.contentWindow?.postMessage({ type: 'pick-end' }, '*')
  } else if (!previewDoc.value) {
    antMessage.info('当前预览不支持拾取（注入失败），将仅展示')
  }
}

// 通知 iframe 内的拾取脚本启用/禁用（脚本禁用时零副作用，行为等同普通预览）
const syncPickEnabled = () => {
  previewIframe.value?.contentWindow?.postMessage(
    { type: 'pick-set-enabled', enabled: pickMode.value },
    '*'
  )
}

// 接收 iframe 内拾取脚本上报的拖拽开始
const handlePickMessage = (e: MessageEvent) => {
  if (!pickMode.value) return
  const data = e.data
  if (!data || typeof data !== 'object' || data.type !== 'pick-drag-start') return
  const payload = data.payload as ReferenceElement
  if (!payload || !payload.tagName || !payload.outerHTML) return
  pickPayload = payload
  isDragOver.value = true
  window.addEventListener('mousemove', handlePickMove)
  window.addEventListener('mouseup', handlePickUp)
}

// 拖拽中（鼠标可能位于父文档，保持状态即可）
const handlePickMove = () => {}

// 松开鼠标：落在输入区则添加引用，否则取消
const handlePickUp = (e: MouseEvent) => {
  window.removeEventListener('mousemove', handlePickMove)
  window.removeEventListener('mouseup', handlePickUp)
  isDragOver.value = false
  const el = inputAreaRef.value
  if (el && pickPayload) {
    const rect = el.getBoundingClientRect()
    const inside =
      e.clientX >= rect.left && e.clientX <= rect.right &&
      e.clientY >= rect.top && e.clientY <= rect.bottom
    if (inside) {
      referenceElements.value.push(pickPayload)
      antMessage.success(`已添加 ${pickPayload.tagName.toLowerCase()} 为修改上下文`)
    }
  }
  pickPayload = null
  // 通知 iframe 复位拾取状态
  previewIframe.value?.contentWindow?.postMessage({ type: 'pick-end' }, '*')
}

// 移除引用上下文
const removeReference = (index: number) => {
  referenceElements.value.splice(index, 1)
}

// 修改应用弹窗
const editModalOpen = ref(false)
const handleAppEdited = (appName: string) => {
  if (appInfo.value) {
    appInfo.value = { ...appInfo.value, appName }
  }
}

// 获取应用信息
const loadAppInfo = async () => {
  try {
    const res = await getAppById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
    }
  } catch (error) {
    console.error('获取应用信息失败', error)
  }
}

const loadProjectPreview = async (generatedAfter?: number) => {
  const previewBaseUrl = `/api/app/preview/${appId}/`
  const query = new URLSearchParams({ t: String(Date.now()) })
  if (generatedAfter) query.set('after', String(generatedAfter))

  const response = await fetch(`${previewBaseUrl}?${query}`, {
    credentials: 'include',
    cache: 'no-store'
  })
  const contentType = response.headers.get('content-type') || ''
  if (!response.ok || !contentType.includes('text/html')) return false

  if (appInfo.value?.codeGenType === 'vue_project') {
    previewUrl.value = `${generatedPreviewOrigin}/preview/${appId}/?t=${Date.now()}`
    // 尝试注入拾取脚本；跨域且无 CORS 时回退直连
    previewDoc.value = await tryBuildInjectedDoc(previewUrl.value, `${generatedPreviewOrigin}/preview/${appId}/`)
  } else {
    const tokenResponse = await createPreviewToken({ appId })
    const token = tokenResponse.data.data
    if (tokenResponse.data.code !== 0 || !token) return false
    previewUrl.value = `${previewBaseUrl}${token}/?t=${Date.now()}`
    previewDoc.value = await tryBuildInjectedDoc(previewUrl.value, `${previewBaseUrl}${token}/`)
  }
  previewMode.value = previewDoc.value ? 'srcdoc' : 'src'
  previewReady.value = true
  return true
}

const waitForProjectPreview = async (generatedAfter: number) => {
  const deadline = Date.now() + PREVIEW_POLL_TIMEOUT_MS
  while (Date.now() < deadline) {
    try {
      if (await loadProjectPreview(generatedAfter)) return true
    } catch (error) {
      console.debug('项目预览尚未就绪', error)
    }
    await new Promise(resolve => setTimeout(resolve, PREVIEW_POLL_INTERVAL_MS))
  }
  return false
}

const loadProjectFiles = async () => {
  try {
    const response = await getProjectFiles({ appId })
    const files = response.data.data
    if (response.data.code !== 0 || !files?.length) return false

    codeFiles.value = files.map((file, index) => ({
      filename: file.path || `file-${index + 1}`,
      language: file.language || getFileExtension(file.path || ''),
      code: file.content || '',
      expanded: index === 0
    }))
    return true
  } catch (error) {
    console.error('加载项目源码失败', error)
    return false
  }
}

// 加载历史对话记录（按时间正序分页拉取，最多 5 页）
const loadChatHistory = async () => {
  let lastCreateTime: string | undefined
  const historyMsgs: ChatMessage[] = []
  for (let i = 0; i < 5; i++) {
    try {
      const res = await listAppChatHistory({
        appId,
        pageSize: 20,
        lastCreateTime,
      })
      if (res.data.code !== 0 || !res.data.data) break
      const records = res.data.data.records || []
      if (records.length === 0) break
      const pageMessages = records
        .slice()
        .reverse()
        .reduce<ChatMessage[]>((items, r) => {
          if (!r.message) return items
          if (r.messageType === 'user') {
            items.push({ id: `hist-${r.id}`, role: 'user', content: r.message })
          }
          if (r.messageType === 'ai') {
            items.push({ id: `hist-${r.id}`, role: 'assistant', content: r.message })
          }
          return items
        }, [])
      historyMsgs.unshift(...pageMessages)
      lastCreateTime = records[records.length - 1].createTime
      if (records.length < 20) break
    } catch (error) {
      console.warn('加载历史对话失败', error)
      break
    }
  }
  if (historyMsgs.length > 0) {
    messages.value = historyMsgs
  }
}

// 发送消息
const sendMessage = async (promptTextArg?: string) => {
  const userMessage = (promptTextArg ?? inputMessage.value).trim()
  if (!userMessage || generating.value) return

  codeComplete.value = false
  previewReady.value = false
  previewUrl.value = ''
  previewDoc.value = ''
  previewMode.value = 'src'
  livePreviewHtml.value = ''
  extractedCode.value = ''
  streamCompleted.value = false
  pendingCompletion = null
  const generationStartedAt = Date.now()

  // 添加用户消息
  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: userMessage
  })
  inputMessage.value = ''

  // 拼装最终 Prompt：用户的修改需求 + 拖拽引用的 HTML 上下文
  let promptText = userMessage
  if (referenceElements.value.length > 0) {
    const ctx = referenceElements.value
      .map(r => '```html\n' + r.outerHTML + '\n```')
      .join('\n\n')
    promptText = `用户的修改需求：${userMessage}\n\n用户指定的当前修改目标元素（HTML上下文）：\n${ctx}`
  }
  referenceElements.value = []

  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }

  generating.value = true
  startBuildProgress()

  // 创建 AI 消息（带打字状态）
  const assistantMessage: ChatMessage = {
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    isTyping: true
  }
  messages.value.push(assistantMessage)
  currentTypingMessage.value = assistantMessage
  typewriterQueue.value = ''
  lastMarkdownRenderAt = 0

  // 发送新消息时恢复自动跟随并滚动到底部
  autoScrollEnabled.value = true
  scrollToBottom(true)

  // 创建 SSE 连接
  const url = `/api/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(promptText)}`
  eventSource = new EventSource(url)

  let watchdogTimer: ReturnType<typeof setTimeout> | null = null
  const resetWatchdog = () => {
    if (watchdogTimer) clearTimeout(watchdogTimer)
    watchdogTimer = setTimeout(() => {
      // SSE 超时看门狗：如果 45 秒没有任何事件，则强行断开避免卡死
      if (!streamCompleted.value && eventSource) {
        console.warn('SSE Watchdog timeout. Aborting connection.')
        eventSource.onerror?.(new Event('error'))
      }
    }, 45000)
  }
  resetWatchdog()

  eventSource.onmessage = (event) => {
    resetWatchdog()
    try {
      const data = JSON.parse(event.data)

      if (data.type === 'ai_response' && data.data) {
        // 添加到打字机队列
        addToTypewriter(data.data)
        // 启动打字机（如果还没启动）
        if (!typewriterTimer.value) {
          processTypewriter()
        }
      } else if (data.type === 'tool_request') {
        const toolInfo = `\n[工具调用] ${data.name || '工具'}\n`
        addToTypewriter(toolInfo)
        if (!typewriterTimer.value) {
          processTypewriter()
        }
      } else if (data.type === 'tool_executed') {
        // 工具执行完成：统一写入完成标记，驱动工具块进入「完成」状态
        const toolName = data.result?.toolName || data.name || ''
        if (toolName === 'writeFile' && data.result?.result) {
          const fileMatch = data.result.result.match(/文件写入成功: (.+)/)
          if (fileMatch) {
            addToTypewriter(`✓ ${fileMatch[1]}\n`)
            if (!typewriterTimer.value) processTypewriter()
          }
        } else if (toolName) {
          addToTypewriter(`✓ ${toolName}\n`)
          if (!typewriterTimer.value) processTypewriter()
        }
      } else if (data.d) {
        addToTypewriter(data.d)
        if (!typewriterTimer.value) {
          processTypewriter()
        }
      }
    } catch {
      addToTypewriter(event.data)
      if (!typewriterTimer.value) {
        processTypewriter()
      }
    }
  }

  eventSource.addEventListener('generation-error', (event) => {
    if (watchdogTimer) clearTimeout(watchdogTimer)
    let errorMessage = '生成失败，请重试'
    try {
      const payload = JSON.parse((event as MessageEvent<string>).data)
      if (payload.message) errorMessage = payload.message
    } catch {
      // 保留默认错误文案，避免错误事件本身影响会话状态恢复。
    }
    eventSource?.close()
    streamCompleted.value = true
    pendingCompletion = null
    stopTypewriter()
    generating.value = false
    stopBuildProgress()
    assistantMessage.content = errorMessage
    assistantMessage.isTyping = false
  })

  eventSource.onerror = () => {
    if (watchdogTimer) clearTimeout(watchdogTimer)
    // EventSource 在服务端正常结束后也可能触发一次 error，不能覆盖已完成的响应。
    if (streamCompleted.value) return
    eventSource?.close()
    streamCompleted.value = false
    pendingCompletion = null
    stopTypewriter()
    generating.value = false
    stopBuildProgress()
    if (!assistantMessage.content) {
      assistantMessage.content = '生成失败，请重试'
      assistantMessage.isTyping = false
    }
  }

  eventSource.addEventListener('done', () => {
    if (watchdogTimer) clearTimeout(watchdogTimer)
    eventSource?.close()
    streamCompleted.value = true
    pendingCompletion = async () => {
      const [previewLoaded, filesLoaded] = await Promise.all([
        waitForProjectPreview(generationStartedAt),
        loadProjectFiles()
      ])
      if (!filesLoaded) extractAllCodeFiles(assistantMessage.content)
      previewReady.value = previewLoaded
      codeComplete.value = true
      generating.value = false
      stopBuildProgress()
      pendingCompletion = null
    }

    // SSE 完成时，打字机队列可能仍有大量内容，等队列消费完再结束生成。
    if (typewriterQueue.value.length === 0 && !typewriterTimer.value) {
      processTypewriter()
    }
  })
}

// 判断用户是否位于消息列表底部附近
const isNearBottom = () => {
  const el = messagesContainer.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < FOLLOW_THRESHOLD
}

// 滚动到底部（节流 + 无动画，避免平滑滚动动画互相打断造成弹跳）
const scrollToBottom = (force = false) => {
  if (!autoScrollEnabled.value && !force) return
  if (!force && !isNearBottom()) return

  const now = Date.now()
  if (now - lastAutoScrollTime < SCROLL_THROTTLE_MS) return
  lastAutoScrollTime = now

  requestAnimationFrame(() => {
    const el = messagesContainer.value
    if (!el) return
    el.scrollTop = el.scrollHeight
    // 滚动到底后隐藏「回到底部」按钮
    if (showScrollTopBtn.value) showScrollTopBtn.value = false
  })
}

// 回到底部（用户点击悬浮按钮时强制跳转）
const jumpToBottom = () => {
  autoScrollEnabled.value = true
  scrollToBottom(true)
}

// 键盘事件处理
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 清空消息
const clearMessages = () => {
  messages.value = []
  extractedCode.value = ''
  previewUrl.value = ''
  previewReady.value = false
  codeFiles.value = []
  codeComplete.value = false
  renderedMarkdownCache.clear()
  parsedTokensCache.clear()
  typewriterQueue.value = ''
  stopTypewriter()
}

// 刷新预览
const refreshPreview = async () => {
  if (previewReady.value && codeComplete.value) {
    await loadProjectPreview()
  }
}

// 在新窗口打开构建好的网站预览
const openPreviewWindow = () => {
  if (!previewUrl.value || !canOpenPreview.value) return
  window.open(previewUrl.value, '_blank', 'noopener,noreferrer')
}

// 自动调整文本框高度
watch(inputMessage, () => {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 150) + 'px'
  }
})

// 处理初始提示词
const handleInitPrompt = () => {
  const initPrompt = route.query.initPrompt as string
  if (initPrompt) {
    nextTick(() => {
      inputMessage.value = initPrompt
      sendMessage()
      router.replace({ query: {} })
    })
  }
}

// 处理消息容器滚动：用户手动上滑时暂停自动跟随，下滑到底时恢复跟随
const handleContainerScroll = () => {
  const el = messagesContainer.value
  if (!el) return

  // 位于底部附近：恢复自动跟随，并隐藏回到底部按钮
  if (isNearBottom()) {
    autoScrollEnabled.value = true
    showScrollTopBtn.value = false
    return
  }

  // 正在生成时用户上滑阅读：暂停跟随，显示回到底部按钮
  if (generating.value) {
    autoScrollEnabled.value = false
    showScrollTopBtn.value = true
  } else {
    showScrollTopBtn.value = true
  }
}

// 清理
onUnmounted(() => {
  eventSource?.close()
  if (buildProgressTimer) clearInterval(buildProgressTimer)
  stopTypewriter()
  messagesContainer.value?.removeEventListener('scroll', handleContainerScroll)
  window.removeEventListener('message', handlePickMessage)
  window.removeEventListener('mousemove', handlePickMove)
  window.removeEventListener('mouseup', handlePickUp)
})

onMounted(async () => {
  // 重置外层滚动容器位置：SPA 路由切换会保留 .main-content 的 scrollTop，
  // 若不归位会导致进入页面时直接停留在底部（页脚顶入视口、看不到头部）。
  nextTick(() => {
    const scrollContainer = document.querySelector<HTMLElement>('.main-content')
    if (scrollContainer && scrollContainer.scrollTop > 0) {
      scrollContainer.scrollTop = 0
    }
  })

  window.addEventListener('message', handlePickMessage)
  await loadAppInfo()
  const hasInitPrompt = !!route.query.initPrompt
  const [previewLoaded] = await Promise.all([
    hasInitPrompt ? Promise.resolve(false) : loadProjectPreview().catch(() => false),
    loadProjectFiles(),
    loadChatHistory(),
  ])
  if (!hasInitPrompt) codeComplete.value = previewLoaded

  // 项目文件接口无数据时，从最后一条 AI 回复中提取代码文件作为兜底
  if (codeFiles.value.length === 0) {
    const lastAi = [...messages.value].reverse().find(m => m.role === 'assistant')
    if (lastAi) extractAllCodeFiles(lastAi.content)
  }

  // 有历史消息时滚动到底部
  if (messages.value.length > 0) {
    scrollToBottom(true)
  }

  handleInitPrompt()
  messagesContainer.value?.addEventListener('scroll', handleContainerScroll, { passive: true })
})
</script>

<style scoped>
/* ========== 页面布局 ========== */
.ai-code-page {
  display: flex;
  /* 顶部偏移已由 BasicLayout 的 .main-content (padding-top: 64px) 处理，这里不再重复 */
  height: calc(100vh - 64px);
  box-sizing: border-box;
  background:
    radial-gradient(900px 480px at 92% -12%, rgba(16, 185, 129, 0.05), transparent 60%),
    radial-gradient(720px 400px at -6% 110%, rgba(20, 184, 166, 0.04), transparent 55%),
    linear-gradient(rgba(148, 163, 184, 0.026) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.026) 1px, transparent 1px),
    #0b0f14;
  background-size: auto, auto, 44px 44px, 44px 44px, auto;
}

/* ========== 左侧面板 ========== */
.left-panel {
  position: relative;
  width: 45%;
  min-width: 400px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(148, 163, 184, 0.1);
  background: rgba(148, 163, 184, 0.04);
}

/* 面板头部 */
.panel-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(255, 255, 255, 0.02);
}

/* 当前生成任务 Header：悬浮卡片样式 */
.task-header {
  position: relative;
  isolation: isolate;
  padding: 13px 14px;
  margin: 14px 18px 0;
  border-radius: 14px;
  border: 1px solid rgba(110, 231, 183, 0.16);
  background: linear-gradient(135deg, rgba(20, 34, 39, 0.94), rgba(17, 24, 33, 0.96) 58%, rgba(20, 29, 39, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.07), 0 10px 30px rgba(0, 0, 0, 0.18);
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.task-header::after {
  content: '';
  position: absolute;
  z-index: -1;
  left: 18px;
  right: 18px;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(52, 211, 153, 0.7), transparent);
  opacity: 0.55;
}

.task-header:hover {
  background: linear-gradient(135deg, rgba(23, 47, 48, 0.96), rgba(18, 29, 39, 0.98) 58%, rgba(22, 36, 45, 0.96));
  border-color: rgba(110, 231, 183, 0.3);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08), 0 12px 34px rgba(0, 0, 0, 0.25), 0 0 22px rgba(16, 185, 129, 0.08);
  transform: translateY(-1px);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 13px;
  min-width: 0;
  flex: 1;
  overflow: hidden;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  flex: 1;
}

.header-kicker {
  color: rgba(110, 231, 183, 0.72);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  line-height: 1;
  text-transform: uppercase;
}

/* 提示词文本：单行截断三件套 */
.header-prompt {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(255, 255, 255, 0.92);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.35;
}

.title-icon {
  position: relative;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7dd3fc;
  background: linear-gradient(145deg, rgba(14, 116, 144, 0.32), rgba(16, 185, 129, 0.12));
  border: 1px solid rgba(125, 211, 252, 0.36);
  border-radius: 12px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    0 5px 15px rgba(8, 145, 178, 0.16);
  transition: border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.title-icon::before {
  content: '';
  position: absolute;
  inset: 6px;
  border: 1px solid rgba(125, 211, 252, 0.16);
  border-radius: 8px;
}

.title-icon :deep(svg) {
  position: relative;
  z-index: 1;
  width: 21px;
  height: 21px;
  filter: drop-shadow(0 0 7px rgba(125, 211, 252, 0.5));
}

.title-icon-dot {
  position: absolute;
  z-index: 2;
  right: 6px;
  top: 6px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.9);
}

.task-header:hover .title-icon {
  border-color: rgba(125, 211, 252, 0.62);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.16),
    0 5px 18px rgba(8, 145, 178, 0.3);
  transform: translateY(-1px);
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.gen-indicator {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #6ee7b7;
  padding: 6px 11px;
  border-radius: 9px;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(52, 211, 153, 0.28);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
  font-family:
    system-ui,
    -apple-system,
    'Segoe UI',
    Roboto,
    'PingFang SC',
    'Hiragino Sans GB',
    'Microsoft YaHei',
    sans-serif;
  white-space: nowrap;
}

.gen-indicator i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.9);
}

.assistant-thinking {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 26px;
  color: rgba(226, 232, 240, 0.68);
  font-size: 13px;
}

.thinking-icon {
  display: inline-flex;
  color: #6ee7b7;
  animation: thinking-spin 1.1s linear infinite;
}

.thinking-dots {
  display: inline-flex;
  gap: 3px;
  margin-left: 1px;
}

.thinking-dots i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #6ee7b7;
  animation: thinking-dot 1.1s ease-in-out infinite;
}

.thinking-dots i:nth-child(2) { animation-delay: 0.14s; }
.thinking-dots i:nth-child(3) { animation-delay: 0.28s; }

@keyframes thinking-spin {
  to { transform: rotate(360deg); }
}

@keyframes thinking-dot {
  0%, 60%, 100% { opacity: 0.28; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-3px); }
}

.btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, opacity 0.25s ease, transform 0.25s ease;
}

.btn-icon:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.09);
  color: rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.btn-icon:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-icon svg {
  width: 18px;
  height: 18px;
}

/* 新窗口打开预览按钮 */
.open-window-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  height: 36px;
  border: 1px solid rgba(16, 185, 129, 0.35);
  border-radius: 10px;
  background: rgba(16, 185, 129, 0.08);
  color: #6ee7b7;
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.25s ease, border-color 0.25s ease, color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
}

.open-window-btn svg {
  width: 14px;
  height: 14px;
}

.open-window-btn:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.16);
  border-color: rgba(16, 185, 129, 0.6);
  color: #a7f3d0;
  box-shadow: 0 0 16px rgba(16, 185, 129, 0.2);
  transform: translateY(-1px);
}

.open-window-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* 修改应用按钮：复用荧光主题按钮，微调字体与呼吸光晕 */
.edit-app-btn {
  font-weight: 500;
  letter-spacing: 0.2px;
}

.edit-app-btn svg {
  width: 14px;
  height: 14px;
}

.edit-app-btn:hover:not(:disabled) svg {
  animation: edit-gear-spin 1.6s linear infinite;
}

@keyframes edit-gear-spin {
  to { transform: rotate(90deg); }
}

/* 拖拽修改开关按钮 */
.pick-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 10px;
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.25s ease, border-color 0.25s ease, color 0.25s ease, box-shadow 0.25s ease;
}

.pick-switch .switch-track {
  position: relative;
  width: 30px;
  height: 16px;
  flex-shrink: 0;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.14);
  transition: background-color 0.25s ease;
}

.pick-switch .switch-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
  transition: transform 0.25s ease, background-color 0.25s ease;
}

.pick-switch.on {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.45);
  color: #6ee7b7;
  box-shadow: 0 0 14px rgba(16, 185, 129, 0.12);
}

.pick-switch.on .switch-track {
  background: linear-gradient(135deg, #10b981, #059669);
}

.pick-switch.on .switch-knob {
  transform: translateX(14px);
  background: #ffffff;
}

.pick-switch:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.09);
  border-color: rgba(255, 255, 255, 0.32);
  color: #ffffff;
}

.pick-switch.on:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.18);
  border-color: rgba(16, 185, 129, 0.6);
  color: #a7f3d0;
}

.pick-switch:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* 清空对话按钮：三段式分层 Hover */
.clear-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, color 0.3s ease, opacity 0.3s ease, transform 0.3s ease;
  /* 1) 默认弱化隐藏 */
  opacity: 0.3;
}

/* 2) 悬停整个任务卡片时恢复可见 */
.task-header:hover .clear-btn {
  opacity: 1;
}

/* 3) 精准悬停按钮：危险操作红色反馈 */
.clear-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.22);
  color: #fca5a5;
  box-shadow: 0 0 12px rgba(239, 68, 68, 0.3);
}

.clear-btn:active:not(:disabled) {
  transform: scale(0.92);
}

/* ========== 消息列表 ========== */
.messages-container {
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 24px 22px;
  scrollbar-width: thin;
  scrollbar-color: rgba(16, 185, 129, 0.35) transparent;
}

.messages-container::-webkit-scrollbar {
  width: 5px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: rgba(16, 185, 129, 0.35);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

/* 回到底部按钮 */
.scroll-bottom-btn {
  position: absolute;
  right: 24px;
  bottom: 110px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(16, 185, 129, 0.45);
  border-radius: 50%;
  background: rgba(14, 20, 30, 0.92);
  color: #6ee7b7;
  cursor: pointer;
  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.45),
    0 0 14px rgba(16, 185, 129, 0.14);
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.scroll-bottom-btn svg {
  width: 18px;
  height: 18px;
}

.scroll-bottom-btn:hover {
  background: rgba(18, 26, 38, 0.95);
  border-color: rgba(16, 185, 129, 0.75);
  color: #a7f3d0;
  transform: translateY(-2px);
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.5),
    0 0 18px rgba(16, 185, 129, 0.25);
}

.scroll-fade-enter-active,
.scroll-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.scroll-fade-enter-from,
.scroll-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 40px 20px;
}

.empty-illustration {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 28px;
}

.empty-orb {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 60px;
  height: 60px;
}

.orb-ring {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(16, 185, 129, 0.35);
  border-radius: 50%;
  animation: orb-pulse 2s ease-in-out infinite;
}

.orb-core {
  position: absolute;
  inset: 15px;
  background: linear-gradient(135deg, #10b981, #0d9488);
  border-radius: 50%;
  box-shadow: 0 0 30px rgba(16, 185, 129, 0.45);
}

@keyframes orb-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.5;
  }
}

.empty-particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: #10b981;
  border-radius: 50%;
  animation: particle-float 3s ease-in-out infinite;
}

.particle:nth-child(1) {
  top: 10%;
  left: 20%;
  animation-delay: 0s;
}

.particle:nth-child(2) {
  top: 30%;
  right: 15%;
  animation-delay: 1s;
}

.particle:nth-child(3) {
  bottom: 20%;
  left: 30%;
  animation-delay: 2s;
}

@keyframes particle-float {
  0%, 100% {
    transform: translateY(0) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translateY(-10px) scale(1.2);
    opacity: 1;
  }
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 10px;
}

.empty-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
  line-height: 1.6;
}

.empty-chips {
  display: flex;
  gap: 8px;
  margin-top: 22px;
  flex-wrap: wrap;
  justify-content: center;
}

.empty-chip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  padding: 5px 12px;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* ========== 消息气泡 ========== */
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 22px;
  animation: message-fade-in 0.35s ease-out both;
  animation-delay: calc(min(var(--i, 0), 10) * 40ms);
}

.user-message {
  justify-content: flex-end;
}

.assistant-message {
  justify-content: flex-start;
}

@keyframes message-fade-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 头像 */
.message-avatar {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  margin-top: 2px;
}

.user-avatar {
  background: #121a26;
  color: #6ee7b7;
  border: 1px solid rgba(16, 185, 129, 0.3);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.assistant-avatar {
  background: linear-gradient(135deg, #10b981, #0d9488);
  color: #fff;
  box-shadow:
    0 0 10px rgba(16, 185, 129, 0.45),
    0 0 22px rgba(13, 148, 136, 0.28),
    0 4px 16px rgba(4, 120, 87, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.avatar-icon {
  width: 20px;
  height: 20px;
  display: block;
}

/* 消息气泡 */
.message-bubble {
  max-width: 72%;
  min-width: 80px;
}

.bubble-content {
  padding: 13px 17px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.75;
  overflow-wrap: break-word;
}

/* 用户气泡（右侧）—— 微发光卡片，保留绿色隐喻但低饱和 */
.user-bubble {
  position: relative;
}

.user-bubble .bubble-content {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(4, 120, 87, 0.15));
  border: 1px solid rgba(16, 185, 129, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 12px;
  border-top-right-radius: 4px;
  padding: 12px 15px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 6px 20px rgba(0, 0, 0, 0.18);
}

/* 用户气泡内正文：高对比浅色 + 舒展行高 */
.user-bubble .markdown-body {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14.5px;
  line-height: 1.6;
}

.user-bubble .markdown-body :deep(p) {
  margin: 0 0 8px;
}

.user-bubble .markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.user-bubble .markdown-body :deep(strong) {
  color: #ffffff;
  font-weight: 600;
}

/* 前缀说明标签：弱化（缩小 + 低透明度） */
.markdown-body :deep(.ctx-label) {
  display: inline;
  font-size: 12px;
  opacity: 0.55;
  color: rgba(255, 255, 255, 0.75);
  font-weight: 400;
  letter-spacing: 0.2px;
}

/* 用户气泡内代码块：更深背景 + 隔离边框，与正文层次分明 */
.user-bubble .markdown-body :deep(.code-block) {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  margin: 10px 0;
  box-shadow: none;
}

.user-bubble .markdown-body :deep(.cb-head) {
  height: 32px;
  padding: 0 10px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.user-bubble .markdown-body :deep(.cb-lang) {
  font-size: 9.5px;
  color: rgba(255, 255, 255, 0.28);
}

/* 用户气泡内复制按钮：克制微图标，hover 显现 */
.user-bubble .markdown-body :deep(.cb-copy-btn) {
  padding: 3px 6px;
  font-size: 11px;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  background: transparent;
  opacity: 0;
  transition: opacity 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.user-bubble .markdown-body :deep(.cb-copy-btn span) {
  display: none;
}

.user-bubble:hover .markdown-body :deep(.cb-copy-btn) {
  opacity: 0.7;
}

.user-bubble .markdown-body :deep(.cb-copy-btn:hover) {
  opacity: 1;
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.user-bubble .markdown-body :deep(.code-block pre) {
  padding: 11px 12px;
}

.user-bubble .markdown-body :deep(pre code) {
  font-size: 12.5px;
}

.user-bubble .markdown-body :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 11px 12px;
}

/* 用户消息悬浮复制按钮 */
.bubble-copy-btn {
  position: absolute;
  top: -11px;
  right: 8px;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 7px;
  background: rgba(16, 21, 30, 0.9);
  color: rgba(230, 234, 242, 0.6);
  cursor: pointer;
  opacity: 0;
  transform: translateY(3px);
  transition: opacity 0.2s ease, transform 0.2s ease, color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease;
}

.user-bubble:hover .bubble-copy-btn,
.user-bubble:focus-within .bubble-copy-btn,
.bubble-copy-btn:focus-visible {
  opacity: 1;
  transform: translateY(0);
}

.bubble-copy-btn:hover {
  color: #6ee7b7;
  border-color: rgba(16, 185, 129, 0.5);
  background: rgba(18, 26, 38, 0.95);
}

.bubble-copy-btn svg {
  width: 13px;
  height: 13px;
}

/* AI 气泡（左侧）—— 高级深色质感 */
.assistant-bubble .bubble-content {
  background: rgba(30, 33, 40, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 12px;
  border-top-left-radius: 4px;
  padding: 14px 16px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.2);
}

/* 工具调用状态块 */
.tool-call {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  margin: 8px 0;
  border-radius: 8px;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* 执行完成：左侧小绿点 */
.tool-call.done::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #34d399;
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.8);
}

.tool-call.is-fail::before {
  background: #f87171;
  box-shadow: 0 0 6px rgba(248, 113, 113, 0.8);
}

/* 执行中：底部流动进度条 */
.tool-call:not(.done)::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  height: 2px;
  width: 40%;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, rgba(251, 191, 36, 0.75), transparent);
  animation: toolProgress 1.5s linear infinite;
}

@keyframes toolProgress {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(350%); }
}

.tool-call.done {
  background: rgba(30, 41, 59, 0.3);
  border-color: rgba(255, 255, 255, 0.06);
}

.tc-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #60a5fa;
}

.tc-icon.running {
  color: #fbbf24;
}

.tool-call.done .tc-icon {
  color: #34d399;
}

.tool-call.is-fail .tc-icon {
  color: #f87171;
}

.tc-icon svg {
  width: 14px;
  height: 14px;
}

.tc-name {
  font-size: 12.5px;
  font-weight: 500;
  color: #e2e8f0;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  white-space: nowrap;
}

.tc-file {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.tc-tag {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.5px;
  padding: 2px 9px;
  border-radius: 100px;
  background: rgba(251, 191, 36, 0.12);
  color: #fcd34d;
  border: 1px solid rgba(251, 191, 36, 0.28);
}

.tool-call.done .tc-tag {
  background: rgba(52, 211, 153, 0.12);
  color: #6ee7b7;
  border-color: rgba(52, 211, 153, 0.3);
}

.tool-call.is-fail .tc-tag {
  background: rgba(248, 113, 113, 0.12);
  color: #fca5a5;
  border-color: rgba(248, 113, 113, 0.3);
}

/* 底部操作栏：默认微隐，气泡悬停渐显，按钮悬停全亮 */
.msg-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 8px;
  padding: 0 4px;
  opacity: 0.2;
  transition: opacity 0.25s ease;
}

.assistant-bubble:hover .msg-actions {
  opacity: 0.6;
}

.ma-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, opacity 0.2s ease;
}

.assistant-bubble:hover .ma-btn:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.ma-btn:focus-visible {
  outline: 2px solid rgba(16, 185, 129, 0.7);
  outline-offset: 1px;
}

.ma-btn.liked {
  color: #6ee7b7;
}

.ma-btn.disliked {
  color: #fca5a5;
}

.ma-btn svg {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

.ma-divider {
  width: 1px;
  height: 14px;
  margin: 0 4px;
  background: rgba(255, 255, 255, 0.14);
}

/* ========== Markdown 样式 ========== */
.markdown-body {
  color: #d1d5db;
  font-size: 15px;
  line-height: 1.7;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.markdown-body :deep(p) {
  margin: 0 0 10px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(ul), .markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 8px 0 12px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
  color: #d1d5db;
}

.markdown-body :deep(li > p) {
  margin: 0;
}

.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3),
.markdown-body :deep(h4) {
  color: #f3f4f6;
  margin: 18px 0 10px;
  font-weight: 600;
  line-height: 1.4;
}

.markdown-body :deep(h1) { font-size: 20px; }
.markdown-body :deep(h2) { font-size: 18px; }
.markdown-body :deep(h3) { font-size: 16px; }

.markdown-body :deep(a) {
  color: #34d399;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(strong) {
  color: #f3f4f6;
  font-weight: 600;
}

.markdown-body :deep(blockquote) {
  margin: 10px 0;
  padding: 9px 14px;
  border-left: 3px solid rgba(16, 185, 129, 0.55);
  background: rgba(16, 185, 129, 0.07);
  border-radius: 0 10px 10px 0;
  font-size: 13.5px;
}

.markdown-body :deep(blockquote p) {
  margin: 0;
}

.markdown-body :deep(code:not(pre code)) {
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: #fca5a5;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
}

.markdown-body :deep(hr) {
  border: none;
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 16px 0;
}

/* 代码块（带头部控制栏）—— 殿堂级代码渲染 */
.markdown-body :deep(.code-block) {
  margin: 14px 0;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.07);
  background: #0d1117;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.03),
    0 8px 24px rgba(0, 0, 0, 0.35);
}

/* 控制栏：36px 高，左语言名 / 右复制 */
.markdown-body :deep(.cb-head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 36px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.markdown-body :deep(.cb-lang) {
  font-size: 10px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  text-transform: uppercase;
  letter-spacing: 1.2px;
  flex-shrink: 0;
  user-select: none;
}

/* 复制按钮：干净图标 + 文字 */
.markdown-body :deep(.cb-copy-btn) {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 9px;
  font-size: 11.5px;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  color: rgba(255, 255, 255, 0.55);
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease, opacity 0.2s ease, transform 0.2s ease;
}

.markdown-body :deep(.cb-copy-btn svg) {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
}

.markdown-body :deep(.cb-copy-btn:hover) {
  color: #f3f4f6;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.22);
}

.markdown-body :deep(.cb-copy-btn:active) {
  transform: translateY(1px) scale(0.97);
}

.markdown-body :deep(.cb-copy-btn.copied) {
  color: #34d399;
  background: rgba(52, 211, 153, 0.14);
  border-color: rgba(52, 211, 153, 0.5);
  box-shadow: 0 0 12px rgba(52, 211, 153, 0.35);
}

/* 代码内容 */
.markdown-body :deep(.code-block pre) {
  margin: 0;
  padding: 14px 16px;
  overflow-x: auto;
  background: transparent;
  border: none;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.14) transparent;
}

.markdown-body :deep(.code-block pre::-webkit-scrollbar) {
  height: 5px;
}

.markdown-body :deep(.code-block pre::-webkit-scrollbar-thumb) {
  background: rgba(255, 255, 255, 0.14);
  border-radius: 3px;
}

.markdown-body :deep(.code-block pre::-webkit-scrollbar-track) {
  background: transparent;
}

.markdown-body :deep(.code-block pre code) {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #d1d5db;
  background: transparent;
  padding: 0;
}

/* 兼容无头部结构的旧 pre */
.markdown-body :deep(pre) {
  background: #0d1117;
  border-radius: 10px;
  padding: 14px 16px;
  overflow-x: auto;
  margin: 12px 0;
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.markdown-body :deep(pre code) {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #d1d5db;
  background: transparent;
  padding: 0;
}

/* ========== 输入区域 ========== */
.input-area {
  flex-shrink: 0;
  padding: 16px 20px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(0, 0, 0, 0.25);
  position: relative;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

/* 拖拽吸附态：整块输入区高亮 */
.input-area.drag-active {
  border-top-color: rgba(16, 185, 129, 0.5);
  box-shadow: 0 -8px 32px rgba(16, 185, 129, 0.12);
}

/* 吸附遮罩 */
.drop-overlay {
  position: absolute;
  inset: 0;
  z-index: 30;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(8, 12, 18, 0.82);
  border: 2px dashed rgba(16, 185, 129, 0.7);
  border-radius: 12px;
  color: #6ee7b7;
  font-size: 14px;
  font-weight: 500;
  pointer-events: none;
  backdrop-filter: blur(4px);
}

.drop-overlay svg {
  width: 30px;
  height: 30px;
  opacity: 0.85;
  animation: dropBounce 1.2s ease-in-out infinite;
}

@keyframes dropBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.drop-fade-enter-active,
.drop-fade-leave-active {
  transition: opacity 0.2s ease;
}

.drop-fade-enter-from,
.drop-fade-leave-to {
  opacity: 0;
}

/* 引用上下文标签（chips） */
.context-chips {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.chip-pop-enter-active,
.chip-pop-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.chip-pop-enter-from,
.chip-pop-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 14px;
  padding: 11px 13px 11px 16px;
  transition: border-color 0.3s ease, box-shadow 0.3s ease, background-color 0.3s ease;
}

.input-wrapper:focus-within {
  border-color: rgba(16, 185, 129, 0.55);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.13), 0 8px 24px rgba(16, 185, 129, 0.09);
  background: rgba(148, 163, 184, 0.07);
}

.input-glow {
  position: absolute;
  inset: 0;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.11), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.input-wrapper:focus-within .input-glow {
  opacity: 1;
}

.input-wrapper textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #fff;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  max-height: 150px;
  font-family: inherit;
}

.input-wrapper textarea::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, opacity 0.25s ease, transform 0.25s ease;
  position: relative;
  overflow: hidden;
}

.send-btn.active {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
}

.send-btn.active::after {
  content: '';
  position: absolute;
  top: 0;
  left: -60%;
  width: 50%;
  height: 100%;
  background: linear-gradient(105deg, transparent, rgba(255, 255, 255, 0.35), transparent);
  transform: skewX(-20deg);
  transition: left 0.5s ease;
}

.send-btn.active:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.5);
}

.send-btn.active:hover::after {
  left: 120%;
}

.send-btn.sending {
  background: rgba(16, 185, 129, 0.2);
  cursor: wait;
}

.send-btn:disabled {
  cursor: not-allowed;
}

.send-btn svg {
  width: 18px;
  height: 18px;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.input-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
}

.hint-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.hint-item kbd {
  display: inline-block;
  padding: 2px 6px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-bottom-width: 2px;
  border-radius: 4px;
  font-family: inherit;
  font-size: 11px;
}

.hint-divider {
  width: 1px;
  height: 12px;
  background: rgba(255, 255, 255, 0.15);
}

/* ========== 右侧面板 ========== */
.right-panel {
  width: 55%;
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(148, 163, 184, 0.02);
}

/* Tabs */
.tabs {
  display: flex;
  gap: 6px;
  padding: 3px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
}

.tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, opacity 0.25s ease, transform 0.25s ease;
}

.tab:hover {
  color: rgba(255, 255, 255, 0.8);
}

.tab.active {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.25), rgba(13, 148, 136, 0.25));
  color: #6ee7b7;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.15);
}

.tab svg {
  width: 15px;
  height: 15px;
}

/* 预览容器 */
.preview-container {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.preview-content, .code-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 预览空状态 */
.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 40px;
}

.empty-code-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  color: rgba(255, 255, 255, 0.14);
}

.empty-code-icon svg {
  width: 100%;
  height: 100%;
}

.preview-empty .empty-title {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 8px;
}

.preview-empty .empty-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
  margin: 0;
}

/* 构建动画 */
.build-animation {
  position: relative;
  width: 100px;
  height: 100px;
  margin-bottom: 28px;
}

.build-orb {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 50px;
  height: 50px;
}

.orb-inner {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #10b981, #0d9488);
  border-radius: 50%;
  animation: build-pulse 1.5s ease-in-out infinite;
  box-shadow: 0 0 30px rgba(16, 185, 129, 0.4);
}

.build-orb .orb-ring {
  position: absolute;
  inset: -10px;
  border: 2px solid transparent;
  border-top-color: #10b981;
  border-radius: 50%;
  animation: rotate 1s linear infinite;
}

.build-particles {
  position: absolute;
  inset: 0;
}

.build-particle {
  position: absolute;
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  animation: build-particle-float 2s ease-in-out infinite;
  animation-delay: var(--delay);
}

.build-particle:nth-child(1) { top: 0; left: 50%; }
.build-particle:nth-child(2) { top: 25%; right: 10%; }
.build-particle:nth-child(3) { top: 75%; right: 10%; }
.build-particle:nth-child(4) { bottom: 0; left: 50%; }
.build-particle:nth-child(5) { top: 75%; left: 10%; }
.build-particle:nth-child(6) { top: 25%; left: 10%; }

@keyframes build-pulse {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.1); opacity: 1; }
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}

@keyframes build-particle-float {
  0%, 100% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(1.3); opacity: 0.8; }
}

.build-title {
  color: #6ee7b7 !important;
  font-size: 18px !important;
  font-weight: 600 !important;
  margin-bottom: 10px !important;
}

.build-desc {
  margin-bottom: 24px !important;
}

.build-progress {
  width: 220px;
}

.progress-track {
  position: relative;
  height: 4px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #0d9488);
  border-radius: 2px;
  transition: width 0.15s ease;
}

.progress-glow {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 30px;
  height: 10px;
  background: radial-gradient(ellipse, rgba(16, 185, 129, 0.6) 0%, transparent 70%);
  filter: blur(4px);
}

/* 精简预览容器（无 Mac 窗口栏） */
.browser-frame {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin: 14px 16px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: #fff;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

/* 预览 iframe */
.preview-iframe {
  flex: 1;
  width: 100%;
  border: none;
  background: #fff;
}

/* ========== 代码展示 ========== */
.code-files-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin: 14px 16px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  overflow: hidden;
}

.files-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(255, 255, 255, 0.03);
}

.files-count {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 500;
}

.files-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease, opacity 0.2s ease, transform 0.2s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  transform: translateY(-1px);
}

.action-btn svg {
  width: 15px;
  height: 15px;
}

.files-list {
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 12px 14px;
  scrollbar-width: thin;
  scrollbar-color: rgba(16, 185, 129, 0.3) transparent;
}

.files-list::-webkit-scrollbar {
  width: 5px;
}

.files-list::-webkit-scrollbar-thumb {
  background: rgba(16, 185, 129, 0.3);
  border-radius: 3px;
}

.file-item {
  margin-bottom: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.07);
  overflow: hidden;
  transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, color 0.25s ease, opacity 0.25s ease, transform 0.25s ease;
}

.file-item:hover {
  border-color: rgba(16, 185, 129, 0.3);
  background: rgba(255, 255, 255, 0.045);
}

.file-item.expanded {
  border-color: rgba(16, 185, 129, 0.35);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.08);
}

.file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 15px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.file-header:hover {
  background: rgba(255, 255, 255, 0.02);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}

.expand-icon {
  width: 15px;
  height: 15px;
  color: rgba(255, 255, 255, 0.35);
  transition: transform 0.25s ease;
}

.file-item.expanded .expand-icon {
  transform: rotate(90deg);
}

.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 8px;
  background: rgba(16, 185, 129, 0.14);
  color: #34d399;
}

.file-icon.html {
  background: rgba(251, 146, 60, 0.14);
  color: #fb923c;
}

.file-icon.css {
  background: rgba(59, 130, 246, 0.14);
  color: #60a5fa;
}

.file-icon.javascript,
.file-icon.js {
  background: rgba(250, 204, 21, 0.14);
  color: #facc15;
}

.file-icon svg {
  width: 14px;
  height: 14px;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-lang {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.06);
  padding: 3px 7px;
  border-radius: 4px;
  text-transform: uppercase;
  flex-shrink: 0;
}

.copy-file-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease, opacity 0.2s ease, transform 0.2s ease;
  opacity: 0;
}

.file-header:hover .copy-file-btn {
  opacity: 1;
}

.copy-file-btn:hover {
  background: rgba(16, 185, 129, 0.1);
  color: #6ee7b7;
}

.copy-file-btn svg {
  width: 15px;
  height: 15px;
}

/* 展开/折叠动画（grid-rows 方案） */
.file-content {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.3s ease;
}

.file-content.open {
  grid-template-rows: 1fr;
}

.file-content-inner {
  overflow: hidden;
  min-height: 0;
  background: rgba(0, 0, 0, 0.3);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* 代码视口：行号列 + 代码列 */
.code-viewport {
  display: flex;
  align-items: stretch;
}

.code-gutter {
  flex-shrink: 0;
  min-width: 46px;
  padding: 15px 12px 15px 0;
  text-align: right;
  background: rgba(255, 255, 255, 0.025);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  user-select: none;
}

.code-line-no {
  display: block;
  font-size: 12.5px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.22);
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  padding: 0 14px 0 6px;
}

.code-area {
  flex: 1;
  min-width: 0;
  margin: 0;
  padding: 15px 17px;
  font-size: 12.5px;
  line-height: 1.7;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  overflow-x: auto;
}

.code-area code {
  color: #e2e8f0;
}

/* ========== 交互可访问性 ========== */
/* 键盘焦点可见环 */
.btn-icon:focus-visible,
.send-btn:focus-visible,
.tab:focus-visible,
.action-btn:focus-visible,
.copy-file-btn:focus-visible,
.scroll-bottom-btn:focus-visible,
.clear-btn:focus-visible,
.bubble-copy-btn:focus-visible {
  outline: 2px solid rgba(16, 185, 129, 0.7);
  outline-offset: 2px;
}

/* 触屏：消除双击缩放延迟 */
.btn-icon,
.send-btn,
.tab,
.action-btn,
.copy-file-btn,
.scroll-bottom-btn,
.clear-btn,
.bubble-copy-btn,
.cb-copy-btn {
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .ai-code-page {
    flex-direction: column;
    height: auto;
    min-height: calc(100vh - 64px);
  }

  .left-panel {
    width: 100%;
    min-width: auto;
    height: 60vh;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  .right-panel {
    width: 100%;
    height: 60vh;
  }

  .message-bubble {
    max-width: 85%;
  }
}

@media (max-width: 640px) {
  .panel-header {
    padding: 12px 16px;
  }

  .task-header {
    margin: 10px 12px 0;
  }

  .messages-container {
    padding: 16px;
  }

  .message {
    gap: 10px;
    margin-bottom: 18px;
  }

  .message-avatar {
    width: 34px;
    height: 34px;
  }

  .avatar-icon {
    width: 17px;
    height: 17px;
  }

  .bubble-content {
    padding: 11px 14px;
    font-size: 13px;
  }

  .input-area {
    padding: 14px 16px;
  }

  .input-hint {
    display: none;
  }

  .code-files-container,
  .browser-frame {
    margin: 10px 12px;
  }

  .gen-indicator {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .orb-ring,
  .orb-core,
  .particle,
  .build-orb .orb-ring,
  .orb-inner,
  .build-particle,
  .tool-call:not(.done)::after {
    animation: none !important;
  }

  .message {
    animation: none;
  }
}</style>

<!-- One Dark Pro 风格代码高亮主题（highlight.js，非 scoped 以便作用于 v-html 生成的 class） -->
<style>
/* ---------- 基础 ---------- */
.markdown-body pre code.hljs {
  display: block;
  overflow-x: auto;
  padding: 0;
  background: transparent;
  color: #abb2bf;
}

.hljs {
  color: #abb2bf;
  background: transparent;
}

/* ---------- 注释 / 文档类型 ---------- */
.hljs-comment,
.hljs-quote {
  color: #5c6370;
  font-style: italic;
}

/* ---------- 关键字：标签名 / CSS @规则 / DOCTYPE ---------- */
.hljs-doctag,
.hljs-keyword,
.hljs-formula,
.hljs-tag {
  color: #c678dd;
}

/* ---------- 属性名（HTML 属性 / CSS 属性） ---------- */
.hljs-name,
.hljs-selector-tag,
.hljs-selector-id,
.hljs-selector-class,
.hljs-selector-attr,
.hljs-selector-pseudo {
  color: #e06c75;
}

.hljs-attr,
.hljs-attribute {
  color: #d19a66;
}

/* ---------- 字符串 ---------- */
.hljs-string,
.hljs-regexp,
.hljs-inserted {
  color: #98c379;
}

/* ---------- CSS 专有 ---------- */
.hljs-built_in,
.hljs-builtin-name {
  color: #e6c07b;
}

.hljs-number,
.hljs-literal,
.hljs-symbol {
  color: #d19a66;
}

.hljs-variable,
.hljs-template-variable {
  color: #e06c75;
}

.hljs-title,
.hljs-section,
.hljs-title.class_,
.hljs-title.function_ {
  color: #61afef;
}

.hljs-function .hljs-title {
  color: #61afef;
}

.hljs-params {
  color: #abb2bf;
}

.hljs-meta {
  color: #56b6c2;
}

.hljs-bullet,
.hljs-link {
  color: #d19a66;
}

.hljs-type,
.hljs-class .hljs-title {
  color: #e6c07b;
}

.hljs-emphasis {
  font-style: italic;
}

.hljs-strong {
  font-weight: bold;
}

.hljs-deletion {
  color: #e06c75;
}

.hljs-addition {
  color: #98c379;
}
</style>
