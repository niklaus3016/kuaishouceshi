import { registerPlugin, PluginListenerHandle } from '@capacitor/core';

export interface BaiduAdPlugin {
  loadRewardVideoAd(options: { adId: string }): Promise<void>;
  showRewardVideoAd(): Promise<void>;
  isReady(): Promise<{ ready: boolean }>;
  addListener(eventName: string, listenerFunc: (data: any) => void): Promise<PluginListenerHandle>;
  removeListener(eventName: string, listenerFunc: (data: any) => void): Promise<void>;
}

const BaiduAd = registerPlugin<BaiduAdPlugin>('BaiduAd', {
  web: () => import('./BaiduAdPluginWeb').then(m => new m.BaiduAdPluginWeb() as any),
});

export default BaiduAd;
