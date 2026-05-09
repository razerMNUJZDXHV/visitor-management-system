import { request } from '../../utils/request';
Page({
  data: {
    navHeight: 0,
    appointment: {} as any,
    statusText: '待审批',
    showRejectInput: false,
    rejectReason: '',
    reasonLength: 0
  },

  onLoad(options: any) {
    const systemInfo = wx.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 20;
    const navContentHeight = 44;
    this.setData({ navHeight: statusBarHeight + navContentHeight });
  
    const id = options.id ? parseInt(options.id) : null;
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