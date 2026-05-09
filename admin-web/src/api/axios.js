import axios from 'axios'
import { ElMessage, ElLoading } from 'element-plus'

const instance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
})

// 加载动画实例
let loadingInstance = null

// 请求拦截器：添加Token、显示加载动画
instance.interceptors.request.use(
  config => {
    loadingInstance = ElLoading.service({ lock: true, text: '处理中...', background: 'rgba(0,0,0,0.1)' })
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  error => {
    loadingInstance?.close()
    ElMessage.error('请求配置错误')
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理响应/错误
instance.interceptors.response.use(
  response => {
    loadingInstance?.close()
    return response.data // 直接返回data，简化前端取值
  },
  error => {
    loadingInstance?.close()
    if (error.response) {
      const { status } = error.response
      if (status === 401) { // Token过期/无效
        ElMessage.error('登录已过期，请重新登录')
        localStorage.clear()
        window.location.href = '/'
      } else if (status === 403) {
        ElMessage.error('无权限访问')
      } else {
        ElMessage.error(error.response.data?.msg || '服务器错误')
      }
    } else if (error.request) {
      ElMessage.error('网络异常，请检查连接')
    } else {
      ElMessage.error(`请求失败：${error.message}`)
    }
    return Promise.reject(error)
  }
)

export default instance