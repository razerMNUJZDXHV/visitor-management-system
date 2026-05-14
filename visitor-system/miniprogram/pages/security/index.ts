import { request } from '../../utils/request';
import { calculateNavHeight, formatDateTimeFromStr } from '../../utils/util';

type VerifyResult = {
  passed: boolean;
  pendingConfirm?: boolean;
  alertType?: string;
  alertMessage?: string;
  actionType?: 'SIGN_IN' | 'SIGN_OUT';
  appointmentId?: number;
  visitorId?: number;
  visitorName?: string;
  visitorPhone?: string;
  visitorIdCard?: string;
  accessType?: number;
  verifyMethod?: number;
  accessTime?: string;
};

type AlertItem = {
  alertType: string;
  message: string;
  appointmentId?: number;
  visitorName?: string;
  visitorPhone?: string;
  visitorIdCard?: string;
  visitReason?: string;
  signInTime?: string;
  expectedStartTime?: string;
  expectedEndTime?: string;
  graceExpireTime?: string;
  appointmentStatus?: number;
  canManualSignOut?: boolean;
  overstayMinutes?: number;
  occurredAt?: string;
  occurredAtText?: string;
  signInTimeText?: string;
  expectedStartTimeText?: string;
  expectedEndTimeText?: string;
  graceExpireTimeText?: string;
  alertTypeText?: string;
};

const ALERT_POLL_INTERVAL_MS = 15000;
let alertPollTimer: number | null = null;

Page({
  data: {
    navHeight: 0,
    securityName: '安保人员',
    latestVerify: null as VerifyResult | null,
    pendingVerify: null as VerifyResult | null,
    showVerifyConfirm: false,
    confirmingVerify: false,
    alerts: [] as AlertItem[],
    selectedAlert: null as AlertItem | null,
    showAlertDetail: false,
    signingOut: false
  },

  onLoad() {
    const userInfo = wx.getStorageSync('userInfo') || {};
    const realName = wx.getStorageSync('realName') || '';
    this.setData({
      navHeight: calculateNavHeight(),
      securityName: realName || userInfo.realName || '安保人员'
    });
  },

  onShow() {
    this.pollAlerts();
    this.startAlertPolling();
  },

  onHide() {
    this.stopAlertPolling();
  },

  onUnload() {
    this.stopAlertPolling();
  },

  startAlertPolling() {
    if (alertPollTimer !== null) {
      clearInterval(alertPollTimer);
      alertPollTimer = null;
    }
    alertPollTimer = setInterval(() => {
      this.pollAlerts();
    }, ALERT_POLL_INTERVAL_MS) as unknown as number;
  },

  stopAlertPolling() {
    if (alertPollTimer !== null) {
      clearInterval(alertPollTimer);
      alertPollTimer = null;
    }
  },

  handleScanVerify() {
    wx.scanCode({
      onlyFromCamera: true,
      success: (scanRes) => {
        const qrToken = scanRes.result;
        request<VerifyResult>({
          url: '/api/wx/security/access/scan-verify',
          method: 'POST',
          data: { qrToken }
        }).then((res) => {
          if (res.passed && res.pendingConfirm) {
            this.setData({
              pendingVerify: res,
              showVerifyConfirm: true
            });
            return;
          }

          this.setData({ latestVerify: res });
          if (!res.passed) {
            wx.showToast({ title: res.alertMessage || '核验失败', icon: 'none' });
          }
          this.pollAlerts();
        }).catch((err) => {
          wx.showToast({ title: err.msg || '核验失败', icon: 'none' });
        });
      },
      fail: () => {
        wx.showToast({ title: '未获取到二维码内容', icon: 'none' });
      }
    });
  },

  goManualRegister() {
    wx.navigateTo({ url: '/pages/security/manual' });
  },

  goHistory() {
    wx.reLaunch({ url: '/pages/security/history' });
  },

  handleAlertTap(e: { currentTarget: { dataset: { index: string } } }) {
    const index = Number(e.currentTarget.dataset.index);
    const selected = this.data.alerts[index];
    if (!selected) {
      return;
    }
    this.setData({
      selectedAlert: selected,
      showAlertDetail: true
    });
  },

  closeAlertDetail() {
    this.setData({
      showAlertDetail: false,
      selectedAlert: null
    });
  },

  closeVerifyConfirm() {
    if (this.data.confirmingVerify) {
      return;
    }
    this.setData({
      showVerifyConfirm: false,
      pendingVerify: null
    });
  },

  handleVerifyReject() {
    this.closeVerifyConfirm();
    wx.showToast({ title: '已拒绝，不做任何记录', icon: 'none' });
  },

  handleVerifyPass() {
    const pendingVerify = this.data.pendingVerify;
    if (!pendingVerify || !pendingVerify.appointmentId) {
      wx.showToast({ title: '缺少预约信息', icon: 'none' });
      return;
    }
    if (this.data.confirmingVerify) {
      return;
    }

    this.setData({ confirmingVerify: true });
    request<VerifyResult>({
      url: '/api/wx/security/access/scan-confirm',
      method: 'POST',
      data: { appointmentId: pendingVerify.appointmentId }
    }).then((res) => {
      const successText = res.actionType === 'SIGN_OUT' ? '签离成功' : '签到成功';
      this.setData({
        latestVerify: res,
        showVerifyConfirm: false,
        pendingVerify: null
      });
      wx.showToast({ title: successText, icon: 'success' });
      this.pollAlerts();
    }).catch((err) => {
      wx.showToast({ title: err.msg || '操作失败', icon: 'none' });
    }).then(() => {
      this.setData({ confirmingVerify: false });
    });
  },

  preventMove() {
    return false;
  },

  handleManualSignOut() {
    const alert = this.data.selectedAlert;
    if (!alert || !alert.appointmentId) {
      wx.showToast({ title: '缺少预约信息', icon: 'none' });
      return;
    }
    if (this.data.signingOut) {
      return;
    }

    this.setData({ signingOut: true });
    request<any>({
      url: '/api/wx/security/access/manual-signout',
      method: 'POST',
      data: { appointmentId: alert.appointmentId }
    }).then(() => {
      wx.showToast({ title: '已手动签离', icon: 'success' });
      this.setData({
        showAlertDetail: false,
        selectedAlert: null
      });
      this.pollAlerts();
    }).catch((err) => {
      wx.showToast({ title: err.msg || '签离失败', icon: 'none' });
    }).then(() => {
      this.setData({ signingOut: false });
    });
  },

  formatDateTime(value?: string) {
    return formatDateTimeFromStr(value);
  },

  getAlertTypeText(type?: string) {
    const map: Record<string, string> = {
      EMERGENCY_REGISTERED: '紧急登记待签离',
      LEAVE_TIME_REACHED: '到达预计离开时间',
      OVERTIME_EXPIRED: '滞留超时已过期',
      QR_EXPIRED: '通行码过期',
      INVALID_QR: '无效二维码',
      APPOINTMENT_NOT_FOUND: '预约不存在'
    };
    if (!type) {
      return '异常告警';
    }
    return map[type] || type;
  },

  decorateAlert(item: AlertItem): AlertItem {
    const normalizedMessage = item.message ? item.message.replace(/：\s*[^：]*$/, '') : item.message;
    return {
      ...item,
      message: normalizedMessage,
      occurredAtText: this.formatDateTime(item.occurredAt),
      signInTimeText: this.formatDateTime(item.signInTime),
      expectedStartTimeText: this.formatDateTime(item.expectedStartTime),
      expectedEndTimeText: this.formatDateTime(item.expectedEndTime),
      graceExpireTimeText: this.formatDateTime(item.graceExpireTime),
      alertTypeText: this.getAlertTypeText(item.alertType)
    };
  },

  pollAlerts() {
    request<AlertItem[]>({
      url: '/api/wx/security/access/alerts',
      method: 'GET'
    }).then((list) => {
      const alerts = (list || []).map((item: AlertItem) => this.decorateAlert(item));
      this.setData({ alerts });

      if (this.data.showAlertDetail && this.data.selectedAlert && this.data.selectedAlert.appointmentId) {
        const selected = alerts.find((item: AlertItem) => {
          return item.appointmentId === this.data.selectedAlert!.appointmentId
            && item.alertType === this.data.selectedAlert!.alertType;
        });
        if (selected) {
          this.setData({ selectedAlert: selected });
        }
      }
    }).catch(() => {
      // 保留原有告警，避免网络波动时界面突然清空
    });
  }
});
