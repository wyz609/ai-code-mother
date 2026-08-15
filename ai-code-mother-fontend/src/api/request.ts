import axios, { type AxiosResponse } from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器 - 返回完整的 AxiosResponse 以便访问 .data
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        // 未登录，跳转到登录页
        window.location.href = '/user/login'
      } else if (status === 403) {
        // 无权限
        console.error('无权限访问')
      } else if (status === 500) {
        console.error('服务器错误')
      }
    }
    return Promise.reject(error)
  },
)

export default request
