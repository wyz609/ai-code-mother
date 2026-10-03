// 环境配置

// API 基础地址
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 部署 URL（与后端 StaticResourceController 的 /api/static/{deployKey}/ 保持一致）
export const getDeployUrl = (deployKey: string) => {
  return `${window.location.origin}/api/static/${deployKey}/`
}
