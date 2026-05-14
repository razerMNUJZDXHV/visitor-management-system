/**
 * 创建预约页面
 * 
 * 功能：
 * 1. 填写访客信息（姓名、手机号、身份证号、来访事由）
 * 2. 选择预计到达时间和离开时间
 * 3. 检查预约开放状态和爽约状态
 * 4. 提交预约
 * 
 * @author Visitor System
 * @since 1.0
 */
import { request } from '../../utils/request';
import { calculateNavHeight, formatDateTime as formatDateTimeUtil, formatDateTimeForApi, formatDateTimeFromStr, getDaysInMonth, parseDateTime, validateIdCard } from '../../utils/util';

/** 预约开放状态接口 */
interface AppointmentOpenStatus {
  open: boolean;
  message?: string;
}

Page({
  /**
   * 页面数据
   */
  data: {
    navHeight: 0, // 导航栏高度
    form: { // 预约表单
      intervieweeName: '',
      visitorPhone: '',
      visitorName: '',
      visitorIdCard: '',
      visitReason: '',
      expectedStartTime: '',
      expectedEndTime: ''
    },
    banInfo: { bannedUntil: '', missedCount: 0 }, // 爽约状态信息
    showAppointmentClosedTip: false, // 是否显示预约关闭提示
    appointmentClosedMessage: '', // 预约关闭提示信息
    dailyLimit: 0, // 每日预约人数上限（0表示不限制）
    dailyLimitReached: false, // 今日预约是否已达上限
    reasonLength: 0, // 来访事由字数
    applyDate: '', // 申请日期（格式化）
    showPicker: false, // 是否显示时间选择器
    pickerType: 'start' as 'start' | 'end', // 选择器类型（到达/离开）
    pickerTitle: '', // 选择器标题
    pickerValue: [0, 0, 0, 0, 0], // 选择器当前值 [年,月,日,时,分]
    years: [] as number[], // 年份列表
    months: [] as number[], // 月份列表
    days: [] as number[], // 日期列表
    hours: [] as string[], // 小时列表
    minutes: [] as string[] // 分钟列表
  },

  /**
   * 页面加载
   * 初始化导航栏高度、填充手机号、设置默认申请日期
   */
  onLoad() {
    const phone = wx.getStorageSync('phone');
    const today = new Date();

    this.setData({
      navHeight: calculateNavHeight(),
      'form.visitorPhone': phone || '',
      applyDate: this.formatDateTime(today)
    });
  },

  /**
   * 页面显示（每次进入页面时触发）
   * 加载预约开放状态和爽约状态
   */
  onShow() {
    this.loadAppointmentOpenStatus();
    this.loadBanStatus();
    this.loadDailyLimit();
  },

  /**
   * 加载预约开放状态
   * 调用后端接口检查当前是否开放预约
   */
  loadAppointmentOpenStatus() {
    request<AppointmentOpenStatus>({
      url: '/api/wx/visitor/appointment/open-status',
      method: 'GET'
    })
      .then(data => {
        const open = data.open !== false;
        this.setData({
          showAppointmentClosedTip: !open,
          appointmentClosedMessage: open ? '' : (data.message || '当前预约未开放')
        });
      })
      .catch(() => {
        this.setData({
          showAppointmentClosedTip: false,
          appointmentClosedMessage: ''
        });
      });
  },

  /**
   * 加载爽约状态（核心：极简版，直接赋值）
   * 获取用户的爽约次数和禁止截止时间
   */
  loadBanStatus() {
    const userId = wx.getStorageSync('userId');
    if (!userId) return;

    request<{ bannedUntil: string | null; missedCount: number }>({
      url: '/api/wx/user/ban-status',
      method: 'GET',
      data: { userId }
    })
      .then(data => {
        this.setData({
          'banInfo.bannedUntil': data.bannedUntil ? formatDateTimeFromStr(data.bannedUntil) : '',
          'banInfo.missedCount': data.missedCount || 0
        });
      })
      .catch(() => {
        // 静默处理，避免打扰用户
      });
  },

  /**
   * 加载每日预约人数上限状态
   * 调用 /api/wx/visitor/appointment/daily-limit-status 获取上限和今日预约情况
   */
  loadDailyLimit() {
    request<{ dailyLimit: number; todayCount: number; reached: boolean }>({
      url: '/api/wx/visitor/appointment/daily-limit-status',
      method: 'GET'
    })
      .then(data => {
        this.setData({
          dailyLimit: data.dailyLimit || 0,
          dailyLimitReached: data.reached || false
        });
      })
      .catch(() => {
        // 静默处理，避免打扰用户
      });
  },

  // ==================== 时间格式化 ====================
  /**
   * 格式化日期时间为字符串
   * 格式：YYYY-MM-DDTHH:MM:SS
   * 
   * @param date 日期对象
   * @returns 格式化后的字符串
   */
  formatDateTime(date: Date): string {
    return formatDateTimeUtil(date);
  },

  /**
   * 解析日期时间字符串为Date对象
   * 
   * @param dateStr 日期时间字符串（格式：YYYY-MM-DDTHH:MM:SS）
   * @returns Date对象（解析失败返回null）
   */
  parseDateTimeStr(dateStr: string): Date | null {
    return parseDateTime(dateStr);
  },

  // ==================== 生成选择器列数据（无限制） ====================
  /**
   * 生成时间选择器的列数据
   * 年份：当前年 到 当前年+5
   * 月份：1-12
   * 天数：根据年月动态计算
   * 小时：00-23
   * 分钟：00-59
   * 
   * @param date 基准日期（用于计算天数）
   * @returns 选择器列数据
   */
  generateAllColumns(date: Date) {
    // 年份固定从当前年份开始，到 +5 年结束
    const currentYear = new Date().getFullYear();
    const years: number[] = [];
    for (let y = currentYear; y <= currentYear + 5; y++) years.push(y);

    const months: number[] = [];
    for (let m = 1; m <= 12; m++) months.push(m);

    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const daysInMonth = getDaysInMonth(year, month);
    const days: number[] = [];
    for (let d = 1; d <= daysInMonth; d++) days.push(d);

    const hours: string[] = [];
    for (let h = 0; h <= 23; h++) hours.push(String(h).padStart(2, '0'));

    const minutes: string[] = [];
    for (let m = 0; m <= 59; m++) minutes.push(String(m).padStart(2, '0'));

    return { years, months, days, hours, minutes };
  },

  // ==================== 打开选择器 ====================
  /**
   * 打开时间选择器
   * 
   * @param e 点击事件（包含data-type：start/end）
   */
  openPicker(e: any) {
    const type = e.currentTarget.dataset.type;
    const currentTimeStr = this.data.form[type === 'start' ? 'expectedStartTime' : 'expectedEndTime'];
    const now = new Date();
    let defaultDate: Date;

    if (currentTimeStr) {
      const parsed = this.parseDateTimeStr(currentTimeStr);
      defaultDate = parsed || now;
    } else {
      defaultDate = now;
    }

    const { years, months, days, hours, minutes } = this.generateAllColumns(defaultDate);
    const yearIndex = years.indexOf(defaultDate.getFullYear());
    const monthIndex = defaultDate.getMonth();
    const dayIndex = defaultDate.getDate() - 1;
    const hourIndex = defaultDate.getHours();
    const minuteIndex = defaultDate.getMinutes();

    this.setData({
      showPicker: true,
      pickerType: type,
      pickerTitle: type === 'start' ? '选择到达时间' : '选择离开时间',
      years,
      months,
      days,
      hours,
      minutes,
      pickerValue: [yearIndex, monthIndex, dayIndex, hourIndex, minuteIndex]
    });
  },

  /**
   * 关闭时间选择器
   */
  closePicker() {
    this.setData({ showPicker: false });
  },

  /**
   * 阻止滚动（用于弹窗背景）
   */
  preventMove() {
    return false;
  },

  // ==================== 选择器滑动更新天数 ====================
  /**
   * 选择器值改变事件
   * 当年份或月份改变时，重新计算天数
   * 
   * @param e 改变事件
   */
  onPickerChange(e: any) {
    const newVal = e.detail.value;
    const { years, months, pickerValue } = this.data;
    const year = years[newVal[0]];
    const month = months[newVal[1]];

    if (newVal[0] !== pickerValue[0] || newVal[1] !== pickerValue[1]) {
      // 年份或月份改变，重新计算天数
      const daysInMonth = getDaysInMonth(year, month);
      const days: number[] = [];
      for (let d = 1; d <= daysInMonth; d++) days.push(d);

      let dayIndex = newVal[2];
      if (dayIndex >= days.length) dayIndex = days.length - 1;

      this.setData({
        days,
        pickerValue: [newVal[0], newVal[1], dayIndex, newVal[3], newVal[4]]
      });
      return;
    }

    this.setData({ pickerValue: newVal });
  },

  // ==================== 确认选择（校验逻辑） ====================
  confirmPicker() {
    const { years, months, hours, minutes, pickerValue, pickerType } = this.data;
    const year = years[pickerValue[0]];
    const month = months[pickerValue[1]];

    // 重新计算天数并修正索引
    const daysInMonth = getDaysInMonth(year, month);
    const days: number[] = [];
    for (let d = 1; d <= daysInMonth; d++) days.push(d);

    let dayIndex = pickerValue[2];
    if (dayIndex >= days.length) dayIndex = days.length - 1;
    const day = days[dayIndex];

    this.setData({
      days,
      pickerValue: [pickerValue[0], pickerValue[1], dayIndex, pickerValue[3], pickerValue[4]]
    });

    const hour = hours[pickerValue[3]];
    const minute = minutes[pickerValue[4]];
    const selectedDate = new Date(year, month - 1, day, parseInt(hour), parseInt(minute), 0);
    const finalValue = formatDateTimeForApi(selectedDate);
    const now = new Date();

    if (pickerType === 'start') {
      if (selectedDate < now) {
        wx.showToast({ title: '到达时间不能早于当前时间', icon: 'none' });
        return;
      }

      const endTimeStr = this.data.form.expectedEndTime;
      if (endTimeStr) {
        const endTime = this.parseDateTimeStr(endTimeStr);
        if (endTime && selectedDate >= endTime) {
          this.setData({
            'form.expectedStartTime': finalValue,
            'form.expectedEndTime': ''
          });
          wx.showToast({ title: '离开时间已清空，请重新选择', icon: 'none' });
          this.closePicker();
          return;
        }
      }
      this.setData({ 'form.expectedStartTime': finalValue });
      this.closePicker();
      return;
    }

    // 离开时间校验
    if (pickerType === 'end') {
      const startTimeStr = this.data.form.expectedStartTime;
      if (!startTimeStr) {
        this.setData({ 'form.expectedEndTime': finalValue });
        this.closePicker();
        return;
      }

      const startTime = this.parseDateTimeStr(startTimeStr);
      if (!startTime) {
        wx.showToast({ title: '请先选择有效的到达时间', icon: 'none' });
        return;
      }

      if (selectedDate <= startTime) {
        wx.showToast({ title: '离开时间必须晚于到达时间', icon: 'none' });
        return; // 不关闭弹窗
      }

      this.setData({ 'form.expectedEndTime': finalValue });
      this.closePicker();
    }
  },

  // ==================== 输入事件 ====================
  handleInput(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  handleIdCardInput(e: any) {
    let value = e.detail.value.toUpperCase();
    value = value.replace(/[^0-9X]/g, '');
    this.setData({ 'form.visitorIdCard': value });
  },

  handleReasonInput(e: any) {
    const value = e.detail.value;
    const length = value.length;
    this.setData({ 'form.visitReason': value, reasonLength: length });
    if (length >= 500) {
      wx.showToast({ title: '来访事由最多输入500字', icon: 'none', duration: 1500 });
    }
  },

  // ==================== 重置与提交 ====================
  handleReset() {
    this.setData({
      form: {
        intervieweeName: '',
        visitorPhone: wx.getStorageSync('phone') || '',
        visitorName: '',
        visitorIdCard: '',
        visitReason: '',
        expectedStartTime: '',
        expectedEndTime: ''
      },
      reasonLength: 0,
      applyDate: this.formatDateTime(new Date())
    });
    wx.showToast({ title: '已重置', icon: 'success' });
  },

  handleSubmit() {
    if (this.data.showAppointmentClosedTip) {
      wx.showToast({ title: this.data.appointmentClosedMessage || '当前预约未开放', icon: 'none' });
      return;
    }

    const { visitorName, visitorIdCard, visitReason, expectedStartTime, expectedEndTime } = this.data.form;

    if (!visitorName.trim()) {
      wx.showToast({ title: '请输入申请人姓名', icon: 'none' });
      return;
    }
    if (!validateIdCard(visitorIdCard)) {
      wx.showToast({ title: '请输入有效的18位身份证号', icon: 'none' });
      return;
    }
    if (!visitReason.trim()) {
      wx.showToast({ title: '请输入来访事由', icon: 'none' });
      return;
    }
    if (!expectedStartTime) {
      wx.showToast({ title: '请选择预计到达时间', icon: 'none' });
      return;
    }
    if (!expectedEndTime) {
      wx.showToast({ title: '请选择预计离开时间', icon: 'none' });
      return;
    }

    const startDate = this.parseDateTimeStr(expectedStartTime);
    const endDate = this.parseDateTimeStr(expectedEndTime);
    const now = new Date();

    if (!startDate || !endDate) {
      wx.showToast({ title: '日期格式错误', icon: 'none' });
      return;
    }
    if (startDate < now) {
      wx.showToast({ title: '到达时间不能早于当前时间', icon: 'none' });
      return;
    }
    if (startDate >= endDate) {
      wx.showToast({ title: '离开时间必须晚于到达时间', icon: 'none' });
      return;
    }

    // 直接提交预约（应用内通知会自动轮询）
    this.doSubmit();
  },

  doSubmit() {
    wx.showLoading({ title: '提交中...' });
    const visitorId = wx.getStorageSync('userId');
    
    request({
      url: `/api/wx/visitor/appointment/create?visitorId=${visitorId}`,
      method: 'POST',
      data: this.data.form
    })
    .then(() => {
      wx.hideLoading();
      wx.showToast({ title: '预约提交成功', icon: 'success' });
  
      const currentUserInfo = wx.getStorageSync('userInfo') || {};
      currentUserInfo.realName = this.data.form.visitorName;
      wx.setStorageSync('userInfo', currentUserInfo);
  
      this.setData({
        'form.intervieweeName': '',
        'form.visitReason': '',
        'form.expectedStartTime': '',
        'form.expectedEndTime': '',
        reasonLength: 0
      });
  
      setTimeout(() => {
        wx.switchTab({ url: '/pages/appointment/list' });
      }, 1500);
    })
    .catch(() => {
      wx.hideLoading();
      // request 拦截器已处理错误提示
    });
  }
});