<template>
  <aside class="sidebar">
    <!-- 头部 -->
    <div class="sidebar-header">
      <div class="logo">
        <el-icon :size="22" color="#409eff"><ChatDotRound /></el-icon>
        <span>SmartChat</span>
      </div>
    </div>

    <!-- 搜索 + 新建 -->
    <div class="sidebar-actions">
      <el-input
        v-model="keyword"
        placeholder="搜索会话标题"
        size="small"
        clearable
        :prefix-icon="Search"
      />
      <el-button type="primary" size="small" @click="$emit('new')">
        <el-icon><Plus /></el-icon> 新对话
      </el-button>
    </div>

    <!-- 会话列表 -->
    <el-scrollbar class="conversation-scroll">
      <div
        v-for="conv in filteredConversations"
        :key="conv.id"
        class="conversation-item"
        :class="{ active: conv.id === activeId }"
        @click="$emit('select', conv.id)"
      >
        <div class="conv-title">
          {{ conv.title }}
        </div>
        <div class="conv-sub">
          {{ conv.messageCount }} 条消息 · {{ formatTime(conv.updatedAt) }}
        </div>
        <div class="conv-actions">
          <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, conv.id)">
            <el-button link size="small" @click.stop>
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">重命名</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <el-empty v-if="filteredConversations.length === 0" description="暂无会话" :image-size="60" />
    </el-scrollbar>

    <!-- 底部菜单 -->
    <div class="sidebar-footer">
      <el-button text @click="$emit('open-templates')">
        <el-icon><Collection /></el-icon> 模板库
      </el-button>
      <el-button text @click="$emit('open-settings')">
        <el-icon><Setting /></el-icon> 设置
      </el-button>
      <el-dropdown trigger="click" @command="handleUserCommand">
        <el-button text>
          <el-icon><User /></el-icon>
          {{ user?.nickname || user?.username }}
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="isAdmin" command="admin">管理后台</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage, type InputInstance } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { api } from '../api'
import { useUserStore } from '../store'
import { formatTime } from '../markdown'
import type { ConversationItem } from '../types'

const props = defineProps<{
  conversations: ConversationItem[]
  activeId: number | null
}>()

const emit = defineEmits<{
  select: [id: number]
  new: []
  'open-templates': []
  'open-settings': []
  refresh: []
}>()

const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.user)
const isAdmin = computed(() => userStore.isAdmin)

const keyword = ref('')
const filteredConversations = computed(() =>
  props.conversations.filter((c) => c.title.includes(keyword.value.trim())),
)

async function handleCommand(cmd: string, id: number) {
  if (cmd === 'rename') {
    const conv = props.conversations.find((c) => c.id === id)
    const { value } = await ElMessageBox.prompt('输入新标题', '重命名会话', {
      inputValue: conv?.title,
      inputValidator: (v) => (v && v.trim() ? true : '标题不能为空'),
    })
    await api.put(`/conversations/${id}`, { title: value.trim() })
    emit('refresh')
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm('删除后消息不可恢复，确定删除该会话吗？', '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger',
    })
    await api.delete(`/conversations/${id}`)
    emit('refresh')
  }
}

async function handleUserCommand(cmd: string) {
  if (cmd === 'admin') {
    router.push('/admin')
  } else if (cmd === 'logout') {
    userStore.logout()
    router.replace('/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  border-right: 1px solid var(--sc-border);
  background: var(--sc-sidebar-bg);
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar-header {
  padding: 16px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.sidebar-actions {
  display: flex;
  gap: 8px;
  padding: 0 12px 12px;
}

.conversation-scroll {
  flex: 1;
}

.conversation-item {
  position: relative;
  margin: 4px 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.conversation-item:hover {
  background: #f5f7fa;
}

.conversation-item.active {
  background: #ecf5ff;
}

.conv-title {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 24px;
}

.conv-sub {
  font-size: 12px;
  color: var(--sc-text-secondary);
  margin-top: 2px;
}

.conv-actions {
  position: absolute;
  right: 6px;
  top: 6px;
  opacity: 0;
}

.conversation-item:hover .conv-actions {
  opacity: 1;
}

.sidebar-footer {
  border-top: 1px solid var(--sc-border);
  padding: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
