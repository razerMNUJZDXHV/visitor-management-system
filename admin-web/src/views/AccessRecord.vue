<template>
  <div class="page-container access-record-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">通行记录管理</h2>
      </div>
      <div class="header-actions">
        <el-button type="success" plain :loading="exporting" @click="handleExportByCondition">按条件导出</el-button>
        <el-button type="primary" plain :loading="loading" @click="loadRecords">刷新</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="summary-grid">
      <el-col :xs="12" :sm="6">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">总记录数</div>
          <div class="summary-value">{{ recordList.length }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">签到记录</div>
          <div class="summary-value status-success">{{ signInCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">签离记录</div>
          <div class="summary-value status-warn">{{ signOutCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">紧急通行</div>
          <div class="summary-value status-danger">{{ emergencyCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索表单 -->
    <el-card class="panel-card" shadow="never">
      <el-form inline class="search-form access-search-form" @submit.prevent="handleSearch">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="date-range"
          />
        </el-form-item>

        <el-form-item label="通行类型">
          <el-select v-model="accessType" placeholder="全部" clearable class="type-select" @change="handleSearch" @keyup.enter="handleSearch">
            <el-option label="签到" :value="1" />
            <el-option label="签离" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="核验方式">
          <el-select v-model="verifyMethod" placeholder="全部" clearable class="type-select" @change="handleSearch" @keyup.enter="handleSearch">
            <el-option label="扫码" :value="1" />
            <el-option label="手动" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="紧急通行">
          <el-switch v-model="emergencyOnly" @change="handleSearch" @keyup.enter="handleSearch" />
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="keyword"
            clearable
            placeholder="访客姓名/手机号/安保姓名/授权审批人姓名"
            class="search-input"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 批量删除工具栏 -->
      <div class="batch-toolbar">
        <el-button
          type="danger"
          plain
          :disabled="selectedRows.length === 0"
          :loading="batchDeleting"
          @click="handleBatchDelete"
        >
          批量删除（已选 {{ selectedRows.length }} 条）
        </el-button>
      </div>

      <!-- 记录表格 -->
      <el-table
        :data="paginatedList"
        border
        stripe
        empty-text="暂无通行记录"
        class="record-table"
        @row-click="goDetail"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" :selectable="row => canDelete(row)" />
        <el-table-column prop="logId" label="记录ID" width="100" align="center" />
        <el-table-column prop="visitorName" label="访客姓名" min-width="120" />
        <el-table-column prop="visitorPhone" label="手机号" min-width="140" />
        <el-table-column prop="securityName" label="安保姓名" min-width="120" />
        <el-table-column label="核验方式" width="100" align="center">
          <template #default="scope">
            <el-tag :type="Number(scope.row.verifyMethod) === 1 ? '' : 'warning'" effect="light">
              {{ Number(scope.row.verifyMethod) === 1 ? '扫码' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通行类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="Number(scope.row.accessType) === 1 ? 'success' : 'warning'" effect="light">
              {{ Number(scope.row.accessType) === 1 ? '签到' : '签离' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="紧急通行" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.emergency" type="danger" effect="light">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="authorizerName" label="授权审批人" min-width="130">
          <template #default="scope">
            {{ scope.row.authorizerName || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="通行时间" min-width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.accessTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click.stop="goDetail(scope.row)">查看详情</el-button>
            <el-button
              v-if="canDelete(scope.row)"
              type="danger"
              link
              @click.stop="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="recordList.length"
          layout="total, sizes, prev, pager, next, jumper"
          prev-text="上一页"
          next-text="下一页"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
// 通行记录管理：查询、导出、删除与筛选状态恢复。
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchDeleteAccessRecords, deleteAccessRecord, exportAccessRecords, fetchAccessRecords } from '../api/access'
import { formatDateTime } from '../utils/appointment'
import { downloadBlob } from '../utils/download'

// 路由实例
const router = useRouter()

// 列表与筛选状态
const loading = ref(false)
const batchDeleting = ref(false)
const exporting = ref(false)
const keyword = ref('')
const dateRange = ref([])
const accessType = ref(null)
const verifyMethod = ref(null)
const emergencyOnly = ref(false)
const recordList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 统计计算
const signInCount = computed(() => recordList.value.filter(item => Number(item.accessType) === 1).length)
const signOutCount = computed(() => recordList.value.filter(item => Number(item.accessType) === 2).length)
const emergencyCount = computed(() => recordList.value.filter(item => item.emergency).length)

// 分页列表
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return recordList.value.slice(start, start + pageSize.value)
})

// 判断记录是否可删除（关联预约已完成 status=5）
const canDelete = (row) => {
  return Number(row.appointmentStatus) === 5
}

// 加载记录（带筛选条件）
const loadRecords = async () => {
  loading.value = true
  try {
    const params = {}
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim()
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (accessType.value !== null) {
      params.accessType = accessType.value
    }
    if (verifyMethod.value !== null) {
      params.verifyMethod = verifyMethod.value
    }
    if (emergencyOnly.value) {
      params.emergencyOnly = true
    }

    const res = await fetchAccessRecords(params)
    if (res.code === 200) {
      recordList.value = res.data || []
    } else {
      ElMessage.error(res.msg || '加载通行记录失败')
    }
  } catch (error) {
    console.error('加载通行记录失败：', error)
    ElMessage.error('加载通行记录失败')
  } finally {
    loading.value = false
  }
}

// 触发搜索
const handleSearch = () => {
  currentPage.value = 1
  loadRecords()
}

// 重置筛选条件
const handleReset = () => {
  keyword.value = ''
  dateRange.value = []
  accessType.value = null
  verifyMethod.value = null
  emergencyOnly.value = false
  currentPage.value = 1
  loadRecords()
}

// 按条件导出
const handleExportByCondition = async () => {
  if (exporting.value) return
  exporting.value = true
  try {
    const params = {}
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (accessType.value !== null && accessType.value !== undefined) params.accessType = accessType.value
    if (verifyMethod.value !== null && verifyMethod.value !== undefined) params.verifyMethod = verifyMethod.value
    if (emergencyOnly.value) params.emergencyOnly = true
    if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const blob = await exportAccessRecords(params)
    const fileDate = new Date().toISOString().slice(0, 10)
    downloadBlob(blob, `通行记录_${fileDate}.xlsx`)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败：', error)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// 勾选记录变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 进入详情并缓存筛选条件
const goDetail = (row) => {
  // 保存当前筛选状态到 sessionStorage
  const filterState = {
    keyword: keyword.value,
    dateRange: dateRange.value,
    accessType: accessType.value,
    verifyMethod: verifyMethod.value,
    emergencyOnly: emergencyOnly.value,
    currentPage: currentPage.value,
    pageSize: pageSize.value
  }
  sessionStorage.setItem('accessRecordFilter', JSON.stringify(filterState))

  router.push(`/access-record-detail/${row.logId}`)
}

// 删除单条记录
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除记录 #${row.logId} 吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const res = await deleteAccessRecord(row.logId)
    if (res.code === 200) {
      ElMessage.success('记录已删除')
      loadRecords()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    console.error('删除通行记录失败：', error)
    ElMessage.error('删除失败')
  }
}

// 批量删除记录
const handleBatchDelete = async () => {
  const rows = selectedRows.value
  if (rows.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }

  const ids = rows.map(row => row.logId)
  const idStr = ids.join(', ')

  try {
    await ElMessageBox.confirm(
      `确认批量删除以下 ${ids.length} 条记录吗？删除后不可恢复。\n\n记录编号：${idStr}`,
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
    const res = await batchDeleteAccessRecords(ids)
    if (res.code === 200) {
      ElMessage.success(`已删除 ${ids.length} 条记录`)
      selectedRows.value = []
      loadRecords()
    } else {
      ElMessage.error(res.msg || '批量删除失败')
    }
  } catch (error) {
    console.error('批量删除通行记录失败：', error)
    ElMessage.error('批量删除失败')
  } finally {
    batchDeleting.value = false
  }
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

// 页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
}

// 监听页码变化，确保不越界
watch(recordList, (list) => {
  const maxPage = Math.max(1, Math.ceil(list.length / pageSize.value))
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage
  }
})

// 初始化：恢复筛选状态并加载记录
onMounted(() => {
  // 恢复筛选状态
  const savedFilter = sessionStorage.getItem('accessRecordFilter')
  if (savedFilter) {
    try {
      const filterState = JSON.parse(savedFilter)
      keyword.value = filterState.keyword || ''
      dateRange.value = filterState.dateRange || []
      accessType.value = filterState.accessType !== undefined ? filterState.accessType : null
      verifyMethod.value = filterState.verifyMethod !== undefined ? filterState.verifyMethod : null
      emergencyOnly.value = filterState.emergencyOnly || false
      currentPage.value = filterState.currentPage || 1
      pageSize.value = filterState.pageSize || 10
    } catch (e) {
      console.error('恢复筛选条件失败：', e)
    }
    // 清除 sessionStorage 中的对应项，避免刷新页面时重复恢复
    sessionStorage.removeItem('accessRecordFilter')
  }

  loadRecords()
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

.status-success {
  color: #67c23a;
}

.status-warn {
  color: #e6a23c;
}

.status-danger {
  color: #f56c6c;
}

.panel-card {
  border-radius: 12px;
}

.search-form {
  margin-bottom: 16px;
}

.search-input {
  width: 280px;
}

.date-range {
  width: 280px;
  max-width: 100%;
}

.type-select {
  width: 120px;
}

.batch-toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-start;
}

.record-table {
  cursor: pointer;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1200px) {
  .search-input,
  .date-range,
  .type-select {
    width: 100%;
  }

  .pagination-container {
    justify-content: flex-start;
  }
}
</style>
