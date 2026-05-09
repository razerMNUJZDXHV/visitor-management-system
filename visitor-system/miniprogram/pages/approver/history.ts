import { request } from '../../utils/request';

function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

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
}

Page({
  data: {
    navHeight: 0,
    list: [] as any[],
    keyword: '',
    startDate: '',
    endDate: '',
    searchType: 'create' as 'create' | 'process',
    statusOptions: ['全部状态', '已同意', '已拒绝'],
    statusIndex: 0,
    status: null as number | null,
    statusOpen: false,
    startDateOpen: false,
    endDateOpen: false,
    // 开始日期级联
    startYears: [] as number[],
    startMonths: [] as number[],
    startDays: [] as number[],
    startYear: 0,
    startMonth: 0,
    startDay: 0,
    // 结束日期级联
    endYears: [] as number[],
    endMonths: [] as number[],
    endDays: [] as number[],
    endYear: 0,
    endMonth: 0,
    endDay: 0
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 20;
    const navContentHeight = 44;
    this.setData({ navHeight: statusBarHeight + navContentHeight });
  },

  onShow() {
    this.loadHistory();
  },

  setSearchType(e: any) {
    const type = e.currentTarget.dataset.type;
    this.setData({ searchType: type });
    this.loadHistory();
  },

  onKeywordInput(e: any) {
    this.setData({ keyword: e.detail.value });
  },

  // ========== 开始日期级联 ==========
  toggleStartDate() {
    const isOpen = !this.data.startDateOpen;
    this.setData({ startDateOpen: isOpen, endDateOpen: false, statusOpen: false });
    if (isOpen) {
      this.initStartCascade();
    }
  },

  initStartCascade() {
    const now = new Date();
    const current = this.data.startDate ? this.data.startDate.split('-') : null;
    const selYear = current ? parseInt(current[0], 10) : now.getFullYear();
    const selMonth = current ? parseInt(current[1], 10) : now.getMonth() + 1;
    const selDay = current ? parseInt(current[2], 10) : now.getDate();

    const years: number[] = [];
    for (let y = now.getFullYear(); y >= now.getFullYear() - 5; y--) {
      years.push(y);
    }
    const months: number[] = [];
    for (let m = 1; m <= 12; m++) {
      months.push(m);
    }
    const daysCount = getDaysInMonth(selYear, selMonth);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) {
      days.push(d);
    }

    this.setData({
      startYears: years,
      startMonths: months,
      startDays: days,
      startYear: selYear,
      startMonth: selMonth,
      startDay: Math.min(selDay, daysCount)
    });
  },

  selectStartYear(e: any) {
    const year = Number(e.currentTarget.dataset.val);
    const months: number[] = [];
    for (let m = 1; m <= 12; m++) {
      months.push(m);
    }
    const daysCount = getDaysInMonth(year, 1);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) {
      days.push(d);
    }
    this.setData({ startYear: year, startMonth: 1, startDay: 1, startMonths: months, startDays: days });
  },

  selectStartMonth(e: any) {
    const month = Number(e.currentTarget.dataset.val);
    const daysCount = getDaysInMonth(this.data.startYear, month);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) {
      days.push(d);
    }
    this.setData({ startMonth: month, startDay: Math.min(this.data.startDay, daysCount), startDays: days });
  },

  selectStartDay(e: any) {
    this.setData({ startDay: Number(e.currentTarget.dataset.val) });
  },

  confirmStartDate() {
    const { startYear, startMonth, startDay } = this.data;
    if (!startYear || !startMonth || !startDay) {
      return;
    }
    const date = `${startYear}-${pad(startMonth)}-${pad(startDay)}`;
    this.setData({ startDate: date, startDateOpen: false });
    this.loadHistory();
  },

  clearStartDate() {
    this.setData({ startDate: '', startDateOpen: false, startYear: 0, startMonth: 0, startDay: 0 });
    this.loadHistory();
  },

  // ========== 结束日期级联 ==========
  toggleEndDate() {
    const isOpen = !this.data.endDateOpen;
    this.setData({ endDateOpen: isOpen, startDateOpen: false, statusOpen: false });
    if (isOpen) {
      this.initEndCascade();
    }
  },

  initEndCascade() {
    const now = new Date();
    const current = this.data.endDate ? this.data.endDate.split('-') : null;
    const selYear = current ? parseInt(current[0], 10) : now.getFullYear();
    const selMonth = current ? parseInt(current[1], 10) : now.getMonth() + 1;
    const selDay = current ? parseInt(current[2], 10) : now.getDate();

    const years: number[] = [];
    for (let y = now.getFullYear(); y >= now.getFullYear() - 5; y--) {
      years.push(y);
    }
    const months: number[] = [];
    for (let m = 1; m <= 12; m++) {
      months.push(m);
    }
    const daysCount = getDaysInMonth(selYear, selMonth);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) {
      days.push(d);
    }

    this.setData({
      endYears: years,
      endMonths: months,
      endDays: days,
      endYear: selYear,
      endMonth: selMonth,
      endDay: Math.min(selDay, daysCount)
    });
  },

  selectEndYear(e: any) {
    const year = Number(e.currentTarget.dataset.val);
    const months: number[] = [];
    for (let m = 1; m <= 12; m++) {
      months.push(m);
    }
    const daysCount = getDaysInMonth(year, 1);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) {
      days.push(d);
    }
    this.setData({ endYear: year, endMonth: 1, endDay: 1, endMonths: months, endDays: days });
  },

  selectEndMonth(e: any) {
    const month = Number(e.currentTarget.dataset.val);
    const daysCount = getDaysInMonth(this.data.endYear, month);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) {
      days.push(d);
    }
    this.setData({ endMonth: month, endDay: Math.min(this.data.endDay, daysCount), endDays: days });
  },

  selectEndDay(e: any) {
    this.setData({ endDay: Number(e.currentTarget.dataset.val) });
  },

  confirmEndDate() {
    const { endYear, endMonth, endDay } = this.data;
    if (!endYear || !endMonth || !endDay) {
      return;
    }
    const date = `${endYear}-${pad(endMonth)}-${pad(endDay)}`;
    this.setData({ endDate: date, endDateOpen: false });
    this.loadHistory();
  },

  clearEndDate() {
    this.setData({ endDate: '', endDateOpen: false, endYear: 0, endMonth: 0, endDay: 0 });
    this.loadHistory();
  },

  // ========== 状态筛选 ==========
  toggleStatus() {
    this.setData({ statusOpen: !this.data.statusOpen, startDateOpen: false, endDateOpen: false });
  },

  selectStatus(e: any) {
    const index = Number(e.currentTarget.dataset.index);
    const statusMap = [null, 1, 2];
    this.setData({ statusIndex: index, status: statusMap[index], statusOpen: false });
    this.loadHistory();
  },

  // 清空所有筛选条件，显示全部记录
  handleClear() {
    this.setData({
      keyword: '',
      startDate: '',
      endDate: '',
      searchType: 'create',
      statusIndex: 0,
      status: null,
      statusOpen: false,
      startDateOpen: false,
      endDateOpen: false,
      startYear: 0,
      startMonth: 0,
      startDay: 0,
      endYear: 0,
      endMonth: 0,
      endDay: 0
    });
    this.loadHistory();
  },

  handleSearch() {
    this.loadHistory();
  },

  loadHistory() {
    const params: any = {
      searchType: this.data.searchType,
    };
    if (this.data.keyword) params.keyword = this.data.keyword;
    if (this.data.startDate) params.startDate = this.data.startDate;
    if (this.data.endDate) params.endDate = this.data.endDate;
    if (this.data.status !== null) params.status = this.data.status;

    if (this.data.startDate && this.data.endDate && this.data.startDate > this.data.endDate) {
      wx.showToast({ title: '开始日期不能晚于结束日期', icon: 'none' });
      return;
    }

    request<HistoryAppointment[]>({
      url: '/api/wx/approver/appointment/history',
      data: params
    })
      .then(data => {
        const list = data.map((item: HistoryAppointment) => ({
          ...item,
          createTimeText: this.formatDateTime(new Date(item.createTime)),
          processTimeText: item.processTime ? this.formatDateTime(new Date(item.processTime)) : '',
          statusText: this.getStatusText(item.status),
          statusClass: this.getStatusClass(item.status)
        }));
        this.setData({ list });
      })
      .catch(err => {
        wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
      });
  },

  getStatusText(status: number): string {
    if (status === 2) return '已拒绝';
    return '已同意';
  },

  getStatusClass(status: number): string {
    if (status === 2) return 'status-fail';
    return 'status-success';
  },

  formatDateTime(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hour}:${minute}`;
  },

  goDetail(e: any) {
    const id = Number(e.currentTarget.dataset.id);
    if (!id) {
      wx.showToast({ title: '预约ID缺失', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: `/pages/approver/historyDetail?id=${id}` });
  }
});