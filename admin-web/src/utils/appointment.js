const STATUS_TEXT_MAP = {
  0: '待审批',
  1: '已通过',
  2: '已拒绝',
  3: '已取消',
  4: '已签到',
  5: '已完成',
  6: '已过期'
}

const STATUS_CLASS_MAP = {
  0: 'status-pending',
  1: 'status-success',
  2: 'status-fail',
  3: 'status-cancel',
  4: 'status-checkin',
  5: 'status-complete',
  6: 'status-expire'
}

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

export const formatDateOnly = (value) => {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const getApprovalStatusText = (status, mode = 'pending', appointment = {}) => {
  if (status === 6 && mode === 'history') {
    const isProcessedExpired = Boolean(appointment.processTime || appointment.approverId)
    return isProcessedExpired ? '已处理已过期' : '未处理已过期'
  }
  return STATUS_TEXT_MAP[status] || '未知'
}

export const getApprovalStatusClass = (status) => {
  return STATUS_CLASS_MAP[status] || ''
}

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
