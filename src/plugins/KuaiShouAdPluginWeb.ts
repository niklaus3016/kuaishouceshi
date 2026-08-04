import { WebPlugin } from '@capacitor/core';
import type { KuaiShouAdPlugin } from './KuaiShouAdPlugin';

export class KuaiShouAdPluginWeb extends WebPlugin {
  async loadRewardVideoAd(options: { adId: string }): Promise<void> {
    console.log('[KuaiShouAdPluginWeb] Web 环境不支持快手原生激励广告，请使用原生 Android App；adId=' + options.adId);
    return Promise.resolve();
  }

  async showRewardVideoAd(): Promise<void> {
    console.log('[KuaiShouAdPluginWeb] Web 环境不支持快手原生激励广告，请使用原生 Android App');
    return Promise.resolve();
  }

  async isReady(): Promise<{ ready: boolean }> {
    return { ready: false };
  }

  async onUserAgreePrivacy(): Promise<void> {
    return Promise.resolve();
  }

  async addListener(eventName: string, listenerFunc: (data: any) => void): Promise<any> {
    console.log('[KuaiShouAdPluginWeb] addListener no-op (web):', eventName);
    return Promise.resolve({
      remove: async () => { /* noop */ }
    });
  }
}
