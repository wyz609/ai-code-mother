// @ts-ignore
/* eslint-disable */
import request from './request'

export function serveStaticResource(params: API.serveStaticResourceParams): Promise<Blob> {
  return request.get(`/static/${params.deployKey}`, { responseType: 'blob' })
}
