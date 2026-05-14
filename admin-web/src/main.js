// 应用入口：初始化 Vue 实例、UI 组件库、路由与全局依赖。
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 导入中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import axios from './api/axios'

// 创建根应用实例
const app = createApp(App)
// 全局配置 Element Plus 中文语言
app.use(ElementPlus, {
  locale: zhCn
})
// 挂载路由
app.use(router)
// 通过全局属性暴露 axios，便于 Options API 场景使用
app.config.globalProperties.$axios = axios
// 挂载到 DOM
app.mount('#app')