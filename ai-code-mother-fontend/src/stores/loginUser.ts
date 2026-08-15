import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userLogout, getLoginUser } from '@/api/userController'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>({})

  // 获取登录用户信息
  const fetchLoginUser = async () => {
    try {
      const res = await getLoginUser()
      if (res.data.code === 0 && res.data.data) {
        loginUser.value = res.data.data
      } else {
        loginUser.value = {}
      }
    } catch (error) {
      console.error('获取登录用户信息失败', error)
      loginUser.value = {}
    }
  }

  // 设置登录用户信息
  const setLoginUser = (user: API.LoginUserVO) => {
    loginUser.value = user
  }

  // 退出登录
  const logout = async () => {
    try {
      const res = await userLogout()
      if (res.data.code === 0) {
        loginUser.value = {}
        return true
      }
      return false
    } catch (error) {
      console.error('退出登录失败', error)
      return false
    }
  }

  return {
    loginUser,
    fetchLoginUser,
    setLoginUser,
    logout,
  }
})
