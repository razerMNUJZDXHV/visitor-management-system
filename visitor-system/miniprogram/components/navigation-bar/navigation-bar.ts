Component({
  options: {
    multipleSlots: true
  },
  properties: {
    extClass: {
      type: String,
      value: ''
    },
    title: {
      type: String,
      value: '应大访客预约系统'
    },
    background: {
      type: String,
      value: '#1677ff'
    },
    color: {
      type: String,
      value: '#ffffff'
    },
    back: {
      type: Boolean,
      value: true
    },
    loading: {
      type: Boolean,
      value: false
    },
    homeButton: {
      type: Boolean,
      value: false
    },
    animated: {
      type: Boolean,
      value: true
    },
    show: {
      type: Boolean,
      value: true,
      observer: '_showChange'
    },
    delta: {
      type: Number,
      value: 1
    }
  },
  data: {
    displayStyle: '',
    ios: false,
    innerPaddingRight: '',
    leftWidth: '',
    safeAreaTop: ''
  },
  lifetimes: {
    attached() {
      const rect = wx.getMenuButtonBoundingClientRect()
      const res = wx.getSystemInfoSync()
      if (res) {
        const isAndroid = res.platform === 'android'
        const isDevtools = res.platform === 'devtools'
        this.setData({
          ios: !isAndroid,
          innerPaddingRight: `padding-right: ${res.windowWidth - rect.left}px`,
          leftWidth: `width: ${res.windowWidth - rect.left}px`,
          safeAreaTop: `height: calc(44px + ${res.safeArea.top}px); padding-top: ${res.safeArea.top}px`
        })
      }
    }
  },
  methods: {
    _showChange(show: boolean) {
      const animated = this.data.animated
      let displayStyle = ''
      if (animated) {
        displayStyle = `opacity: ${show ? '1' : '0'};transition:opacity 0.5s;`
      } else {
        displayStyle = `display: ${show ? '' : 'none'}`
      }
      this.setData({
        displayStyle
      })
    },
    back() {
      const data = this.data
      if (data.delta) {
        wx.navigateBack({
          delta: data.delta
        })
      }
      this.triggerEvent('back', { delta: data.delta }, {})
    },
    home() {
      wx.switchTab({
        url: '/pages/appointment/create'
      })
      this.triggerEvent('home', {}, {})
    }
  }
})