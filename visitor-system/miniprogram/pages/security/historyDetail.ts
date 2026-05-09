import { request } from '../../utils/request';

Page({
  data: {
    navHeight: 0,
    detail: null as any
  },

  onLoad(options: any) {
    const systemInfo = wx.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 20;
    const navContentHeight = 44;
    this.setData({ navHeight: statusBarHeight + navContentHeight });

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
      this.setData({ detail });
    }).catch((err) => {
      wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
    });
  }
});