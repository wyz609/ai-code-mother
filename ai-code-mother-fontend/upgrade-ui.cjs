const fs = require('fs');
const path = 'src/pages/app/AppChatPage.vue';
let code = fs.readFileSync(path, 'utf-8');

// 1. Accessibility Fixes (web-design-guidelines)
code = code.replace('<button class="btn-icon clear-btn"', '<button class="btn-icon clear-btn" aria-label="清空对话"');
code = code.replace('<button\n          v-if="showScrollTopBtn"\n          class="scroll-bottom-btn"', '<button\n          v-if="showScrollTopBtn"\n          class="scroll-bottom-btn" aria-label="回到底部"');
code = code.replace('<button\n            class="send-btn"', '<button\n            class="send-btn" aria-label="发送消息"');
code = code.replace('<button class="btn-icon" @click="refreshPreview"', '<button class="btn-icon" @click="refreshPreview" aria-label="刷新预览"');

// Markdown copy buttons are generated in JS
code = code.replace(/<button type="button" class="cb-copy-btn"/g, '<button type="button" class="cb-copy-btn" aria-label="复制代码"');

// 2. Global Transition Fix (web-design-guidelines: no transition: all)
code = code.replace(/transition:\s*all\s+(.*?);/g, 'transition: background-color $1, border-color $1, color $1, transform $1, box-shadow $1, opacity $1;');

// 3. Visual Overhaul (frontend-design: precise minimal palette, remove generic purple defaults)
// User Bubble
code = code.replace('background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);', 'background: #27272a;\n  border: 1px solid #3f3f46;');
code = code.replace('box-shadow: 0 6px 20px rgba(59, 130, 246, 0.25);', 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);');

// Assistant Bubble
code = code.replace('background: rgba(255, 255, 255, 0.05);', 'background: transparent;');
code = code.replace('border: 1px solid rgba(255, 255, 255, 0.09);', 'border: 1px solid transparent;');
code = code.replace('inset 0 1px 0 rgba(255, 255, 255, 0.06),\n    0 4px 16px rgba(0, 0, 0, 0.15)', 'none');

// Assistant Avatar
code = code.replace('background: linear-gradient(135deg, #6366f1, #a855f1);', 'background: #18181b;\n  color: #e4e4e7;');
code = code.replace('0 0 10px rgba(139, 92, 246, 0.6),\n    0 0 22px rgba(99, 102, 241, 0.4),\n    0 4px 16px rgba(168, 85, 241, 0.35)', '0 4px 12px rgba(0, 0, 0, 0.3)');

// Task Header Icon
code = code.replace('background: linear-gradient(135deg, #4c5bc7 0%, #6d4bc7 55%, #7b47c7 100%);', 'background: #18181b;');
code = code.replace('border: 1px solid rgba(124, 92, 255, 0.35);', 'border: 1px solid rgba(255, 255, 255, 0.12);');
code = code.replace('inset 0 1px 0 rgba(255, 255, 255, 0.12),\n    0 4px 12px rgba(80, 70, 190, 0.28)', 'inset 0 1px 0 rgba(255, 255, 255, 0.05),\n    0 4px 12px rgba(0, 0, 0, 0.2)');
code = code.replace('color: #d9d4ff;', 'color: #e4e4e7;');
code = code.replace('filter: drop-shadow(0 0 6px rgba(168, 150, 255, 0.65));', 'filter: drop-shadow(0 0 4px rgba(255, 255, 255, 0.15));');

// Gen indicator
code = code.replace('color: #a78bfa;', 'color: #a1a1aa;');
code = code.replace('background: rgba(139, 92, 246, 0.15);', 'background: rgba(255, 255, 255, 0.06);');
code = code.replace('border: 1px solid rgba(139, 92, 246, 0.3);', 'border: 1px solid rgba(255, 255, 255, 0.12);');
code = code.replace('background: #a78bfa;', 'background: #e4e4e7;');
code = code.replace('box-shadow: 0 0 8px rgba(167, 139, 250, 0.9);', 'box-shadow: 0 0 6px rgba(255, 255, 255, 0.6);');

// Input Area
code = code.replace('border-color: rgba(139, 92, 246, 0.55);', 'border-color: rgba(255, 255, 255, 0.25);');
code = code.replace('box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.12), 0 8px 24px rgba(139, 92, 246, 0.08);', 'box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.1), 0 8px 24px rgba(0, 0, 0, 0.2);');
code = code.replace('background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), transparent);', 'background: linear-gradient(135deg, rgba(255, 255, 255, 0.03), transparent);');

// Send Button Active
code = code.replace('.send-btn.active {\n  background: linear-gradient(135deg, #8b5cf6, #7c3aed);\n  color: #fff;', '.send-btn.active {\n  background: #e4e4e7;\n  color: #09090b;');
code = code.replace('box-shadow: 0 4px 16px rgba(139, 92, 246, 0.4);', 'box-shadow: 0 4px 12px rgba(255, 255, 255, 0.15);');

// Markdown Styles
code = code.replace('color: #818cf8;', 'color: #38bdf8;'); // links
code = code.replace('border-left: 3px solid #8b5cf6;', 'border-left: 3px solid #52525b;'); // blockquote border
code = code.replace('background: rgba(139, 92, 246, 0.09);', 'background: rgba(255, 255, 255, 0.04);'); // blockquote bg
code = code.replace('background: rgba(139, 92, 246, 0.16);', 'background: rgba(255, 255, 255, 0.1);'); // inline code bg
code = code.replace('color: #c4b5fd;', 'color: #e4e4e7;'); // inline code text

// Active Tab
code = code.replace('background: linear-gradient(135deg, rgba(139, 92, 246, 0.25), rgba(99, 102, 241, 0.25));', 'background: rgba(255, 255, 255, 0.1);');
code = code.replace('color: #c4b5fd;', 'color: #fff;');
code = code.replace('box-shadow: 0 2px 8px rgba(139, 92, 246, 0.15);', 'box-shadow: none;');

// Scrollbars and Borders
code = code.replace(/rgba\(139,\s*92,\s*246,\s*0\.35\)/g, 'rgba(255, 255, 255, 0.15)');
code = code.replace(/rgba\(139,\s*92,\s*246,\s*0\.3\)/g, 'rgba(255, 255, 255, 0.12)');
code = code.replace('box-shadow: 0 4px 16px rgba(139, 92, 246, 0.08);', 'box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);');

// Build Progress / Animations
code = code.replace('background: linear-gradient(135deg, #8b5cf6, #6366f1);', 'background: #e4e4e7;');
code = code.replace('border-top-color: #8b5cf6;', 'border-top-color: #e4e4e7;');
code = code.replace('background: #8b5cf6;', 'background: #e4e4e7;');
code = code.replace('background: linear-gradient(90deg, #8b5cf6, #6366f1);', 'background: #e4e4e7;');
code = code.replace('rgba(139, 92, 246, 0.6)', 'rgba(255, 255, 255, 0.3)');

// Empty State illustration (remove purple core)
code = code.replace('box-shadow: 0 0 30px rgba(139, 92, 246, 0.45);', 'box-shadow: 0 0 20px rgba(255, 255, 255, 0.15);');
code = code.replace('box-shadow: 0 0 30px rgba(139, 92, 246, 0.4);', 'box-shadow: 0 0 20px rgba(255, 255, 255, 0.15);');

// Add Focus Visible rule at the end if not exists
if (!code.includes('focus-visible')) {
    code += `\n<style>\n/* Accessibility: Focus visible */\nbutton:focus-visible, textarea:focus-visible, input:focus-visible, a:focus-visible {\n  outline: 2px solid #e4e4e7;\n  outline-offset: 2px;\n}\n</style>\n`;
}

fs.writeFileSync(path, code, 'utf-8');
console.log('UI Overhaul completed.');