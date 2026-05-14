// 通行记录相关接口封装。
import axios from './axios'

/**
 * 获取通行记录列表
 * @param {Object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期 (YYYY-MM-DD)
 * @param {string} [params.endDate] - 结束日期 (YYYY-MM-DD)
 * @param {string} [params.keyword] - 关键词（访客姓名/手机号/安保姓名/授权审批人姓名）
 * @param {number} [params.accessType] - 通行类型：1-签到，2-签离
 * @param {number} [params.verifyMethod] - 核验方式：1-扫码，2-手动
 * @param {boolean} [params.emergencyOnly] - 是否仅查询紧急登记
 * @returns {Promise}
 */
export const fetchAccessRecords = (params) => {
  return axios.get('/api/admin/access/records', { params })
}

/**
 * 获取通行记录详情
 * @param {number} logId - 通行记录ID
 * @returns {Promise}
 */
export const fetchAccessRecordDetail = (logId) => {
  return axios.get('/api/admin/access/record-detail', {
    params: { logId }
  })
}

/**
 * 删除通行记录
 * @param {number} logId - 通行记录ID
 * @returns {Promise}
 */
export const deleteAccessRecord = (logId) => {
  return axios.delete('/api/admin/access/delete', {
    params: { logId }
  })
}

/**
 * 批量删除通行记录
 * @param {number[]} logIds - 通行记录ID列表
 * @returns {Promise}
 */
export const batchDeleteAccessRecords = (logIds) => {
  return axios.delete('/api/admin/access/batch-delete', {
    data: { logIds }
  })
}

/**
 * 导出通行记录（Excel）
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export const exportAccessRecords = (params = {}) => {
  return axios.get('/api/admin/export/access-records', {
    params: {
      startDate: params.startDate || null,
      endDate: params.endDate || null,
      keyword: params.keyword || null,
      accessType: params.accessType != null ? params.accessType : null,
      verifyMethod: params.verifyMethod != null ? params.verifyMethod : null,
      emergencyOnly: params.emergencyOnly != null ? params.emergencyOnly : null
    },
    responseType: 'blob'
  })
}
