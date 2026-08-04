import { registerPlugin, PluginListenerHandle } from '@capacitor/core';

export interface KuaiShouAdPlugin {
  /** 加载激励视频广告（adId = 快手 posId，数字字符串） */
  loadRewardVideoAd(options: { adId: string }): Promise<void>;
  /** 显示已缓存的激励视频广告 */
  showRewardVideoAd(): Promise<void>;
  /** 查询广告是否就绪（有缓存对象可展示） */
  isReady(): Promise<{ ready: boolean }>;
  /**
   * 前端通知原生：用户已同意隐私协议（原生会在此时触发真正的 SDK init）。
   * 若业务未调用，则首次 loadRewardVideoAd 时会默认视为同意。
   */
  onUserAgreePrivacy?(): Promise<void>;

  addListener(eventName: string, listenerFunc: (data: any) => void): Promise<PluginListenerHandle>;
  removeListener(eventName: string, listenerFunc: (data: any) => void): Promise<void>;
}

const KuaiShouAd = registerPlugin<KuaiShouAdPlugin>('KuaiShouAd', {
  web: () => import('./KuaiShouAdPluginWeb').then(m => new m.KuaiShouAdPluginWeb() as any),
});

export default KuaiShouAd;
