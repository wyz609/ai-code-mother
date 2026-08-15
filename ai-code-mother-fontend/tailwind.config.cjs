/** @type {import('tailwindcss').Config} */
module.exports = {
  // 关闭 preflight，避免重置样式破坏 antd 与现有组件
  corePlugins: {
    preflight: false,
  },
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
}
