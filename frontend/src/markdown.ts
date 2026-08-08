import { marked } from 'marked'
import DOMPurify from 'dompurify'

// Markdown 渲染 + XSS 过滤（用户/AI 内容都可能包含 HTML）
marked.setOptions({ gfm: true, breaks: true })

export function renderMarkdown(content: string): string {
  return DOMPurify.sanitize(marked.parse(content) as string)
}

export function formatTime(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  const pad = (n: number) => String(n).padStart(2, '0')
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (sameDay) return hm
  return `${d.getMonth() + 1}-${d.getDate()} ${hm}`
}

export function formatTokens(n: number): string {
  if (n >= 10000) return `${(n / 10000).toFixed(1)}w`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`
  return String(n)
}
