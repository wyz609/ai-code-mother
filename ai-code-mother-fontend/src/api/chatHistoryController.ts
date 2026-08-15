import request from './request'
import type { AxiosResponse } from 'axios'

export function listAppChatHistory(params: API.listAppChatHistoryParams): Promise<AxiosResponse<API.BaseResponsePageChatHistory>> {
  return request.get(`/chatHistory/app/${params.appId}`, {
    params: {
      pageSize: params.pageSize,
      lastCreateTime: params.lastCreateTime,
    },
  })
}

export function listAllChatHistoryByPageForAdmin(body: API.ChatHistoryQueryRequest): Promise<AxiosResponse<API.BaseResponsePageChatHistory>> {
  return request.post('/chatHistory/admin/list/page/vo', body)
}
