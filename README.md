# 应急管理大学访客预约与准入管理系统

基于微信小程序的智能化访客预约与准入管理平台，为应急管理大学提供全流程数字化访客管理解决方案。

---

## 📋 项目简介

本系统旨在解决传统访客管理效率低、安全隐患多的问题，通过微信小程序实现访客预约、审批、核验的全流程数字化管理。系统支持多角色协同工作，包括访客、审批人、安保人员和管理员。

### 核心功能
- ✅ **访客预约**：微信小程序在线预约，自动加密敏感信息
- ✅ **审批流程**：审批人微信端快速审批，支持通过/拒绝
- ✅ **安保核验**：扫码签到/签离，手动登记紧急通行
- ✅ **管理后台**：Vue3 管理驾驶舱，数据统计与导出
- ✅ **安全机制**：JWT 认证、AES 加密、爽约封禁

---

## 🛠️ 技术栈

### 后端（Spring Boot）
- **框架**：Spring Boot 3.5.11
- **ORM**：MyBatis 3.0.5
- **数据库**：MySQL 8.0
- **安全**：JWT (JJWT 0.12.6) + Spring Security Crypto
- **二维码**：ZXing 3.5.3
- **Excel 导出**：Apache POI 5.4.1

### 微信小程序（访客端 + 审批人端 + 安保端）
- **框架**：微信小程序原生 + TypeScript
- **UI 组件**：自定义组件
- **状态管理**：微信小程序内置

### 管理后台（Vue3）
- **框架**：Vue 3 + Vite
- **UI 组件**：Element Plus 2.13.6
- **图表**：ECharts 6.0.0
- **HTTP 客户端**：Axios 1.15.0

---

## 📂 项目结构

```
基于微信小程序的应急管理大学访客预约与准入管理系统/
├── README.md                       # 项目说明文档
├── 命名规范文档.md                 # 代码命名规范
├── .gitignore                     # Git 忽略规则
│
├── admin-web/                     # Vue3 管理后台
│   ├── src/                      # 源代码
│   │   ├── views/               # 页面组件
│   │   ├── router/              # 路由配置
│   │   └── main.js              # 入口文件
│   ├── public/                   # 静态资源
│   ├── dist/                     # 构建输出（已忽略）
│   ├── package.json              # 依赖配置
│   └── vite.config.js           # Vite 配置
│
├── visitor-system/                # 微信小程序
│   ├── miniprogram/             # 小程序源代码
│   │   ├── pages/              # 页面
│   │   ├── components/         # 组件
│   │   └── app.json            # 全局配置
│   ├── project.config.json       # 项目配置
│   └── package.json             # 依赖配置
│
└── visitor-system-backend/       # Spring Boot 后端
    ├── src/
    │   ├── main/
    │   │   ├── java/com/gpj/visitorsystem/
    │   │   │   ├── controller/  # 控制器
    │   │   │   ├── service/     # 业务逻辑
    │   │   │   ├── mapper/     # 数据访问
    │   │   │   ├── entity/     # 实体类
    │   │   │   ├── dto/        # 数据传输对象
    │   │   │   ├── util/       # 工具类
    │   │   │   └── interceptor/ # 拦截器
    │   │   └── resources/
    │   │       ├── mapper/      # MyBatis XML
    │   │       └── application.yaml # 配置文件
    │   └── test/                # 测试代码
    ├── sql/                      # SQL 脚本
    │   └── 访客系统完整版.sql   # 数据库初始化脚本
    ├── target/                   # 编译输出（已忽略）
    └── pom.xml                  # Maven 配置
```

---

## 🚀 快速启动

### 1. 环境准备
- JDK 21+
- Node.js 16+
- MySQL 8.0+
- Maven 3.8+（或使用 Maven Wrapper）
- 微信开发者工具

### 2. 数据库初始化
```sql
-- 登录 MySQL
mysql -u root -p

-- 执行数据库初始化脚本
source sql/访客系统完整版.sql
```

### 3. 启动后端服务
```bash
cd visitor-system-backend
mvn spring-boot:run
```
访问地址：<http://localhost:8080>

### 4. 启动管理后台
```bash
cd admin-web
npm install
npm run dev
```
访问地址：<http://localhost:5173>

### 5. 启动微信小程序
1. 打开微信开发者工具
2. 导入项目：`visitor-system/miniprogram/`
3. 填写 AppID（在 `project.config.json` 中配置）
4. 编译运行

---

## 📖 使用指南

### 访客端（微信小程序）
1. 微信授权登录
2. 填写预约信息（姓名、手机号、身份证号、来访事由等）
3. 提交预约，生成二维码
4. 查看预约状态，取消预约

### 审批人端（微信小程序）
1. 登录审批人账号
2. 查看待审批列表
3. 审批通过/拒绝
4. 查看审批历史

### 安保端（微信小程序）
1. 登录安保账号
2. 扫码核验预约二维码
3. 确认签到/签离
4. 手动登记紧急通行
5. 查看通行记录统计

### 管理后台（Vue3 Web）
1. 管理员登录
2. 管理驾驶舱查看统计数据
3. 用户管理（增删改查）
4. 审批管理（待审批 + 历史）
5. 通行记录管理
6. 预约设置配置
7. 数据导出（Excel）

---

## 🔧 配置说明

### 后端配置（`visitor-system-backend/src/main/resources/application.yaml`）
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/visitor_db
    username: root
    password: mysql

wechat:
  appid: 你的微信小程序AppID
  secret: 你的微信小程序Secret

jwt:
  secret: 你的JWT密钥（至少32字符）
  expiration: 7200000  # 2小时
```

### 管理后台配置（`admin-web/src/`）
- 后端 API 地址配置在 `src/` 中的 HTTP 客户端文件中
- 根据部署环境修改 baseURL

### 微信小程序配置（`visitor-system/miniprogram/project.config.json`）
- 修改 `appid` 为你的微信小程序 AppID

---

## 📊 数据库设计

### 核心表结构
1. **user（用户表）**
   - 存储所有用户（访客、审批人、安保、管理员）
   - 关键字段：`user_id`, `openid`, `phone`, `user_type`, `banned_until`

2. **appointment（预约表）**
   - 存储所有预约记录
   - 关键字段：`appointment_id`, `visitor_id`, `status`, `expected_start_time`

3. **access_log（通行记录表）**
   - 存储所有签到/签离记录
   - 关键字段：`log_id`, `appointment_id`, `access_type`, `access_time`

### 预约状态说明
- `0` - 待审核
- `1` - 预约成功
- `2` - 预约失败（已拒绝）
- `3` - 已取消
- `4` - 已签到
- `5` - 已完成
- `6` - 已过期

### 用户类型说明
- `1` - 访客
- `2` - 审批人
- `3` - 安保人员
- `4` - 系统管理员

---

## 🔒 安全特性

1. **JWT Token 认证**
   - Access Token（2小时） + Refresh Token（7天）
   - 自动刷新机制

2. **数据加密**
   - 身份证号 AES 加密存储
   - 密码 BCrypt 加密

3. **SQL 防注入**
   - MyBatis 预编译（#{} 占位符）
   - 避免动态 SQL 拼接

4. **爽约封禁机制**
   - 两次爽约间隔在30天内，自动封禁3个月
   - 封禁期间无法预约，支持管理员手动解封

5. **CORS 跨域控制**
   - 生产环境限制跨域来源
   - 避免 CSRF 攻击

---

## 📈 性能优化

1. **数据库索引优化**
   - `idx_status_create_time`（状态 + 创建时间）
   - `idx_visitor_status_time`（访客 + 状态 + 时间）
   - `idx_access_time`（通行记录时间索引）

2. **连接池配置**
   - HikariCP 连接池（Spring Boot 默认）

3. **分页查询**
   - 避免大结果集
   - 提升查询响应速度

---



## 📝 开发规范

详见 [命名规范文档.md](./命名规范文档.md)

### 包命名规范
- Controller：`com.gpj.visitorsystem.controller`
- Service：`com.gpj.visitorsystem.service`
- Mapper：`com.gpj.visitorsystem.mapper`
- Entity：`com.gpj.visitorsystem.entity`
- DTO：`com.gpj.visitorsystem.dto`

### 类命名规范
- Controller：`*Controller`
- Service：`*Service`
- Mapper：`*Mapper`
- Entity：`*`
- DTO：`*DTO`

---

## 📦 部署指南

### 后端部署（Spring Boot）
```bash
# 打包
cd visitor-system-backend
mvn clean package

# 运行
java -jar target/visitor-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 管理后台部署（Vue3）
```bash
cd admin-web
npm run build
# 将 dist/ 目录部署到 Web 服务器（如 Nginx）
```

### 微信小程序部署
1. 在微信开发者工具中点击"上传"
2. 登录微信公众平台
3. 提交审核
4. 审核通过后发布

---



**注意**：本项目为应急管理大学中期答辩项目，仅供学习和交流使用。
