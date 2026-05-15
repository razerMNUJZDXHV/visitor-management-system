<template>
  <div class="page-container detail-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ pageTitle }}</h2>
      </div>
      <div class="page-header-actions">
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="10" animated />

    <template v-else>
      <el-row :gutter="16" class="detail-layout">
        <el-col :xs="24" :lg="16">
          <el-card class="main-card" shadow="never">
            <div class="overview-grid">
              <div class="overview-item">
                <div class="overview-label">预约ID</div>
                <div class="overview-value">#{{ appointment.appointmentId || '—' }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">当前状态</div>
                <div class="overview-value">
                  <el-tag :type="statusTagType" effect="light">{{ statusText }}</el-tag>
                </div>
              </div>
            </div>

            <div class="info-section">
              <div class="section-title">访客信息</div>
              <el-descriptions :column="2" border class="detail-descriptions compact-descriptions">
                <el-descriptions-item label="访客ID">{{ appointment.visitorId || '—' }}</el-descriptions-item>
                <el-descriptions-item label="访客姓名">{{ appointment.visitorName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="手机号">{{ appointment.visitorPhone || '—' }}</el-descriptions-item>
                <el-descriptions-item label="身份证号">{{ appointment.visitorIdCard || '—' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="info-section">
              <div class="section-title">审批人信息</div>
              <el-descriptions :column="2" border class="detail-descriptions compact-descriptions">
                <el-descriptions-item label="审批人ID">{{ appointment.approverId || '—' }}</el-descriptions-item>
                <el-descriptions-item label="审批人姓名">{{ appointment.approverName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="审批人手机号">{{ appointment.approverPhone || '—' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="info-section">
              <div class="section-title">预约信息</div>
              <el-descriptions :column="2" border class="detail-descriptions compact-descriptions">
                <el-descriptions-item label="被访人">{{ appointment.intervieweeName || '未填写' }}</el-descriptions-item>
                <el-descriptions-item label="来访事由">{{ appointment.visitReason || '—' }}</el-descriptions-item>
                <el-descriptions-item label="申请时间">{{ formatDateTime(appointment.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="审批时间">{{ formatDateTime(appointment.processTime) }}</el-descriptions-item>
                <el-descriptions-item label="预计到达时间">{{ formatDateTime(appointment.expectedStartTime) }}</el-descriptions-item>
                <el-descriptions-item label="预计离开时间">{{ formatDateTime(appointment.expectedEndTime) }}</el-descriptions-item>
                <el-descriptions-item label="二维码失效时间">{{ formatDateTime(appointment.qrExpireTime) }}</el-descriptions-item>
                <el-descriptions-item label="拒绝理由">{{ appointment.rejectReason || '—' }}</el-descriptions-item>
                <el-descriptions-item label="通行码" :span="2">
                  <div v-if="appointment.qrCodeUrl" class="qrcode-wrapper">
                    <el-image
                      :src="appointment.qrCodeUrl"
                      :preview-src-list="[appointment.qrCodeUrl]"
                      fit="contain"
                      class="qrcode-image"
                    />
                  </div>
                  <span v-else>—</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <el-alert
              v-if="appointment.status === 2 && appointment.rejectReason"
              class="reject-alert"
              type="error"
              :closable="false"
              show-icon
              title="拒绝理由"
              :description="appointment.rejectReason"
            />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card class="side-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>审批操作</span>
              </div>
            </template>

            <div class="side-body">
              <template v-if="canOperate">
                <el-alert
                  type="warning"
                  :closable="false"
                  show-icon
                  title="拒绝需要填写理由后再提交。"
                  class="operate-alert"
                />

                <div v-if="showRejectInput" class="reject-box">
                  <el-input
                    v-model="rejectReason"
                    type="textarea"
                    :rows="5"
                    maxlength="300"
                    show-word-limit
                    placeholder="请输入拒绝理由"
                  />
                  <div class="reject-actions">
                    <el-button @click="cancelReject">取消</el-button>
                    <el-button type="danger" :loading="submitting" @click="submitReject">确认拒绝</el-button>
                  </div>
                </div>

                <div v-else class="action-buttons">
                  <el-button type="danger" plain @click="handleReject">拒绝</el-button>
                  <el-button type="primary" :loading="submitting" @click="handleApprove">同意</el-button>
                </div>
              </template>

              <template v-else>
                <template v-if="canDeleteRecord">
                  <el-button type="danger" plain :loading="submitting" @click="handleDeleteRecord">
                    删除记录
                  </el-button>
                  <el-result
                    icon="success"
                    title="记录已处理"
                    sub-title="当前记录已不可继续审批"
                  />
                </template>
                <template v-else>
                  <el-result
                    icon="info"
                    title="不可删除"
                    sub-title="访客已签到但未签离，完成签离后才可删除"
                  />
                </template>
              </template>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
// 审批详情：展示预约信息并提供审批操作。
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveAppointment,
  deleteApprovalRecord,
  fetchApprovalDetail,
  rejectAppointment
} from '../api/approval'
import { formatDateTime, getApprovalStatusText } from '../utils/appointment'

const route = useRoute()
const router = useRouter()

// 详情加载与操作状态
const loading = ref(false)
const submitting = ref(false)
const appointment = ref({})
const showRejectInput = ref(false)
const rejectReason = ref('')

// 页面模式：历史只读/待审批可操作
const isHistoryMode = computed(() => route.query.mode === 'history')
// 是否允许审批操作
const canOperate = computed(() => !isHistoryMode.value && Number(appointment.value.status) === 0)
// 是否允许删除记录
const canDeleteRecord = computed(() => {
  const status = Number(appointment.value.status)
  if (![2, 3, 5, 6].includes(status)) return false
  // 仅滞留超时（已过期且已签到但未签离）禁止删除
  if (status === 6 && appointment.value.overtimeStaying === true) return false
  return true
})
// 页面标题
const pageTitle = computed(() => '审批详情')
// 状态文案与标签类型
const statusText = computed(() => getApprovalStatusText(appointment.value.status, isHistoryMode.value ? 'history' : 'pending', appointment.value))
const statusTagType = computed(() => {
  const status = Number(appointment.value.status)
  if (status === 2) return 'danger'
  if (status === 1) return 'success'
  if (status === 5) return 'success'
  if (status === 6) return 'warning'
  if (status === 3) return 'info'
  if (status === 0) return 'warning'
  return 'info'
})

// 计算返回路径：优先使用来源路由
const getReturnPath = () => {
  const from = route.query.from
  if (typeof from === 'string' && from.startsWith('/')) {
    return from
  }
  return isHistoryMode.value ? '/approval-center?tab=history' : '/approval-center?tab=pending'
}

// 返回列表页
const navigateBack = () => {
  router.push(getReturnPath())
}

// 加载详情
const loadDetail = async () => {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('预约ID缺失')
    navigateBack()
    return
  }

  loading.value = true
  showRejectInput.value = false
  rejectReason.value = ''

  try {
    const res = await fetchApprovalDetail(id)
    if (res.code === 200) {
      appointment.value = res.data || {}
    } else {
      ElMessage.error(res.msg || '加载详情失败')
    }
  } catch (error) {
    console.error('加载审批详情失败：', error)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

// 审批通过
const handleApprove = async () => {
  try {
    await ElMessageBox.confirm('确定通过该预约申请吗？', '确认通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    const res = await approveAppointment(appointment.value.appointmentId)
    if (res.code === 200) {
      ElMessage.success('审批通过')
      navigateBack()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    console.error('审批通过失败：', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// 点击拒绝按钮（首次弹出输入框）
const handleReject = () => {
  if (!showRejectInput.value) {
    showRejectInput.value = true
    return
  }

  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝理由')
    return
  }

  submitReject()
}

// 取消拒绝操作
const cancelReject = () => {
  showRejectInput.value = false
  rejectReason.value = ''
}

// 提交拒绝
const submitReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝理由')
    return
  }

  submitting.value = true
  try {
    const res = await rejectAppointment(appointment.value.appointmentId, rejectReason.value.trim())
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      navigateBack()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    console.error('审批拒绝失败：', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// 返回按钮
const goBack = () => {
  navigateBack()
}

// 删除记录
const handleDeleteRecord = async () => {
  try {
    await ElMessageBox.confirm('确认删除当前记录吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    const res = await deleteApprovalRecord(appointment.value.appointmentId)
    if (res.code === 200) {
      ElMessage.success('记录已删除')
      navigateBack()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    console.error('删除审批记录失败：', error)
    ElMessage.error('删除失败')
  } finally {
    submitting.value = false
  }
}

// 路由变化时重新加载详情
watch(
  () => route.fullPath,
  () => {
    loadDetail()
  }
)

// 初始化加载
onMounted(() => {
  loadDetail()
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

.page-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: bold;
  color: #303133;
}

.detail-layout {
  align-items: stretch;
}

.main-card,
.side-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 600;
  color: #303133;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.overview-item {
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  background: #fafafa;
}

.overview-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.overview-value {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.detail-descriptions {
  margin-top: 8px;
}

.compact-descriptions :deep(.el-descriptions__label) {
  width: 110px;
  white-space: nowrap;
}

.compact-descriptions :deep(.el-descriptions__content) {
  word-break: break-word;
}

.info-section + .info-section {
  margin-top: 18px;
}

.section-title {
  margin: 14px 0 10px;
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.qrcode-wrapper {
  display: inline-flex;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.qrcode-image {
  width: 170px;
  height: 170px;
}

.reject-alert {
  margin-top: 18px;
}

.side-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
}

.reject-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reject-actions,
.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
