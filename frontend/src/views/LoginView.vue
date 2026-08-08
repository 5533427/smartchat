<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="36" color="#409eff"><ChatDotRound /></el-icon>
        <h1>SmartChat</h1>
        <p>AI 智能对话平台 · 多模型接入 · 流式输出</p>
      </div>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" clearable />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                @keyup.enter="submit"
              />
            </el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" label-position="top" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" placeholder="3-20 位字母/数字/下划线" size="large" clearable />
            </el-form-item>
            <el-form-item label="昵称（可选）">
              <el-input v-model="registerForm.nickname" placeholder="展示名称" size="large" clearable />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="至少 6 位"
                size="large"
                show-password
              />
            </el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-tip">默认管理员：admin / admin123</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store'
import type { UserInfo } from '../types'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '' })

async function submit() {
  loading.value = true
  try {
    if (mode.value === 'login') {
      if (!loginForm.username || !loginForm.password) {
        ElMessage.warning('请输入用户名和密码')
        return
      }
      const data = await api.post<{ token: string; user: UserInfo }>('/auth/login', loginForm)
      store.setAuth(data.token, data.user)
    } else {
      if (registerForm.username.length < 3 || registerForm.password.length < 6) {
        ElMessage.warning('用户名至少 3 位，密码至少 6 位')
        return
      }
      const data = await api.post<{ token: string; user: UserInfo }>('/auth/register', registerForm)
      store.setAuth(data.token, data.user)
    }
    ElMessage.success('欢迎回来！')
    router.replace((route.query.redirect as string) || '/')
  } catch {
    /* 错误提示已由拦截器统一处理 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f1ff 0%, #f5f7fa 50%, #eef7f2 100%);
}

.login-card {
  width: 420px;
  padding: 40px 36px 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-header h1 {
  margin: 8px 0 4px;
  font-size: 26px;
  letter-spacing: 1px;
}

.login-header p {
  margin: 0;
  color: var(--sc-text-secondary);
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.login-tip {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: var(--sc-text-secondary);
}
</style>
