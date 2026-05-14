import { request } from '../../utils/request';
import { calculateNavHeight, formatDateTimeFromStr, initCascadeFromDate, onCascadeYearChange, onCascadeMonthChange, buildDateFromCascade } from '../../utils/util';

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
    this.setData({ navHeight: calculateNavHeight() });
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
    if (!startYear || !startMonth || !startDay) return;
    const date = buildDateFromCascade(startYear, startMonth, startDay);
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
    if (!endYear || !endMonth || !endDay) return;
    const date = buildDateFromCascade(endYear, endMonth, endDay);
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
    if (this.data.emergencyOnly) {
      requestData.emergencyOnly = true;
    }

    request<any[]>({
      url: '/api/wx/security/access/records',
      method: 'GET',
      data: requestData
    }).then((list) => {
      const formattedList = (list || []).map((item: any) => ({
        ...item,
        accessTime: formatDateTimeFromStr(item.accessTime)
      }));
      this.setData({ list: formattedList });
    }).catch((err) => {
      wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
    });
  },

  goDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/security/historyDetail?id=${encodeURIComponent(id)}` });
  }
});
