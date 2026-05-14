import { request } from '../../utils/request';

Page({
  async onLoad() {
    const app = getApp<IAppOption>();
    const bootstrapPromise = app.globalData.authBootstrapPromise;
    if (bootstrapPromise) {
      const refreshed = await bootstrapPromise.catch(() => false);
      if (refreshed) {
        const token = wx.getStorageSync('token');
        const userType = wx.getStorageSync('userType');
        if (token && userType) {
          this.redirectByUserType(userType);
          return;
        }
      }

      this.doWxLogin();
      return;
    }

    const token = wx.getStorageSync('token');
    const userType = wx.getStorageSync('userType');
    if (token && userType) {
      this.redirectByUserType(userType);
      return;
    }
    this.doWxLogin();
  },

  doWxLogin() {
    wx.showLoading({ title: '加载中...' });
    wx.login({
      success: (res) => {
        wx.hideLoading();
        if (res.code) {
          wx.setStorageSync('tempCode', res.code);
        } else {
          wx.showToast({ title: '获取登录凭证失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '微信登录失败', icon: 'none' });
      }
    });
  },

  async handleGetPhoneNumber(e: any) {
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      wx.showToast({ title: '需要授权手机号才能使用', icon: 'none' });
      return;
    }

    const tempCode = wx.getStorageSync('tempCode');
    if (!tempCode) {
      wx.showToast({ title: '登录凭证已过期，请重试', icon: 'none' });
      this.doWxLogin();
      return;
    }

    try {
      wx.showLoading({ title: '登录中...' });

      // 新版：一次性完成登录+手机号绑定
      const loginRes = await request<{
        userId: number;
        openid: string;
        userType: number;
        phone: string | null;
        realName: string | null;
        token: string;
        refreshToken: string;
      }>({
        url: '/api/wx/user/login',
        method: 'POST',
        data: { 
          code: tempCode,
          encryptedData: e.detail.encryptedData,
          iv: e.detail.iv
        }
      });

      wx.hideLoading();

      // 保存用户信息
      wx.setStorageSync('token', loginRes.token);
      wx.setStorageSync('refreshToken', loginRes.refreshToken);
      wx.setStorageSync('userId', loginRes.userId);
      wx.setStorageSync('openid', loginRes.openid);
      wx.setStorageSync('userType', loginRes.userType);
      wx.setStorageSync('phone', loginRes.phone || '');
      wx.setStorageSync('realName', loginRes.realName || '');
      wx.setStorageSync('loginSessionAt', Date.now());

      wx.showToast({ title: '登录成功', icon: 'success' });
      this.redirectByUserType(loginRes.userType);
    } catch (err: any) {
      wx.hideLoading();
      // 不重复弹窗，request 已提示
    }
  },

  redirectByUserType(userType: number) {
    switch (userType) {
      case 1:
        wx.reLaunch({ url: '/pages/appointment/create' });
        break;
      case 2:
        wx.reLaunch({ url: '/pages/approver/index' });
        break;
      case 3:
        wx.reLaunch({ url: '/pages/security/index' });
        break;
      case 4:
        wx.showModal({
          title: '提示',
          content: '管理员请使用Web端登录',
          showCancel: false,
          success: () => {
            (wx as any).exitMiniProgram();
          }
        });
        break;
      default:
        wx.reLaunch({ url: '/pages/appointment/create' });
    }
  }
});