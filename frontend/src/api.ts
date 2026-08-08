import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, SseEvent } from './types'

/**
 * HTTP 客户端：
 * - 自动携带 Authorization: Bearer <token>
 * - 统一解包 ApiResponse{code,message,data}，code!=0 时弹错误提示
 * - 401 时跳转登录页
 */
const http = axios.create({ baseURL: '/api', timeout: 30000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('smartchat_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      if (body.code === 401) {
        redirectToLogin()
      }
      return Promise.reject(new Error(body.message))
    }
    return resp
  },
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      redirectToLogin()
    } else {
      ElMessage.error(err.response?.data?.message || err.message || '网络错误')
    }
    return Promise.reject(err)
  },
)

function redirectToLogin() {
  if (!location.pathname.startsWith('/login')) {
    location.href = '/login'
  }
}

/** 类型安全的请求封装 */
export const api = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return http.get<ApiResponse<T>>(url, config).then((r) => r.data.data)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return http.post<ApiResponse<T>>(url, data, config).then((r) => r.data.data)
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return http.put<ApiResponse<T>>(url, data).then((r) => r.data.data)
  },
  delete<T>(url: string): Promise<T> {
    return http.delete<ApiResponse<T>>(url).then((r) => r.data.data)
  },
}

export interface SseHandlers {
  onEvent: (event: SseEvent) => void
  onError?: (message: string) => void
}

/**
 * SSE 流式请求（基于 fetch，因为 EventSource 不支持 POST）：
 * 返回 AbortController，调用 abort() 即「停止生成」。
 */
export async function streamSse(
  url: string,
  body: unknown,
  handlers: SseHandlers,
): Promise<AbortController> {
  const controller = new AbortController()
  const token = localStorage.getItem('smartchat_token')

  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
    signal: controller.signal,
  })
    .then(async (resp) => {
      if (!resp.ok || !resp.body) {
        const text = await resp.text().catch(() => '')
        handlers.onError?.(text || `HTTP ${resp.status}`)
        return
      }
      const reader = resp.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        // SSE 消息以空行分隔
        let idx: number
        while ((idx = buffer.indexOf('\n\n')) >= 0) {
          const raw = buffer.slice(0, idx)
          buffer = buffer.slice(idx + 2)
          const dataLine = raw
            .split('\n')
            .find((l) => l.startsWith('data:'))
            ?.slice(5)
            .trim()
          if (dataLine) {
            try {
              handlers.onEvent(JSON.parse(dataLine) as SseEvent)
            } catch {
              /* 忽略无法解析的事件 */
            }
          }
        }
      }
    })
    .catch((err) => {
      // 用户主动中止（停止生成）不算错误
      if (err?.name !== 'AbortError') {
        handlers.onError?.(err?.message || '连接中断')
      }
    })
  return controller
}
