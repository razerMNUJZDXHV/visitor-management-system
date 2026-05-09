import { request } from '../../utils/request';
interface HistoryAppointment {
  appointmentId: number;
  visitorName: string;
  visitorPhone: string;
  createTime: string;
  processTime: string | null;
  expectedStartTime: string;
  expectedEndTime: string;
  status: number;
  rejectReason?: string;
  // ... 其他需要的字段
}

Page({
  data: {
    navHeight: 0,
    appointment: {} as any,
    statusText: '',
    statusClass: ''
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
    this.loadDetail(id);
  },

  loadDetail(id: number) {
    request({
      url: `/api/wx/approver/appointment/detail?appointmentId=${id}`,
      method: 'GET'
    })
      .then(data => {
        this.setData({
          appointment: data,
          statusText: this.getStatusText(data.status, data),
          statusClass: this.getStatusClass(data.status)
        });
      })
      .catch(err => {
        wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
        setTimeout(() => wx.navigateBack(), 1500);
      });
  },

  getStatusText(status: number, item: HistoryAppointment): string {
    if (status === 2) return '已拒绝';
    return '已同意';
  },
  
  getStatusClass(status: number): string {
    if (status === 2) return 'status-fail';
    return 'status-success';
  },
});