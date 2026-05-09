import { request } from '../../utils/request';

Page({
  data: {
    navHeight: 0,
    period: 'day',
    totalFlow: 0,
    peakHour: '-',
    flowPoints: [] as any[],
    hourPoints: [] as any[]
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 20;
    const navContentHeight = 44;
    this.setData({ navHeight: statusBarHeight + navContentHeight });
  },

  onShow() {
    this.loadStats();
  },

  changePeriod(e: any) {
    const period = e.currentTarget.dataset.period;
    this.setData({ period }, () => this.loadStats());
  },

  loadStats() {
    request<any>({
      url: '/api/wx/security/access/stats',
      method: 'GET',
      data: { period: this.data.period }
    }).then((data) => {
      const flowPoints = (data.flowPoints || []).map((item: any) => ({ ...item, width: `${Math.min(100, item.value * 10)}%` }));
      const hourPoints = (data.hourPoints || []).map((item: any) => ({ ...item, width: `${Math.min(100, item.value * 10)}%` }));
      this.setData({
        totalFlow: data.totalFlow || 0,
        peakHour: data.peakHour || '-',
        flowPoints,
        hourPoints
      });
    }).catch((err) => {
      wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
    });
  }
});