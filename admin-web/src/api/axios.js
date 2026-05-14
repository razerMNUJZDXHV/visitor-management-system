// Axios 实例：统一配置 baseURL、超时、鉴权与错误提示。
import axios from 'axios'
import { ElMessage, ElLoading } from 'element-plus'

// 允许通过 Vite 环境变量覆盖后端地址，默认走本地开发。
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const instance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
})

// 加载动画实例
let loadingInstance = null

// 请求拦截器：添加 Token 与全局加载遮罩
instance.interceptors.request.use(
  config => {
    // 同一时刻只保留一个 Loading 实例
    loadingInstance = ElLoading.service({ lock: true, text: '处理中...', background: 'rgba(0,0,0,0.1)' })
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  error => {
    // 请求配置异常时及时关闭遮罩并提示
    loadingInstance?.close()
    ElMessage.error('请求配置错误')
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理业务响应与异常提示
instance.interceptors.response.use(
  response => {
    loadingInstance?.close()
    // 只返回 data，减少业务层层级访问
    return response.data // 直接返回data，简化前端取值
  },
  error => {
    loadingInstance?.close()
    if (error.response) {
      const { status } = error.response
      // 依据状态码给出更明确的提示
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