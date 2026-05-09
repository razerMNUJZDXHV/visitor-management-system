<template>
  <div class="page-container detail-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">通行记录详情</h2>
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
                <div class="overview-label">记录编号</div>
                <div class="overview-value">#{{ record.logId || '—' }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">通行类型</div>
                <div class="overview-value">
                  <el-tag :type="record.accessType === 1 ? 'success' : 'warning'" effect="light">
                    {{ record.accessType === 1 ? '签到' : '签离' }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div class="info-section">
              <div class="section-title">访客信息</div>
              <el-descriptions :column="2" border class="detail-descriptions compact-descriptions">
                <el-descriptions-item label="访客ID">{{ record.visitorId || '—' }}</el-descriptions-item>
                <el-descriptions-item label="访客姓名">{{ record.visitorName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="手机号">{{ record.visitorPhone || '—' }}</el-descriptions-item>
                <el-descriptions-item label="身份证号">{{ record.visitorIdCard || '—' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="info-section">
              <div class="section-title">安保信息</div>
              <el-descriptions :column="2" border class="detail-descriptions compact-descriptions">
                <el-descriptions-item label="安保ID">{{ record.securityId || '—' }}</el-descriptions-item>
                <el-descriptions-item label="安保姓名">{{ record.securityName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="安保手机号">{{ record.securityPhone || '—' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="info-section">
              <div class="section-title">授权审批人信息</div>
              <el-descriptions :column="2" border class="detail-descriptions compact-descriptions">
                <el-descriptions-item label="审批人ID">{{ record.authorizerId || '—' }}</el-descriptions-item>
                <el-descriptions-item label="审批人姓名">{{ record.authorizerName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="审批人手机号">{{ record.authorizerPhone || '—' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="info-section">
              <div class="section-title">通行信息</div>
              <el-descriptions :column="2" border class="detail-descriptions compact-descriptions">
                <el-descriptions-item label="来访事由">{{ record.visitReason || '—' }}</el-descriptions-item>
                <el-descriptions-item label="核验方式">
                  <el-tag :type="record.verifyMethod === 1 ? '' : 'warning'" effect="light">
                    {{ record.verifyMethod === 1 ? '扫码' : '手动' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="紧急通行">
                  <el-tag v-if="record.emergency" type="danger" effect="light">是</el-tag>
                  <span v-else>否</span>
                </el-descriptions-item>
                <el-descriptions-item label="预计到达时间">{{ formatDateTime(record.expectedStartTime) }}</el-descriptions-item>
                <el-descriptions-item label="预计离开时间">{{ formatDateTime(record.expectedEndTime) }}</el-descriptions-item>
                <el-descriptions-item label="通行时间">{{ formatDateTime(record.accessTime) }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card class="side-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>操作</span>
              </div>
            </template>

            <div class="side-body">
              <div class="meta-block status-block">
                <div class="meta-label">通行类型</div>
                <div class="meta-value">
                  <el-tag :type="record.accessType === 1 ? 'success' : 'warning'" effect="light">
                    {{ record.accessType === 1 ? '签到' : '签离' }}
                  </el-tag>
                </div>
              </div>

              <div class="meta-block">
                <div class="meta-label">记录编号</div>
                <div class="meta-value">#{{ record.logId || '—' }}</div>
              </div>

              <div class="meta-block">
                <div class="meta-label">通行时间</div>
                <div class="meta-value">{{ formatDateTime(record.accessTime) }}</div>
              </div>

              <template v-if="canDelete">
                <el-alert
                  type="warning"
                  :closable="false"
                  show-icon
                  title="该记录关联的预约已完成，可以删除。"
                  class="operate-alert"
                />
                <el-button type="danger" plain :loading="deleting" @click="handleDelete">删除记录</el-button>
              </template>

              <template v-else>
                <el-result
                  icon="info"
                  title="不可删除"
                  sub-title="仅关联预约已完成的记录可删除"
                />
              </template>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAccessRecord, fetchAccessRecordDetail } from '../api/access'
import { formatDateTime } from '../utils/appointment'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const deleting = ref(false)
const record = ref({})

const canDelete = computed(() => Number(record.value.appointmentStatus) === 5)

const loadDetail = async () => {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('记录ID缺失')
    goBack()
    return
  }

  loading.value = true
  try {
    const res = await fetchAccessRecordDetail(id)
    if (res.code === 200) {
      record.value = res.data || {}
    } else {
      ElMessage.error(res.msg || '加载详情失败')
    }
  } catch (error) {
    console.error('加载通行记录详情失败：', error)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(`确认删除记录 #${record.value.logId} 吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  deleting.value = true
  try {
    const res = await deleteAccessRecord(record.value.logId)
    if (res.code === 200) {
      ElMessage.success('记录已删除')
      goBack()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    console.error('删除通行记录失败：', error)
    ElMessage.error('删除失败')
  } finally {
    deleting.value = false
  }
}

const goBack = () => {
  router.push('/access-record')
}

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

.side-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
}

.meta-block {
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  background: #fafafa;
}

.meta-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.meta-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.status-block .meta-value {
  display: flex;
  align-items: center;
}

.operate-alert {
  margin-top: 4px;
}

@media (max-width: 1100px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
