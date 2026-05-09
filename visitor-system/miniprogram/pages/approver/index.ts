import { request } from '../../utils/request';

Page({
  data: {
    navHeight: 0,
    approverName: '',
    todayList: [] as any[],
    earlierList: [] as any[],
    todayExpanded: true,      // 今日待审批默认展开
    earlierExpanded: false,   // 更早默认折叠
    totalCount: 0             // 全部待审批数量
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 20;
    const navContentHeight = 44;
    this.setData({ navHeight: statusBarHeight + navContentHeight });
  
    // 优先从独立存储的 realName 读取，其次从 userInfo 对象读取
    const realName = wx.getStorageSync('realName') || '';
    const userInfo = wx.getStorageSync('userInfo') || {};
    const approverName = realName || userInfo.realName || '审批人';
    this.setData({ approverName });
  },

  onShow() {
    this.loadPendingList();
  },

  loadPendingList() {
    request<any[]>({
      url: '/api/wx/approver/appointment/pending',
      method: 'GET'
    })
      .then(data => {
        const list = data.map((item: any) => ({
          ...item,
          createTimeText: this.formatDateTime(new Date(item.createTime)),
          expectedStartTimeText: this.formatDateTime(new Date(item.expectedStartTime))
        }));
        this.splitByToday(list);
      })
      .catch(err => {
        wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
      });
  },

  splitByToday(list: any[]) {
    const todayStr = this.formatDateOnly(new Date());
    const todayList: any[] = [];
    const earlierList: any[] = [];
    list.forEach(item => {
      const createDay = item.createTimeText.split(' ')[0];
      if (createDay === todayStr) {
        todayList.push(item);
      } else {
        earlierList.push(item);
      }
    });
    this.setData({ todayList, earlierList, totalCount: list.length });
  },

  formatDateTime(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hour}:${minute}`;
  },

  formatDateOnly(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  toggleTodayExpand() {
    this.setData({ todayExpanded: !this.data.todayExpanded });
  },

  toggleEarlierExpand() {
    this.setData({ earlierExpanded: !this.data.earlierExpanded });
  },

  goDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/approver/detail?id=${id}` });
  }
});
