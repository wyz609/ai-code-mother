import router from '@/router'
import { useLoginUserStore } from '@/stores/loginUser'

// 不需要登录的页面
const WHITE_LIST = ['/user/login', '/user/register', '/']

// 权限检查
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()

  // 如果没有用户信息，尝试获取
  if (!loginUserStore.loginUser.id) {
    await loginUserStore.fetchLoginUser()
  }

  const isLoggedIn = !!loginUserStore.loginUser.id

  // 在白名单中，直接放行
  if (WHITE_LIST.includes(to.path)) {
    next()
    return
  }

  // 需要登录的页面
  if (!isLoggedIn) {
    next('/user/login')
    return
  }

  // 管理员权限检查
  if (to.path.startsWith('/admin')) {
    if (loginUserStore.loginUser.userRole !== 'admin') {
      next('/')
      return
    }
  }

  next()
})
