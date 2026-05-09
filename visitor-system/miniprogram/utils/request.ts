/**
 * 网络请求工具
 * 封装微信小程序的网络请求，支持JWT认证、自动刷新token、登录态管理
 * 
 * 功能：
 * 1. 统一请求封装（自动拼接BASE_URL）
 * 2. JWT认证（自动添加Authorization header）
 * 3. 自动刷新token（access token过期时使用refresh token）
 * 4. 登录态管理（保存、清除、引导重新登录）
 * 5. 统一错误处理
 * 
 * @author Visitor System
 * @since 1.0
 */

// ==================== 配置常量 ====================
const isDev = true; // 是否为开发环境
const DEV_IP = '192.168.56.1'; // 开发环境电脑的实际局域网IP
const BASE_URL = isDev ? `http://${DEV_IP}:8080` : 'https://你的生产域名';
const AUTH_LOGIN_URL = '/api/wx/user/login'; // 登录接口路径
const AUTH_REFRESH_URL = '/api/wx/user/refresh-token'; // 刷新token接口路径
const AUTH_EXPIRED_MESSAGE = '登录状态失效，请重新登录'; // 认证过期提示

// ==================== 类型定义 ====================
/** 登录响应数据 */
interface LoginResponseData {
  userId: number;
  openid: string;
  userType: number;
  phone: string | null;
  realName: string | null;
  token: string;
  refreshToken: string;
}

/** 请求选项（扩展微信请求选项） */
interface RequestOptions extends WechatMiniprogram.RequestOption {
  url: string;
  skipAuthRefresh?: boolean; // 是否跳过自动刷新token
}

/** 响应信封（统一响应格式） */
interface ResponseEnvelope<T = any> {
  code: number;
  msg?: string;
  data?: T;
}

// ==================== 全局状态 ====================
let refreshingAuthPromise: Promise<boolean> | null = null; // 刷新token的Promise（防止并发）
let redirectingToLogin = false; // 是否正在重定向到登录页

// ==================== 工具函数 ====================

/**
 * 判断是否为登录相关请求（避免无限循环）
 */
const isLoginRequest = (url: string) => {
  return url.indexOf(AUTH_LOGIN_URL) !== -1 || url.indexOf(AUTH_REFRESH_URL) !== -1;
};

/**
 * 构建请求头
 * 
 * @param options 请求选项
 * @param includeAuth 是否包含认证信息
 * @returns 请求头对象
 */
const buildHeader = (options: RequestOptions, includeAuth: boolean) => {
  const header: Record<string, string> = {
    'Content-Type': 'application/json'
  };

  if (includeAuth) {
    header.Authorization = 'Bearer ' + (wx.getStorageSync('token') || '');
  }

  return {
    ...header,
    ...options.header
  };
};

/**
 * 发送请求（底层封装）
 * 
 * @param options 请求选项
 * @param includeAuth 是否包含认证信息
 * @returns Promise<{statusCode, data}>
 */
const sendRequest = <T = any>(options: RequestOptions, includeAuth = true): Promise<{ statusCode: number; data: T }> => {
  const { skipAuthRefresh, ...requestOptions } = options;

  return new Promise((resolve, reject) => {
    wx.request({
      ...requestOptions,
      url: BASE_URL + requestOptions.url,
      header: buildHeader(requestOptions, includeAuth),
      success(res) {
        resolve({
          statusCode: res.statusCode,
          data: res.data as T
        });
      },
      fail(err) {
        reject(err);
      }
    });
  });
};

/**
 * 保存登录态到本地存储
 * 
 * @param loginRes 登录响应数据
 * @param updateSessionAt 是否更新session时间戳
 */
const saveLoginSession = (loginRes: LoginResponseData, updateSessionAt = true) => {
  wx.setStorageSync('token', loginRes.token);
  if (loginRes.refreshToken) {
    wx.setStorageSync('refreshToken', loginRes.refreshToken);
  }
  wx.setStorageSync('userId', loginRes.userId);
  wx.setStorageSync('openid', loginRes.openid);
  wx.setStorageSync('userType', loginRes.userType);
  wx.setStorageSync('realName', loginRes.realName || '');

  if (loginRes.phone) {
    wx.setStorageSync('phone', loginRes.phone);
  } else {
    wx.removeStorageSync('phone');
  }

  if (updateSessionAt) {
    wx.setStorageSync('loginSessionAt', Date.now());
  }
};

/**
 * 清除登录态（退出登录或token失效时调用）
 */
const clearLoginSession = () => {
  wx.removeStorageSync('token');
  wx.removeStorageSync('refreshToken');
  wx.removeStorageSync('userId');
  wx.removeStorageSync('openid');
  wx.removeStorageSync('userType');
  wx.removeStorageSync('realName');
  wx.removeStorageSync('phone');
  wx.removeStorageSync('loginSessionAt');
  wx.removeStorageSync('tempCode');
};

/**
 * 微信登录（获取临时code）
 * 
 * @returns Promise<code>
 */
const wxLoginAsync = () => {
  return new Promise<string>((resolve, reject) => {
    wx.login({
      success(res) {
        if (res.code) {
          resolve(res.code);
          return;
        }
        reject(new Error('获取微信登录凭证失败'));
      },
      fail() {
        reject(new Error('微信登录失败'));
      }
    });
  });
};

/**
 * 使用 refresh token 刷新 access token
 * 自动防止并发刷新
 * 
 * @returns Promise<boolean> true-刷新成功 false-刷新失败
 */
const refreshAuthSession = async (): Promise<boolean> => {
  // 如果正在刷新，返回同一个Promise（防止并发）
  if (refreshingAuthPromise) {
    return refreshingAuthPromise;
  }

  const refreshToken = wx.getStorageSync('refreshToken');
  if (!refreshToken) {
    return false;
  }

  refreshingAuthPromise = (async () => {
    try {
      const response = await sendRequest<ResponseEnvelope<{ accessToken: string; refreshToken: string }>>(
        {
          url: AUTH_REFRESH_URL,
          method: 'POST',
          data: { refreshToken }
        },
        false // 刷新token的请求不包含认证信息
      );

      const payload = response.data;
      if (response.statusCode !== 200 || !payload || payload.code !== 200 || !payload.data) {
        throw new Error(payload && payload.msg ? payload.msg : '刷新登录态失败');
      }

      // 保存新的token
      wx.setStorageSync('token', payload.data.accessToken);
      wx.setStorageSync('refreshToken', payload.data.refreshToken);
      
      return true;
    } catch (error) {
      console.error('自动刷新登录态失败：', error);
      return false;
    } finally {
      refreshingAuthPromise = null;
    }
  })();

  return refreshingAuthPromise;
};

/**
 * 重定向到登录页
 * 
 * @param message 提示信息
 */
const redirectToLogin = (message = AUTH_EXPIRED_MESSAGE) => {
  if (redirectingToLogin) {
    return; // 防止重复重定向
  }

  redirectingToLogin = true;
  clearLoginSession(); // 清除登录态
  wx.showToast({ title: message, icon: 'none' });
  setTimeout(() => {
    wx.reLaunch({ url: '/pages/index/index' }); // 重启到首页
    setTimeout(() => {
      redirectingToLogin = false;
    }, 1200);
  }, 300);
};

// ==================== 导出函数 ====================

/**
 * 启动时的认证恢复
 * App.onLaunch时调用，尝试用refreshToken恢复登录态
 * 
 * @returns Promise<boolean> true-恢复成功 false-需要重新登录
 */
export const bootstrapAuthSession = async (): Promise<boolean> => {
  // 检查本地是否有登录信息
  if (!wx.getStorageSync('token') || !wx.getStorageSync('userType') || !wx.getStorageSync('userId')) {
    return false;
  }

  // 尝试刷新token
  return refreshAuthSession();
};

/**
 * 发起网络请求（带认证自动刷新）
 * 
 * 使用方式：import { request } from '../../utils/request';
 * request<DataType>({ url: '/api/...', method: 'GET' }).then(data => { ... })
 * 
 * @param options 请求选项
 * @returns Promise<T> 响应数据
 */
export const request = <T = any>(options: RequestOptions): Promise<T> => {
  const execute = async (allowRefresh: boolean): Promise<T> => {
    let response: { statusCode: number; data: ResponseEnvelope<T> };

    try {
      response = await sendRequest<ResponseEnvelope<T>>(options, true);
    } catch (err) {
      wx.showToast({ title: '网络异常，请检查连接', icon: 'none' });
      throw err;
    }

    const data = response.data || ({} as ResponseEnvelope<T>);
    const authExpired = response.statusCode === 401 || data.code === 401;

    // 认证过期处理
    if (authExpired) {
      const canRefresh = allowRefresh 
        && !options.skipAuthRefresh 
        && !isLoginRequest(options.url) 
        && Boolean(wx.getStorageSync('refreshToken'));
      if (canRefresh) {
        const refreshed = await refreshAuthSession();
        if (refreshed) {
          return execute(false); // 刷新成功后重试（不再允许刷新）
        }
      }

      redirectToLogin(data.msg || AUTH_EXPIRED_MESSAGE);
      throw { code: 401, msg: data.msg || AUTH_EXPIRED_MESSAGE };
    }

    // 成功
    if (data.code === 200) {
      return data.data as T;
    }

    // 业务错误
    wx.showToast({ title: data.msg || '请求失败', icon: 'none' });
    throw data;
  };

  return execute(true);
};
