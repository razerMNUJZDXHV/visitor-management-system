import { request } from '../../utils/request';

function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

Page({
  data: {
    navHeight: 0,
    list: [] as any[],
    keyword: '',
    startDate: '',
    endDate: '',
    accessTypeIndex: 0,
    accessTypeOptions: ['全部类型', '签到', '签离'],
    verifyMethodIndex: 0,
    verifyMethodOptions: ['全部方式', '扫码', '手动'],
    emergencyOnly: false,
    accessTypeOpen: false,
    verifyMethodOpen: false,
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
    this.loadRecords();
  },

  onInput(e: any) {
    this.setData({ keyword: e.detail.value });
  },

  // ========== 开始日期级联 ==========
  toggleStartDate() {
    const isOpen = !this.data.startDateOpen;
    this.setData({ startDateOpen: isOpen, endDateOpen: false, accessTypeOpen: false, verifyMethodOpen: false });
    if (isOpen) {
      this.initStartCascade();
    }
  },

  initStartCascade() {
    const now = new Date();
    const current = this.data.startDate ? this.data.startDate.split('-') : null;
    const selYear = current ? parseInt(current[0]) : now.getFullYear();
    const selMonth = current ? parseInt(current[1]) : now.getMonth() + 1;
    const selDay = current ? parseInt(current[2]) : now.getDate();

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
    for (let m = 1; m <= 12; m++) months.push(m);
    const daysCount = getDaysInMonth(year, 1);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) days.push(d);
    this.setData({ startYear: year, startMonth: 1, startDay: 1, startMonths: months, startDays: days });
  },

  selectStartMonth(e: any) {
    const month = Number(e.currentTarget.dataset.val);
    const daysCount = getDaysInMonth(this.data.startYear, month);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) days.push(d);
    this.setData({ startMonth: month, startDay: Math.min(this.data.startDay, daysCount), startDays: days });
  },

  selectStartDay(e: any) {
    this.setData({ startDay: Number(e.currentTarget.dataset.val) });
  },

  confirmStartDate() {
    const { startYear, startMonth, startDay } = this.data;
    if (!startYear || !startMonth || !startDay) return;
    const date = `${startYear}-${pad(startMonth)}-${pad(startDay)}`;
    this.setData({ startDate: date, startDateOpen: false });
    this.loadRecords();
  },

  clearStartDate() {
    this.setData({ startDate: '', startDateOpen: false, startYear: 0, startMonth: 0, startDay: 0 });
    this.loadRecords();
  },

  // ========== 结束日期级联 ==========
  toggleEndDate() {
    const isOpen = !this.data.endDateOpen;
    this.setData({ endDateOpen: isOpen, startDateOpen: false, accessTypeOpen: false, verifyMethodOpen: false });
    if (isOpen) {
      this.initEndCascade();
    }
  },

  initEndCascade() {
    const now = new Date();
    const current = this.data.endDate ? this.data.endDate.split('-') : null;
    const selYear = current ? parseInt(current[0]) : now.getFullYear();
    const selMonth = current ? parseInt(current[1]) : now.getMonth() + 1;
    const selDay = current ? parseInt(current[2]) : now.getDate();

    const years: number[] = [];
    for (let y = now.getFullYear(); y >= now.getFullYear() - 5; y--) {
      years.push(y);
    }
    const months: number[] = [];
    for (let m = 1; m <= 12; m++) months.push(m);
    const daysCount = getDaysInMonth(selYear, selMonth);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) days.push(d);

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
    for (let m = 1; m <= 12; m++) months.push(m);
    const daysCount = getDaysInMonth(year, 1);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) days.push(d);
    this.setData({ endYear: year, endMonth: 1, endDay: 1, endMonths: months, endDays: days });
  },

  selectEndMonth(e: any) {
    const month = Number(e.currentTarget.dataset.val);
    const daysCount = getDaysInMonth(this.data.endYear, month);
    const days: number[] = [];
    for (let d = 1; d <= daysCount; d++) days.push(d);
    this.setData({ endMonth: month, endDay: Math.min(this.data.endDay, daysCount), endDays: days });
  },

  selectEndDay(e: any) {
    this.setData({ endDay: Number(e.currentTarget.dataset.val) });
  },

  confirmEndDate() {
    const { endYear, endMonth, endDay } = this.data;
    if (!endYear || !endMonth || !endDay) return;
    const date = `${endYear}-${pad(endMonth)}-${pad(endDay)}`;
    this.setData({ endDate: date, endDateOpen: false });
    this.loadRecords();
  },

  clearEndDate() {
    this.setData({ endDate: '', endDateOpen: false, endYear: 0, endMonth: 0, endDay: 0 });
    this.loadRecords();
  },

  // ========== 其他筛选 ==========
  toggleAccessType() {
    this.setData({ accessTypeOpen: !this.data.accessTypeOpen, verifyMethodOpen: false });
  },

  selectAccessType(e: any) {
    const index = Number(e.currentTarget.dataset.index);
    this.setData({ accessTypeIndex: index, accessTypeOpen: false });
    this.loadRecords();
  },

  toggleVerifyMethod() {
    this.setData({ verifyMethodOpen: !this.data.verifyMethodOpen, accessTypeOpen: false });
  },

  selectVerifyMethod(e: any) {
    const index = Number(e.currentTarget.dataset.index);
    this.setData({ verifyMethodIndex: index, verifyMethodOpen: false });
    this.loadRecords();
  },

  onEmergencySwitch(e: any) {
    this.setData({ emergencyOnly: e.detail.value });
    this.loadRecords();
  },

  search() {
    this.loadRecords();
  },

  clear() {
    this.setData({
      keyword: '',
      startDate: '',
      endDate: '',
      accessTypeIndex: 0,
      verifyMethodIndex: 0,
      emergencyOnly: false,
      startYear: 0, startMonth: 0, startDay: 0,
      endYear: 0, endMonth: 0, endDay: 0
    });
    this.loadRecords();
  },

  loadRecords() {
    const accessTypeMap = [null, 1, 2];
    const verifyMethodMap = [null, 1, 2];

    const requestData: any = {};

    if (this.data.keyword) {
      requestData.keyword = this.data.keyword;
    }
    if (this.data.startDate) {
      requestData.startDate = this.data.startDate;
    }
    if (this.data.endDate) {
      requestData.endDate = this.data.endDate;
    }
    const accessType = accessTypeMap[this.data.accessTypeIndex];
    if (accessType !== null && accessType !== undefined) {
      requestData.accessType = accessType;
    }
    const verifyMethod = verifyMethodMap[this.data.verifyMethodIndex];
    if (verifyMethod !== null && verifyMethod !== undefined) {
      requestData.verifyMethod = verifyMethod;
    }
    if (this.data.emergencyOnly !== undefined && this.data.emergencyOnly !== null) {
      requestData.emergencyOnly = this.data.emergencyOnly;
    }

    request<any[]>({
      url: '/api/wx/security/access/records',
      method: 'GET',
      data: requestData
    }).then((list) => {
      this.setData({ list: list || [] });
    }).catch((err) => {
      wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
    });
  },

  goDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/security/historyDetail?id=${id}` });
  }
});
