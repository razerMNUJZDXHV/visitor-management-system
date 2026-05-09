import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../views/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import UserManage from '../views/UserManage.vue'
import AppointmentSetting from '../views/AppointmentSetting.vue'
import ApprovalCenter from '../views/ApprovalCenter.vue'
import ApprovalDetail from '../views/ApprovalDetail.vue'
import AccessRecord from '../views/AccessRecord.vue'
import AccessRecordDetail from '../views/AccessRecordDetail.vue'
import Statistics from '../views/Statistics.vue'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '首页' } // 首页不需要登录校验
      },
      {
        path: 'user-manage',
        name: 'UserManage',
        component: UserManage,
        meta: { requiresAuth: true, title: '用户管理' }
      },
      {
        path: 'appointment-setting',
        name: 'AppointmentSetting',
        component: AppointmentSetting,
        meta: { requiresAuth: true, title: '预约设置' }
      },
      {
        path: 'approval-center',
        name: 'ApprovalCenter',
        component: ApprovalCenter,
        meta: { requiresAuth: true, title: '审批管理' }
      },
      {
        path: 'approval-pending',
        redirect: '/approval-center'
      },
      {
        path: 'approval-history',
        redirect: '/approval-center?tab=history'
      },
      {
        path: 'approval-detail/:id',
        name: 'ApprovalDetail',
        component: ApprovalDetail,
        meta: { requiresAuth: true, title: '审批详情' }
      },

      {
        path: 'access-record',
        name: 'AccessRecord',
        component: AccessRecord,
        meta: { requiresAuth: true, title: '通行记录管理' }
      },
      {
        path: 'access-record-detail/:id',
        name: 'AccessRecordDetail',
        component: AccessRecordDetail,
        meta: { requiresAuth: true, title: '通行记录详情' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: Statistics,
        meta: { requiresAuth: true, title: '访客数据统计' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const requiresAuth = to.meta.requiresAuth

  if (requiresAuth && !token) {
    ElMessage.warning('请先登录')
    next('/dashboard') // 未登录跳转到首页（登录页）
    return
  }
  next()
})

export default router