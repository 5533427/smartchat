<template>
  <div class="chat-page">
    <!-- 左侧会话栏 -->
    <ConversationSidebar
      :conversations="conversations"
      :active-id="activeId"
      @select="selectConversation"
      @new="newConversation"
      @refresh="loadConversations"
      @open-templates="templateDrawer = true"
      @open-settings="settingsDrawer = true"
    />

    <!-- 右侧聊天区 -->
    <main class="chat-main">
      <!-- 头部 -->
      <header class="chat-header">
        <div class="chat-title">
          {{ activeConversation?.title || 'SmartChat' }}
          <span v-if="activeConversation" class="chat-sub">
            {{ activeConversation.messageCount }} 条消息
          </span>
        </div>
      </header>

      <!-- 消息列表 -->
      <MessageList
        :messages="messages"
        :stream-text="streamText"
        @regenerate="regenerate"
        @remove="removeMessage"
      />

      <!-- 输入区 -->
      <ChatInput
        :streaming="streaming"
        :active-template="activeTemplate"
        @send="sendMessage"
        @stop="stopGeneration"
        @open-templates="templateDrawer = true"
        @clear-template="activeTemplate = null"
      />
    </main>

    <!-- 抽屉 -->
    <TemplateDrawer v-model:visible="templateDrawer" @use="(t) => (activeTemplate = t)" />
    <SettingsDrawer v-model:visible="settingsDrawer" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ConversationSidebar from '../components/ConversationSidebar.vue'
import MessageList from '../components/MessageList.vue'
import ChatInput from '../components/ChatInput.vue'
import TemplateDrawer from '../components/TemplateDrawer.vue'
import SettingsDrawer from '../components/SettingsDrawer.vue'
import { api, streamSse } from '../api'
import type { ConversationItem, MessageInfo, SseEvent, TemplateInfo } from '../types'

// ---------- 状态 ----------
const conversations = ref<ConversationItem[]>([])
const activeId = ref<number | null>(null)
const messages = ref<MessageInfo[]>([])
/** 流式输出中的半成品文本；null = 没有进行中的生成 */
const streamText = ref<string | null>(null)
const streaming = ref(false)
const activeTemplate = ref<TemplateInfo | null>(null)
const templateDrawer = ref(false)
const settingsDrawer = ref(false)
let abortController: AbortController | null = null

const activeConversation = computed(() =>
  conversations.value.find((c) => c.id === activeId.value),
)

// ---------- 会话管理 ----------
async function loadConversations() {
  conversations.value = await api.get<ConversationItem[]>('/conversations')
}

async function selectConversation(id: number) {
  if (streaming.value) {
    ElMessage.warning('请先停止当前生成')
    return
  }
  activeId.value = id
  messages.value = await api.get<MessageInfo[]>(`/conversations/${id}/messages`)
  streamText.value = null
}

async function newConversation() {
  if (streaming.value) {
    ElMessage.warning('请先停止当前生成')
    return
  }
  const conv = await api.post<ConversationItem>('/conversations', {})
  activeId.value = conv.id
  messages.value = []
  streamText.value = null
  await loadConversations()
}

// ---------- 消息 ----------
async function sendMessage(content: string, action?: string) {
  if (streaming.value) return

  // 没有会话时先创建一个
  let convId = activeId.value
  if (!convId) {
    const conv = await api.post<ConversationItem>('/conversations', {})
    convId = conv.id
    activeId.value = conv.id
  }

  // 乐观渲染用户消息（-1 = 临时 id，start 事件后替换为真实 id）
  messages.value.push({
    id: -1,
    role: 'user',
    content,
    tokens: 0,
    meta: null,
    createdAt: new Date().toISOString(),
  })
  streaming.value = true
  streamText.value = ''

  abortController = await streamSse(
    `/api/conversations/${convId}/messages`,
    { content, action: action || null, templateId: activeTemplate.value?.id ?? null },
    {
      onEvent: handleSse(convId),
      onError: (msg) => {
        ElMessage.error(msg)
        streaming.value = false
        streamText.value = null
        reloadMessages(convId)
      },
    },
  )
}

function handleSse(convId: number) {
  return (event: SseEvent) => {
    switch (event.type) {
      case 'start':
        // 用户消息已落库，替换乐观渲染的 id
        const last = messages.value[messages.value.length - 1]
        if (last && last.id === -1 && event.userMessageId) {
          last.id = event.userMessageId
        }
        break
      case 'delta':
        streamText.value = (streamText.value || '') + event.content
        break
      case 'done':
        messages.value.push({
          id: event.messageId,
          role: 'assistant',
          content: streamText.value || '',
          tokens: event.totalTokens,
          meta: null,
          createdAt: new Date().toISOString(),
        })
        streamText.value = null
        streaming.value = false
        loadConversations()
        break
      case 'error':
        ElMessage.error(event.message)
        streaming.value = false
        streamText.value = null
        reloadMessages(convId)
        break
    }
  }
}

/** 停止生成：中断连接，服务端会取消上游请求并丢弃半截回复 */
function stopGeneration() {
  abortController?.abort()
  streaming.value = false
  streamText.value = null
  if (activeId.value) {
    reloadMessages(activeId.value)
  }
}

/** 重新生成：移除最后一条 AI 回复，重新流式请求 */
async function regenerate(messageId: number) {
  if (streaming.value || !activeId.value) return
  messages.value = messages.value.filter((m) => m.id !== messageId)
  streaming.value = true
  streamText.value = ''

  abortController = await streamSse(
    `/api/conversations/${activeId.value}/regenerate`,
    {},
    {
      onEvent: handleSse(activeId.value),
      onError: (msg) => {
        ElMessage.error(msg)
        streaming.value = false
        streamText.value = null
        reloadMessages(activeId.value!)
      },
    },
  )
}

async function removeMessage(messageId: number) {
  if (!activeId.value) return
  await api.delete(`/conversations/${activeId.value}/messages/${messageId}`)
  messages.value = messages.value.filter((m) => m.id !== messageId)
}

async function reloadMessages(convId: number) {
  messages.value = await api.get<MessageInfo[]>(`/conversations/${convId}/messages`)
}

// ---------- 初始化 ----------
onMounted(async () => {
  await loadConversations()
  if (conversations.value.length > 0) {
    await selectConversation(conversations.value[0].id)
  }
})
</script>

<style scoped>
.chat-page {
  height: 100%;
  display: flex;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid var(--sc-border);
  background: #fff;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
}

.chat-sub {
  font-size: 12px;
  font-weight: 400;
  color: var(--sc-text-secondary);
  margin-left: 10px;
}
</style>
