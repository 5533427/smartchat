<template>
  <div class="admin-page">
    <!-- 顶栏 -->
    <header class="admin-header">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回聊天
      </el-button>
      <span class="admin-title">管理后台</span>
      <span style="flex: 1"></span>
      <el-tag type="danger" effect="plain">管理员</el-tag>
    </header>

    <div class="admin-body">
      <el-tabs v-model="tab" class="admin-tabs">
        <!-- 概览 -->
        <el-tab-pane label="数据概览" name="overview">
          <div class="stat-cards">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">用户总数</div>
              <div class="stat-value">{{ overview.totalUsers }}</div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">会话总数</div>
              <div class="stat-value">{{ overview.totalConversations }}</div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">消息总数</div>
              <div class="stat-value">{{ overview.totalMessages }}</div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">今日消息</div>
              <div class="stat-value">{{ overview.todayMessages }}</div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">Token 总量（约）</div>
              <div class="stat-value">{{ formatTokens(overview.totalTokens) }}</div>
            </el-card>
          </div>

          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>近 14 天消息趋势</span>
                <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                  <el-radio-button :value="7">7 天</el-radio-button>
                  <el-radio-button :value="14">14 天</el-radio-button>
                  <el-radio-button :value="30">30 天</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div ref="chartRef" class="chart"></div>
          </el-card>
        </el-tab-pane>

        <!-- 用户管理 -->
        <el-tab-pane label="用户管理" name="users">
          <el-card shadow="never">
            <div class="user-toolbar">
              <el-input
                v-model="keyword"
                placeholder="搜索用户名 / 昵称"
                clearable
                style="width: 240px"
                @change="loadUsers"
                @clear="loadUsers"
              />
              <el-button @click="loadUsers">搜索</el-button>
            </div>

            <el-table :data="users" stripe>
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column prop="nickname" label="昵称" min-width="100" />
              <el-table-column label="角色" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
                    {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="conversations" label="会话数" width="80" />
              <el-table-column prop="messages" label="消息数" width="80" />
              <el-table-column prop="createdAt" label="注册时间" width="160">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.enabled"
                    :disabled="row.role === 'ADMIN'"
                    @change="(v: boolean) => toggleUser(row, v)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90">
                <template #default="{ row }">
                  <el-button
                    link
                    type="danger"
                    :disabled="row.role === 'ADMIN'"
                    @click="removeUser(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              class="pagination"
              layout="total, prev, pager, next"
              :total="total"
              :page-size="size"
              v-model:current-page="page"
              @current-change="loadUsers"
            />
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { api } from '../api'
import { formatTime, formatTokens } from '../markdown'
import type { AdminUserItem, OverviewStats, PageResult, TrendPoint } from '../types'

const router = useRouter()
const tab = ref('overview')

// ---------- 概览 ----------
const overview = ref<OverviewStats>({
  totalUsers: 0,
  totalConversations: 0,
  totalMessages: 0,
  todayMessages: 0,
  todayTokens: 0,
  totalTokens: 0,
})
const trendDays = ref(14)
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

async function loadOverview() {
  overview.value = await api.get<OverviewStats>('/stats/overview')
}

async function loadTrend() {
  const data = await api.get<{ points: TrendPoint[] }>(`/stats/trend?days=${trendDays.value}`)
  if (!chartRef.value) return
  chart ||= echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['消息数', 'Token 消耗'] },
    grid: { left: 40, right: 40, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: data.points.map((p) => p.date.slice(5)) },
    yAxis: [{ type: 'value' }, { type: 'value' }],
    series: [
      {
        name: '消息数',
        type: 'line',
        smooth: true,
        data: data.points.map((p) => p.messages),
        areaStyle: { opacity: 0.15 },
      },
      {
        name: 'Token 消耗',
        type: 'bar',
        yAxisIndex: 1,
        data: data.points.map((p) => p.tokens),
        itemStyle: { color: '#67c23a' },
      },
    ],
  })
}

// ---------- 用户管理 ----------
const users = ref<AdminUserItem[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')

async function loadUsers() {
  const data = await api.get<PageResult<AdminUserItem>>(
    `/admin/users?page=${page.value - 1}&size=${size}&keyword=${encodeURIComponent(keyword.value)}`,
  )
  users.value = data.items
  total.value = data.total
}

async function toggleUser(row: AdminUserItem, enabled: boolean) {
  await api.put(`/admin/users/${row.id}/status`, { enabled })
  row.enabled = enabled
  ElMessage.success(enabled ? `已启用用户 ${row.username}` : `已禁用用户 ${row.username}`)
}

async function removeUser(row: AdminUserItem) {
  await ElMessageBox.confirm(
    `删除用户「${row.username}」将同时删除其全部会话与消息，确定吗？`,
    '删除用户',
    { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' },
  )
  await api.delete(`/admin/users/${row.id}`)
  ElMessage.success('已删除')
  loadUsers()
}

// 窗口大小变化时自适应图表
function onResize() {
  chart?.resize()
}

onMounted(async () => {
  await loadOverview()
  await loadTrend()
  await loadUsers()
  window.addEventListener('resize', onResize)
})
</script>

<style scoped>
.admin-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.admin-header {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid var(--sc-border);
}

.admin-title {
  font-size: 16px;
  font-weight: 600;
}

.admin-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.admin-tabs {
  height: 100%;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card .stat-label {
  color: var(--sc-text-secondary);
  font-size: 13px;
}

.stat-card .stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 8px;
  color: #303133;
}

.chart-card {
  margin-bottom: 16px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart {
  height: 320px;
}

.user-toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
