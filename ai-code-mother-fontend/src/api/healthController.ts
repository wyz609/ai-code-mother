// @ts-ignore
/* eslint-disable */
import request from './request'
import type { AxiosResponse } from 'axios'

/** 此处后端没有提供注释 GET /health/ */
export async function healthCheck(options?: { [key: string]: any }): Promise<AxiosResponse<API.BaseResponseString>> {
  return request<API.BaseResponseString>('/health/', {
    method: 'GET',
    ...(options || {}),
  })
}
