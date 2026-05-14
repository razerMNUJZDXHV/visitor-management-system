<template>
  <div class="page-container">
    <h2 class="page-title">用户管理</h2>

    <!-- 搜索区域 -->
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="手机号">
        <el-input 
          v-model="searchForm.phone" 
          placeholder="请输入手机号" 
          clearable 
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input 
          v-model="searchForm.realName" 
          placeholder="请输入姓名" 
          clearable 
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="用户类型">
        <el-select 
          v-model="searchForm.userType" 
          placeholder="全部类型" 
          clearable 
          style="width: 120px;"
          @change="handleSearch"
        >
          <el-option label="访客" :value="1" />
          <el-option label="审批人" :value="2" />
          <el-option label="安保" :value="3" />
          <el-option label="管理员" :value="4" />
        </el-select>
      </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openCreateDialog">新增用户</el-button>
          <el-button type="primary" plain :loading="loading" @click="loadUserList">刷新</el-button>
        </el-form-item>
    </el-form>

    <!-- 用户列表 -->
    <el-table :data="userList" border stripe class="user-table" empty-text="暂无用户数据">
      <el-table-column prop="userId" label="用户ID" width="100" align="center" />
      <el-table-column prop="phone" label="手机号" width="150" align="center" />
      <el-table-column prop="realName" label="姓名" width="120" align="center" />
      <el-table-column prop="userType" label="用户类型" width="120" align="center">
        <template #default="scope">
          <span>{{ getUserTypeText(scope.row.userType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="200" align="center">
        <template #default="scope">
          {{ formatDateTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center">
        <template #default="scope">
          <el-button 
            type="primary" 
            size="small" 
            @click="openUpdateDialog(scope.row)"
          >
            修改
          </el-button>
          
          <el-popconfirm
            title="确认删除该用户吗？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm="handleDelete(scope.row.userId)"
          >
            <template #reference>
              <el-button 
                type="danger" 
                size="small"
                :disabled="scope.row.userId === currentUser.userId"
              >
                {{ scope.row.userId === currentUser.userId ? '不可删除' : '删除' }}
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <div class="pagination-container" style="margin-top: 20px; text-align: right;">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        prev-text="上一页"
        next-text="下一页"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/修改用户弹窗 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入11位手机号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select 
            v-model="form.userType" 
            placeholder="请选择用户类型" 
            style="width: 100%;"
            :disabled="isOnlyAdmin && !isCreate && form.userId === currentUser.userId"
          >
            <el-option label="访客" :value="1" />
            <el-option label="审批人" :value="2" />
            <el-option label="安保" :value="3" />
            <el-option label="管理员" :value="4" />
          </el-select>
          <div v-if="isOnlyAdmin && !isCreate && form.userId === currentUser.userId" style="color: #f56c6c; font-size: 12px; margin-top: 5px;">
            系统仅您一位管理员，不可修改用户类型，但可修改手机号、姓名和密码
          </div>
        </el-form-item>
        <!-- ==================== 核心修复1：密码输入框去掉 readonly hack ==================== -->
        <el-form-item v-if="form.userType === 4" label="密码" prop="password">
          <el-input 
            v-model="form.password" 
            :placeholder="isCreate ? '请输入6-20位字母数字组合' : '请输入新密码（6-20位字母数字组合）'" 
            show-password
            clearable
            autocomplete="new-password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 用户管理：用户列表、分页、创建/编辑/删除。
import { ref, reactive, onMounted, computed, inject, watch, nextTick } from 'vue'
import axios from '../api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { formatDateTime } from '../utils/appointment'
import { PASSWORD_REGEX } from '../utils/validators'

// 路由与全局状态
const router = useRouter()
const globalState = inject('globalState')

// 从本地缓存读取当前登录用户
const getCurrentUser = () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      return JSON.parse(userInfoStr)
    } catch (e) {
      return { userId: null, userType: null }
    }
  }
  return { userId: null, userType: null }
}
const currentUser = ref(getCurrentUser())

// 搜索条件
const searchForm = reactive({
  phone: '',
  realName: '',
  userType: null
})

// 分页状态
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 列表与权限状态
const loading = ref(false)
const userList = ref([])
const adminCount = ref(0)
const isOnlyAdmin = computed(() => adminCount.value === 1)

// 弹窗与表单状态
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isCreate = ref(true)
const formRef = ref(null)
const form = reactive({
  userId: null,
  phone: '',
  realName: '',
  userType: 2,
  password: null
})

// 记录修改时的原始用户类型（用于密码校验）
const originalUserType = ref(null)
// 表单校验规则
const formRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ],
  password: [
    {
      validator: (rule, value, callback) => {
        // 当用户类型为管理员时，执行密码必填校验
        if (form.userType === 4) {
          // 情况1：创建管理员 → 密码必填
          if (isCreate.value) {
            if (!value || value.trim() === '') {
              return callback(new Error('请输入密码'))
            }
          } else {
            // 情况2：修改用户，且原始类型不是管理员 → 密码必填（防止创建无密码管理员）
            if (originalUserType.value !== 4) {
              if (!value || value.trim() === '') {
                return callback(new Error('将用户提升为管理员必须设置密码'))
              }
            }
            // 情况3：修改原有管理员，密码留空表示不修改密码，允许通过
          }
        }
        // 格式校验（非空时）
        if (value && value.trim() !== '') {
          if (!PASSWORD_REGEX.test(value)) {
            return callback(new Error('密码需包含字母和数字，长度6-20位'))
          }
        }
        callback()
      },
      trigger: ['blur', 'change', 'submit']
    }
  ]
}

// 用户类型文本转换
const getUserTypeText = (type) => {
  const map = { 1: '访客', 2: '审批人', 3: '安保', 4: '管理员' }
  return map[type] || '未知'
}

// 获取管理员数量，用于判断是否允许降级/删除
const fetchAdminCount = async () => {
  try {
    const res = await axios.get('/api/admin/user/admin/count')
    if (res.code === 200) {
      adminCount.value = res.data
    }
  } catch (err) {
    console.error('获取管理员总数失败', err)
  }
}

// 加载用户列表（支持分页与条件查询）
const loadUserList = async () => {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    const res = await axios.get('/api/admin/user/list', { params })
    if (res.code === 200) {
      userList.value = res.data.list
      pagination.total = res.data.total
    } else {
      ElMessage.error(res.msg)
    }
  } catch (err) {
    console.error('加载用户列表失败：', err)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 分页大小变化
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.pageNum = 1
  loadUserList()
}

// 页码变化
const handleCurrentChange = (val) => {
  pagination.pageNum = val
  loadUserList()
}

// 新增/编辑提交
const handleSubmit = async () => {
  if (isOnlyAdmin.value && !isCreate.value && form.userId === currentUser.value.userId && form.userType !== 4) {
    ElMessage.warning('系统仅您一位管理员，不可修改为非管理员类型')
    return
  }

  if (form.password && form.password.trim() === '') {
    form.password = null
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 判断是否将自己从管理员降级为非管理员
  const isSelfAndDowngrade = !isCreate.value
    && form.userId === currentUser.value.userId
    && currentUser.value.userType === 4
    && form.userType !== 4

  if (isSelfAndDowngrade) {
    try {
      await ElMessageBox.confirm(
        '确认将自己的用户类型修改为非管理员吗？修改后将自动退出登录',
        '重要提示',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      ElMessage.info('已取消修改')
      return
    }
  }

  try {
    let res
    if (isCreate.value) {
      res = await axios.post('/api/admin/user/create', form)
    } else {
      res = await axios.put('/api/admin/user/update', form)
    }

    if (res.code === 200) {
      ElMessage.success(isCreate.value ? '创建成功' : '修改成功')
      dialogVisible.value = false
      await Promise.all([loadUserList(), fetchAdminCount()]) // 同时刷新列表和管理员计数

      if (!isCreate.value && form.userId === currentUser.value.userId) {
        // 同步更新本地缓存与全局状态
        const updatedUserInfo = {
          ...currentUser.value,
          phone: form.phone,
          realName: form.realName,
          userType: form.userType
        }
        localStorage.setItem('userInfo', JSON.stringify(updatedUserInfo))
        globalState.userInfo.value = updatedUserInfo
        currentUser.value = updatedUserInfo
      }

      if (isSelfAndDowngrade) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        globalState.isLoggedIn.value = false
        globalState.userInfo.value = {}
        ElMessage.info('您的用户类型已修改，即将退出登录')
        setTimeout(() => {
          router.push('/dashboard')
        }, 1500)
      }
    } else {
      ElMessage.error(res.msg)
    }
  } catch (err) {
    console.error('提交失败：', err)
    ElMessage.error('提交失败，请检查网络')
  }
}

// 删除用户
const handleDelete = async (userId) => {
  if (userId === currentUser.value.userId) {
    ElMessage.warning('不可删除自己的账号')
    return
  }

  try {
    const res = await axios.delete(`/api/admin/user/delete/${userId}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await Promise.all([loadUserList(), fetchAdminCount()])
    } else {
      ElMessage.error(res.msg)
    }
  } catch (err) {
    console.error('删除失败：', err)
    ElMessage.error('删除失败，请检查网络')
  }
}

// 触发搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadUserList()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.phone = ''
  searchForm.realName = ''
  searchForm.userType = null
  pagination.pageNum = 1
  loadUserList()
}

// 当用户类型切换时重新校验密码字段
watch(() => form.userType, async () => {
  if (formRef.value) {
    formRef.value.clearValidate('password')
    await nextTick()
    formRef.value.validateField('password')
  }
}, { immediate: true })

// 打开新增用户弹窗
const openCreateDialog = () => {
  isCreate.value = true
  dialogTitle.value = '新增用户'
  formRef.value?.resetFields()
  formRef.value?.clearValidate()
  form.userId = null
  form.phone = ''
  form.realName = ''
  form.userType = 2
  form.password = null
  originalUserType.value = null   // 创建模式无需原始类型
  dialogVisible.value = true
}

// 打开编辑用户弹窗
const openUpdateDialog = (row) => {
  isCreate.value = false
  dialogTitle.value = '修改用户'
  formRef.value?.resetFields()
  formRef.value?.clearValidate()
  form.userId = row.userId
  form.phone = row.phone
  form.realName = row.realName
  form.userType = row.userType
  form.password = null
  originalUserType.value = row.userType   // 记录原始类型
  dialogVisible.value = true
}

// 初始化加载
onMounted(() => {
  loadUserList()
  fetchAdminCount()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #303133;
}

.search-form {
  margin-bottom: 20px;
}

.user-table {
  width: 100%;
}
</style>