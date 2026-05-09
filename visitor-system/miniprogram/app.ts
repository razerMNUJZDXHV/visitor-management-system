import { bootstrapAuthSession } from './utils/request';

App<IAppOption>({
  globalData: {
    authBootstrapPromise: null
  },
  onLaunch() {
    this.globalData.authBootstrapPromise = bootstrapAuthSession();
  },
})