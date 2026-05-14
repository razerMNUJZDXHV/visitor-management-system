import { request } from '../../utils/request';
import { calculateNavHeight, formatDateTimeFromStr } from '../../utils/util';

Page({
  data: {
    navHeight: 0,
    detail: null as any,
    isEmergency: false,
    isManual: false,
    isSignOut: false,
    durationText: ''
  },

  onLoad(options: any) {
    this.setData({ navHeight: calculateNavHeight() });

    const id = options.id ? Number(options.id) : 0;
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' });
      return;
    }
    this.loadDetail(id);
  },

  loadDetail(logId: number) {
    request<any>({
      url: '/api/wx/security/access/record-detail',
      method: 'GET',
      data: { logId }
    }).then((detail) => {
      detail.accessTime = formatDateTimeFromStr(detail.accessTime);
      detail.expectedStartTime = detail.expectedStartTime ? formatDateTimeFromStr(detail.expectedStartTime) : '';
      detail.expectedEndTime = detail.expectedEndTime ? formatDateTimeFromStr(detail.expectedEndTime) : '';
      detail.signInTime = detail.signInTime ? formatDateTimeFromStr(detail.signInTime) : '';

      const isManual = detail.verifyMethod === 2;
      const isSignOut = detail.accessType === 2;
      let durationText = '';
      if (isSignOut && detail.signInTime && detail.accessTime) {
        const start = new Date(detail.signInTime.replace(/-/g, '/'));
        const end = new Date(detail.accessTime.replace(/-/g, '/'));
        const diff = Math.floor((end.getTime() - start.getTime()) / 60000);
        const h = Math.floor(diff / 60);
        const m = diff % 60;
        durationText = h > 0 ? `${h}小时${m}分钟` : `${m}分钟`;
      }

      this.setData({
        detail,
        isEmergency: !!detail.emergency,
        isManual,
        isSignOut,
        durationText
      });
    }).catch((err) => {
      wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
    });
  }
});