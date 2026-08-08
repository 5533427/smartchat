<template>
  <div class="chat-input">
    <!-- 快捷动作 + 模板标签 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-tag
          v-for="a in actions"
          :key="a.key"
          :effect="action === a.key ? 'dark' : 'plain'"
          type="primary"
          size="small"
          class="action-tag"
          @click="toggleAction(a.key)"
        >
          {{ a.label }}
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-tag v-if="activeTemplate" closable size="small" @close="$emit('clear-template')">
          模板：{{ activeTemplate.name }}
        </el-tag>
        <el-button link size="small" @click="$emit('open-templates')">
          <el-icon><Collection /></el-icon> 模板库
        </el-button>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-area">
      <el-input
        ref="inputRef"
        v-model="text"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 8 }"
        resize="none"
        placeholder="输入消息，Ctrl + Enter 发送"
        :disabled="streaming"
        @keydown.ctrl.enter="send('')"
        @keydown.enter.exact.prevent="send('')"
      />
      <el-button
        v-if="streaming"
        type="danger"
        class="send-btn"
        circle
        title="停止生成"
        @click="$emit('stop')"
      >
        <el-icon :size="18"><VideoPause /></el-icon>
      </el-button>
      <el-button v-else type="primary" class="send-btn" circle title="发送 (Ctrl+Enter)" @click="send('')">
        <el-icon :size="18"><Promotion /></el-icon>
      </el-button>
    </div>
    <div class="hint">Enter 发送 · Ctrl+Enter 换行 · 支持 Markdown</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { TemplateInfo } from '../types'

const props = defineProps<{
  streaming: boolean
  activeTemplate: TemplateInfo | null
}>()

const emit = defineEmits<{
  send: [content: string, action?: string]
  stop: []
  'open-templates': []
  'clear-template': []
}>()

const text = ref('')
/** 当前生效的快捷动作（polish/translate/summarize/expand） */
const action = ref<string | null>(null)

const actions = [
  { key: 'polish', label: '润色' },
  { key: 'translate', label: '翻译' },
  { key: 'summarize', label: '总结' },
  { key: 'expand', label: '扩写' },
]

function toggleAction(key: string) {
  action.value = action.value === key ? null : key
}

function send(extra?: string) {
  const content = text.value.trim() + (extra ?? '')
  if (!content) {
    ElMessage.warning('请输入消息内容')
    return
  }
  if (props.activeTemplate && action.value) {
    ElMessage.warning('模板与快捷动作不能同时使用')
    return
  }
  emit('send', content, action.value || undefined)
  text.value = ''
  action.value = null
}
</script>

<style scoped>
.chat-input {
  border-top: 1px solid var(--sc-border);
  padding: 12px 24px 8px;
  background: #fff;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.toolbar-left {
  display: flex;
  gap: 6px;
}

.action-tag {
  cursor: pointer;
  user-select: none;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.send-btn {
  flex-shrink: 0;
  margin-bottom: 2px;
}

.hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--sc-text-secondary);
  text-align: right;
}
</style>
