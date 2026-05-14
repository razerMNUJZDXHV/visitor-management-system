<template>
  <div class="page-container approval-center-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">审批管理</h2>
      </div>
      <div class="header-actions">
        <el-button
          v-if="activeTab === 'history'"
          type="success"
          plain
          :loading="historyExporting"
          @click="handleHistoryExport"
        >
          按条件导出
        </el-button>
        <el-button type="primary" plain :loading="pendingLoading || historyLoading" @click="reloadAll">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="summary-grid">
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">待审批</div>
          <div class="summary-value status-warn">{{ pendingList.length }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">历史记录</div>
          <div class="summary-value">{{ historyList.length }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">已拒绝</div>
          <div class="summary-value status-danger">{{ rejectedCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="approval-tabs">
      <el-tab-pane name="pending">
        <template #label>
          <span>待审批</span>
        </template>

        <el-card class="panel-card" shadow="never">
          <el-form inline class="search-form" @submit.prevent="handlePendingSearch">
            <el-form-item label="关键词">
              <el-input
                v-model="pendingKeyword"
                clearable
                placeholder="访客姓名/手机号/被访人"
                class="search-input"
                @keyup.enter="handlePendingSearch"
              />
            </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handlePendingSearch">搜索</el-button>
          <el-button @click="handlePendingReset">重置</el-button>
        </el-form-item>
          </el-form>

          <el-table
            :data="paginatedPendingList"
            border
            stripe
            empty-text="暂无待审批申请"
            class="approval-table"
            @row-click="row => goDetail(row, 'pending')"
          >
            <el-table-column prop="appointmentId" label="编号" width="100" align="center" />
            <el-table-column prop="visitorName" label="访客姓名" min-width="120" />
            <el-table-column prop="visitorPhone" label="手机号" min-width="140" />
            <el-table-column prop="intervieweeName" label="被访人" min-width="140" />
            <el-table-column prop="createTimeText" label="申请时间" min-width="180" />
            <el-table-column prop="visitorIdCard" label="身份证号" min-width="200" />
            <el-table-column label="状态" width="110" align="center">
              <template #default>
                <el-tag type="warning" effect="light">待审批</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button type="primary" link @click.stop="goDetail(scope.row, 'pending')">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pendingPage"
              v-model:page-size="pendingPageSize"
              :page-sizes="[10, 20, 50]"
              :total="pendingTotal"
              layout="total, sizes, prev, pager, next, jumper"
              prev-text="上一页"
              next-text="下一页"
              @size-change="handlePendingSizeChange"
              @current-change="handlePendingCurrentChange"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane name="history">
        <template #label>
          <span>审批记录</span>
        </template>

        <el-card class="panel-card" shadow="never">
          <el-form inline class="search-form history-search-form" @submit.prevent>
            <el-form-item label="范围">
              <el-radio-group v-model="historyScope">
                <el-radio-button label="all">全部记录</el-radio-button>
                <el-radio-button label="mine">我的审批</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="时间类型">
              <el-radio-group v-model="historySearchType">
                <el-radio-button label="create">申请时间</el-radio-button>
                <el-radio-button label="process">处理时间</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="日期范围">
              <el-date-picker
                v-model="historyDateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                class="history-range"
              />
            </el-form-item>

            <el-form-item label="状态">
              <el-select v-model="historyStatus" placeholder="全部状态" class="history-status-select" clearable @change="handleHistorySearch">
                <el-option label="已通过" :value="1" />
                <el-option label="已拒绝" :value="2" />
                <el-option label="已取消" :value="3" />
                <el-option label="已签到" :value="4" />
                <el-option label="已完成" :value="5" />
                <el-option label="已过期" :value="6" />
              </el-select>
            </el-form-item>

            <el-form-item label="关键词">
              <el-input
                v-model="historyKeyword"
                clearable
                placeholder="按审批人姓名、访客姓名或手机号搜索"
                class="history-search-input"
                @keyup.enter="handleHistorySearch"
              />
            </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleHistorySearch">搜索</el-button>
          <el-button @click="handleHistoryReset">重置</el-button>
        </el-form-item>
          </el-form>

          <div class="batch-toolbar">
            <el-button
              type="danger"
              plain
              :disabled="selectedHistoryRows.length === 0"
              :loading="batchDeleting"
              @click="handleBatchDelete"
            >
              批量删除（已选 {{ selectedHistoryRows.length }} 条）
            </el-button>
          </div>

          <el-table
            :data="paginatedHistoryList"
            border
            stripe
            empty-text="暂无审批记录"
            class="approval-table"
            @row-click="row => goDetail(row, 'history')"
            @selection-change="handleHistorySelectionChange"
          >
            <el-table-column type="selection" width="55" align="center" :selectable="row => canDeleteRecord(row.status)" />
            <el-table-column prop="appointmentId" label="编号" width="100" align="center" />
            <el-table-column prop="visitorName" label="访客姓名" min-width="120" />
            <el-table-column prop="visitorPhone" label="手机号" min-width="140" />
            <el-table-column prop="statusText" label="状态" width="110" align="center">
              <template #default="scope">
                <el-tag :type="getStatusTagType(scope.row.status)" effect="light">{{ scope.row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approverName" label="审批人姓名" min-width="130" />
            <el-table-column prop="createTimeText" label="申请时间" min-width="180" />
            <el-table-column prop="processTimeText" label="处理时间" min-width="180" />
            <el-table-column label="操作" width="180" align="center">
              <template #default="scope">
                <el-button type="primary" link @click.stop="goDetail(scope.row, 'history')">查看详情</el-button>
                <el-button
                  v-if="canDeleteRecord(scope.row.status)"
                  type="danger"
                  link
                  @click.stop="handleDeleteRecord(scope.row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="historyPage"
              v-model:page-size="historyPageSize"
              :page-sizes="[10, 20, 50]"
              :total="historyTotal"
              layout="total, sizes, prev, pager, next, jumper"
              prev-text="上一页"
              next-text="下一页"
              @size-change="handleHistorySizeChange"
              @current-change="handleHistoryCurrentChange"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
// 审批中心：待审批/历史记录查询、导出与删除。
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchDeleteApprovalRecords, deleteApprovalRecord, exportApprovalHistory, fetchApprovalHistory, fetchPendingAppointments } from '../api/approval'
import { formatDateOnly, mapAppointment } from '../utils/appointment'
import { downloadBlob } from '../utils/download'

// 路由实例
const route = useRoute()
const router = useRouter()
// 允许删除的状态码
const DELETABLE_STATUS = [2, 3, 5, 6]

// 视图状态与筛选条件
const activeTab = ref(route.query.tab === 'history' ? 'history' : 'pending')
const pendingLoading = ref(false)
const historyLoading = ref(false)
const pendingKeyword = ref('')
const pendingList = ref([])
const historyScope = ref(route.query.scope === 'mine' ? 'mine' : 'all')
const historySearchType = ref('create')
const historyDateRange = ref([])
const historyStatus = ref(null)
const historyKeyword = ref('')
const historyList = ref([])
const pendingPage = ref(Number(route.query.pPage) > 0 ? Number(route.query.pPage) : 1)
const pendingPageSize = ref(10)
const historyPage = ref(Number(route.query.hPage) > 0 ? Number(route.query.hPage) : 1)
const historyPageSize = ref(10)
const selectedHistoryRows = ref([])
const batchDeleting = ref(false)
const historyExporting = ref(false)

// 待审批筛选（前端关键字过滤）
const filteredPendingList = computed(() => {
  const searchText = pendingKeyword.value.trim().toLowerCase()
  if (!searchText) return pendingList.value
  return pendingList.value.filter((item) => {
    return [item.visitorName, item.visitorPhone, item.intervieweeName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(searchText))
  })
})

// 待审批总数（用于分页）
const pendingTotal = computed(() => filteredPendingList.value.length)

// 待审批分页结果
const paginatedPendingList = computed(() => {
  const start = (pendingPage.value - 1) * pendingPageSize.value
  return filteredPendingList.value.slice(start, start + pendingPageSize.value)
})

// 历史总数（用于分页）
const historyTotal = computed(() => historyList.value.length)

// 历史分页结果
const paginatedHistoryList = computed(() => {
  const start = (historyPage.value - 1) * historyPageSize.value
  return historyList.value.slice(start, start + historyPageSize.value)
})

// 已拒绝数量
const rejectedCount = computed(() => historyList.value.filter((item) => Number(item.status) === 2).length)

// 拉取待审批列表
const loadPendingList = async () => {
  pendingLoading.value = true
  try {
    const res = await fetchPendingAppointments()
    if (res.code === 200) {
      pendingList.value = (res.data || []).map((item) => mapAppointment(item, 'pending'))
    } else {
      ElMessage.error(res.msg || '加载待审批列表失败')
    }
  } catch (error) {
    console.error('加载待审批列表失败：', error)
    ElMessage.error('加载待审批列表失败')
  } finally {
    pendingLoading.value = false
  }
}

// 拉取审批历史列表（含筛选条件）
const loadHistoryList = async () => {
  historyLoading.value = true
  try {
    const params = {
      scope: historyScope.value,
      searchType: historySearchType.value
    }

    if (historyKeyword.value.trim()) {
      params.keyword = historyKeyword.value.trim()
    }
    if (Array.isArray(historyDateRange.value) && historyDateRange.value.length === 2) {
      params.startDate = historyDateRange.value[0]
      params.endDate = historyDateRange.value[1]
    }
    if (historyStatus.value !== null && historyStatus.value !== undefined) {
      params.status = historyStatus.value
    }

    const res = await fetchApprovalHistory(params)
    if (res.code === 200) {
      historyList.value = (res.data || []).map((item) => mapAppointment(item, 'history'))
    } else {
      ElMessage.error(res.msg || '加载审批记录失败')
    }
  } catch (error) {
    console.error('加载审批记录失败：', error)
    ElMessage.error('加载审批记录失败')
  } finally {
    historyLoading.value = false
  }
}

// 按条件导出审批历史
const handleHistoryExport = async () => {
  if (historyExporting.value) return
  historyExporting.value = true
  try {
    const params = {}
    if (historyKeyword.value.trim()) params.keyword = historyKeyword.value.trim()
    if (historyStatus.value !== null && historyStatus.value !== undefined) params.status = historyStatus.value
    if (Array.isArray(historyDateRange.value) && historyDateRange.value.length === 2) {
      params.startDate = historyDateRange.value[0]
      params.endDate = historyDateRange.value[1]
    }
    if (historySearchType.value) params.searchType = historySearchType.value

    const res = await exportApprovalHistory(params)
    const fileDate = formatDateOnly(new Date()) || '导出'
    downloadBlob(res, `审批记录_${fileDate}.xlsx`)
    ElMessage.success('审批记录已导出')
  } catch (error) {
    console.error('导出审批记录失败：', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    historyExporting.value = false
  }
}

// 刷新两类列表
const reloadAll = async () => {
  await Promise.all([loadPendingList(), loadHistoryList()])
}

// 触发待审批关键字筛选
const handlePendingSearch = () => {
  pendingPage.value = 1
  if (pendingList.value.length === 0) {
    loadPendingList()
  }
}

// 重置待审批关键字筛选
const handlePendingReset = () => {
  pendingKeyword.value = ''
  pendingPage.value = 1
  if (pendingList.value.length === 0) {
    loadPendingList()
  }
}

// 触发历史筛选
const handleHistorySearch = () => {
  historyPage.value = 1
  loadHistoryList()
}

// 重置历史筛选
const handleHistoryReset = () => {
  historySearchType.value = 'create'
  historyDateRange.value = []
  historyStatus.value = null
  historyKeyword.value = ''
  historyScope.value = 'all'
  historyPage.value = 1
  loadHistoryList()
}

// 构造详情页返回路径（保留筛选条件）
const buildReturnPath = () => {
  const query = { tab: activeTab.value }
  if (activeTab.value === 'history') {
    query.scope = historyScope.value
    query.hPage = String(historyPage.value)
  } else {
    query.pPage = String(pendingPage.value)
  }
  return router.resolve({ path: '/approval-center', query }).fullPath
}

// 同步筛选状态到路由 query，便于刷新恢复
const syncRouteQuery = () => {
  const query = { tab: activeTab.value }
  if (activeTab.value === 'history') {
    query.scope = historyScope.value
    query.hPage = String(historyPage.value)
  } else {
    query.pPage = String(pendingPage.value)
  }

  const nextFullPath = router.resolve({ path: '/approval-center', query }).fullPath
  if (nextFullPath !== route.fullPath) {
    router.replace({ path: '/approval-center', query })
  }
}

// 待审批分页大小变化
const handlePendingSizeChange = (size) => {
  pendingPageSize.value = size
  pendingPage.value = 1
}

// 待审批页码变化
const handlePendingCurrentChange = (page) => {
  pendingPage.value = page
}

// 历史分页大小变化
const handleHistorySizeChange = (size) => {
  historyPageSize.value = size
  historyPage.value = 1
}

// 历史页码变化
const handleHistoryCurrentChange = (page) => {
  historyPage.value = page
}

// 跳转详情并缓存筛选条件
const goDetail = (item, mode) => {
  // 保存当前筛选状态到 sessionStorage
  const filterState = {
    activeTab: activeTab.value,
    pendingKeyword: pendingKeyword.value,
    pendingPage: pendingPage.value,
    historyScope: historyScope.value,
    historySearchType: historySearchType.value,
    historyDateRange: historyDateRange.value,
    historyStatus: historyStatus.value,
    historyKeyword: historyKeyword.value,
    historyPage: historyPage.value
  }
  sessionStorage.setItem('approvalCenterFilter', JSON.stringify(filterState))

  router.push({
    path: `/approval-detail/${item.appointmentId}`,
    query: {
      mode,
      from: buildReturnPath()
    }
  })
}

// 判断是否可删除记录
const canDeleteRecord = (status) => {
  return DELETABLE_STATUS.includes(Number(status))
}

// 勾选记录变化
const handleHistorySelectionChange = (selection) => {
  selectedHistoryRows.value = selection
}

// 删除单条历史记录
const handleDeleteRecord = async (item) => {
  try {
    await ElMessageBox.confirm(`确认删除预约 #${item.appointmentId} 吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const res = await deleteApprovalRecord(item.appointmentId)
    if (res.code === 200) {
      ElMessage.success('记录已删除')
      loadHistoryList()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    console.error('删除审批记录失败：', error)
    ElMessage.error('删除失败')
  }
}

// 批量删除历史记录
const handleBatchDelete = async () => {
  const rows = selectedHistoryRows.value
  if (rows.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }

  const ids = rows.map(row => row.appointmentId)
  const idStr = ids.join(', ')

  try {
    await ElMessageBox.confirm(
      `确认批量删除以下 ${ids.length} 条记录吗？删除后不可恢复。\n\n预约编号：${idStr}`,
      '批量删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  batchDeleting.value = true
  try {
    const res = await batchDeleteApprovalRecords(ids)
    if (res.code === 200) {
      ElMessage.success(`已删除 ${ids.length} 条记录`)
      selectedHistoryRows.value = []
      loadHistoryList()
    } else {
      ElMessage.error(res.msg || '批量删除失败')
    }
  } catch (error) {
    console.error('批量删除审批记录失败：', error)
    ElMessage.error('批量删除失败')
  } finally {
    batchDeleting.value = false
  }
}

// 状态标签类型映射
const getStatusTagType = (status) => {
  const value = Number(status)
  if (value === 1) return 'success'
  if (value === 2) return 'danger'
  if (value === 3) return 'info'
  if (value === 4) return 'warning'
  if (value === 5) return 'success'
  if (value === 6) return 'warning'
  return 'info'
}

watch(
  () => route.query.tab,
  (tab) => {
    // URL 参数变化时同步标签页
    activeTab.value = tab === 'history' ? 'history' : 'pending'
  },
  { immediate: true }
)

watch(
  () => route.query.scope,
  (scope) => {
    // URL 参数变化时同步范围筛选
    historyScope.value = scope === 'mine' ? 'mine' : 'all'
  },
  { immediate: true }
)

watch(
  () => route.query.pPage,
  (pPage) => {
    // URL 参数变化时同步待审批页码
    const page = Number(pPage)
    if (Number.isFinite(page) && page > 0) {
      pendingPage.value = page
    }
  }
)

watch(
  () => route.query.hPage,
  (hPage) => {
    // URL 参数变化时同步历史页码
    const page = Number(hPage)
    if (Number.isFinite(page) && page > 0) {
      historyPage.value = page
    }
  }
)

watch(activeTab, (tab) => {
  // 切换标签页时按需加载数据
  if (tab === 'history' && historyList.value.length === 0) {
    loadHistoryList()
  }
  if (tab === 'pending' && pendingList.value.length === 0) {
    loadPendingList()
  }
  syncRouteQuery()
})

watch(historyScope, () => {
  // 范围切换时刷新历史列表
  if (activeTab.value === 'history') {
    historyPage.value = 1
    loadHistoryList()
  }
  syncRouteQuery()
})

watch(pendingPage, () => {
  // 待审批页码变化时同步 URL
  if (activeTab.value === 'pending') {
    syncRouteQuery()
  }
})

watch(historyPage, () => {
  // 历史页码变化时同步 URL
  if (activeTab.value === 'history') {
    syncRouteQuery()
  }
})

watch(filteredPendingList, (list) => {
  // 过滤后总数变化，防止页码越界
  const maxPage = Math.max(1, Math.ceil(list.length / pendingPageSize.value))
  if (pendingPage.value > maxPage) {
    pendingPage.value = maxPage
  }
})

watch(historyList, (list) => {
  // 历史总数变化时纠正页码
  const maxPage = Math.max(1, Math.ceil(list.length / historyPageSize.value))
  if (historyPage.value > maxPage) {
    historyPage.value = maxPage
  }
})

// 初始化：恢复筛选条件并加载数据
onMounted(() => {
  // 恢复筛选状态
  const savedFilter = sessionStorage.getItem('approvalCenterFilter')
  if (savedFilter) {
    try {
      const filterState = JSON.parse(savedFilter)
      activeTab.value = filterState.activeTab || 'pending'
      pendingKeyword.value = filterState.pendingKeyword || ''
      pendingPage.value = filterState.pendingPage || 1
      historyScope.value = filterState.historyScope || 'all'
      historySearchType.value = filterState.historySearchType || 'create'
      historyDateRange.value = filterState.historyDateRange || []
      historyStatus.value = filterState.historyStatus !== undefined ? filterState.historyStatus : null
      historyKeyword.value = filterState.historyKeyword || ''
      historyPage.value = filterState.historyPage || 1
    } catch (e) {
      console.error('恢复筛选条件失败：', e)
    }
    // 清除 sessionStorage 中的对应项，避免刷新页面时重复恢复
    sessionStorage.removeItem('approvalCenterFilter')
  }

  reloadAll()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 22px;
  font-weight: bold;
  margin: 0 0 8px;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-grid {
  margin-bottom: 20px;
}

.summary-card {
  border-radius: 12px;
}

.summary-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.summary-value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.4;
  color: #303133;
}

.status-warn {
  color: #e6a23c;
}

.status-danger {
  color: #f56c6c;
}

.approval-tabs :deep(.el-tabs__item) {
  font-weight: 600;
}

.tab-badge {
  margin-left: 8px;
}

.panel-card {
  border-radius: 12px;
}

.search-form {
  margin-bottom: 16px;
}

.search-input {
  width: 260px;
}

.history-range {
  width: 280px;
  max-width: 100%;
}

.history-status-select {
  width: 130px;
}

.history-search-input {
  width: 260px;
  max-width: 100%;
}

.approval-table {
  cursor: pointer;
}

.batch-toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-start;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1200px) {
  .search-input,
  .history-range,
  .history-status-select,
  .history-search-input {
    width: 100%;
  }

  .pagination-container {
    justify-content: flex-start;
  }
}
</style>
