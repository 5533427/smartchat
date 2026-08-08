<template>
  <div ref="scrollRef" class="message-list">
    <!-- 空状态 -->
    <div v-if="messages.length === 0 && !streamText" class="empty-state">
      <el-icon :size="48" color="#c0c4cc"><ChatDotRound /></el-icon>
      <h3>开始新的对话</h3>
      <p>支持 Markdown 输出 · 可润色 / 翻译 / 总结 / 扩写 · 多模型自由切换</p>
    </div>

    <template v-else>
      <MessageItem
        v-for="(m, i) in messages"
        :key="m.id"
        :message="m"
        :is-last="i === messages.length - 1"
        @regenerate="$emit('regenerate', $event)"
        @remove="$emit('remove', $event)"
      />
      <!-- 流式输出的 AI 回复 -->
      <MessageItem
        v-if="streamText !== null"
        :message="streamMessage"
        :is-last="true"
        :stream-text="streamText"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import MessageItem from './MessageItem.vue'
import type { MessageInfo } from '../types'

const props = defineProps<{
  messages: MessageInfo[]
  /** 流式输出的半成品文本；null 表示没有进行中的生成 */
  streamText: string | null
}>()

defineEmits<{
  regenerate: [messageId: number]
  remove: [messageId: number]
}>()

const scrollRef = ref<HTMLElement>()

/** 流式占位消息（仅用于渲染，不会落库） */
const streamMessage = computed<MessageInfo>(() => ({
  id: -1,
  role: 'assistant',
  content: '',
  tokens: 0,
  meta: null,
  createdAt: new Date().toISOString(),
}))

// 消息变化或流式增量时自动滚到底部
watch(
  () => [props.messages.length, props.streamText] as const,
  async () => {
    await nextTick()
    scrollRef.value?.scrollTo({ top: scrollRef.value.scrollHeight })
  },
  { flush: 'post' },
)
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 24px;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--sc-text-secondary);
}

.empty-state h3 {
  margin: 12px 0 4px;
  font-size: 20px;
  color: var(--sc-text);
}

.empty-state p {
  font-size: 13px;
  margin: 0;
}
</style>
