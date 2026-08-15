// @ts-ignore
/* eslint-disable */
import request from './request'
import type { AxiosResponse } from 'axios'

export function addApp(body: API.AppAddRequest): Promise<AxiosResponse<API.BaseResponseLong>> {
  return request.post('/app/add', body)
}

export function updateApp(body: API.AppUpdateRequest): Promise<AxiosResponse<API.BaseResponseBoolean>> {
  return request.post('/app/update', body)
}

export function deleteApp(body: API.DeleteRequest): Promise<AxiosResponse<API.BaseResponseBoolean>> {
  return request.post('/app/delete', body)
}

export function getAppById(params: API.getAppByIdParams): Promise<AxiosResponse<API.BaseResponseAppVO>> {
  return request.get('/app/get/vo', { params })
}

export function listMyAppsByPage(body: API.AppQueryRequest): Promise<AxiosResponse<API.BaseResponsePageAppVO>> {
  return request.post('/app/my/list/page/vo', body)
}

export function listFeaturedAppsByPage(body: API.AppQueryRequest): Promise<AxiosResponse<API.BaseResponsePageAppVO>> {
  return request.post('/app/good/list/page/vo', body)
}

export function listAppByPage(body: API.AppQueryRequest): Promise<AxiosResponse<API.BaseResponsePageAppVO>> {
  return request.post('/app/list/page', body)
}

export function deployApp(body: API.AppDeployRequest): Promise<AxiosResponse<API.BaseResponseString>> {
  return request.post('/app/deploy', body)
}

export function downloadAppCode(params: API.downloadAppCodeParams): Promise<AxiosResponse<Blob>> {
  return request.get(`/app/download/${params.appId}`, { responseType: 'blob' })
}

export function getProjectFiles(params: API.getProjectFilesParams): Promise<AxiosResponse<API.BaseResponseListProjectFileVO>> {
  return request.get(`/app/project/files/${params.appId}`)
}

export function createPreviewToken(params: API.createPreviewTokenParams): Promise<AxiosResponse<API.BaseResponseString>> {
  return request.get(`/app/preview/token/${params.appId}`)
}

export function chatToGenCode(params: API.chatToGenCodeParams, body: string): Promise<void> {
  return request.post(`/app/chat?appId=${params.appId}&message=${encodeURIComponent(params.message)}`, body)
}

export function listAppChatHistory(params: API.listAppChatHistoryParams): Promise<AxiosResponse<API.BaseResponsePageChatHistory>> {
  return request.get('/app/chat/history', { params })
}
