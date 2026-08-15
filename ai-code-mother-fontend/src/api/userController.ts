// @ts-ignore
/* eslint-disable */
import request from './request'
import type { AxiosResponse } from 'axios'

export function userLogin(body: API.UserLoginRequest): Promise<AxiosResponse<API.BaseResponseLoginUserVO>> {
  return request.post('/user/login', body)
}

export function userRegister(body: API.UserRegisterRequest): Promise<AxiosResponse<API.BaseResponseLong>> {
  return request.post('/user/register', body)
}

export function userLogout(): Promise<AxiosResponse<API.BaseResponseBoolean>> {
  return request.post('/user/logout')
}

export function getLoginUser(): Promise<AxiosResponse<API.BaseResponseLoginUserVO>> {
  return request.get('/user/get/login')
}

export function getUserById(params: API.getUserByIdParams): Promise<AxiosResponse<API.BaseResponseUserVO>> {
  return request.get('/user/get', { params })
}

export function getUserVOById(params: API.getUserVOByIdParams): Promise<AxiosResponse<API.BaseResponseUserVO>> {
  return request.get('/user/get/vo', { params })
}

export function listUserByPage(body: API.UserQueryRequest): Promise<AxiosResponse<API.BaseResponsePageUserVO>> {
  return request.post('/user/list/page', body)
}

export function updateUser(body: API.UserUpdateRequest): Promise<AxiosResponse<API.BaseResponseBoolean>> {
  return request.post('/user/update', body)
}

export function deleteUser(body: API.DeleteRequest): Promise<AxiosResponse<API.BaseResponseBoolean>> {
  return request.post('/user/delete', body)
}

export function addUser(body: API.UserAddRequest): Promise<AxiosResponse<API.BaseResponseLong>> {
  return request.post('/user/add', body)
}
