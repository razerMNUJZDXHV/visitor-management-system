import { request } from '../../utils/request';
import { calculateNavHeight, formatDateTimeFromStr, getStatusText as getStatusTextUtil, getStatusClass as getStatusClassUtil } from '../../utils/util';

Page({
  data: {
    navHeight: 0,
    appointment: {} as any,
    statusText: '',
    statusClass: '',
    overtimeNotice: '',
    overtimeNoticeLevel: ''
  },

  onLoad(options: any) {
    this.setData({ navHeight: calculateNavHeight() });

    const id = options.id;
    this.loadDetail(id);
  },

  loadDetail(id: number) {
    const visitorId = wx.getStorageSync('userId');
    request({
      url: '/api/wx/visitor/appointment/detail',
      data: { appointmentId: id, visitorId }
    })
      .then(data => {
        const notice = this.buildOverstayNotice(data);
        data.createTime = formatDateTimeFromStr(data.createTime);
        data.expectedStartTime = formatDateTimeFromStr(data.expectedStartTime);
        data.expectedEndTime = formatDateTimeFromStr(data.expectedEndTime);
        this.setData({
          appointment: data,
          statusText: this.getStatusText(data.status),
          statusClass: this.getStatusClass(data.status),
          overtimeNotice: notice.text,
          overtimeNoticeLevel: notice.level
        });
      })
      .catch(err => {
        wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
        setTimeout(() => wx.navigateBack(), 1500);
      });
  },

  buildOverstayNotice(appointment: any): { text: string; level: string } {
    if (!appointment || appointment.status !== 4 || !appointment.expectedEndTime) {
      return { text: '', level: '' };
    }
    const endMs = new Date(appointment.expectedEndTime).getTime();
    if (Number.isNaN(endMs)) {
      return { text: '', level: '' };
    }

    const nowMs = Date.now();
    const graceEndMs = endMs + 30 * 60 * 1000;
    if (nowMs < endMs) {
      return { text: '', level: '' };
    }
    if (nowMs >= graceEndMs) {
      return {
        text: '滞留超时：已超过预计离开时间30分钟宽限，请立即联系安保协助签离。',
        level: 'danger'
      };
    }

    const leftMinutes = Math.max(Math.ceil((graceEndMs - nowMs) / 60000), 0);
    if (leftMinutes <= 5) {
      return {
        text: `滞留超时：临时通行码将在 ${leftMinutes} 分钟内过期，请尽快签离。`,
        level: 'danger'
      };
    }
    return {
      text: '滞留超时：已到预计离开时间，临时通行码进入30分钟宽限期，请尽快签离。',
      level: 'warn'
    };
  },

  getStatusText(status: number): string {
    return getStatusTextUtil(status);
  },

  getStatusClass(status: number): string {
    return getStatusClassUtil(status);
  },

  goBack() {
    wx.navigateBack();
  }
});