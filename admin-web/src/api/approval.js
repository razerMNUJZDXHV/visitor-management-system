// 预约审批相关接口封装。
import axios from './axios'

/**
 * 获取待审批预约列表。
 * @returns {Promise}
 */
export const fetchPendingAppointments = () => {
  return axios.get('/api/admin/appointment/pending')
}

/**
 * 获取预约审批详情。
 * @param {number} appointmentId - 预约ID
 * @returns {Promise}
 */
export const fetchApprovalDetail = (appointmentId) => {
  return axios.get('/api/admin/appointment/detail', {
    params: { appointmentId }
  })
}

/**
 * 审批通过预约。
 * @param {number} appointmentId - 预约ID
 * @returns {Promise}
 */
export const approveAppointment = (appointmentId) => {
  return axios.post('/api/admin/appointment/approve', null, {
    params: { appointmentId }
  })
}

/**
 * 审批拒绝预约。
 * @param {number} appointmentId - 预约ID
 * @param {string} reason - 拒绝理由
 * @returns {Promise}
 */
export const rejectAppointment = (appointmentId, reason) => {
  return axios.post('/api/admin/appointment/reject', null, {
    params: { appointmentId, reason }
  })
}

/**
 * 获取审批历史列表。
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export const fetchApprovalHistory = (params) => {
  return axios.get('/api/admin/appointment/history', {
    params
  })
}

/**
 * 导出审批历史（Excel）。
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export const exportApprovalHistory = (params = {}) => {
  return axios.get('/api/admin/export/appointments', {
    params: {
      startDate: params.startDate || null,
      endDate: params.endDate || null,
      keyword: params.keyword || null,
      status: params.status != null ? params.status : null,
      searchType: params.searchType || null
    },
    responseType: 'blob'
  })
}

/**
 * 删除单条审批记录。
 * @param {number} appointmentId - 预约ID
 * @returns {Promise}
 */
export const deleteApprovalRecord = (appointmentId) => {
  return axios.delete('/api/admin/appointment/delete', {
    params: { appointmentId }
  })
}

/**
 * 批量删除审批记录。
 * @param {number[]} appointmentIds - 预约ID列表
 * @returns {Promise}
 */
export const batchDeleteApprovalRecords = (appointmentIds) => {
  return axios.delete('/api/admin/appointment/batch-delete', {
    data: { appointmentIds }
  })
}
