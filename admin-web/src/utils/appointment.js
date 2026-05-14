// 预约相关工具：状态映射与时间格式化。
// 预约状态文本映射（后端状态码 -> 前端展示文案）。
const STATUS_TEXT_MAP = {
  0: '待审批',
  1: '已通过',
  2: '已拒绝',
  3: '已取消',
  4: '已签到',
  5: '已完成',
  6: '已过期'
}

// 预约状态样式类映射（用于标签/列表的样式绑定）。
const STATUS_CLASS_MAP = {
  0: 'status-pending',
  1: 'status-success',
  2: 'status-fail',
  3: 'status-cancel',
  4: 'status-checkin',
  5: 'status-complete',
  6: 'status-expire'
}

/**
 * 格式化日期时间为 YYYY-MM-DD HH:mm。
 * @param {string|number|Date} value - 时间值
 * @returns {string}
 */
export const formatDateTime = (value) => {
  if (!value) return '—'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '—'

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

/**
 * 格式化日期为 YYYY-MM-DD。
 * @param {string|number|Date} value - 时间值
 * @returns {string}
 */
export const formatDateOnly = (value) => {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 获取审批状态文案（对历史过期状态做细分）。
 * @param {number} status - 状态码
 * @param {'pending'|'history'} mode - 列表模式
 * @param {Object} appointment - 预约记录
 * @returns {string}
 */
export const getApprovalStatusText = (status, mode = 'pending', appointment = {}) => {
  if (status === 6 && mode === 'history') {
    // 历史列表中的过期状态，区分是否已处理
    const isProcessedExpired = Boolean(appointment.processTime || appointment.approverId)
    return isProcessedExpired ? '已处理已过期' : '未处理已过期'
  }
  return STATUS_TEXT_MAP[status] || '未知'
}

/**
 * 获取审批状态样式类。
 * @param {number} status - 状态码
 * @returns {string}
 */
export const getApprovalStatusClass = (status) => {
  return STATUS_CLASS_MAP[status] || ''
}

/**
 * 规范化预约记录的显示字段，避免在视图层重复计算。
 * @param {Object} item - 原始预约记录
 * @param {'pending'|'history'} mode - 列表模式
 * @returns {Object}
 */
export const mapAppointment = (item, mode = 'pending') => {
  return {
    ...item,
    createTimeText: formatDateTime(item.createTime),
    processTimeText: formatDateTime(item.processTime),
    expectedStartTimeText: formatDateTime(item.expectedStartTime),
    expectedEndTimeText: formatDateTime(item.expectedEndTime),
    statusText: getApprovalStatusText(item.status, mode, item),
    statusClass: getApprovalStatusClass(item.status)
  }
}
