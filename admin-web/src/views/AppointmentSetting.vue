<template>
  <div class="page-container appointment-setting-page">
    <div class="page-header">
      <h2 class="page-title">预约时段设置</h2>
    </div>

    <el-row :gutter="16" class="summary-grid">
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">今日预约状态</div>
          <div class="summary-value" :class="todayStatusClass">{{ todayStatusText }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">规则数量</div>
          <div class="summary-value">{{ form.rules.length }} 条</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">更新时间</div>
          <div class="summary-value update-text">{{ updatedTimeText }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="form-card" shadow="never">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="130px" class="setting-form">
        <el-form-item label="每日预约人数上限" prop="dailyLimit">
          <el-input-number
            v-model="form.dailyLimit"
            :min="0"
            :max="9999"
            controls-position="right"
            placeholder="不填或填0表示不限制"
            style="width: 200px"
          />
          <span class="field-hint">（0 或不填表示不限制）</span>
        </el-form-item>

        <el-form-item label="提示文案" prop="notice">
          <el-input
            v-model="form.notice"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="预约关闭时在访客端展示的补充提示"
          />
        </el-form-item>
      </el-form>

      <div class="rule-toolbar">
        <span class="rule-title">按日期配置预约规则</span>
        <el-button type="primary" plain @click="addRule">新增规则</el-button>
      </div>

      <el-table :data="form.rules" border stripe empty-text="暂无规则，默认所有日期开放预约" class="rules-table">
        <el-table-column label="日期范围" width="280" align="center">
          <template #default="scope">
            <el-date-picker
              v-model="scope.row.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 240px"
            />
          </template>
        </el-table-column>

        <el-table-column label="时间段" min-width="240" align="center">
          <template #default="scope">
            <el-time-picker
              v-model="scope.row.timeRange"
              is-range
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              format="HH:mm"
              value-format="HH:mm"
              style="width: 220px"
            />
          </template>
        </el-table-column>

        <el-table-column label="是否开放" width="160" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.open" active-text="开放" inactive-text="关闭" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button type="danger" link @click="removeRule(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="form-actions">
        <el-button :disabled="loading || saving" @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import axios from '../api/axios'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const updatedTimeText = ref('—')

const form = reactive({
  notice: '',
  dailyLimit: 0,
  rules: []
})

const formRules = {
  notice: [
    { max: 300, message: '提示文案不能超过300个字符', trigger: 'blur' }
  ]
}

const createEmptyRule = () => ({
  dateRange: ['', ''],
  timeRange: ['00:00', '23:59'],
  open: false
})

const isDateInRange = (dateText, dateRange) => {
  if (!Array.isArray(dateRange) || dateRange.length !== 2 || !dateRange[0] || !dateRange[1]) {
    return false
  }
  return dateText >= dateRange[0] && dateText <= dateRange[1]
}

const toMinutes = (timeText) => {
  const [hours, minutes] = (timeText || '').split(':')
  return Number(hours) * 60 + Number(minutes)
}

const todayStatusText = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const todayText = `${year}-${month}-${day}`
  const currentMinutes = now.getHours() * 60 + now.getMinutes()

  const todayRules = form.rules
    .filter(item => isDateInRange(todayText, item.dateRange))
    .map(item => ({
      open: item.open,
      start: toMinutes(item.timeRange?.[0]),
      end: toMinutes(item.timeRange?.[1])
    }))
    .sort((a, b) => a.start - b.start)

  if (todayRules.length === 0) {
    return '默认开放'
  }

  const hitRule = todayRules.find(item => currentMinutes >= item.start && currentMinutes < item.end)
  if (!hitRule) {
    return '当前关闭'
  }
  return hitRule.open ? '当前开放' : '当前关闭'
})

const todayStatusClass = computed(() => {
  if (todayStatusText.value.includes('开放')) {
    return 'status-on'
  }
  if (todayStatusText.value.includes('关闭')) {
    return 'status-off'
  }
  return ''
})

const formatDateTime = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const addRule = () => {
  form.rules.push(createEmptyRule())
}

const removeRule = (index) => {
  form.rules.splice(index, 1)
}

const validateRules = () => {
  for (let i = 0; i < form.rules.length; i++) {
    const item = form.rules[i]
    if (!Array.isArray(item.dateRange) || item.dateRange.length !== 2 || !item.dateRange[0] || !item.dateRange[1]) {
      ElMessage.error(`第 ${i + 1} 条规则缺少完整日期范围`)
      return false
    }
    if (item.dateRange[1] < item.dateRange[0]) {
      ElMessage.error(`第 ${i + 1} 条规则结束日期必须不早于开始日期`)
      return false
    }
    if (!Array.isArray(item.timeRange) || item.timeRange.length !== 2 || !item.timeRange[0] || !item.timeRange[1]) {
      ElMessage.error(`第 ${i + 1} 条规则缺少完整时间段`)
      return false
    }
    const start = toMinutes(item.timeRange[0])
    const end = toMinutes(item.timeRange[1])
    if (Number.isNaN(start) || Number.isNaN(end)) {
      ElMessage.error(`第 ${i + 1} 条规则时间格式不正确`)
      return false
    }
    if (end <= start) {
      ElMessage.error(`第 ${i + 1} 条规则结束时间必须晚于开始时间`)
      return false
    }
  }

  // 检查规则重叠
  for (let i = 0; i < form.rules.length; i++) {
    for (let j = i + 1; j < form.rules.length; j++) {
      const a = form.rules[i]
      const b = form.rules[j]
      // 日期范围是否相交
      const dateOverlap = !(a.dateRange[1] < b.dateRange[0] || b.dateRange[1] < a.dateRange[0])
      // 时间范围是否相交
      const timeOverlap = !(toMinutes(a.timeRange[1]) <= toMinutes(b.timeRange[0]) || toMinutes(b.timeRange[1]) <= toMinutes(a.timeRange[0]))
      if (dateOverlap && timeOverlap) {
        ElMessage.error(`第 ${i + 1} 条规则与第 ${j + 1} 条规则存在重叠，请调整后再保存`)
        return false
      }
    }
  }

  return true
}

const loadSetting = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/admin/appointment-setting')
    if (res.code === 200) {
      const data = res.data || {}
      form.notice = data.notice || ''
      form.dailyLimit = data.dailyLimit || 0
      form.rules = (data.rules || []).map(item => ({
        dateRange: [item.startDate || '', item.endDate || ''],
        timeRange: [item.startTime || '08:00', item.endTime || '18:00'],
        open: item.open !== false
      }))
      updatedTimeText.value = formatDateTime(data.updatedTime)
      formRef.value?.clearValidate()
    } else {
      ElMessage.error(res.msg || '加载预约设置失败')
    }
  } catch (error) {
    console.error('加载预约设置失败：', error)
    ElMessage.error('加载预约设置失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  loadSetting()
}

const handleSave = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!validateRules()) return

  const saveRules = form.rules.map(item => ({
    startDate: item.dateRange[0],
    endDate: item.dateRange[1],
    startTime: item.timeRange[0],
    endTime: item.timeRange[1],
    open: item.open
  }))

  saving.value = true
  try {
    const res = await axios.put('/api/admin/appointment-setting', {
      notice: form.notice,
      dailyLimit: form.dailyLimit || 0,
      rules: saveRules
    })
    if (res.code === 200) {
      ElMessage.success('预约设置已保存')
      await loadSetting()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存预约设置失败：', error)
    ElMessage.error('保存预约设置失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadSetting()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #303133;
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
}

.status-on {
  color: #67c23a;
}

.status-off {
  color: #f56c6c;
}

.window-text,
.update-text {
  font-size: 18px;
  color: #303133;
}

.form-card {
  border-radius: 12px;
}

.setting-form {
  max-width: 760px;
}

.field-hint {
  font-size: 13px;
  color: #909399;
  margin-left: 8px;
}

.rule-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0;
}

.rule-title {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.rules-table {
  margin-bottom: 20px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
