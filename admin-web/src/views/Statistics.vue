<template>
  <div class="page-container statistics-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">访客数据统计</h2>
      </div>
      <div class="header-actions">
        <el-button type="primary" plain :loading="loading" @click="loadStats">刷新</el-button>
      </div>
    </div>

    <div class="section-title">今日通行数据</div>
    <el-row :gutter="16" class="summary-grid">
      <el-col :xs="12" :sm="6">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">今日流量</div>
          <div class="summary-value">{{ stats.todayFlow }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">今日签到</div>
          <div class="summary-value status-success">{{ stats.todaySignIn }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">今日签离</div>
          <div class="summary-value status-warn">{{ stats.todaySignOut }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="summary-card" shadow="never">
          <div class="summary-label">今日紧急通行</div>
          <div class="summary-value status-danger">{{ stats.todayEmergency }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="section-title">预约状态分析</div>
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="panel-card chart-panel" shadow="never">
          <template #header>
            <span class="chart-title">预约状态分布</span>
          </template>
          <div class="chart-wrapper-small">
            <div v-loading="chartsLoading" class="chart-loading-overlay"></div>
            <div ref="statusChartRef" class="chart-box-small"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="panel-card chart-panel" shadow="never">
          <template #header>
            <span class="chart-title">紧急通行统计</span>
          </template>
          <div class="chart-wrapper-small">
            <div v-loading="chartsLoading" class="chart-loading-overlay"></div>
            <div ref="emergencyChartRef" class="chart-box-small"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="section-title">流量趋势分析</div>

    <el-card class="panel-card chart-panel" shadow="never">
      <template #header>
        <div class="chart-header">
          <div class="chart-title-area">
            <span class="chart-title">每日流量趋势</span>
            <span v-if="stats.peakHour" class="peak-tag">
              <el-tag type="warning" effect="light">高峰时段：{{ stats.peakHour }}</el-tag>
            </span>
          </div>
          <div class="chart-actions">
            <span class="action-label">统计天数：</span>
            <el-select v-model="days" class="days-select" @change="updateDailyChartOnly">
              <el-option :value="7" label="近7天" />
              <el-option :value="14" label="近14天" />
              <el-option :value="30" label="近30天" />
            </el-select>
          </div>
        </div>
      </template>
      <div class="chart-wrapper">
        <div v-loading="chartsLoading" class="chart-loading-overlay"></div>
        <div ref="dailyChartRef" class="chart-box"></div>
      </div>
    </el-card>

    <el-card class="panel-card chart-panel" shadow="never">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">时段流量分布</span>
          <el-radio-group v-model="hourlyType" size="small" @change="updateHourlyChart">
            <el-radio-button value="day">今日</el-radio-button>
            <el-radio-button value="week">本周</el-radio-button>
            <el-radio-button value="month">本月</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div class="chart-wrapper">
        <div v-loading="chartsLoading" class="chart-loading-overlay"></div>
        <div ref="hourlyChartRef" class="chart-box"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { fetchAdminStats } from '../api/stats'

const loading = ref(false)
const chartsLoading = ref(false)
const days = ref(7)
const hourlyType = ref('day') // 时段流量类型：day/week/month
const stats = ref({
  todayFlow: 0,
  todaySignIn: 0,
  todaySignOut: 0,
  todayEmergency: 0,
  totalAppointments: 0,
  pendingCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
  canceledCount: 0,
  checkedInCount: 0,
  completedCount: 0,
  expiredCount: 0,
  totalEmergency: 0,
  totalNonEmergency: 0,
  peakHour: '',
  dailyFlow: [],
  hourlyFlow: [],
  weeklyHourlyFlow: [],
  monthlyHourlyFlow: [],
  allHourlyFlow: []
})

const dailyChartRef = ref(null)
const statusChartRef = ref(null)
const hourlyChartRef = ref(null)
const emergencyChartRef = ref(null)
let dailyChart = null
let statusChart = null
let hourlyChart = null
let emergencyChart = null

const loadStats = async () => {
  loading.value = true
  chartsLoading.value = true
  try {
    const res = await fetchAdminStats(days.value)
    if (res.code === 200) {
      stats.value = res.data || stats.value
      await nextTick()
      updateCharts()
    } else {
      ElMessage.error(res.msg || '加载统计数据失败')
    }
  } catch (error) {
    console.error('加载统计数据失败：', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
    // 延迟关闭图表加载状态，避免闪烁
    setTimeout(() => {
      chartsLoading.value = false
    }, 300)
  }
}

const initDailyChart = () => {
  if (!dailyChartRef.value) return
  dailyChart = echarts.init(dailyChartRef.value)
}

const initStatusChart = () => {
  if (!statusChartRef.value) return
  statusChart = echarts.init(statusChartRef.value)
}

const initEmergencyChart = () => {
  if (!emergencyChartRef.value) return
  emergencyChart = echarts.init(emergencyChartRef.value)
}

const initHourlyChart = () => {
  if (!hourlyChartRef.value) return
  hourlyChart = echarts.init(hourlyChartRef.value)
}

const updateDailyChart = () => {
  if (!dailyChart) return
  const flow = stats.value.dailyFlow || []
  const labels = flow.map(item => item.label)
  const values = flow.map(item => item.value)

  dailyChart.setOption({
    backgroundColor: '#fff',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#606266', fontSize: 12 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } },
      axisLabel: { color: '#909399', fontSize: 12 }
    },
    series: [
      {
        name: '访客流量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        showSymbol: false,
        data: values,
        lineStyle: { color: '#409eff', width: 3 },
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.35)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        emphasis: {
          focus: 'series',
          itemStyle: { borderColor: '#409eff', borderWidth: 2 }
        }
      }
    ]
  })
}

const updateStatusChart = () => {
  if (!statusChart) return
  const {
    pendingCount,
    approvedCount,
    rejectedCount,
    canceledCount,
    checkedInCount,
    completedCount,
    expiredCount
  } = stats.value

  const total = pendingCount + approvedCount + rejectedCount + canceledCount + checkedInCount + completedCount + expiredCount

  statusChart.setOption({
    backgroundColor: '#fff',
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: {
      orient: 'horizontal',
      bottom: '0%',
      left: 'center',
      textStyle: {
        fontSize: 12,
        color: '#606266'
      },
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: true,
          formatter: '{b}\n{c}',
          fontSize: 12,
          lineHeight: 18
        },
        labelLine: {
          length: 15,
          length2: 10
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          }
        },
        data: [
          { value: pendingCount, name: '待审批', itemStyle: { color: '#e6a23c' } },
          { value: approvedCount, name: '已通过', itemStyle: { color: '#67c23a' } },
          { value: checkedInCount, name: '已签到', itemStyle: { color: '#409eff' } },
          { value: completedCount, name: '已完成', itemStyle: { color: '#2f9d9a' } },
          { value: rejectedCount, name: '已拒绝', itemStyle: { color: '#f56c6c' } },
          { value: canceledCount, name: '已取消', itemStyle: { color: '#909399' } },
          { value: expiredCount, name: '已过期', itemStyle: { color: '#a65b17' } }
        ]
      },
      {
        type: 'pie',
        radius: ['0%', '0%'],
        center: ['50%', '50%'],
        label: {
          show: true,
          position: 'center',
          formatter: '{total|' + total + '}',
          rich: {
            total: {
              fontSize: 24,
              fontWeight: 'bold',
              color: '#303133',
              lineHeight: 1.2
            }
          }
        },
        data: [{ value: 0, name: '' }],
        tooltip: { show: false },
        itemStyle: { color: 'none' },
        emphasis: { disabled: true }
      }
    ]
  })
}

const updateEmergencyChart = () => {
  if (!emergencyChart) return
  const emergency = stats.value.totalEmergency || 0
  const nonEmergency = stats.value.totalNonEmergency || 0
  const total = emergency + nonEmergency

  emergencyChart.setOption({
    backgroundColor: '#fff',
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: {
      orient: 'horizontal',
      bottom: '0%',
      left: 'center',
      textStyle: {
        fontSize: 12,
        color: '#606266'
      },
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: true,
          formatter: '{b}\n{c}',
          fontSize: 12,
          lineHeight: 18
        },
        labelLine: {
          length: 15,
          length2: 10
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          }
        },
        data: [
          { value: emergency, name: '紧急通行', itemStyle: { color: '#f56c6c' } },
          { value: nonEmergency, name: '非紧急通行', itemStyle: { color: '#67c23a' } }
        ]
      },
      {
        type: 'pie',
        radius: ['0%', '0%'],
        center: ['50%', '50%'],
        label: {
          show: true,
          position: 'center',
          formatter: '{total|' + total + '}',
          rich: {
            total: {
              fontSize: 28,
              fontWeight: 'bold',
              color: '#303133',
              lineHeight: 1.2
            }
          }
        },
        data: [{ value: 0, name: '' }],
        tooltip: { show: false },
        itemStyle: { color: 'none' },
        emphasis: { disabled: true }
      }
    ]
  })
}

const updateHourlyChart = () => {
  if (!hourlyChart) return

  // 根据选择类型获取数据
  let flow = []
  if (hourlyType.value === 'day') {
    flow = stats.value.hourlyFlow || []
  } else if (hourlyType.value === 'week') {
    flow = stats.value.weeklyHourlyFlow || []
  } else if (hourlyType.value === 'month') {
    flow = stats.value.monthlyHourlyFlow || []
  } else {
    flow = stats.value.allHourlyFlow || []
  }

  const labels = flow.map(item => item.label)
  const values = flow.map(item => item.value)

  hourlyChart.setOption({
    backgroundColor: '#fff',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#606266', fontSize: 12 },
      axisTick: { alignWithLabel: true, show: false }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } },
      axisLabel: { color: '#909399', fontSize: 12 }
    },
    series: [
      {
        name: '访客流量',
        type: 'bar',
        barWidth: '60%',
        data: values,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' }
          ]),
          borderRadius: [6, 6, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#337ecc' },
              { offset: 1, color: '#529bff' }
            ])
          }
        }
      }
    ]
  })
}

const updateCharts = () => {
  updateDailyChart()
  updateStatusChart()
  updateEmergencyChart()
  updateHourlyChart()
}

const updateDailyChartOnly = async () => {
  loading.value = true
  chartsLoading.value = true
  try {
    const res = await fetchAdminStats(days.value)
    if (res.code === 200) {
      // 只更新 dailyFlow 和 peakHour
      stats.value.dailyFlow = res.data.dailyFlow || []
      stats.value.peakHour = res.data.peakHour || ''
      await nextTick()
      updateDailyChart()
    } else {
      ElMessage.error(res.msg || '加载统计数据失败')
    }
  } catch (error) {
    console.error('加载统计数据失败：', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
    setTimeout(() => {
      chartsLoading.value = false
    }, 300)
  }
}

const handleResize = () => {
  dailyChart?.resize()
  statusChart?.resize()
  emergencyChart?.resize()
  hourlyChart?.resize()
}

onMounted(() => {
  initDailyChart()
  initStatusChart()
  initEmergencyChart()
  initHourlyChart()
  loadStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  dailyChart?.dispose()
  statusChart?.dispose()
  emergencyChart?.dispose()
  hourlyChart?.dispose()
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

.action-label {
  font-size: 14px;
  color: #606266;
}

.days-select {
  width: 120px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 24px 0 16px 0;
  padding-left: 12px;
  border-left: 4px solid #409eff;
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

.status-info {
  color: #409eff;
}

.status-muted {
  color: #909399;
}

.status-expire {
  color: #a65b17;
}

.summary-completed {
  color: #2f9d9a;
}

.chart-row {
  margin-bottom: 20px;
}

.panel-card {
  border-radius: 12px;
}

.chart-panel {
  margin-bottom: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.chart-title-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.peak-tag {
  margin-left: 12px;
}

.chart-box {
  width: 100%;
  height: 350px;
}

.chart-wrapper {
  position: relative;
  width: 100%;
  height: 350px;
}

.chart-wrapper .chart-box {
  height: 100%;
}

.chart-wrapper-small {
  position: relative;
  width: 100%;
  height: 280px;
}

.chart-wrapper-small .chart-box-small {
  height: 100%;
}

@media (max-width: 1200px) {
  .header-actions {
    width: 100%;
  }

  .chart-box {
    height: 280px;
  }
}
</style>
