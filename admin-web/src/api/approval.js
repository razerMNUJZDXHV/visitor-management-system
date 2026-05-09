import axios from './axios'

export const fetchPendingAppointments = () => {
  return axios.get('/api/admin/appointment/pending')
}

export const fetchApprovalDetail = (appointmentId) => {
  return axios.get('/api/admin/appointment/detail', {
    params: { appointmentId }
  })
}

export const approveAppointment = (appointmentId) => {
  return axios.post('/api/admin/appointment/approve', null, {
    params: { appointmentId }
  })
}

export const rejectAppointment = (appointmentId, reason) => {
  return axios.post('/api/admin/appointment/reject', null, {
    params: { appointmentId, reason }
  })
}

export const fetchApprovalHistory = (params) => {
  return axios.get('/api/admin/appointment/history', {
    params
  })
}

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

export const deleteApprovalRecord = (appointmentId) => {
  return axios.delete('/api/admin/appointment/delete', {
    params: { appointmentId }
  })
}

export const batchDeleteApprovalRecords = (appointmentIds) => {
  return axios.delete('/api/admin/appointment/batch-delete', {
    data: { appointmentIds }
  })
}
