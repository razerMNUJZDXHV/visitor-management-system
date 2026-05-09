// 定义地图标记点类型
interface Marker {
  id: number;
  longitude: number;
  latitude: number;
  title: string;
  iconPath: string;
  width: number;
  height: number;
}

// 定义建筑类型
interface Building {
  id: number;
  name: string;
  longitude: number;
  latitude: number;
}

// 定义路线点类型
interface PolylinePoint {
  longitude: number;
  latitude: number;
}

// 定义路线类型
interface Polyline {
  points: PolylinePoint[];
  color: string;
  width: number;
}

// 定义页面 data 类型
interface PageData {
  navHeight: number;
  longitude: number;
  latitude: number;
  scale: number;
  markers: Marker[];
  buildings: Building[];
  polyline: Polyline[];
}

// 自定义方法接口
interface PageCustom {
  handleBuildingTap(e: any): void;
  decodePolyline(polyline: string): PolylinePoint[];
  moveToMyLocation(): void;
  initMarkers(): void;
}

Page<PageData, PageCustom>({
  data: {
    navHeight: 0,
    longitude: 116.803572,   // 学校中心经度
    latitude: 39.958321,     // 学校中心纬度
    scale: 16,               // 默认放大级别
    markers: [],
    buildings: [
      
      { id: 1, name: '应急管理大学东校区', longitude: 116.804774, latitude: 39.95433 },
      { id: 2, name: '应急管理大学西校区', longitude: 116.800541, latitude: 39.954016 },
      { id: 3, name: '应急管理大学北一校区', longitude: 116.803544, latitude: 39.960092 },
      { id: 4, name: '应急管理大学北二校区', longitude: 116.799809, latitude: 39.963471 },
      { id: 5, name: '应急管理大学北北校区', longitude: 116.804325, latitude: 39.962669 },
      { id: 6, name: '应急管理大学南区', longitude: 116.804194, latitude: 39.95193 }
    ],
    polyline: []
  },

  onLoad() {
    // 计算导航栏高度
    const systemInfo = wx.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 20;
    const navContentHeight = 44;
    const totalNavHeight = statusBarHeight + navContentHeight;
    this.setData({ navHeight: totalNavHeight });

    this.initMarkers();
    this.moveToMyLocation();
  },

  initMarkers() {
    const markers: Marker[] = this.data.buildings.map(building => ({
      id: building.id,
      longitude: building.longitude,
      latitude: building.latitude,
      title: building.name,
      iconPath: '/images/marker.png',
      width: 30,
      height: 30
    }));
    this.setData({ markers });
  },

  // 获取用户当前位置并移动地图
  moveToMyLocation() {
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,  // 启用高精度定位
      success: (res) => {
        console.log('定位成功：', res.longitude, res.latitude);
        this.setData({
          longitude: res.longitude,
          latitude: res.latitude,
          scale: 17
        });
        const mapCtx = wx.createMapContext('myMap');
        mapCtx.moveToLocation({
          longitude: res.longitude,
          latitude: res.latitude
        });
      },
      fail: (err) => {
        console.error('定位失败：', err);
        wx.showToast({
          title: '请开启手机定位权限',
          icon: 'none'
        });
      }
    });
  },
  
  onShow() {
    // 首次打开时可能不显示定位点，强制刷新一次
    setTimeout(() => {
      const mapCtx = wx.createMapContext('myMap');
      mapCtx.moveToLocation();
    }, 500);
  },

  handleBuildingTap(e: any) {
    const buildingId = e.currentTarget.dataset.id;
    const building = this.data.buildings.find(item => item.id === buildingId);
    if (building) {
      const mapCtx = wx.createMapContext('myMap');
      mapCtx.moveToLocation({
        longitude: building.longitude,
        latitude: building.latitude
      });
      // 路径规划
      wx.getLocation({
        type: 'gcj02',
        success: (res) => {
          const start = `${res.longitude},${res.latitude}`;
          const end = `${building.longitude},${building.latitude}`;
          wx.request({
            url: `https://apis.map.qq.com/ws/direction/v1/walking/?from=${start}&to=${end}&key=R2XBZ-MY56J-QN6FJ-XPSJD-QTNV2-BXBRA`,
            method: 'GET',
            success: (res: any) => {
              if (res.data.status === 0) {
                const polylineStr = res.data.result.routes[0].polyline;
                const points = this.decodePolyline(polylineStr);
                this.setData({
                  polyline: [{
                    points: points,
                    color: '#1677ff',
                    width: 4
                  }]
                });
              }
            }
          });
        }
      });
    }
  },

  decodePolyline(polyline: string): PolylinePoint[] {
    const points: PolylinePoint[] = [];
    let index = 0;
    let lat = 0;
    let lng = 0;
    while (index < polyline.length) {
      let b, shift = 0, result = 0;
      do {
        b = polyline.charCodeAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      const dlat = ((result & 1) ? ~(result >> 1) : (result >> 1));
      lat += dlat;
      shift = 0;
      result = 0;
      do {
        b = polyline.charCodeAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      const dlng = ((result & 1) ? ~(result >> 1) : (result >> 1));
      lng += dlng;
      points.push({
        latitude: lat / 1e5,
        longitude: lng / 1e5
      });
    }
    return points;
  }
});