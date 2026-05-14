/**
 * 公共工具函数库
 * 
 * 包含项目中重复使用的工具函数，避免代码重复
 * 
 * @author Visitor System
 * @since 1.0
 */

// ==================== 日期时间工具 ====================

/**
 * 获取某月的天数
 * @param year 年份
 * @param month 月份 (1-12)
 * @returns 该月的天数
 */
export function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

/**
 * 数字补零 (1 -> "01")
 * @param n 数字
 * @returns 补零后的字符串
 */
export function pad(n: number): string {
  return String(n).padStart(2, '0');
}

/**
 * 格式化日期时间为字符串 (YYYY-MM-DD HH:mm:ss)
 * @param date 日期对象或日期字符串
 * @returns 格式化后的字符串
 */
export function formatDateTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  const year = d.getFullYear();
  const month = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const hour = pad(d.getHours());
  const minute = pad(d.getMinutes());
  const second = pad(d.getSeconds());
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

/**
 * 格式化完整日期时间 (YYYY-MM-DD HH:mm:ss)
 * @param date 日期对象或日期字符串
 * @returns 格式化后的字符串
 */
export function formatFullDateTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  const year = d.getFullYear();
  const month = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const hour = pad(d.getHours());
  const minute = pad(d.getMinutes());
  const second = pad(d.getSeconds());
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

/**
 * 格式化日期 (YYYY-MM-DD)
 * @param date 日期对象
 * @returns 格式化后的日期字符串
 */
export function formatDateOnly(date: Date): string {
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  return `${year}-${month}-${day}`;
}

/**
 * 格式化日期时间字符串 (用于API，格式: YYYY-MM-DDTHH:mm:ss)
 * @param date 日期对象
 * @returns 格式化后的字符串
 */
export function formatDateTimeForApi(date: Date): string {
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  const second = pad(date.getSeconds());
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
}

/**
 * 解析日期时间字符串为 Date 对象
 * @param dateStr 日期时间字符串 (格式: YYYY-MM-DDTHH:MM:SS)
 * @returns Date对象，解析失败返回null
 */
export function parseDateTime(dateStr: string): Date | null {
  if (!dateStr) return null;
  const parts = dateStr.split(/[-T:]/);
  if (parts.length >= 5) {
    return new Date(
      parseInt(parts[0]),
      parseInt(parts[1]) - 1,
      parseInt(parts[2]),
      parseInt(parts[3]),
      parseInt(parts[4]),
      parts[5] ? parseInt(parts[5]) : 0
    );
  }
  return null;
}

/**
 * 格式化日期时间字符串 (从后端接收，格式: "2024-01-01T12:00:00" -> "2024-01-01 12:00:00")
 * @param value 日期时间字符串
 * @returns 格式化后的字符串，空值返回 '—'
 */
export function formatDateTimeFromStr(value?: string): string {
  if (!value) {
    return '—';
  }
  return value.replace('T', ' ').slice(0, 19);
}

// ==================== 导航栏工具 ====================

/**
 * 计算导航栏高度
 * @returns 导航栏总高度
 */
export function calculateNavHeight(): number {
  const systemInfo = wx.getSystemInfoSync();
  const statusBarHeight = systemInfo.statusBarHeight || 20;
  const navContentHeight = 44;
  return statusBarHeight + navContentHeight;
}

// ==================== 预约状态工具 ====================

/**
 * 预约状态枚举
 */
export enum AppointmentStatus {
  PENDING = 0,      // 待审核
  APPROVED = 1,     // 预约成功/已同意
  REJECTED = 2,     // 预约失败/已拒绝
  CANCELLED = 3,    // 已取消
  CHECKED_IN = 4,   // 已签到
  COMPLETED = 5,    // 已完成
  EXPIRED = 6       // 已过期
}

/**
 * 获取预约状态文本
 * @param status 状态值
 * @returns 状态文本
 */
export function getStatusText(status: number): string {
  const map: Record<number, string> = {
    [AppointmentStatus.PENDING]: '待审核',
    [AppointmentStatus.APPROVED]: '预约成功',
    [AppointmentStatus.REJECTED]: '预约失败',
    [AppointmentStatus.CANCELLED]: '已取消',
    [AppointmentStatus.CHECKED_IN]: '已签到',
    [AppointmentStatus.COMPLETED]: '已完成',
    [AppointmentStatus.EXPIRED]: '已过期'
  };
  return map[status] || '未知';
}

/**
 * 获取预约状态样式类
 * @param status 状态值
 * @returns 样式类名
 */
export function getStatusClass(status: number): string {
  const map: Record<number, string> = {
    [AppointmentStatus.PENDING]: 'status-pending',
    [AppointmentStatus.APPROVED]: 'status-success',
    [AppointmentStatus.REJECTED]: 'status-fail',
    [AppointmentStatus.CANCELLED]: 'status-cancel',
    [AppointmentStatus.CHECKED_IN]: 'status-checkin',
    [AppointmentStatus.COMPLETED]: 'status-complete',
    [AppointmentStatus.EXPIRED]: 'status-expire'
  };
  return map[status] || '';
}

/**
 * 审批状态枚举 (仅用于审批历史)
 */
export enum ApproveHistoryStatus {
  APPROVED = 1,     // 已同意
  REJECTED = 2      // 已拒绝
}

/**
 * 获取审批历史状态文本 (仅已同意/已拒绝)
 * @param status 状态值
 * @returns 状态文本
 */
export function getApproveHistoryStatusText(status: number): string {
  if (status === ApproveHistoryStatus.REJECTED) return '已拒绝';
  return '已同意';
}

/**
 * 获取审批历史状态样式类
 * @param status 状态值
 * @returns 样式类名
 */
export function getApproveHistoryStatusClass(status: number): string {
  if (status === ApproveHistoryStatus.REJECTED) return 'status-fail';
  return 'status-success';
}

// ==================== 验证工具 ====================

/**
 * 身份证号码正则
 */
export const ID_CARD_REGEX = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;

/**
 * 手机号码正则 (中国)
 */
export const PHONE_REGEX = /^1[3-9]\d{9}$/;

/**
 * 验证身份证号码
 * @param idCard 身份证号
 * @returns 是否有效
 */
export function validateIdCard(idCard: string): boolean {
  return ID_CARD_REGEX.test(idCard);
}

/**
 * 验证手机号码
 * @param phone 手机号
 * @returns 是否有效
 */
export function validatePhone(phone: string): boolean {
  return PHONE_REGEX.test(phone);
}

// ==================== 访问类型工具 ====================

/**
 * 访问类型枚举
 */
export enum AccessType {
  SIGN_IN = 1,      // 签到
  SIGN_OUT = 2      // 签离
}

/**
 * 获取访问类型文本
 * @param type 类型值
 * @returns 类型文本
 */
export function getAccessTypeText(type: number): string {
  const map: Record<number, string> = {
    [AccessType.SIGN_IN]: '签到',
    [AccessType.SIGN_OUT]: '签离'
  };
  return map[type] || '未知';
}

/**
 * 验证方式枚举
 */
export enum VerifyMethod {
  SCAN = 1,         // 扫码
  MANUAL = 2        // 手动
}

/**
 * 获取验证方式文本
 * @param method 验证方式值
 * @returns 方式文本
 */
export function getVerifyMethodText(method: number): string {
  const map: Record<number, string> = {
    [VerifyMethod.SCAN]: '扫码',
    [VerifyMethod.MANUAL]: '手动'
  };
  return map[method] || '未知';
}

// ==================== 日期级联选择工具 ====================

/**
 * 生成年份列表（当前年 ~ 当前年-5）
 */
export function buildYearList(): number[] {
  const now = new Date();
  const years: number[] = [];
  for (let y = now.getFullYear(); y >= now.getFullYear() - 5; y--) {
    years.push(y);
  }
  return years;
}

/**
 * 生成月份列表（1-12）
 */
export function buildMonthList(): number[] {
  const months: number[] = [];
  for (let m = 1; m <= 12; m++) months.push(m);
  return months;
}

/**
 * 根据年月生成天数列表
 */
export function buildDayList(year: number, month: number): number[] {
  const daysCount = getDaysInMonth(year, month);
  const days: number[] = [];
  for (let d = 1; d <= daysCount; d++) days.push(d);
  return days;
}

/**
 * 解析日期字符串，返回 [year, month, day]（失败返回 null）
 */
export function parseDateParts(dateStr: string): [number, number, number] | null {
  if (!dateStr) return null;
  const parts = dateStr.split('-');
  if (parts.length >= 3) {
    return [parseInt(parts[0], 10), parseInt(parts[1], 10), parseInt(parts[2], 10)];
  }
  return null;
}

/**
 * 初始化日期级联选择器的数据
 * @param dateStr 当前已选日期字符串 (YYYY-MM-DD)，为空则使用今天
 * @returns 用于 setData 的对象
 */
export function initCascadeFromDate(dateStr: string | null, type: 'start' | 'end') {
  const now = new Date();
  const parts = dateStr ? parseDateParts(dateStr) : null;
  const selYear = parts ? parts[0] : now.getFullYear();
  const selMonth = parts ? parts[1] : now.getMonth() + 1;
  const selDay = parts ? parts[2] : now.getDate();

  const years = buildYearList();
  const months = buildMonthList();
  const daysCount = getDaysInMonth(selYear, selMonth);
  const days = buildDayList(selYear, selMonth);

  const prefix = type === 'start' ? 'start' : 'end';

  const result: Record<string, any> = {};
  result[`${prefix}Years`] = years;
  result[`${prefix}Months`] = months;
  result[`${prefix}Days`] = days;
  result[`${prefix}Year`] = selYear;
  result[`${prefix}Month`] = selMonth;
  result[`${prefix}Day`] = Math.min(selDay, daysCount);
  return result;
}

/**
 * 选择年份后，更新月份和天数
 * @returns 用于 setData 的对象
 */
export function onCascadeYearChange(type: 'start' | 'end', year: number) {
  const months = buildMonthList();
  const days = buildDayList(year, 1);
  const prefix = type === 'start' ? 'start' : 'end';
  const result: Record<string, any> = {};
  result[`${prefix}Year`] = year;
  result[`${prefix}Month`] = 1;
  result[`${prefix}Day`] = 1;
  result[`${prefix}Months`] = months;
  result[`${prefix}Days`] = days;
  return result;
}

/**
 * 选择月份后，更新天数
 * @returns 用于 setData 的对象
 */
export function onCascadeMonthChange(type: 'start' | 'end', year: number, month: number, currentDay: number) {
  const daysCount = getDaysInMonth(year, month);
  const days = buildDayList(year, month);
  const prefix = type === 'start' ? 'start' : 'end';
  const result: Record<string, any> = {};
  result[`${prefix}Month`] = month;
  result[`${prefix}Day`] = Math.min(currentDay, daysCount);
  result[`${prefix}Days`] = days;
  return result;
}

/**
 * 根据级联选择器的当前值，生成格式化的日期字符串 (YYYY-MM-DD)
 */
export function buildDateFromCascade(year: number, month: number, day: number): string {
  return `${year}-${pad(month)}-${pad(day)}`;
}
