<template>
  <el-container class="root-container">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="header-left"></div>
      <div class="header-title">应急管理大学访客预约与准入管理系统</div>
      <div class="header-right" v-if="isLoggedIn">
        <span class="user-name">管理员 {{ userInfo?.realName || '' }}</span>
        <el-button type="primary" @click="handleLogout">退出登录</el-button>
      </div>
      <div class="header-right-placeholder" v-else></div>
    </el-header>

    <!-- 下方主容器 -->
    <el-container class="main-container">
      <!-- 左侧菜单栏 -->
      <el-aside width="150px" class="sidebar">
        <el-menu
          class="sidebar-menu"
          :default-active="activeMenu"
          @select="handleMenuSelect"
          :background-color="menuBgColor"
          :text-color="menuTextColor"
          :active-text-color="menuActiveTextColor"
        >
          <el-menu-item index="dashboard">首页</el-menu-item>
          <el-menu-item index="user-manage">用户管理</el-menu-item>
          <el-menu-item index="appointment-setting">预约设置</el-menu-item>
          <el-menu-item index="approval-center">审批管理</el-menu-item>
          <el-menu-item index="access-record">通行记录管理</el-menu-item>
          <el-menu-item index="statistics">访客数据统计</el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 内容区域 -->
      <el-main class="content-area">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, provide, watch } from 'vue' // 确保引入 onMounted
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

// ==================== 修复全局响应式状态初始化（核心修复） ====================
// 1. isLoggedIn：直接从 localStorage 读取并初始化
const isLoggedIn = ref(!!localStorage.getItem('token'))

// 2. userInfo：直接从 localStorage 读取并解析，不要传函数给 ref！
const initUserInfo = () => {
  const info = localStorage.getItem('userInfo')
  return info ? JSON.parse(info) : {}
}
const userInfo = ref(initUserInfo())

// 将状态 provide 给子组件
provide('globalState', {
  isLoggedIn,
  userInfo
})

// ==================== 样式配置 ====================
const headerBgColor = '#2c3e50'
const menuBgColor = '#34495e'
const menuTextColor = '#ecf0f1'
const menuActiveTextColor = '#fff'

// ==================== 菜单相关 ====================
// 监听路由变化，自动更新高亮菜单
const activeMenu = ref('dashboard')

const resolveActiveMenu = () => {
  if (route.name === 'ApprovalDetail') {
    return 'approval-center'
  }
  if (route.name === 'AccessRecordDetail') {
    return 'access-record'
  }

  const firstSegment = route.path.replace('/', '').split('/')[0]
  return firstSegment || 'dashboard'
}

watch(
  () => [route.path, route.name, route.query.mode],
  () => {
    activeMenu.value = resolveActiveMenu()
  },
  { immediate: true }
)

// 同步 localStorage 状态（刷新时保险）
onMounted(() => {
  isLoggedIn.value = !!localStorage.getItem('token')
  userInfo.value = initUserInfo()
})

const handleMenuSelect = (index) => {
  if (!isLoggedIn.value && index !== 'dashboard') {
    ElMessage.warning('请先登录后再操作')
    return
  }
  // activeMenu 已由 watch 自动更新，无需手动设置
  router.push(`/${index}`)
}

// ==================== 退出登录（同步更新） ====================
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  
  // 更新响应式状态
  isLoggedIn.value = false
  userInfo.value = {}
  
  ElMessage.success('已退出登录')
  router.push('/dashboard')
}
</script>

<style scoped>
/* 样式保持不变，和之前一致 */
.root-container {
  height: 100vh;
  margin: 0;
  padding: 0;
}

.header {
  background-color: v-bind(headerBgColor);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 50px;
  line-height: 50px;
  color: #fff;
}

.header-left, .header-right-placeholder {
  width: 200px;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  text-align: center;
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
  width: 215px;
  justify-content: flex-end;
}

.user-name {
  font-size: 16px;
}

.main-container {
  height: calc(100vh - 50px);
}

/* 左侧菜单栏样式（优化文字居中） */
.sidebar {
  background-color: v-bind(menuBgColor);
}

.sidebar-menu {
  border-right: none;
  height: 100%;
  /* 覆盖 Element Plus 菜单默认的 padding */
  padding: 0 !important;
}

/* 核心修复：使用深度选择器覆盖 el-menu-item 内部样式 */
.sidebar-menu .el-menu-item {
  /* 强制使用 flex 布局 */
  display: flex !important;
  /* 水平居中 */
  justify-content: center !important;
  /* 垂直居中 */
  align-items: center !important;
  /* 固定高度 */
  height: 60px !important;
  /* 去掉所有 padding */
  padding: 0 !important;
  /* 菜单项分隔线 */
  border-bottom: 1px solid #2c3e50;
  font-size: 16px;
  font-weight: 500;
  /* 去掉 line-height，避免和 flex 冲突 */
  line-height: normal !important;
}

/* 覆盖激活状态的样式 */
.sidebar-menu .el-menu-item.is-active {
  padding: 0 !important;
  background-color: #409eff !important;
}

/* 覆盖 hover 状态的样式 */
.sidebar-menu .el-menu-item:hover {
  background-color: #2c3e50 !important;
}

/* 深度选择器：覆盖 Element Plus 内部的图标占位和内容容器 */
.sidebar-menu :deep(.el-menu-item__icon) {
  display: none !important; /* 隐藏图标占位（关键！） */
  margin: 0 !important;
}

.sidebar-menu :deep(.el-menu-item__title) {
  /* 确保标题容器也居中 */
  text-align: center !important;
  margin: 0 !important;
  padding: 0 !important;
}
</style>