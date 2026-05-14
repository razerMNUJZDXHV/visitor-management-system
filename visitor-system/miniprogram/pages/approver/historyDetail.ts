import { request } from '../../utils/request';
import { calculateNavHeight, formatDateTimeFromStr, getApproveHistoryStatusText, getApproveHistoryStatusClass } from '../../utils/util';

Page({
  data: {
    navHeight: 0,
    appointment: {} as any,
    statusText: '',
    statusClass: ''
  },

  onLoad(options: any) {
    this.setData({ navHeight: calculateNavHeight() });

    const id = options.id ? parseInt(options.id, 10) : null;
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1500);
      return;
    }
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
        data.processTime = formatDateTimeFromStr(data.processTime);
        this.setData({
          appointment: data,
          statusText: this.getStatusText(data.status),
          statusClass: this.getStatusClass(data.status)
        });
      })
      .catch(err => {
        wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
        setTimeout(() => wx.navigateBack(), 1500);
      });
  },

  getStatusText(status: number): string {
    return getApproveHistoryStatusText(status);
  },
  
  getStatusClass(status: number): string {
    return getApproveHistoryStatusClass(status);
  },
});