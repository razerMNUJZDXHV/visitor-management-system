import { request } from '../../utils/request';
import { calculateNavHeight, formatDateTime as formatDateTimeUtil, getApproveHistoryStatusText, getApproveHistoryStatusClass, initCascadeFromDate, onCascadeYearChange, onCascadeMonthChange, buildDateFromCascade } from '../../utils/util';

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
    this.setData({ navHeight: calculateNavHeight() });
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
    const data = initCascadeFromDate(this.data.startDate, 'start');
    this.setData(data);
  },

  selectStartYear(e: any) {
    const year = Number(e.currentTarget.dataset.val);
    const data = onCascadeYearChange('start', year);
    this.setData(data);
  },

  selectStartMonth(e: any) {
    const month = Number(e.currentTarget.dataset.val);
    const data = onCascadeMonthChange('start', this.data.startYear, month, this.data.startDay);
    this.setData(data);
  },

  selectStartDay(e: any) {
    this.setData({ startDay: Number(e.currentTarget.dataset.val) });
  },

  confirmStartDate() {
    const { startYear, startMonth, startDay } = this.data;
    if (!startYear || !startMonth || !startDay) {
      return;
    }
    const date = buildDateFromCascade(startYear, startMonth, startDay);
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
    const data = initCascadeFromDate(this.data.endDate, 'end');
    this.setData(data);
  },

  selectEndYear(e: any) {
    const year = Number(e.currentTarget.dataset.val);
    const data = onCascadeYearChange('end', year);
    this.setData(data);
  },

  selectEndMonth(e: any) {
    const month = Number(e.currentTarget.dataset.val);
    const data = onCascadeMonthChange('end', this.data.endYear, month, this.data.endDay);
    this.setData(data);
  },

  selectEndDay(e: any) {
    this.setData({ endDay: Number(e.currentTarget.dataset.val) });
  },

  confirmEndDate() {
    const { endYear, endMonth, endDay } = this.data;
    if (!endYear || !endMonth || !endDay) {
      return;
    }
    const date = buildDateFromCascade(endYear, endMonth, endDay);
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
          createTimeText: this.formatDateTime(item.createTime),
          processTimeText: item.processTime ? this.formatDateTime(item.processTime) : '',
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
    return getApproveHistoryStatusText(status);
  },

  getStatusClass(status: number): string {
    return getApproveHistoryStatusClass(status);
  },

  formatDateTime(date: Date | string): string {
    return formatDateTimeUtil(date);
  },

  goDetail(e: any) {
    const id = Number(e.currentTarget.dataset.id);
    if (!id) {
      wx.showToast({ title: '预约ID缺失', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: `/pages/approver/historyDetail?id=${encodeURIComponent(id)}` });
  }
});