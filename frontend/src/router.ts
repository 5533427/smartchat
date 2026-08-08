import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from './store'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('./views/LoginView.vue'), meta: { public: true } },
    { path: '/', component: () => import('./views/ChatView.vue') },
    { path: '/admin', component: () => import('./views/AdminView.vue'), meta: { admin: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// 路由守卫：未登录跳登录页；管理员页面校验角色
router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.meta.public) {
    return true
  }
  if (!store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!store.user) {
    try {
      await store.fetchMe()
    } catch {
      store.logout()
      return { path: '/login' }
    }
  }
  if (to.meta.admin && !store.isAdmin) {
    ElMessage.warning('仅管理员可访问管理后台')
    return { path: '/' }
  }
  return true
})

export default router
