<template>
  <div class="dashboard-container">
    <div v-if="isLoggedIn" class="cockpit-container">
      <div class="page-header">
        <div>
          <h2 class="page-title">管理工作台</h2>
          <div class="page-subtitle">
            <span class="subtitle-name">欢迎回来，{{ displayName }}</span>
            <span class="subtitle-sep">·</span>
            <span>最近更新 {{ lastUpdatedText }}</span>
          </div>
        </div>
        <div class="header-actions">
          <el-button type="primary" plain :loading="loadingStats || loadingPending" @click="reloadDashboard">刷新</el-button>
        </div>
      </div>

      <div class="section-header">
        <h3 class="section-title">关键指标概览</h3>
      </div>

      <el-row :gutter="12" class="summary-grid">
        <el-col :xs="12" :sm="8" :lg="4">
          <el-card class="summary-card cockpit-card" shadow="never">
            <div class="summary-label">今日流量</div>
            <div class="summary-value">{{ stats.todayFlow }}</div>
            <div class="summary-note">含签到与签离</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <el-card class="summary-card cockpit-card" shadow="never">
            <div class="summary-label">今日签到</div>
            <div class="summary-value status-success">{{ stats.todaySignIn }}</div>
            <div class="summary-note">已完成核验</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <el-card class="summary-card cockpit-card" shadow="never">
            <div class="summary-label">今日签离</div>
            <div class="summary-value status-info">{{ stats.todaySignOut }}</div>
            <div class="summary-note">已安全离校</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <el-card class="summary-card cockpit-card" shadow="never">
            <div class="summary-label">紧急通行</div>
            <div class="summary-value status-danger">{{ stats.todayEmergency }}</div>
            <div class="summary-note">需重点复核</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <el-card class="summary-card cockpit-card" shadow="never">
            <div class="summary-label">今日待审批</div>
            <div class="summary-value status-warn">{{ pendingCount }}</div>
            <div class="summary-note">需要优先处理</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <el-card class="summary-card cockpit-card" shadow="never">
            <div class="summary-label">今日预约</div>
            <div class="summary-value">{{ stats.todayAppointments || 0 }}</div>
            <div class="summary-note">新增预约</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="panel-grid">
        <el-col :xs="24" :lg="8">
          <el-card class="panel-card cockpit-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>待办提醒</span>
                <el-button type="primary" link @click="goQuick('/approval-center?tab=pending')">查看全部</el-button>
              </div>
            </template>
            <div class="todo-list">
              <div v-for="item in todoItems" :key="item.label" class="todo-item">
                <div class="todo-left">
                  <el-tag :type="item.tagType" effect="light">{{ item.label }}</el-tag>
                  <span v-if="item.tip" class="todo-tip">{{ item.tip }}</span>
                </div>
                <el-button v-if="item.route" type="primary" link @click="goQuick(item.route)">前往</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card class="panel-card cockpit-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>快捷入口</span>
                <span class="card-subtitle">一键进入高频模块</span>
              </div>
            </template>
            <div class="quick-grid">
              <div
                v-for="link in quickLinks"
                :key="link.title"
                class="quick-item"
                role="button"
                tabindex="0"
                @click="goQuick(link.route)"
                @keyup.enter="goQuick(link.route)"
              >
                <div class="quick-title">{{ link.title }}</div>
                <div class="quick-desc">{{ link.desc }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card class="panel-card cockpit-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>异常告警</span>
                <span class="card-subtitle">自动识别风险波动</span>
              </div>
            </template>
            <div class="alert-list">
              <div v-for="alert in alertItems" :key="alert.title" class="alert-item" :class="`alert-${alert.level}`">
                <div class="alert-title">{{ alert.title }}</div>
                <div class="alert-desc">{{ alert.desc }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="panel-card cockpit-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>待审批清单</span>
            <el-button type="primary" link @click="goQuick('/approval-center?tab=pending')">查看全部</el-button>
          </div>
        </template>
        <el-skeleton v-if="loadingPending" :rows="4" animated />
        <template v-else>
          <div v-if="pendingPreview.length === 0" class="empty-block">
            <el-empty description="暂无待审批申请" />
          </div>
          <el-table
            v-else
            :data="pendingPreview"
            stripe
            class="pending-table"
            @row-click="goPendingDetail"
          >
            <el-table-column prop="appointmentId" label="编号" width="100" align="center" />
            <el-table-column prop="visitorName" label="访客姓名" min-width="120" />
            <el-table-column prop="intervieweeName" label="被访人" min-width="120" />
            <el-table-column prop="createTimeText" label="申请时间" min-width="170" />
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button type="primary" link @click.stop="goPendingDetail(scope.row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-card>
    </div>

    <div v-else class="login-container">
      <div class="login-box">
        <h2 class="login-title">欢迎使用应急管理大学访客预约与准入管理系统</h2>
        
        <div class="btn-group">
          <el-button 
            type="primary" 
            :class="{ active: showLogin }"
            @click="showLogin = true; showRegister = false"
          >
            登录
          </el-button>
          <el-button 
            type="success" 
            :class="{ active: showRegister }"
            @click="showRegister = true; showLogin = false"
          >
            注册
          </el-button>
        </div>

        <div v-if="showLogin" class="form-wrapper">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="80px">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="loginForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input type="password" v-model="loginForm.password" placeholder="请输入密码" />
            </el-form-item>
            <el-form-item class="form-buttons">
              <el-button type="primary" @click="handleLogin">登录</el-button>
              <el-button @click="resetLoginForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="showRegister" class="form-wrapper">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input type="password" v-model="registerForm.password" placeholder="密码需包含字母和数字，6-20位" />
            </el-form-item>
            <el-form-item class="form-buttons">
              <el-button type="primary" @click="handleRegister">注册</el-button>
              <el-button @click="resetRegisterForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 管理工作台：登录/注册入口与数据驾驶舱。
import { computed, inject, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../api/axios'
import { fetchPendingAppointments } from '../api/approval'
import { fetchAdminStats } from '../api/stats'
import { debounce } from '../utils/debounce'
import { formatDateTime, mapAppointment } from '../utils/appointment'
import { PHONE_REGEX, PASSWORD_REGEX } from '../utils/validators' 

// 路由实例
const router = useRouter()

// 共享登录态
const globalState = inject('globalState')
const isLoggedIn = globalState.isLoggedIn
const userInfo = globalState.userInfo

// 登录/注册面板切换
const showLogin = ref(true)
const showRegister = ref(false)

// 登录表单模型
const loginForm = reactive({
  phone: '',
  password: ''
})

// 注册表单模型
const registerForm = reactive({
  phone: '',
  realName: '',
  password: ''
})

// 表单实例引用
const loginFormRef = ref(null)
const registerFormRef = ref(null)

// 登录校验规则
const loginRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_REGEX, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 注册校验规则
const registerRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_REGEX, message: '手机号格式不正确', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: PASSWORD_REGEX, message: '密码必须包含字母和数字，长度6-20位', trigger: 'blur' }
  ]
}

// 登录提交（防抖，避免重复点击）
const handleLogin = debounce(async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const res = await axios.post('/api/admin/login', loginForm)
      if (res.code === 200) {
        ElMessage.success('登录成功')
        // 写入 token 与用户信息，供路由守卫和页面展示使用
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('userInfo', JSON.stringify(res.data))
        isLoggedIn.value = true
        userInfo.value = res.data
        router.push('/dashboard')
      } else {
        ElMessage.error(res.msg)
      }
    } catch (err) {
      console.error('登录失败详情：', err)
      ElMessage.error('登录失败，请检查网络或账号信息')
    }
  })
}, 500)

// 注册提交（防抖，避免重复点击）
const handleRegister = debounce(async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const res = await axios.post('/api/admin/register', registerForm)
      if (res.code === 200) {
        ElMessage.success('注册成功，请登录')
        resetRegisterForm()
        showRegister.value = false
        showLogin.value = true
      } else {
        ElMessage.error(res.msg)
      }
    } catch (err) {
      console.error('注册失败详情：', err)
      ElMessage.error('注册失败，请检查网络')
    }
  })
}, 500)

// 重置登录表单
const resetLoginForm = () => {
  loginForm.phone = ''
  loginForm.password = ''
  nextTick(() => {
    if (loginFormRef.value) {
      loginFormRef.value.resetFields()
    }
  })
}

// 重置注册表单
const resetRegisterForm = () => {
  registerForm.phone = ''
  registerForm.realName = ''
  registerForm.password = ''
  nextTick(() => {
    registerFormRef.value?.resetFields()
  })
}

// 统计与待办加载状态
const loadingStats = ref(false)
const loadingPending = ref(false)
// 最近更新时间
const lastUpdated = ref(null)
// 仪表盘统计数据
const stats = ref({
  todayFlow: 0,
  todaySignIn: 0,
  todaySignOut: 0,
  todayEmergency: 0,
  totalAppointments: 0,
  pendingCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
  completedCount: 0
})
// 待审批列表
const pendingList = ref([])

// 展示名称：优先真实姓名，其次账号信息
const displayName = computed(() => {
  const info = userInfo.value || {}
  return info.realName || info.name || info.username || info.phone || '管理员'
})

// 最近更新时间文案
const lastUpdatedText = computed(() => (lastUpdated.value ? formatDateTime(lastUpdated.value) : '—'))

// 待审批数量：优先使用统计值，兜底列表长度
const pendingCount = computed(() => {
  const statsCount = Number(stats.value.pendingCount || 0)
  return statsCount || pendingList.value.length
})

// 待审批预览：取前 5 条
const pendingPreview = computed(() => pendingList.value.slice(0, 5))

// 待办提示清单
const todoItems = computed(() => {
  const items = []
  if (pendingCount.value > 0) {
    items.push({
      label: `待审批 ${pendingCount.value} 条`,
      tagType: 'warning',
      route: '/approval-center?tab=pending',
      tip: '建议优先处理'
    })
  }
  if (stats.value.todayEmergency > 0) {
    items.push({
      label: `紧急通行 ${stats.value.todayEmergency} 起`,
      tagType: 'danger',
      route: '/access-record',
      tip: '请及时关注'
    })
  }
  if (stats.value.todayFlow > 0) {
    items.push({
      label: `今日流量 ${stats.value.todayFlow} 人次`,
      tagType: 'info',
      route: '/statistics',
      tip: '查看趋势'
    })
  }
  if (items.length === 0) {
    items.push({ label: '暂无待办事项', tagType: 'success' })
  }
  return items
})

// 告警信息：依据统计数据生成
const alertItems = computed(() => {
  const alerts = []
  if (stats.value.todayEmergency > 0) {
    alerts.push({
      level: 'danger',
      title: `紧急通行 ${stats.value.todayEmergency} 起`,
      desc: '请及时关注紧急通行情况。'
    })
  }
  if (pendingCount.value >= 10) {
    alerts.push({
      level: 'warn',
      title: `待审批积压 ${pendingCount.value} 条`,
      desc: '可优先处理超过24小时的申请。'
    })
  }
  if (stats.value.rejectedCount >= 5) {
    alerts.push({
      level: 'info',
      title: `本周期拒绝 ${stats.value.rejectedCount} 条`,
      desc: '关注重复申请与材料缺失问题。'
    })
  }
  if (alerts.length === 0) {
    alerts.push({ level: 'success', title: '暂无异常告警', desc: '当前运行正常。' })
  }
  return alerts
})

// 快捷入口
const quickLinks = [
  { title: '审批中心', desc: '处理待审批申请', route: '/approval-center?tab=pending' },
  { title: '通行记录', desc: '查看访客通行情况', route: '/access-record' },
  { title: '数据统计', desc: '查看趋势与分布', route: '/statistics' },
  { title: '预约设置', desc: '调整预约规则', route: '/appointment-setting' }
]

// 拉取统计数据
const loadStats = async () => {
  loadingStats.value = true
  try {
    const res = await fetchAdminStats()
    if (res.code === 200) {
      stats.value = { ...stats.value, ...(res.data || {}) }
      lastUpdated.value = new Date()
    } else {
      ElMessage.error(res.msg || '加载统计数据失败')
    }
  } catch (error) {
    console.error('加载统计数据失败：', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loadingStats.value = false
  }
}

// 拉取待审批列表
const loadPendingList = async () => {
  loadingPending.value = true
  try {
    const res = await fetchPendingAppointments()
    if (res.code === 200) {
      pendingList.value = (res.data || []).map(item => mapAppointment(item, 'pending'))
    } else {
      ElMessage.error(res.msg || '加载待审批列表失败')
    }
  } catch (error) {
    console.error('加载待审批列表失败：', error)
    ElMessage.error('加载待审批列表失败')
  } finally {
    loadingPending.value = false
  }
}

// 同步刷新统计与待审批列表
const reloadDashboard = async () => {
  await Promise.all([loadStats(), loadPendingList()])
}

// 快捷跳转
const goQuick = (route) => {
  if (route) router.push(route)
}

// 进入待审批详情
const goPendingDetail = (row) => {
  router.push({
    path: `/approval-detail/${row.appointmentId}`,
    query: { mode: 'pending', from: '/dashboard' }
  })
}

// 登录成功后自动刷新数据
watch(isLoggedIn, (value) => {
  if (value) {
    reloadDashboard()
  }
})

// 初次进入时，如果已登录则加载数据
onMounted(() => {
  if (isLoggedIn.value) {
    reloadDashboard()
  }
})
</script>

<style scoped>
.dashboard-container {
  min-height: 100%;
}

.cockpit-container {
  position: relative;
  min-height: 100%;
  padding: 20px;
  background: #fff;
  /* 驾驶舱主题变量，便于统一调色 */
  --cockpit-text: #303133;
  --cockpit-muted: #909399;
  --cockpit-card: #ffffff;
  --cockpit-border: #e4e7ed;
  --cockpit-warn: #e6a23c;
  --cockpit-danger: #f56c6c;
  --cockpit-primary: #409eff;
  --cockpit-accent: #00a87a;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #303133;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--cockpit-text);
  font-family: 'Rubik', 'Noto Sans SC', sans-serif;
}

.page-subtitle {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 14px;
  color: var(--cockpit-muted);
}

.subtitle-name {
  font-weight: 600;
  color: var(--cockpit-text);
}

.subtitle-sep {
  color: #c2c8d1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 12px;
  position: relative;
  z-index: 1;
}

.section-title {
  font-size: 18px;
  margin: 0;
  font-weight: 600;
}

.summary-grid {
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.summary-card {
  border-radius: 16px;
  background: var(--cockpit-card);
  border: 1px solid var(--cockpit-border);
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.summary-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.12);
}

.summary-label {
  font-size: 14px;
  color: var(--cockpit-muted);
  margin-bottom: 8px;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--cockpit-text);
}

.summary-note {
  margin-top: 6px;
  font-size: 12px;
  color: var(--cockpit-muted);
}

.status-warn {
  color: var(--cockpit-warn);
}

.status-danger {
  color: var(--cockpit-danger);
}

.status-success {
  color: #1d8f5f;
}

.status-info {
  color: #3a6edb;
}

.panel-grid {
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.panel-card {
  border-radius: 16px;
  background: var(--cockpit-card);
  border: 1px solid var(--cockpit-border);
  backdrop-filter: blur(6px);
}

.cockpit-card {
  animation: fadeUp 0.6s ease both;
}

.summary-grid .el-col:nth-child(1) .summary-card { animation-delay: 0.05s; }
.summary-grid .el-col:nth-child(2) .summary-card { animation-delay: 0.1s; }
.summary-grid .el-col:nth-child(3) .summary-card { animation-delay: 0.15s; }
.summary-grid .el-col:nth-child(4) .summary-card { animation-delay: 0.2s; }
.summary-grid .el-col:nth-child(5) .summary-card { animation-delay: 0.25s; }
.summary-grid .el-col:nth-child(6) .summary-card { animation-delay: 0.3s; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.card-subtitle {
  font-size: 12px;
  color: var(--cockpit-muted);
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.todo-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.todo-tip {
  font-size: 12px;
  color: var(--cockpit-muted);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.quick-item {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(31, 111, 235, 0.16);
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.12), rgba(0, 168, 122, 0.08));
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.quick-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 20px rgba(31, 111, 235, 0.18);
}

.quick-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--cockpit-text);
}

.quick-desc {
  font-size: 12px;
  color: var(--cockpit-muted);
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #fff;
}

.alert-item.alert-danger {
  border-left: 4px solid var(--cockpit-danger);
  background: rgba(230, 70, 70, 0.08);
}

.alert-item.alert-warn {
  border-left: 4px solid var(--cockpit-warn);
  background: rgba(240, 162, 2, 0.08);
}

.alert-item.alert-info {
  border-left: 4px solid var(--cockpit-primary);
  background: rgba(31, 111, 235, 0.08);
}

.alert-item.alert-success {
  border-left: 4px solid var(--cockpit-accent);
  background: rgba(0, 168, 122, 0.08);
}

.alert-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.alert-desc {
  font-size: 12px;
  color: var(--cockpit-muted);
}

.pending-table {
  border-radius: 12px;
  overflow: hidden;
}

.empty-block {
  padding: 12px 0;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 100px;
  height: 100%;
}

.login-box {
  width: 500px;
  padding: 30px;
  background-color: #f0f2f5;
  border-radius: 4px;
}

.login-title {
  text-align: center;
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 30px;
  color: #303133;
}

.btn-group {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 30px;
}

.btn-group .el-button {
  width: 100px;
}

.form-wrapper {
  background-color: #fff;
  padding: 30px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-buttons {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}

@media (max-width: 1200px) {
  .header-actions {
    width: 100%;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
}

@media (max-width: 768px) {
  .cockpit-container {
    padding: 18px;
  }

  .page-title {
    font-size: 24px;
  }
}
</style>