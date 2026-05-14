// 统计数据相关接口封装。
import axios from './axios'

/**
 * 获取管理端统计数据。
 * @param {number} days - 统计天数（默认 7 天）
 * @returns {Promise}
 */
export const fetchAdminStats = (days = 7) => {
  return axios.get('/api/admin/stats', { params: { days } })
}
