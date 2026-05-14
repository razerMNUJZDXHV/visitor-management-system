/**
 * 通用表单校验正则常量
 */

// 手机号：1 开头的 11 位数字，第二位为 3-9
export const PHONE_REGEX = /^1[3-9]\d{9}$/

// 密码：必须包含字母和数字，长度 6-20 位
export const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/