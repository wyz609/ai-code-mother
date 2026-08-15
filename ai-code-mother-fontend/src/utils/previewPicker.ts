// 预览元素拾取与拖拽序列化（供对话页 iframe 注入与新窗口拾取页共用）

export interface PickPayload {
  elementId: string
  tagName: string
  classList: string[]
  outerHTML: string
  xpath?: string
}

// 注入到预览文档内的拾取脚本：悬停高亮 + 手动拖拽（postMessage 上报，不依赖原生 DnD）
// 脚本常驻于预览文档，由父窗口通过 pick-set-enabled 消息启停：
// 禁用时零副作用（不高亮、不锁选择、不拦截拖拽），因此 iframe 无需随拾取模式重建，避免切换闪烁。
export const PICKER_SCRIPT = `(function () {
  var HOVER_CLS = 'ai-pick-hover'
  var enabled = true
  var cur = null
  var dragStartEl = null
  var downX = 0
  var downY = 0
  var dragging = false
  var style = document.createElement('style')
  style.textContent = '.' + HOVER_CLS + '{outline:2px dashed #10b981 !important;outline-offset:2px !important;background:rgba(16,185,129,0.08) !important;cursor:grab !important;}'
  document.head.appendChild(style)

  // ===== 彻底禁用原生文本选择 =====
  // user-select 不是继承属性，仅设置 body 无法阻止子元素被选中。
  // 因此在拖拽按下瞬间注入全局样式 *{user-select:none!important}，
  // 让浏览器在 mousedown 默认行为（开始选择）执行前就判定不可选中，
  // 从根本上杜绝选区产生、避免选中区域被刷白。
  var SEL_LOCK_ID = 'ai-pick-sel-lock'
  function injectSelLock() {
    if (document.getElementById(SEL_LOCK_ID)) return
    var s = document.createElement('style')
    s.id = SEL_LOCK_ID
    s.textContent = '*{user-select:none !important;-webkit-user-select:none !important;-moz-user-select:none !important;-ms-user-select:none !important;}'
    ;(document.head || document.documentElement).appendChild(s)
  }
  function removeSelLock() {
    var s = document.getElementById(SEL_LOCK_ID)
    if (s && s.parentNode) s.parentNode.removeChild(s)
  }
  // 兜底：只要拖拽锁定中，任何已产生的选区一律清除
  function clearAnySelection() {
    try {
      var sel = window.getSelection()
      if (sel && sel.removeAllRanges) sel.removeAllRanges()
    } catch (err) {}
  }
  var selLockActive = false
  function lockSelection() {
    selLockActive = true
    injectSelLock()
    injectDragLock()
    clearAnySelection()
  }
  function unlockSelection() {
    selLockActive = false
    removeSelLock()
    removeDragLock()
    clearAnySelection()
  }
  document.addEventListener('selectionchange', function () {
    if (selLockActive) clearAnySelection()
  })

  // ===== 阻止原生 HTML 拖拽（图片/链接默认 draggable） =====
  // 原生拖拽会生成半透明镜像，在彩色背景上呈现“发白”，与文本选择刷白无关。
  // 拾取模式下一律禁用 draggable 并拦截 dragstart，保证统一走自定义拖拽。
  var DRAG_LOCK_ID = 'ai-pick-drag-lock'
  function injectDragLock() {
    if (document.getElementById(DRAG_LOCK_ID)) return
    var s = document.createElement('style')
    s.id = DRAG_LOCK_ID
    s.textContent = '*{-webkit-user-drag:none !important;user-drag:none !important;}'
    ;(document.head || document.documentElement).appendChild(s)
  }
  function removeDragLock() {
    var s = document.getElementById(DRAG_LOCK_ID)
    if (s && s.parentNode) s.parentNode.removeChild(s)
  }
  // 拾取模式下始终禁用原生拖拽（图片/链接默认 draggable），
  // 避免产生半透明拖拽镜像导致彩色背景“发白”。
  document.addEventListener('dragstart', function (e) {
    e.preventDefault()
    e.stopPropagation()
  }, true)
  document.addEventListener('dragover', function (e) {
    e.preventDefault()
  }, true)

  function closestPickable(el) {
    return el && el.closest
      ? el.closest('button,a,p,img,div,span,section,header,footer,nav,ul,ol,li,h1,h2,h3,h4,h5,h6,form,input,textarea,table,tr,td')
      : null
  }
  function clearHover() {
    if (cur) {
      cur.classList.remove(HOVER_CLS)
      cur = null
    }
  }
  function getXPath(el) {
    var parts = []
    while (el && el.nodeType === 1) {
      var idx = 1
      var sib = el.previousElementSibling
      while (sib) { if (sib.tagName === el.tagName) idx++; sib = sib.previousElementSibling }
      parts.unshift(el.tagName.toLowerCase() + '[' + idx + ']')
      el = el.parentElement
      if (el && (el.id === 'app' || el.tagName === 'BODY')) break
    }
    return parts.join('/')
  }
  function send(msg) {
    try { parent.postMessage(msg, '*') } catch (err) {}
  }

  // 悬停高亮
  document.addEventListener('mousemove', function (e) {
    if (!enabled || dragging) return
    var t = closestPickable(e.target)
    if (t !== cur) { clearHover(); cur = t }
    if (cur) cur.classList.add(HOVER_CLS)
  }, true)

  // 按下记录起点，并在按下瞬间锁定文本选择（阻止浏览器开始创建选区）
  document.addEventListener('mousedown', function (e) {
    if (!enabled) return
    var t = closestPickable(e.target)
    if (!t) return
    downX = e.clientX
    downY = e.clientY
    dragStartEl = t
    lockSelection()
  }, true)

  // 位移超过阈值 → 进入拖拽，上报 payload 给父窗口
  document.addEventListener('mousemove', function (e) {
    if (!enabled || !dragStartEl || dragging) return
    if (Math.abs(e.clientX - downX) + Math.abs(e.clientY - downY) < 6) return
    dragging = true
    clearHover()
    if (e.preventDefault) e.preventDefault()
    var el = dragStartEl
    var payload = {
      elementId: 'ref-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 7),
      tagName: el.tagName,
      classList: Array.prototype.slice.call(el.classList || []),
      outerHTML: el.outerHTML.slice(0, 3000),
      xpath: getXPath(el)
    }
    send({ type: 'pick-drag-start', payload: payload })
  }, true)

  // 在 iframe 内松开：通知父窗口结束并解锁文本选择
  document.addEventListener('mouseup', function () {
    if (!enabled) return
    if (dragging) send({ type: 'pick-drag-end' })
    dragging = false
    dragStartEl = null
    unlockSelection()
  }, true)

  // 父窗口通知：启用/禁用拾取、复位状态
  window.addEventListener('message', function (e) {
    var d = e.data
    if (d && d.type === 'pick-set-enabled') {
      enabled = !!d.enabled
      if (!enabled) {
        dragging = false
        dragStartEl = null
        clearHover()
        unlockSelection()
      }
    }
    if (d && d.type === 'pick-end') {
      dragging = false
      dragStartEl = null
      clearHover()
      unlockSelection()
    }
  })

  document.addEventListener('mouseleave', function () {
    if (!enabled) return
    if (!dragging) {
      clearHover()
      // 鼠标离开 iframe：解锁文本选择（若拖拽跨出 iframe，将由父窗口 pick-end 解锁）
      unlockSelection()
    }
  }, true)
})();`

// 在预览 HTML 中注入 base 定位 + 拾取脚本
export const buildPreviewDoc = (html: string, baseHref: string): string => {
  const base = `<base href="${baseHref}">`
  const script = `<script>${PICKER_SCRIPT}<\/script>`
  const head = base + script
  if (html.includes('</head>')) return html.replace('</head>', `${head}</head>`)
  if (html.includes('<head>')) return html.replace('<head>', `<head>${head}`)
  return `${head}${html}`
}
