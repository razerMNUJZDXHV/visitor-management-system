import { request } from '../../utils/request';

const QR_GRACE_MINUTES = 30;
const EXPIRE_REMIND_BEFORE_MINUTES = 5;

type AppointmentItem = {
  appointmentId: number;
  visitorId: number;
  status: number;
  expectedStartTime?: string;
  expectedEndTime?: string;
  [key: string]: unknown;
};

Page({
  data: {
    navHeight: 0,
    appointmentList: [] as any[],
    showQrModal: false,
    qrCodeUrl: '',
    qrStartTime: '',
    qrEndTime: '',
    showNoShowPenaltyTip: true,
    noShowPenaltyTipDismissKey: ''
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 20;
    const navContentHeight = 44;
    const totalNavHeight = statusBarHeight + navContentHeight;
    this.setData({ navHeight: totalNavHeight });
  },

  onShow() {
    this.initNoShowPenaltyTip();
    this.loadAppointmentList();
  },

  initNoShowPenaltyTip() {
    const userId = wx.getStorageSync('userId') || '';
    const loginSessionAt = wx.getStorageSync('loginSessionAt') || 'default';
    const dismissKey = `appointmentListNoShowPenaltyTipDismissed_${userId}_${loginSessionAt}`;
    const dismissed = wx.getStorageSync(dismissKey);

    this.setData({
      noShowPenaltyTipDismissKey: dismissKey,
      showNoShowPenaltyTip: !dismissed
    });
  },

  handleCloseNoShowPenaltyTip() {
    const dismissKey = this.data.noShowPenaltyTipDismissKey;
    if (dismissKey) {
      wx.setStorageSync(dismissKey, true);
    }
    this.setData({ showNoShowPenaltyTip: false });
  },

  // 阻止事件冒泡（点击按钮时不触发跳转详情）
  stopPropagation() { },

  // 跳转详情页
  goDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/appointment/detail?id=${id}`
    });
  },

  //拒绝理由
  handleViewReason(e: any) {
    const reason = e.currentTarget.dataset.reason;
    wx.showModal({
      title: '拒绝理由',
      content: reason,
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  loadAppointmentList() {
    const visitorId = wx.getStorageSync('userId');
    request<any[]>({
      url: `/api/wx/visitor/appointment/list?visitorId=${visitorId}`,
      method: 'GET'
    })
    .then(data => {
      const now = Date.now();
      const list = data.map((item: any) => {
        const createDate = item.createTime ? new Date(item.createTime) : new Date();
        const startDate = item.expectedStartTime ? new Date(item.expectedStartTime) : null;
        const endDate = item.expectedEndTime ? new Date(item.expectedEndTime) : null;
        const overstayHint = this.getOverstayHint(item.status, endDate, now);
        return {
          ...item,
          statusText: this.getStatusText(item.status),
          statusClass: this.getStatusClass(item.status),
          applyDateText: this.formatFullDateTime(createDate),
          expectedStartTimeText: startDate ? this.formatDateTime(startDate) : '—',
          expectedEndTimeText: endDate ? this.formatDateTime(endDate) : '—',
          overstayHint
        };
      });
      this.setData({ appointmentList: list });
      this.triggerOverstayReminders(list as AppointmentItem[]);
    })
    .catch(err => {
      wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
    });
  },

  getOverstayHint(status: number, endDate: Date | null, nowMs: number): string {
    if (status !== 4 || !endDate) {
      return '';
    }
    const endMs = endDate.getTime();
    if (Number.isNaN(endMs)) {
      return '';
    }
    const graceEndMs = endMs + QR_GRACE_MINUTES * 60 * 1000;
    if (nowMs < endMs || nowMs >= graceEndMs) {
      return '';
    }
    const leftMinutes = Math.max(Math.ceil((graceEndMs - nowMs) / 60000), 0);
    if (leftMinutes <= EXPIRE_REMIND_BEFORE_MINUTES) {
      return `滞留超时：通行码将在 ${leftMinutes} 分钟内过期`;
    }
    return `滞留超时：已到预计离开时间，30分钟宽限中`;
  },

  getReminderStorageKey(appointmentId: number, stage: string): string {
    return `stayTimeoutReminder_${appointmentId}_${stage}`;
  },

  triggerOverstayReminders(list: AppointmentItem[]) {
    const nowMs = Date.now();
    for (const item of list) {
      if (item.status !== 4 || !item.expectedEndTime) {
        continue;
      }
      const endMs = new Date(item.expectedEndTime).getTime();
      if (Number.isNaN(endMs)) {
        continue;
      }

      const graceEndMs = endMs + QR_GRACE_MINUTES * 60 * 1000;
      if (nowMs < endMs || nowMs >= graceEndMs) {
        continue;
      }

      let stage = 'reached-end';
      if (nowMs >= graceEndMs - EXPIRE_REMIND_BEFORE_MINUTES * 60 * 1000) {
        stage = 'before-expire-5';
      }

      const key = this.getReminderStorageKey(item.appointmentId, stage);
      if (wx.getStorageSync(key)) {
        continue;
      }

      wx.setStorageSync(key, true);
      const graceTimeText = this.formatFullDateTime(new Date(graceEndMs));
      const content = stage === 'before-expire-5'
        ? `提醒：您已签到但未签退，临时通行码将于 ${graceTimeText} 过期，请立即签离。`
        : `提示：您已签到但未签退，已到预计离开时间，当前处于30分钟宽限期，临时通行码将于 ${graceTimeText} 过期。`;

      wx.showModal({
        title: '滞留超时提醒',
        content,
        showCancel: false,
        confirmText: '我知道了'
      });
      break;
    }
  },

  // 状态文本映射
  getStatusText(status: number): string {
    const map: Record<number, string> = {
      0: '待审核',
      1: '预约成功',
      2: '预约失败',
      3: '已取消',
      4: '已签到',
      5: '已完成',
      6: '已过期'
    };
    return map[status] || '未知';
  },

  getStatusClass(status: number): string {
    const map: Record<number, string> = {
      0: 'status-pending',
      1: 'status-success',
      2: 'status-fail',
      3: 'status-cancel',
      4: 'status-checkin',
      5: 'status-complete',
      6: 'status-expire'
    };
    return map[status] || '';
  },

  // 格式化完整日期时间 (YYYY-MM-DD HH:mm)
  formatDateTime(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hour}:${minute}`;
  },

  // 格式化日期
  formatFullDateTime(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hour}:${minute}`;
  },

  // 阻止弹窗背景滚动
  preventMove() {
    return false;
  },

  // 关闭弹窗
  closeQrModal() {
    this.setData({
      showQrModal: false,
      qrCodeUrl: '',
      qrStartTime: '',
      qrEndTime: ''
    });
  },

  // 查看通行码（改为弹窗显示）
  handleViewQRCode(e: any) {
    const item = e.currentTarget.dataset.item;
    const now = new Date();
    const startTime = new Date(item.expectedStartTime);
    const endTime = new Date(item.expectedEndTime);
    const graceEndTime = new Date(endTime.getTime() + QR_GRACE_MINUTES * 60 * 1000);

    if (now < startTime) {
      wx.showToast({ title: '预约时间未到，暂无法生成通行码', icon: 'none' });
      return;
    }
    if (now > graceEndTime) {
      wx.showToast({ title: '预约已过期，通行码失效', icon: 'none' });
      return;
    }

    if (item.status === 4 && now >= endTime && now < graceEndTime) {
      const leftMinutes = Math.max(Math.ceil((graceEndTime.getTime() - now.getTime()) / 60000), 0);
      const tip = leftMinutes <= EXPIRE_REMIND_BEFORE_MINUTES
        ? '滞留超时，通行码将在5分钟内过期，请尽快签离'
        : '已到预计离开时间，当前进入30分钟宽限期，请尽快签离';
      wx.showToast({ title: tip, icon: 'none', duration: 2000 });
    }

    // 先显示弹窗，显示加载状态
    this.setData({
      showQrModal: true,
      qrCodeUrl: '',
      qrStartTime: this.formatFullDateTime(startTime),
      qrEndTime: this.formatFullDateTime(graceEndTime)
    });

    // 请求后端生成二维码
    request<string>({
      url: '/api/wx/visitor/appointment/qrcode',
      method: 'GET',
      data: {
        appointmentId: item.appointmentId,
        visitorId: item.visitorId
      }
    })
    .then(qrCodeUrl => {
      this.setData({ qrCodeUrl });
    })
    .catch(err => {
      wx.showToast({ title: err.msg || '生成失败', icon: 'none' });
      this.closeQrModal();
    });
  },

  handleCancel(e: any) {
    const appointmentId = e.currentTarget.dataset.id;
    const visitorId = wx.getStorageSync('userId');
    wx.showModal({
      title: '确认取消',
      content: '确定要取消该预约吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '取消中...' });
          request({
            url: `/api/wx/visitor/appointment/cancel?appointmentId=${appointmentId}&visitorId=${visitorId}`,
            method: 'POST'
          })
            .then(() => {
              wx.hideLoading();
              wx.showToast({ title: '取消成功', icon: 'success' });
              this.loadAppointmentList();
            })
            .catch(err => {
              wx.hideLoading();
              console.error('取消失败', err);
            });
        }
      }
    });
  }
});
