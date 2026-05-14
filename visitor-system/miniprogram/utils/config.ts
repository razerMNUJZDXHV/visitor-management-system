/**
 * 配置文件
 * 集中管理项目中的常量配置
 */

// ==================== 网络配置 ====================

/** 是否为开发环境 */
export const IS_DEV = true;

/** 开发环境电脑的实际局域网IP */
export const DEV_IP = '192.168.56.1';

/** 基础请求地址 */
export const BASE_URL = IS_DEV ? `http://${DEV_IP}:8080` : 'https://你的生产域名';

/** 登录接口路径 */
export const AUTH_LOGIN_URL = '/api/wx/user/login';

/** 刷新token接口路径 */
export const AUTH_REFRESH_URL = '/api/wx/user/refresh-token';

/** 认证过期提示 */
export const AUTH_EXPIRED_MESSAGE = '登录状态失效，请重新登录';

// ==================== 地图配置 ====================

/** 腾讯地图 WebService API Key */
export const TX_MAP_API_KEY = 'R2XBZ-MY56J-QN6FJ-XPSJD-QTNV2-BXBRA';

/** 默认坐标（学校中心） */
export const DEFAULT_LOCATION = {
  longitude: 116.803572,
  latitude: 39.958321
} as const;
