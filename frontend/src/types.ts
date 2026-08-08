// 与后端共享的接口类型（后端: backend/src/main/java/com/smartchat/dto/）

/** 统一响应包装 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  role: 'USER' | 'ADMIN'
  enabled: boolean
  createdAt: string
}

export interface ConversationItem {
  id: number
  title: string
  updatedAt: string
  messageCount: number
  lastMessage: string | null
}

export interface MessageInfo {
  id: number
  role: 'user' | 'assistant'
  content: string
  tokens: number
  meta: string | null
  createdAt: string
}

export interface TemplateInfo {
  id: number
  name: string
  description: string | null
  systemPrompt: string
  system: boolean
  userId: number | null
  createdAt: string
}

export interface TemplateGroup {
  system: TemplateInfo[]
  mine: TemplateInfo[]
}

export interface ApiConfigInfo {
  provider: 'openai' | 'anthropic'
  baseUrl: string
  apiKeyMasked: string | null
  model: string
}

export interface MineStats {
  totalConversations: number
  totalMessages: number
  totalTokens: number
  todayMessages: number
}

export interface OverviewStats {
  totalUsers: number
  totalConversations: number
  totalMessages: number
  todayMessages: number
  todayTokens: number
  totalTokens: number
}

export interface TrendPoint {
  date: string
  messages: number
  tokens: number
}

export interface AdminUserItem {
  id: number
  username: string
  nickname: string
  role: string
  enabled: boolean
  createdAt: string
  conversations: number
  messages: number
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

/** SSE 事件（服务端推送） */
export type SseEvent =
  | { type: 'start'; userMessageId: number | null }
  | { type: 'delta'; content: string }
  | { type: 'done'; messageId: number; promptTokens: number; completionTokens: number; totalTokens: number }
  | { type: 'error'; message: string }
