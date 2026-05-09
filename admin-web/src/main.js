import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 导入中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import axios from './api/axios'

const app = createApp(App)
// 全局配置中文语言
app.use(ElementPlus, {
  locale: zhCn
})
app.use(router)
app.config.globalProperties.$axios = axios
app.mount('#app')