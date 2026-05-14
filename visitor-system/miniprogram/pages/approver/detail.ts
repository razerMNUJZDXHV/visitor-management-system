import { request } from '../../utils/request';
import { calculateNavHeight, formatDateTimeFromStr } from '../../utils/util';
Page({
  data: {
    navHeight: 0,
    appointment: {} as any,
    showRejectInput: false,
    rejectReason: '',
    reasonLength: 0
  },

  onLoad(options: any) {
    this.setData({ navHeight: calculateNavHeight() });
  
    const id = options.id ? parseInt(options.id, 10) : null;
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1500);
      return;
    }
    this.setData({ 'appointment.appointmentId': id });
    this.loadDetail(id);
  },

  loadDetail(id: number) {
    request({
      url: `/api/wx/approver/appointment/detail?appointmentId=${id}`,
      method: 'GET'
    })
    .then(data => {
      data.createTime = formatDateTimeFromStr(data.createTime);
      data.expectedStartTime = formatDateTimeFromStr(data.expectedStartTime);
      data.expectedEndTime = formatDateTimeFromStr(data.expectedEndTime);
      this.setData({ appointment: data });
    })
    .catch(err => {
      wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
    });
  },

  onReasonInput(e: any) {
    const value = e.detail.value;
    this.setData({ rejectReason: value, reasonLength: value.length });
  },

  handleApprove() {
    wx.showModal({
      title: '确认通过',
      content: '确定通过该预约申请吗？',
      success: (res) => {
        if (res.confirm) {
          this.submitApprove();
        }
      }
    });
  },

  handleReject() {
    if (!this.data.showRejectInput) {
      this.setData({ showRejectInput: true });
      return;
    }
    if (!this.data.rejectReason.trim()) {
      wx.showToast({ title: '请填写拒绝理由', icon: 'none' });
      return;
    }
    this.submitReject();
  },

  submitApprove() {
    const appointmentId = this.data.appointment.appointmentId;
    request({
      url: `/api/wx/approver/appointment/approve?appointmentId=${appointmentId}`,
      method: 'POST'
    })
    .then(() => {
      wx.showToast({ title: '审批通过', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1500);
    })
    .catch(err => {
      wx.showToast({ title: err.msg || '操作失败', icon: 'none' });
    });
  },
  
  submitReject() {
    const appointmentId = this.data.appointment.appointmentId;
    const reason = this.data.rejectReason;
    request({
      url: `/api/wx/approver/appointment/reject?appointmentId=${appointmentId}&reason=${encodeURIComponent(reason)}`,
      method: 'POST'
    })
    .then(() => {
      wx.showToast({ title: '已拒绝', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1500);
    })
    .catch(err => {
      wx.showToast({ title: err.msg || '操作失败', icon: 'none' });
    });
  }
});