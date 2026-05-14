Component({
  data: {
    selected: 0,
    list: [] as any[]
  },

  lifetimes: {
    attached() {
      this.updateTabBar();
    }
  },

  pageLifetimes: {
    show() {
      this.updateTabBar();
    }
  },

  methods: {
    // 根据 userType 更新菜单列表
    updateTabBar() {
      const userType = Number(wx.getStorageSync('userType') || 1);
      let list: any[] = [];
      if (userType === 1) {
        list = [
          { pagePath: 'pages/appointment/create', text: '预约申请', iconPath: '/images/appointment.png', selectedIconPath: '/images/appointment-active.png' },
          { pagePath: 'pages/appointment/list', text: '我的预约', iconPath: '/images/list.png', selectedIconPath: '/images/list-active.png' },
          { pagePath: 'pages/appointment/map', text: '校园地图', iconPath: '/images/map.png', selectedIconPath: '/images/map-active.png' }
        ];
      } else if (userType === 2) {
        list = [
          { pagePath: 'pages/approver/index', text: '待审批', iconPath: '/images/approval-list.png', selectedIconPath: '/images/approval-list-active.png' },
          { pagePath: 'pages/approver/history', text: '审批记录', iconPath: '/images/history.png', selectedIconPath: '/images/history-active.png' }
        ];
      } else if (userType === 3) {
        list = [
          { pagePath: 'pages/security/index', text: '核验登记', iconPath: '/images/verify.png', selectedIconPath: '/images/verify-active.png' },
          { pagePath: 'pages/security/history', text: '通行记录', iconPath: '/images/record.png', selectedIconPath: '/images/record-active.png' },
          { pagePath: 'pages/security/stats', text: '数据统计', iconPath: '/images/statistic.png', selectedIconPath: '/images/statistic-active.png' }
        ];
      }
      this.setData({ list }, () => {
        this.updateSelected();
      });
    },

    // 更新选中状态
    updateSelected() {
      const pages = getCurrentPages();
      if (!pages || pages.length === 0) return;
      const currentPage = pages[pages.length - 1];
      if (!currentPage) return;
      const route = currentPage.route;
      const list = this.data.list;
      const index = list.findIndex((item: any) => item.pagePath === route);
      if (index !== -1) {
        this.setData({ selected: index });
      }
    },

    // 切换 Tab
    switchTab(e: any) {
      const path = e.currentTarget.dataset.path;
      const userType = Number(wx.getStorageSync('userType') || 1);

      if (userType === 3) {
        wx.reLaunch({ url: `/${path}` });
        return;
      }

      wx.switchTab({ url: `/${path}` });
    }
  }
});