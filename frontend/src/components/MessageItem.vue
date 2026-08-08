<template>
  <div class="message-row" :class="message.role">
    <!-- 头像 -->
    <div class="avatar">
      <el-icon v-if="message.role === 'assistant'" :size="18" color="#fff"><MagicStick /></el-icon>
      <el-icon v-else :size="16" color="#fff"><User /></el-icon>
    </div>

    <!-- 气泡 -->
    <div class="message-body">
      <div class="bubble">
        <!-- 流式输出：增量文本 + 光标 -->
        <div v-if="isStreaming" class="markdown-body">
          <span v-html="renderedStream"></span><span class="stream-cursor"></span>
        </div>
        <div v-else class="markdown-body" v-html="rendered"></div>
      </div>

      <!-- 操作栏 -->
      <div v-if="!isStreaming" class="message-actions">
        <span class="msg-meta">{{ formatTime(message.createdAt) }}</span>
        <span v-if="message.role === 'assistant' && message.tokens > 0" class="msg-meta">
          · {{ formatTokens(message.tokens) }} tokens
        </span>
        <el-button link size="small" title="复制" @click="copy">
          <el-icon><CopyDocument /></el-icon>
        </el-button>
        <el-button
          v-if="message.role === 'assistant' && isLast"
          link
          size="small"
          title="重新生成"
          @click="$emit('regenerate', message.id)"
        >
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button link size="small" title="删除" @click="$emit('remove', message.id)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { MessageInfo } from '../types'
import { renderMarkdown, formatTime, formatTokens } from '../markdown'

const props = defineProps<{
  message: MessageInfo
  isLast: boolean
  /** 流式输出的半成品内容 */
  streamText?: string
}>()

defineEmits<{
  regenerate: [messageId: number]
  remove: [messageId: number]
}>()

const rendered = computed(() => renderMarkdown(props.message.content))
const renderedStream = computed(() => renderMarkdown(props.streamText || ''))

const isStreaming = computed(() => props.streamText !== undefined)

async function copy() {
  try {
    await navigator.clipboard.writeText(props.message.content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}
</script>

<style scoped>
.message-row {
  display: flex;
  gap: 12px;
  padding: 16px 0;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-row.assistant .avatar {
  background: linear-gradient(135deg, #409eff, #7c6bf5);
}

.message-row.user .avatar {
  background: #67c23a;
}

.message-body {
  max-width: 78%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-row.user .message-body {
  align-items: flex-end;
}

.bubble {
  background: var(--sc-assistant-bubble);
  border: 1px solid var(--sc-border);
  border-radius: 12px;
  padding: 10px 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.message-row.user .bubble {
  background: var(--sc-user-bubble);
  border: none;
  color: #fff;
}

.message-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-body:hover .message-actions {
  opacity: 1;
}

.msg-meta {
  font-size: 12px;
  color: var(--sc-text-secondary);
  margin-right: 4px;
}
</style>
