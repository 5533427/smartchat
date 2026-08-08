<template>
  <el-drawer v-model="visible" title="提示词模板库" size="420px" :destroy-on-close="true">
    <!-- 新建模板 -->
    <el-collapse class="new-template">
      <el-collapse-item title="＋ 新建个人模板" name="new">
        <el-form label-position="top" size="small">
          <el-form-item label="模板名称">
            <el-input v-model="form.name" placeholder="如：英文邮件助手" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" placeholder="一句话说明用途" />
          </el-form-item>
          <el-form-item label="系统提示词">
            <el-input
              v-model="form.systemPrompt"
              type="textarea"
              :rows="5"
              placeholder="定义 AI 的角色与行为规则"
            />
          </el-form-item>
          <el-button type="primary" size="small" :loading="saving" @click="saveTemplate">
            {{ editingId ? '更新模板' : '保存模板' }}
          </el-button>
          <el-button v-if="editingId" size="small" @click="resetForm">取消编辑</el-button>
        </el-form>
      </el-collapse-item>
    </el-collapse>

    <el-divider />

    <!-- 系统模板 -->
    <div class="template-group">
      <div class="group-title">
        <el-icon><Star /></el-icon> 系统模板
      </div>
      <div v-for="t in group.system" :key="t.id" class="template-card">
        <div class="template-head">
          <span class="template-name">{{ t.name }}</span>
          <div>
            <el-button v-if="isAdmin" link size="small" @click="editTemplate(t)">编辑</el-button>
            <el-button v-if="isAdmin" link size="small" type="danger" @click="removeTemplate(t)">
              删除
            </el-button>
          </div>
        </div>
        <div class="template-desc">{{ t.description }}</div>
        <div class="template-prompt">{{ t.systemPrompt }}</div>
        <el-button type="primary" size="small" plain @click="useTemplate(t)">
          <el-icon><Promotion /></el-icon> 使用
        </el-button>
      </div>
    </div>

    <!-- 个人模板 -->
    <div class="template-group">
      <div class="group-title">
        <el-icon><FolderOpened /></el-icon> 我的模板
      </div>
      <div v-for="t in group.mine" :key="t.id" class="template-card">
        <div class="template-head">
          <span class="template-name">{{ t.name }}</span>
          <div>
            <el-button link size="small" @click="editTemplate(t)">编辑</el-button>
            <el-button link size="small" type="danger" @click="removeTemplate(t)">删除</el-button>
          </div>
        </div>
        <div class="template-desc">{{ t.description }}</div>
        <div class="template-prompt">{{ t.systemPrompt }}</div>
        <el-button type="primary" size="small" plain @click="useTemplate(t)">
          <el-icon><Promotion /></el-icon> 使用
        </el-button>
      </div>
      <el-empty v-if="group.mine.length === 0" description="还没有个人模板" :image-size="50" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store'
import type { TemplateGroup, TemplateInfo } from '../types'

const visible = defineModel<boolean>('visible', { default: false })
const emit = defineEmits<{ use: [t: TemplateInfo] }>()

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const group = ref<TemplateGroup>({ system: [], mine: [] })
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', description: '', systemPrompt: '' })

async function load() {
  group.value = await api.get<TemplateGroup>('/templates')
}

watch(visible, (v) => {
  if (v) load()
})

function editTemplate(t: TemplateInfo) {
  editingId.value = t.id
  form.name = t.name
  form.description = t.description || ''
  form.systemPrompt = t.systemPrompt
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.description = ''
  form.systemPrompt = ''
}

async function saveTemplate() {
  if (!form.name.trim() || !form.systemPrompt.trim()) {
    ElMessage.warning('请填写模板名称和系统提示词')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/templates/${editingId.value}`, form)
      ElMessage.success('模板已更新')
    } else {
      await api.post('/templates', form)
      ElMessage.success('模板已创建')
    }
    resetForm()
    load()
  } finally {
    saving.value = false
  }
}

async function removeTemplate(t: TemplateInfo) {
  await ElMessageBox.confirm(`确定删除模板「${t.name}」吗？`, '删除模板', { type: 'warning' })
  await api.delete(`/templates/${t.id}`)
  ElMessage.success('已删除')
  load()
}

function useTemplate(t: TemplateInfo) {
  emit('use', t)
  visible.value = false
  ElMessage.success(`已选用模板：${t.name}，发送消息时将生效`)
}
</script>

<style scoped>
.group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  margin-bottom: 10px;
}

.template-card {
  border: 1px solid var(--sc-border);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.template-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.template-name {
  font-weight: 600;
}

.template-desc {
  font-size: 12px;
  color: var(--sc-text-secondary);
  margin: 4px 0;
}

.template-prompt {
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-wrap;
}
</style>
