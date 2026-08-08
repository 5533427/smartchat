<template>
  <el-drawer v-model="visible" title="设置" size="420px" :destroy-on-close="true">
    <el-tabs v-model="tab">
      <!-- AI 配置 -->
      <el-tab-pane label="AI 配置" name="ai">
        <el-alert
          type="info"
          :closable="false"
          title="配置你的 API Key；不配置则使用系统环境变量（AI_API_KEY）"
          class="tab-alert"
        />
        <el-form label-position="top" size="default">
          <el-form-item label="模型厂商">
            <el-select v-model="config.provider" style="width: 100%">
              <el-option label="OpenAI 兼容协议（DeepSeek / OpenAI / Kimi / 通义千问）" value="openai" />
              <el-option label="Anthropic Claude" value="anthropic" />
            </el-select>
          </el-form-item>
          <el-form-item label="接口地址">
            <el-input
              v-model="config.baseUrl"
              placeholder="如 https://api.deepseek.com 或 https://api.anthropic.com"
            />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input
              v-model="config.apiKey"
              type="password"
              show-password
              :placeholder="config.apiKeyMasked || 'sk-...'"
            />
          </el-form-item>
          <el-form-item label="模型名称">
            <el-select v-model="config.model" allow-create filterable style="width: 100%">
              <el-option-group
                v-if="config.provider === 'anthropic'"
                label="Claude"
              >
                <el-option label="claude-opus-4-8（最强）" value="claude-opus-4-8" />
                <el-option label="claude-sonnet-4-6（均衡）" value="claude-sonnet-4-6" />
                <el-option label="claude-haiku-4-5（最快最省）" value="claude-haiku-4-5" />
              </el-option-group>
              <el-option-group v-else label="OpenAI 兼容">
                <el-option label="deepseek-chat（DeepSeek-V3）" value="deepseek-chat" />
                <el-option label="deepseek-reasoner（DeepSeek-R1）" value="deepseek-reasoner" />
                <el-option label="gpt-4o-mini（OpenAI）" value="gpt-4o-mini" />
                <el-option label="moonshot-v1-8k（Kimi）" value="moonshot-v1-8k" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-button type="primary" :loading="savingAi" @click="saveAiConfig">保存配置</el-button>
        </el-form>
      </el-tab-pane>

      <!-- 个人资料 -->
      <el-tab-pane label="个人资料" name="profile">
        <el-form label-position="top">
          <el-form-item label="用户名">
            <el-input :model-value="user?.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="nickname" />
          </el-form-item>
          <el-button type="primary" :loading="savingNick" @click="saveNickname">保存昵称</el-button>

          <el-divider>修改密码</el-divider>
          <el-form-item label="原密码">
            <el-input v-model="pwd.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" />
          </el-form-item>
          <el-button type="warning" :loading="savingPwd" @click="savePassword">修改密码</el-button>
        </el-form>
      </el-tab-pane>

      <!-- 我的统计 -->
      <el-tab-pane label="我的统计" name="stats">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="会话数">{{ stats.totalConversations }}</el-descriptions-item>
          <el-descriptions-item label="消息数">{{ stats.totalMessages }}</el-descriptions-item>
          <el-descriptions-item label="今日消息">{{ stats.todayMessages }}</el-descriptions-item>
          <el-descriptions-item label="Token 消耗（约）">
            {{ stats.totalTokens.toLocaleString() }}
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store'
import type { ApiConfigInfo, MineStats } from '../types'

const visible = defineModel<boolean>('visible', { default: false })
const userStore = useUserStore()
const user = computed(() => userStore.user)

const tab = ref('ai')
const config = reactive<{ provider: string; baseUrl: string; apiKey: string; model: string; apiKeyMasked: string | null }>({
  provider: 'openai',
  baseUrl: '',
  apiKey: '',
  model: '',
  apiKeyMasked: null,
})
const savingAi = ref(false)

const nickname = ref('')
const savingNick = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '' })
const savingPwd = ref(false)

const stats = ref<MineStats>({ totalConversations: 0, totalMessages: 0, totalTokens: 0, todayMessages: 0 })

watch(visible, async (v) => {
  if (!v) return
  const [cfg, mine] = await Promise.all([
    api.get<ApiConfigInfo>('/user/api-config'),
    api.get<MineStats>('/stats/me'),
  ])
  Object.assign(config, cfg, { apiKey: '' })
  nickname.value = userStore.user?.nickname || ''
  stats.value = mine
})

async function saveAiConfig() {
  if (!config.baseUrl.trim() || !config.model.trim()) {
    ElMessage.warning('请填写接口地址和模型名称')
    return
  }
  if (!config.apiKey && !config.apiKeyMasked) {
    ElMessage.warning('请填写 API Key')
    return
  }
  savingAi.value = true
  try {
    const saved = await api.put<ApiConfigInfo>('/user/api-config', {
      provider: config.provider,
      baseUrl: config.baseUrl,
      apiKey: config.apiKey,
      model: config.model,
    })
    Object.assign(config, saved, { apiKey: '' })
    ElMessage.success('AI 配置已保存')
  } finally {
    savingAi.value = false
  }
}

async function saveNickname() {
  savingNick.value = true
  try {
    await api.put('/user/profile', { nickname: nickname.value })
    userStore.user!.nickname = nickname.value
    ElMessage.success('昵称已更新')
  } finally {
    savingNick.value = false
  }
}

async function savePassword() {
  if (pwd.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  savingPwd.value = true
  try {
    await api.put('/user/password', pwd)
    ElMessage.success('密码已修改，下次登录生效')
    pwd.oldPassword = ''
    pwd.newPassword = ''
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped>
.tab-alert {
  margin-bottom: 16px;
}
</style>
