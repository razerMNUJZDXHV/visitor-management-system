import axios from './axios'

export const fetchAdminStats = (days = 7) => {
  return axios.get('/api/admin/stats', { params: { days } })
}
