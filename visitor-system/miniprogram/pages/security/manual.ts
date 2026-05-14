import { request } from '../../utils/request';
import { calculateNavHeight, validateIdCard, validatePhone } from '../../utils/util';

Page({
  data: {
    navHeight: 0,
    reasonLength: 0,
    form: {
      authorizerId: '',
      visitorName: '',
      visitorPhone: '',
      visitorIdCard: '',
      visitReason: ''
    }
  },

  onLoad() {
    this.setData({ navHeight: calculateNavHeight() });
  },

  onInput(e: { currentTarget: { dataset: { field: string } }; detail: { value: string } }) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  handleIdCardInput(e: { detail: { value: string } }) {
    let value = e.detail.value.toUpperCase();
    value = value.replace(/[^0-9X]/g, '');
    this.setData({ 'form.visitorIdCard': value });
  },

  handleReasonInput(e: { detail: { value: string } }) {
    const value = e.detail.value;
    const length = value.length;
    this.setData({
      'form.visitReason': value,
      reasonLength: length
    });
    if (length >= 500) {
      wx.showToast({ title: '来访事由最多输入500字', icon: 'none', duration: 1500 });
    }
  },

  handleReset() {
    this.setData({
      form: {
        authorizerId: '',
        visitorName: '',
        visitorPhone: '',
        visitorIdCard: '',
        visitReason: ''
      },
      reasonLength: 0
    });
    wx.showToast({ title: '已重置', icon: 'success' });
  },

  submit() {
    const form = this.data.form;

    if (!form.authorizerId) {
      wx.showToast({ title: '请填写授权审批人ID', icon: 'none' });
      return;
    }
    if (!/^\d+$/.test(form.authorizerId)) {
      wx.showToast({ title: '授权审批人ID应为数字', icon: 'none' });
      return;
    }
    if (!form.visitorPhone) {
      wx.showToast({ title: '请填写访客手机号', icon: 'none' });
      return;
    }
    if (!validatePhone(form.visitorPhone)) {
      wx.showToast({ title: '请输入有效的11位手机号', icon: 'none' });
      return;
    }
    if (!form.visitorName.trim()) {
      wx.showToast({ title: '请填写访客姓名', icon: 'none' });
      return;
    }
    if (!validateIdCard(form.visitorIdCard)) {
      wx.showToast({ title: '请输入有效的18位身份证号', icon: 'none' });
      return;
    }
    if (!form.visitReason.trim()) {
      wx.showToast({ title: '请填写来访事由', icon: 'none' });
      return;
    }

    request<any>({
      url: '/api/wx/security/access/manual-register',
      method: 'POST',
      data: {
        scenario: 2,
        accessType: 1,
        appointmentId: null,
        authorizerId: form.authorizerId ? Number(form.authorizerId) : null,
        visitorName: form.visitorName.trim(),
        visitorPhone: form.visitorPhone.trim(),
        visitorIdCard: form.visitorIdCard.trim().toUpperCase(),
        visitReason: form.visitReason.trim()
      }
    }).then(() => {
      wx.showToast({ title: '登记成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1000);
    }).catch((err) => {
      wx.showToast({ title: err.msg || '登记失败', icon: 'none' });
    });
  }
});
