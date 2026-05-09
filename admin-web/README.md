# 应大访客预约与准入管理系统 - 管理后台

Vue 3 + Vite + Element Plus 构建的管理后台系统。

---

## 📋 功能模块

- **登录/登出**：管理员身份认证
- **管理驾驶舱**：数据概览、快捷入口
- **用户管理**：用户增删改查
- **审批管理**：预约审批、审批历史
- **通行记录管理**：通行记录查询、导出
- **预约设置**：预约规则配置
- **数据统计**：访客流量分析、预约统计

---

## 🛠️ 技术栈

- **框架**：Vue 3 (Composition API)
- **构建工具**：Vite
- **UI 组件库**：Element Plus
- **图表**：ECharts + Vue-ECharts
- **HTTP 客户端**：Axios
- **路由**：Vue Router

---

## 🚀 快速启动

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问：http://localhost:5173

### 3. 构建生产版本

```bash
npm run build
```

构建输出在 `dist/` 目录。

---

## 📂 项目结构

```
admin-web/
├── public/              # 静态资源
├── src/
│   ├── components/      # 公共组件
│   ├── views/           # 页面组件
│   ├── router/          # 路由配置
│   ├── api/             # API 接口
│   ├── utils/           # 工具函数
│   ├── App.vue          # 根组件
│   └── main.js          # 入口文件
├── index.html           # HTML 模板
├── vite.config.js       # Vite 配置
└── package.json         # 项目配置
```

---

## ⚙️ 配置说明

### API 地址配置

在 `src/utils/request.js` 中修改后端 API 地址：

```javascript
const request = axios.create({
  baseURL: 'http://localhost:8080/api',  // 修改为你自己的后端地址
  timeout: 5000
})
```

---

## 📦 依赖说明

| 依赖 | 版本 | 说明 |
|------|------|------|
| vue | ^3.5.32 | Vue 3 框架 |
| element-plus | ^2.13.6 | UI 组件库 |
| echarts | ^6.0.0 | 图表库 |
| axios | ^1.15.0 | HTTP 客户端 |
| vue-router | ^4.6.4 | 路由管理 |
| vue-echarts | ^8.0.1 | Vue ECharts 组件 |

---

## 📝 开发规范

- 组件命名：PascalCase（如 `UserManage.vue`）
- 变量命名：camelCase（如 `userName`）
- 常量命名：UPPER_SNAKE_CASE（如 `MAX_RETRY_COUNT`）
- 提交规范：遵循 Conventional Commits

---

## 📞 联系方式

- **开发**：[你的名字]
- **学校**：应急管理大学

---

**最后更新**：2026年5月10日
