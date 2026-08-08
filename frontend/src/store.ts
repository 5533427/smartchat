import { defineStore } from 'pinia'
import type { UserInfo } from './types'
import { api } from './api'

const TOKEN_KEY = 'smartchat_token'

/** 用户状态：token 持久化在 localStorage，角色用于路由守卫 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as UserInfo | null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN',
  },
  actions: {
    setAuth(token: string, user: UserInfo) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
    },
    async fetchMe() {
      this.user = await api.get<UserInfo>('/auth/me')
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    },
  },
})
