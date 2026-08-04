import { ref, onMounted, onUnmounted } from 'vue';
import KuaiShouAd from '../plugins/KuaiShouAdPlugin';
import { sendRedPacket, recordAdView, getPoolStatus, getUserTickets } from '../api/apiService';

interface AdConfig {
  appId: string;
  slotIds: string[];
}

const isLoaded = ref(false);
const isAdSdkReady = ref(false);
const isAdLoading = ref(false);
const isAdReady = ref(false);
const lastError = ref('');
const preloadAd = ref(false);

// ================================================================
// 模块级：全局监听器 + 上下文路由
// 避免每次预加载/展示都 add/remove 监听器，桥接调用从 96 次→0 次
// ================================================================

/** 全局监听器是否已注册（模块级单例，只注册一次） */
let globalListenersRegistered = false;

/** 当前预加载上下文（串行，同一时刻只有一个） */
let currentPreloadContext: {
  slotId: string;
  resolve: (success: boolean) => void;
  isResolved: boolean;
} | null = null;

/** 当前展示上下文（同一时刻只有一个） */
let currentShowContext: {
  slotId: string;
  resolve: (result: { ecpm: number; slotId: string } | null) => void;
  reject: (reason?: any) => void;
  isResolved: boolean;
  currentAdSuccess: boolean;
  onAdShowCallback?: () => void; // 展示开始时触发（用于 smartPreload）
} | null = null;

/** ECPM 计算函数引用（由 useAdManager 内部设置，因为依赖 deviceId） */
let generateSimulatedEcpmFn: ((slotId: string) => number) | null = null;
let calculateActualEcpmFn: ((ecpm: number) => number) | null = null;

// ---------- 模块级：ECPM 相关函数（不依赖实例状态，直接提升） ----------

const getDeviceId = (): string => {
  let deviceId = localStorage.getItem('deviceId');
  if (!deviceId) {
    deviceId = 'device_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    localStorage.setItem('deviceId', deviceId);
  }
  return deviceId;
};

const getEcpmPool = (deviceId: string): number => {
  const key = `ecpm_pool_${deviceId}`;
  const stored = localStorage.getItem(key);
  return stored ? parseFloat(stored) : 0;
};

const saveEcpmPool = (deviceId: string, pool: number): void => {
  const key = `ecpm_pool_${deviceId}`;
  localStorage.setItem(key, pool.toString());
};

const calculateActualEcpm = (simulatedEcpm: number): number => {
  try {
    if (simulatedEcpm < 0) {
      console.warn('⚠️ 模拟eCPM值为负数，设置为0');
      simulatedEcpm = 0;
    }
    const deviceId = getDeviceId();
    const previousPool = getEcpmPool(deviceId);
    const ECPM_THRESHOLD = 700;
    const HIGH_VALUE_RATIO = 0.7;
    const RELEASE_RATIO = 0.3;
    const ROLL_OVER_RATIO = 0.7;
    let baseTransmitAmount: number;
    let currentRetainAmount: number;
    if (simulatedEcpm > ECPM_THRESHOLD) {
      baseTransmitAmount = simulatedEcpm * HIGH_VALUE_RATIO;
      currentRetainAmount = simulatedEcpm * (1 - HIGH_VALUE_RATIO);
    } else {
      baseTransmitAmount = simulatedEcpm;
      currentRetainAmount = 0;
    }
    const currentReleaseAmount = previousPool * RELEASE_RATIO;
    const actualEcpm = baseTransmitAmount + currentReleaseAmount;
    let newPool = previousPool * ROLL_OVER_RATIO + currentRetainAmount;
    newPool = Math.max(0, newPool);
    saveEcpmPool(deviceId, newPool);
    console.log(`💰 eCPM算法: 模拟=${simulatedEcpm} 实际=${actualEcpm.toFixed(2)} 池=${newPool.toFixed(2)}`);
    return actualEcpm;
  } catch (error) {
    console.error('❌ eCPM算法计算失败:', error);
    return simulatedEcpm;
  }
};

const generateSimulatedEcpm = (slotId: string): number => {
  const ecpmRanges: { [key: string]: [number, number] } = {
    '35383000001': [1620, 1800],
    '35383000003': [1350, 1500],
    '35383000005': [900, 1000],
    '35383000007': [720, 800],
    '35383000009': [540, 600],
    '35383000011': [360, 400],
    '35383000013': [270, 300],
    '35383000015': [180, 200],
    '35383000019': [90, 100],
    '35383000021': [45, 50],
    '35383000023': [20, 30],
    '35383000025': [20, 30]
  };
  const range = ecpmRanges[slotId];
  if (!range) return 0;
  return Math.floor(Math.random() * (range[1] - range[0] + 1)) + range[0];
};

// 模块级设置 ECPM 函数引用
generateSimulatedEcpmFn = generateSimulatedEcpm;
calculateActualEcpmFn = calculateActualEcpm;

// ---------- 模块级：全局监听器注册（只执行一次） ----------

const ensureGlobalListeners = () => {
  if (globalListenersRegistered) return;
  globalListenersRegistered = true;
  console.log('🔧 注册全局广告监听器（一次性）');

  // === 预加载期事件（路由到 currentPreloadContext） ===

  KuaiShouAd.addListener('onVideoDownloadSuccess', (data: any) => {
    const ctx = currentPreloadContext;
    const eventPosId = String(data?.posId || '');
    if (ctx && !ctx.isResolved && eventPosId === ctx.slotId) {
      ctx.isResolved = true;
      console.log(`✅ 串行预加载成功: ${ctx.slotId}`);
      currentPreloadContext = null;
      ctx.resolve(true);
    }
  });

  KuaiShouAd.addListener('onVideoDownloadFailed', (data: any) => {
    const ctx = currentPreloadContext;
    const eventPosId = String(data?.posId || '');
    if (ctx && !ctx.isResolved && eventPosId === ctx.slotId) {
      ctx.isResolved = true;
      console.log(`❌ 串行预加载失败: ${ctx.slotId} (视频下载失败)`, data);
      currentPreloadContext = null;
      ctx.resolve(false);
    }
  });

  KuaiShouAd.addListener('onAdFailed', (data: any) => {
    const ctx = currentPreloadContext;
    const eventPosId = String(data?.posId || '');
    if (ctx && !ctx.isResolved && eventPosId === ctx.slotId) {
      ctx.isResolved = true;
      console.log(`❌ 串行预加载失败: ${ctx.slotId} (广告加载失败)`, data);
      currentPreloadContext = null;
      ctx.resolve(false);
    }
  });

  // === 展示期事件（路由到 currentShowContext） ===

  KuaiShouAd.addListener('onRewardVerify', (data: any) => {
    const ctx = currentShowContext;
    const eventPosId = String(data?.posId || '');
    if (!ctx || ctx.isResolved || ctx.currentAdSuccess) return;
    if (eventPosId && eventPosId !== ctx.slotId) return;

    console.log(`========== 预加载广告奖励回调 (${ctx.slotId}) ==========`);
    ctx.currentAdSuccess = true;

    const simulatedEcpm = generateSimulatedEcpmFn ? generateSimulatedEcpmFn(ctx.slotId) : 0;
    const ecpm = calculateActualEcpmFn ? calculateActualEcpmFn(simulatedEcpm) : simulatedEcpm;
    console.log(`✅ 预加载广告成功 (${ctx.slotId})，返回 ECPM:`, ecpm);

    ctx.isResolved = true;
    currentShowContext = null;
    ctx.resolve({ ecpm, slotId: ctx.slotId });
  });

  KuaiShouAd.addListener('onAdShow', (data: any) => {
    const ctx = currentShowContext;
    const eventPosId = String(data?.posId || '');
    if (!ctx || ctx.isResolved) return;
    if (eventPosId && eventPosId !== ctx.slotId) return;
    console.log(`📺 预加载广告页面已打开 (${ctx.slotId})，500ms后智能触发预加载`);
    // 延迟500ms再预加载，避免与当前广告播放产生资源竞争
    if (ctx.onAdShowCallback) {
      setTimeout(() => ctx.onAdShowCallback?.(), 500);
    }
  });

  KuaiShouAd.addListener('onAdClose', (data: any) => {
    const ctx = currentShowContext;
    const eventPosId = String(data?.posId || '');
    if (!ctx || ctx.isResolved) return;
    if (eventPosId && eventPosId !== ctx.slotId) return;
    console.log(`✅ 预加载广告关闭回调 (${ctx.slotId})`);
    if (!ctx.currentAdSuccess) {
      console.log(`预加载广告关闭但未获得奖励 (${ctx.slotId})，标记为失败`);
      ctx.isResolved = true;
      currentShowContext = null;
      ctx.resolve(null);
    }
  });

  // onAdExpired：展示中的广告未展示就被关闭，可能影响 show context
  KuaiShouAd.addListener('onAdExpired', (data: any) => {
    const ctx = currentShowContext;
    const eventPosId = String(data?.posId || '');
    if (!ctx || ctx.isResolved) return;
    if (eventPosId && eventPosId !== ctx.slotId) return;
    console.warn(`⚠️ 预加载广告已失效 (${ctx.slotId})，show 将失败`);
    ctx.isResolved = true;
    currentShowContext = null;
    ctx.reject(new Error('预加载广告已失效'));
  });
};

export function useAdManager(config: AdConfig) {
  let rewardVerifyListener: any = null;
  let adFailedListener: any = null;
  let videoDownloadSuccessListener: any = null;
  let videoDownloadFailedListener: any = null;
  let adLoadedListener: any = null;
  let adCloseListener: any = null;
  let timeoutId: any = null;
  let retryTimeoutId: any = null;
  let currentResolve: any = null;
  let currentReject: any = null;
  let currentSlotIndex = 0;
  let triedSlots = 0;
  let slotTimeoutId: any = null;
  let currentSessionId = 0;
  let isProcessing = false; // 是否正在处理广告，防止并发
  let hasShownAd = false; // 是否已经显示过广告（用于防止用户跳过后继续尝试其他广告位）
  
  // 预加载状态管理
  let preloadedAd: {
    slotId: string;
    isReady: boolean;
    loadedAt: number;
  } | null = null;
  let isPreloading = false; // 是否正在预加载
  let preloadingPromise: Promise<void> | null = null; // 预加载Promise，用于等待预加载完成
  
  // 广告位分组配置（快手联盟 - 激励视频 posId）
  // 已替换为快手联盟后台申请的 12 个真实激励视频 posId，按高→低保价档位分到 4 个 group。
  const AD_GROUPS = {
    group1: [
      '35383000001', // group1 第1位：保价 1800（ECPM 1620-1800）
      '35383000003', // group1 第2位：保价 1500（ECPM 1350-1500）
      '35383000005'  // group1 第3位：保价 1000（ECPM 900-1000）
    ],
    group2: [
      '35383000007', // group2 第1位：保价 800（ECPM 720-800）
      '35383000009', // group2 第2位：保价 600（ECPM 540-600）
      '35383000011'  // group2 第3位：保价 400（ECPM 360-400）
    ],
    group3: [
      '35383000013', // group3 第1位：保价 300（ECPM 270-300）
      '35383000015', // group3 第2位：保价 200（ECPM 180-200）
      '35383000019'  // group3 第3位：保价 100（ECPM 90-100）
    ],
    group4: [
      '35383000021', // group4 第1位：保价 50（ECPM 45-50）
      '35383000023', // group4 第2位：竞价（ECPM 20-30）
      '35383000025'  // group4 第3位：保价 0 / 填充位（ECPM 20-30）
    ]
  }; // 共12个广告位
  
  // 并行请求超时时间（毫秒）
  const PARALLEL_TIMEOUT = 2000;
  // 组间延迟时间（毫秒）
  const GROUP_DELAY = 500;
  // 广告位间隔时间（毫秒）
  const GROUP5_SLOT_DELAY = 200;
  
  const delay = (ms: number): Promise<void> => {
    return new Promise(resolve => setTimeout(resolve, ms));
  };
  
  // 注：getDeviceId / getEcpmPool / saveEcpmPool / calculateActualEcpm / generateSimulatedEcpm
  // 已提升到模块级，供全局监听器使用（避免每次预加载/展示都 add/remove 监听器）

  const isBiddingSlot = (slotId: string): boolean => {
    const biddingSlots = ['35383000023']; // group4 第2位为竞价位
    return biddingSlots.includes(slotId);
  };

  // 并行请求广告组
  const tryParallelAdGroup = async (slotIds: string[]): Promise<{ ecpm: number; slotId: string } | null> => {
    console.log(`========== 开始并行请求广告组: ${slotIds.join(', ')} ==========`);
    
    const sessionId = currentSessionId;
    const checkSession = () => sessionId === currentSessionId;
    
    const adPromises = slotIds.map(slotId => {
      return new Promise<{ ecpm: number; slotId: string } | null>((resolve) => {
        let isResolved = false;
        let slotTimeoutId: any = null;
        let currentAdSuccess = false;
        
        const resolveOnce = (result: { ecpm: number; slotId: string } | null) => {
          if (!isResolved && checkSession()) {
            isResolved = true;
            cleanupSlotListeners();
            if (slotTimeoutId) clearTimeout(slotTimeoutId);
            resolve(result);
          }
        };
        
        const onRewardVerify = (result: any) => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          
          console.log(`========== 广告奖励回调 (${slotId}) ==========`);
          console.log('结果:', result);
          
          currentAdSuccess = true;
          if (slotTimeoutId) clearTimeout(slotTimeoutId);
          
          // 所有广告位都使用模拟 ECPM 值
          console.log('使用模拟 ECPM 值');
          const simulatedEcpm = generateSimulatedEcpm(slotId);
          const ecpm = calculateActualEcpm(simulatedEcpm);
          
          console.log(`✅ 广告成功 (${slotId})，返回 ECPM:`, ecpm);
          
          resolveOnce({ ecpm, slotId });
        };
        
        const onAdFailed = (error: any) => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          console.warn(`⚠️ 广告加载失败 (${slotId}):`, error?.error || error);
          resolveOnce(null);
        };
        
        const onVideoDownloadSuccess = async () => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          
          console.log(`✅ 视频下载成功 (${slotId})，准备显示广告`);
          
          // 立即设置广告显示标志，防止用户跳过后继续尝试其他广告位
          hasShownAd = true;
          
          try {
            if (slotTimeoutId) clearTimeout(slotTimeoutId);
            
            // 检查广告是否就绪
            console.log(`🔍 检查广告就绪状态 (${slotId})...`);
            try {
              const readyStatus = await KuaiShouAd.isReady();
              console.log(`📊 广告就绪状态 (${slotId}):`, readyStatus);
              
              if (!readyStatus.ready) {
                console.warn(`⚠️ 广告未就绪 (${slotId})，尝试强制显示...`);
              }
            } catch (error) {
              console.warn(`⚠️ 检查广告就绪状态失败 (${slotId}):`, error);
            }
            
            console.log(`✅ 广告位加载成功且已就绪 (${slotId})，准备播放`);
            
            // 显示广告
            KuaiShouAd.showRewardVideoAd();
            console.log(`✅ 广告显示命令已发送 (${slotId})`);
          } catch (error) {
            console.error(`❌ 显示广告失败 (${slotId}):`, error);
            resolveOnce(null);
          }
        };
        
        const onVideoDownloadFailed = () => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          console.warn(`⚠️ 视频下载失败 (${slotId})`);
          resolveOnce(null);
        };
        
        const onAdClose = () => {
          if (!checkSession()) return;
          console.log(`✅ 广告关闭回调 (${slotId})`);
          // 如果已经显示过广告（用户跳过），停止尝试其他广告位
          if (hasShownAd) {
            console.log(`🛑 已显示过广告，停止尝试其他广告位 (${slotId})`);
            resolveOnce(null);
            return;
          }
          if (!currentAdSuccess) {
            console.log(`广告关闭但未获得奖励 (${slotId})，标记为失败`);
            resolveOnce(null);
          }
        };
        
        // 注册监听器
        KuaiShouAd.addListener('onRewardVerify', onRewardVerify);
        KuaiShouAd.addListener('onAdFailed', onAdFailed);
        KuaiShouAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
        KuaiShouAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
        KuaiShouAd.addListener('onAdClose', onAdClose);
        
        // 清理监听器的函数
        const cleanupSlotListeners = () => {
          try {
            KuaiShouAd.removeListener('onRewardVerify', onRewardVerify);
            KuaiShouAd.removeListener('onAdFailed', onAdFailed);
            KuaiShouAd.removeListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
            KuaiShouAd.removeListener('onVideoDownloadFailed', onVideoDownloadFailed);
            KuaiShouAd.removeListener('onAdClose', onAdClose);
          } catch (e) {
            console.warn(`清理监听器失败 (${slotId}):`, e);
          }
        };
        
        // 加载广告
        KuaiShouAd.loadRewardVideoAd({ adId: slotId })
          .then(() => console.log(`✅ 广告加载请求已发送 (${slotId})`))
          .catch((err: any) => {
            console.error(`❌ 加载广告请求失败 (${slotId}):`, err);
            resolveOnce(null);
          });
        
        // 广告位超时
        slotTimeoutId = setTimeout(() => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          console.warn(`⏱️ 广告加载超时 (${slotId})`);
          resolveOnce(null);
        }, PARALLEL_TIMEOUT);
      });
    });
    
    // 等待所有并行请求完成，返回第一个成功的结果
    const results = await Promise.all(adPromises);
    for (const result of results) {
      if (result && checkSession()) {
        console.log(`🎉 并行请求成功，使用广告位: ${result.slotId}，ECPM: ${result.ecpm}`);
        return result;
      }
    }
    
    console.log('❌ 并行请求组所有广告位均失败');
    return null;
  };
  
  const getNextSlotId = (): string => {
    if (!config.slotIds?.length) throw new Error('广告位配置为空');
    const slotId = config.slotIds[currentSlotIndex];
    const currentRound = Math.floor(triedSlots / config.slotIds.length) + 1;
    const positionInRound = (triedSlots % config.slotIds.length) + 1;
    console.log(`当前轮询广告位: ${slotId} (第${currentRound}轮 ${positionInRound}/${config.slotIds.length})`);
    currentSlotIndex = (currentSlotIndex + 1) % config.slotIds.length;
    triedSlots++;
    return slotId;
  };
  
  // 触发预加载（延迟1秒）
  const triggerPreloadAfterDelay = () => {
    setTimeout(() => {
      preloadNextAd();
    }, 1000);
  };
  
  // 智能预加载触发函数（方案C：避免重复触发）
  const smartPreload = () => {
    // 条件1：已有预加载广告，跳过
    if (preloadedAd) {
      console.log('📋 已有预加载广告，跳过预加载');
      return;
    }
    
    // 条件2：正在预加载中，跳过
    if (isPreloading && preloadingPromise) {
      console.log('⏳ 预加载进行中，跳过重复触发');
      return;
    }
    
    // 条件3：没有预加载，也未在预加载，开始新的预加载
    console.log('🚀 开始新的预加载任务');
    preloadNextAd();
  };
  
  // 预加载一组并行广告位（第一个成功就返回）
  const preloadParallelGroup = (slotIds: string[]): Promise<{ success: boolean; slotId: string | null }> => {
    return new Promise((resolve) => {
      let resolved = false;
      const listeners: { slotId: string; cleanup: () => void }[] = [];
      
      // 为每个广告位创建监听器
      slotIds.forEach(slotId => {
        let isSlotResolved = false;
        
        const onVideoDownloadSuccess = () => {
          if (isSlotResolved || resolved) return;
          isSlotResolved = true;
          
          console.log(`✅ 并行预加载成功: ${slotId}`);
          cleanupAllListeners();
          
          if (!resolved) {
            resolved = true;
            resolve({ success: true, slotId });
          }
        };
        
        const onVideoDownloadFailed = () => {
          if (isSlotResolved || resolved) return;
          isSlotResolved = true;
          console.log(`❌ 并行预加载失败: ${slotId} (视频下载失败)`);
          cleanupSlotListeners(slotId);
        };
        
        const onAdFailed = (error: any) => {
          if (isSlotResolved || resolved) return;
          isSlotResolved = true;
          console.log(`❌ 并行预加载失败: ${slotId} (广告加载失败)`, error);
          cleanupSlotListeners(slotId);
        };
        
        const cleanupSlot = () => {
          try {
            KuaiShouAd.removeListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
            KuaiShouAd.removeListener('onVideoDownloadFailed', onVideoDownloadFailed);
            KuaiShouAd.removeListener('onAdFailed', onAdFailed);
          } catch (e) {
            // 忽略清理错误
          }
        };
        
        listeners.push({ slotId, cleanup: cleanupSlot });
        
        // 注册监听器
        KuaiShouAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
        KuaiShouAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
        KuaiShouAd.addListener('onAdFailed', onAdFailed);
        
        // 设置超时（2秒）
        setTimeout(() => {
          if (!isSlotResolved && !resolved) {
            isSlotResolved = true;
            console.log(`⏱️ 并行预加载超时: ${slotId}`);
            cleanupSlotListeners(slotId);
          }
        }, 2000);
        
        // 发起请求
        KuaiShouAd.loadRewardVideoAd({ adId: slotId }).catch((error) => {
          if (!isSlotResolved && !resolved) {
            isSlotResolved = true;
            console.log(`❌ 并行预加载请求失败: ${slotId}`, error);
            cleanupSlotListeners(slotId);
          }
        });
      });
      
      // 清理单个广告位的监听器
      const cleanupSlotListeners = (slotId: string) => {
        const listener = listeners.find(l => l.slotId === slotId);
        if (listener) {
          listener.cleanup();
        }
      };
      
      // 清理所有监听器
      const cleanupAllListeners = () => {
        listeners.forEach(l => l.cleanup());
      };
      
      // 如果所有广告位都失败，返回失败
      setTimeout(() => {
        if (!resolved) {
          resolved = true;
          cleanupAllListeners();
          resolve({ success: false, slotId: null });
        }
      }, 3100);
    });
  };

  // 预加载单个广告位（串行用）
  // 使用全局监听器 + posId 路由，不再每次 add/remove 监听器
  const preloadSingleSlot = (slotId: string): Promise<boolean> => {
    return new Promise((resolve) => {
      // 设置全局预加载上下文，全局监听器会根据 posId 路由
      currentPreloadContext = {
        slotId,
        resolve,
        isResolved: false,
      };

      // 设置超时（2秒）
      setTimeout(() => {
        const ctx = currentPreloadContext;
        if (ctx && ctx.slotId === slotId && !ctx.isResolved) {
          ctx.isResolved = true;
          console.log(`⏱️ 串行预加载超时: ${slotId}`);
          currentPreloadContext = null;
          resolve(false);
        }
      }, 2000);

      // 调用loadRewardVideoAd()加载广告
      KuaiShouAd.loadRewardVideoAd({ adId: slotId }).catch((error) => {
        const ctx = currentPreloadContext;
        if (ctx && ctx.slotId === slotId && !ctx.isResolved) {
          ctx.isResolved = true;
          console.log(`❌ 串行预加载请求失败: ${slotId}`, error);
          currentPreloadContext = null;
          resolve(false);
        }
      });
    });
  };

  // 预加载下一个广告（策略：一轮轮询所有17个广告位）
  const preloadNextAd = async (): Promise<void> => {
    // 如果已经在预加载，返回现有的Promise
    if (isPreloading && preloadingPromise) {
      return preloadingPromise;
    }
    
    // 如果已经有预加载的广告，直接返回
    if (preloadedAd) {
      console.log('📋 已有预加载广告，跳过预加载');
      return;
    }
    
    isPreloading = true;
    console.log('🚀 开始预加载任务（策略：一轮轮询所有17个广告位）');
    
    // 创建新的预加载Promise
    preloadingPromise = (async () => {
      const totalStartTime = Date.now();
      let foundAd = false;
      
      // 获取所有广告位列表
      const allSlots = Object.values(AD_GROUPS).flat();
      console.log(`📊 广告位总数：${allSlots.length}个`);
      
      // ========== 一轮轮询所有广告位 ==========
      console.log(`\n🔄 预加载尝试 1/1`);
      const startTime = Date.now();
      const TOTAL_TIMEOUT = 15000; // 总超时15秒
      
      for (let i = 0; i < allSlots.length; i++) {
        // 检查总超时
        if (Date.now() - startTime > TOTAL_TIMEOUT) {
          console.log('⏱️ 预加载总超时（16秒），终止任务');
          break;
        }
        
        const slotId = allSlots[i];
        console.log(`🔄 串行 [${i + 1}/${allSlots.length}]: ${slotId}`);
        
        const isReady = await preloadSingleSlot(slotId);
        
        if (isReady) {
          preloadedAd = {
            slotId: slotId,
            isReady: true,
            loadedAt: Date.now()
          };
          console.log(`🎉 串行预加载成功: ${slotId}`);
          foundAd = true;
          break;
        }
        
        // 广告位之间延迟500ms
        if (i < allSlots.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 500));
        }
      }
      
      isPreloading = false;
      preloadingPromise = null;
      const totalTime = ((Date.now() - totalStartTime) / 1000).toFixed(1);
      console.log(`📋 预加载任务全部结束，${foundAd ? '成功' : '未找到广告'}，总耗时${totalTime}秒`);
    })();
    
    return preloadingPromise;
  };
  
  // 串行请求广告组
  const trySerialAdGroup = async (slotIds: string[], slotDelay: number = 0): Promise<{ ecpm: number; slotId: string } | null> => {
    console.log(`========== 开始串行请求广告组（共${slotIds.length}个广告位） ==========`);
    
    const sessionId = currentSessionId;
    const checkSession = () => sessionId === currentSessionId;
    
    for (let i = 0; i < slotIds.length; i++) {
      const slotId = slotIds[i];
      const slotIndex = i + 1; // 序号从1开始
      const totalSlots = slotIds.length;
      
      // 检查是否已经显示过广告（用户跳过后停止尝试其他广告位）
      if (hasShownAd) {
        console.log('🛑 已显示过广告，停止尝试其他广告位');
        return null;
      }
      
      if (!checkSession()) {
        console.log('会话已过期，停止加载');
        return null;
      }
      
      // 广告位间延迟（除了第一个）
      if (i > 0 && slotDelay > 0) {
        console.log(`等待 ${slotDelay}ms 后尝试下一个广告位...`);
        await delay(slotDelay);
      }
      
      console.log(`尝试加载广告位 [${slotIndex}/${totalSlots}]: ${slotId}`);
      
      const result = await new Promise<{ ecpm: number; slotId: string } | null>((resolve) => {
        let isResolved = false;
        let slotTimeoutId: any = null;
        let currentAdSuccess = false;
        
        const resolveOnce = (result: { ecpm: number; slotId: string } | null) => {
          if (!isResolved && checkSession()) {
            isResolved = true;
            cleanupSlotListeners();
            if (slotTimeoutId) clearTimeout(slotTimeoutId);
            resolve(result);
          }
        };
        
        const onRewardVerify = (result: any) => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          
          console.log(`========== 广告奖励回调 (${slotId}) ==========`);
          console.log('结果:', result);
          
          currentAdSuccess = true;
          if (slotTimeoutId) clearTimeout(slotTimeoutId);
          
          // 所有广告位都使用模拟 ECPM 值
          console.log('使用模拟 ECPM 值');
          const simulatedEcpm = generateSimulatedEcpm(slotId);
          const ecpm = calculateActualEcpm(simulatedEcpm);
          
          console.log(`✅ 广告成功 (${slotId})，返回 ECPM:`, ecpm);
          
          resolveOnce({ ecpm, slotId });
        };
        
        const onAdFailed = (error: any) => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          console.warn(`⚠️ 广告加载失败 (${slotId}):`, error?.error || error);
          resolveOnce(null);
        };
        
        const onVideoDownloadSuccess = async () => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          
          console.log(`✅ 视频下载成功 (${slotId})，准备显示广告`);
          try {
            if (slotTimeoutId) clearTimeout(slotTimeoutId);
            
            // 检查广告是否就绪
            console.log(`🔍 检查广告就绪状态 (${slotId})...`);
            try {
              const readyStatus = await KuaiShouAd.isReady();
              console.log(`📊 广告就绪状态 (${slotId}):`, readyStatus);
              
              if (!readyStatus.ready) {
                console.warn(`⚠️ 广告未就绪 (${slotId})，尝试强制显示...`);
              }
            } catch (error) {
              console.warn(`⚠️ 检查广告就绪状态失败 (${slotId}):`, error);
            }
            
            console.log(`✅ 广告位加载成功且已就绪 (${slotId})，准备播放`);
            await KuaiShouAd.showRewardVideoAd();
            console.log(`✅ 广告显示命令已发送 (${slotId})`);
          } catch (error) {
            console.error(`❌ 显示广告失败 (${slotId}):`, error);
            resolveOnce(null);
          }
        };
        
        const onVideoDownloadFailed = () => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          console.warn(`⚠️ 视频下载失败 (${slotId})`);
          resolveOnce(null);
        };
        
        const onAdClose = () => {
          if (!checkSession()) return;
          console.log(`✅ 广告关闭回调 (${slotId})`);
          if (!currentAdSuccess) {
            console.log(`广告关闭但未获得奖励 (${slotId})，标记为失败`);
            resolveOnce(null);
          }
        };
        
        // 注册监听器
        KuaiShouAd.addListener('onRewardVerify', onRewardVerify);
        KuaiShouAd.addListener('onAdFailed', onAdFailed);
        KuaiShouAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
        KuaiShouAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
        KuaiShouAd.addListener('onAdClose', onAdClose);
        
        // 清理监听器的函数
        const cleanupSlotListeners = () => {
          try {
            KuaiShouAd.removeListener('onRewardVerify', onRewardVerify);
            KuaiShouAd.removeListener('onAdFailed', onAdFailed);
            KuaiShouAd.removeListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
            KuaiShouAd.removeListener('onVideoDownloadFailed', onVideoDownloadFailed);
            KuaiShouAd.removeListener('onAdClose', onAdClose);
          } catch (e) {
            console.warn(`清理监听器失败 (${slotId}):`, e);
          }
        };
        
        // 加载广告
        KuaiShouAd.loadRewardVideoAd({ adId: slotId })
          .then(() => console.log(`✅ 广告加载请求已发送 (${slotId})`))
          .catch((err: any) => {
            console.error(`❌ 加载广告请求失败 (${slotId}):`, err);
            resolveOnce(null);
          });
        
        // 广告位超时
        slotTimeoutId = setTimeout(() => {
          if (!checkSession() || currentAdSuccess || isResolved) return;
          console.warn(`⏱️ 广告加载超时 (${slotId})`);
          resolveOnce(null);
        }, PARALLEL_TIMEOUT);
      });
      
      // 如果当前广告位成功，立即返回
      if (result) {
        console.log(`🎉 串行请求成功，使用广告位: ${result.slotId}，ECPM: ${result.ecpm}`);
        return result;
      }
      
      // 检查是否已经显示过广告（用户跳过后停止尝试其他广告位）
      if (hasShownAd) {
        console.log('🛑 已显示过广告，停止尝试其他广告位');
        return null;
      }
      
      console.log(`广告位 ${slotId} 失败，尝试下一个...`);
    }
    
    console.log('❌ 串行请求组所有广告位均失败');
    return null;
  };
  
  const resetAdState = () => {
    currentSlotIndex = 0;
    triedSlots = 0;
    isAdLoading.value = false;
    isAdReady.value = false;
    hasShownAd = false; // 重置广告显示标志
    currentSessionId++;
    console.log(`🆕 新会话开始，会话ID: ${currentSessionId}`);
  };

  onMounted(() => initializeAdSdk());
  onUnmounted(() => cleanupListeners());

  const cleanupListeners = () => {
    console.log('🔄 清理广告监听器...');
    
    const listeners = [
      { name: 'onRewardVerify', handler: rewardVerifyListener },
      { name: 'onAdFailed', handler: adFailedListener },
      { name: 'onVideoDownloadSuccess', handler: videoDownloadSuccessListener },
      { name: 'onVideoDownloadFailed', handler: videoDownloadFailedListener },
      { name: 'onAdLoaded', handler: adLoadedListener },
      { name: 'onAdClose', handler: adCloseListener }
    ];
    
    listeners.forEach(({ name, handler }) => {
      if (handler) {
        try {
          KuaiShouAd.removeListener(name, handler);
        } catch (e) {
          console.warn(`移除 ${name} 监听器失败:`, e);
        }
      }
    });
    
    rewardVerifyListener = null;
    adFailedListener = null;
    videoDownloadSuccessListener = null;
    videoDownloadFailedListener = null;
    adLoadedListener = null;
    adCloseListener = null;
    
    [timeoutId, retryTimeoutId, slotTimeoutId].forEach(id => {
      if (id) {
        clearTimeout(id);
        id = null;
      }
    });
    
    console.log('✅ 监听器清理完成');
  };

  const isNativeApp = () => {
    return typeof window !== 'undefined' && 
           (window as any).Capacitor?.getPlatform() === 'android';
  };

  const initializeAdSdk = async () => {
    if (typeof window === 'undefined') return;

    try {
      if (isNativeApp()) {
        console.log('原生 Android 环境，使用快手原生 SDK');
        isAdSdkReady.value = true;
        isLoaded.value = true;
        preloadAd.value = true;

        // 注册全局监听器（一次性，模块级单例）
        ensureGlobalListeners();

        // 通知原生：用户已同意隐私协议（会补触发 SDK init）
        try {
          await KuaiShouAd.onUserAgreePrivacy?.();
        } catch (e) {
          // 忽略：旧版本插件未暴露此方法不影响主流程
        }

        // SDK 加载成功后 500ms 触发预加载
        setTimeout(() => {
          console.log('📱 原生环境 SDK 就绪，开始预加载广告');
          preloadNextAd();
        }, 500);

        return;
      }

      // 非原生环境（浏览器预览、H5 页面等）不提供激励视频广告
      console.log('非原生 Android 环境，快手激励视频广告不可用（仅 Android APK 原生环境可用）');
      isLoaded.value = true;
      isAdSdkReady.value = false;
      preloadAd.value = true;
    } catch (error) {
      console.error('初始化广告 SDK 失败:', error);
      isLoaded.value = true;
      isAdSdkReady.value = false;
      preloadAd.value = true;
    }
  };
  
  // 显示预加载的广告
  // 使用全局监听器 + posId 路由，不再每次 add/remove 监听器
  const showPreloadedAd = async (resolve: (value: { ecpm: number; slotId: string }) => void, reject: (reason?: any) => void) => {
    if (!preloadedAd || !preloadedAd.isReady) {
      console.log('预加载广告未就绪，开始正常加载');
      reject(new Error('预加载广告未就绪'));
      return;
    }

    const slotId = preloadedAd.slotId;
    console.log(`🚀 使用预加载的广告位: ${slotId}`);

    // 清空预加载引用
    preloadedAd = null;

    // 设置广告显示标志
    hasShownAd = true;

    // 设置全局展示上下文，全局监听器会根据 posId 路由
    currentShowContext = {
      slotId,
      resolve: (result) => {
        if (result) {
          resolve(result);
        } else {
          reject(new Error('广告显示失败'));
        }
      },
      reject,
      isResolved: false,
      currentAdSuccess: false,
      onAdShowCallback: () => smartPreload(),
    };

    try {
      // 显示广告
      await KuaiShouAd.showRewardVideoAd();
      console.log(`✅ 预加载广告显示命令已发送 (${slotId})`);
    } catch (error) {
      console.error(`❌ 显示预加载广告失败 (${slotId}):`, error);
      const ctx = currentShowContext;
      if (ctx && ctx.slotId === slotId && !ctx.isResolved) {
        ctx.isResolved = true;
        currentShowContext = null;
        reject(new Error('广告显示失败'));
      }
    }
  };

  // 获取用户ID
  const getUserId = (): string | null => {
    return localStorage.getItem('userId') || null;
  };

  // 获取员工ID
  const getEmployeeId = (): string | null => {
    return localStorage.getItem('empId') || null;
  };

  // 红包触发逻辑已移至后端处理

  const showAd = async (): Promise<{ ecpm: number; slotId: string }> => {
    return new Promise(async (resolve, reject) => {
      // 防止并发请求
      if (isProcessing) {
        console.log('⚠️ 已有广告正在处理，请等待');
        reject(new Error('已有广告正在处理'));
        return;
      }
      
      // 红包触发逻辑已移到广告成功后
      
      isProcessing = true;
      resetAdState();
      currentResolve = resolve;
      currentReject = reject;
      
      console.log('========== 开始加载激励视频广告 ==========');
      console.log('所有广告位:', config.slotIds);
      console.log('是否原生环境:', isNativeApp());
      
      // 检查是否有预加载的广告
      if (preloadedAd && preloadedAd.isReady) {
        console.log('🚀 检测到预加载的广告，准备使用');
        try {
          await showPreloadedAd(resolve, reject);
          isProcessing = false;
          // 使用预加载成功，智能触发预加载为下次做准备
          console.log('📋 直接使用预加载成功，智能触发预加载');
          smartPreload();
          return;
        } catch (error) {
          console.log('预加载广告显示失败，开始新的预加载');
          // 继续预加载流程
        }
      }
      
      // 如果正在预加载，等待预加载完成
      if (isPreloading && preloadingPromise) {
        console.log('⏳ 正在等待预加载完成...');
        await preloadingPromise;
        
        // 等待完成后，检查是否有预加载的广告
        if (preloadedAd && preloadedAd.isReady) {
          console.log('🚀 预加载完成，准备使用');
          try {
            await showPreloadedAd(resolve, reject);
            isProcessing = false;
            // 等待预加载后使用成功，智能触发预加载为下次做准备
            console.log('📋 等待预加载后使用成功，智能触发预加载');
            smartPreload();
            return;
          } catch (error) {
            console.log('预加载广告显示失败');
          }
        }
      }
      
      // 没有预加载的广告，也没有正在进行的预加载，开始新的预加载
      console.log('🔄 没有预加载的广告，开始预加载...');
      await preloadNextAd();
      
      // 预加载完成后，检查是否有预加载的广告
      if (preloadedAd && preloadedAd.isReady) {
        console.log('🚀 预加载成功，准备使用');
        try {
          await showPreloadedAd(resolve, reject);
          isProcessing = false;
          // 使用预加载成功，智能触发预加载为下次做准备
          console.log('📋 预加载广告使用成功，智能触发预加载');
          smartPreload();
          return;
        } catch (error) {
          console.log('预加载广告显示失败');
          isProcessing = false;
          reject(new Error('暂无广告'));
          return;
        }
      } else {
        console.log('❌ 预加载失败，直接请求所有广告位');
        // 预加载失败，直接请求所有广告位串行
        const allSlots = Object.values(AD_GROUPS).flat();
        console.log('🔄 直接请求所有广告位:', allSlots);
        
        for (let i = 0; i < allSlots.length; i++) {
          const slotId = allSlots[i];
          console.log(`🔄 紧急加载 [${i + 1}/${allSlots.length}]: ${slotId}`);
          
          const isReady = await preloadSingleSlot(slotId);
          
          if (isReady) {
            console.log(`🎉 紧急加载成功: ${slotId}`);
            // 使用这个广告位显示广告
            preloadedAd = {
              slotId: slotId,
              isReady: true,
              loadedAt: Date.now()
            };
            try {
                await showPreloadedAd(resolve, reject);
                isProcessing = false;
                // 紧急加载成功，智能触发预加载为下次做准备
                console.log('📋 紧急加载成功，智能触发预加载');
                smartPreload();
                return;
              } catch (error) {
                console.log('紧急加载广告显示失败');
                isProcessing = false;
                reject(new Error('暂无广告'));
                return;
              }
          }
          
          // 广告位之间延迟300ms
          if (i < allSlots.length - 1) {
            await new Promise(resolve => setTimeout(resolve, 300));
          }
        }
        
        console.log('❌ 所有广告位都加载失败');
        isProcessing = false;
        reject(new Error('暂无广告'));
        return;
      }
    });
  };

  // 串行加载单个广告位
  const tryLoadAd = async (): Promise<'success' | 'failed' | 'session_expired'> => {
    const sessionId = currentSessionId;
    let currentAdSuccess = false; // 当前广告是否成功
    
    const checkSession = () => sessionId === currentSessionId;
    
    if (!checkSession()) {
      console.log('会话已过期，停止加载');
      return 'session_expired';
    }
    
    // 检查是否已尝试所有轮次
    const maxSlots = config.slotIds.length;
    if (triedSlots >= maxSlots) {
      console.log('所有广告位都已尝试');
      return 'failed';
    }
    
    // 清理之前的监听器
    cleanupListeners();
    
    const selectedSlotId = getNextSlotId();
    console.log(`尝试加载广告位: ${selectedSlotId}`);
    
    return new Promise((resolveLoad) => {
      let isResolved = false; // 标记当前加载是否已解决
      
      const resolveOnce = (result: 'success' | 'failed') => {
        if (!isResolved) {
          isResolved = true;
          resolveLoad(result);
        }
      };
      
      const onAdLoaded = () => {
        if (!checkSession()) return;
        console.log('✅ 广告加载成功回调');
      };

      const onRewardVerify = (result: any) => {
        if (!checkSession() || currentAdSuccess) return;
        
        console.log('========== 广告奖励回调 ==========');
        console.log('结果:', result);
        
        currentAdSuccess = true;
        if (slotTimeoutId) clearTimeout(slotTimeoutId);
        
        let ecpm = result.ecpm || 0;
        const currentSlotId = config.slotIds[(currentSlotIndex - 1 + config.slotIds.length) % config.slotIds.length];

        if (isBiddingSlot(currentSlotId)) {
          console.log('竞价位广告，使用模拟 ECPM');
          const simulatedEcpm = generateSimulatedEcpm(currentSlotId);
          ecpm = calculateActualEcpm(simulatedEcpm);
        } else if (ecpm === 0) {
          console.log('保价位广告 ECPM 为 0，生成模拟 ECPM');
          const simulatedEcpm = generateSimulatedEcpm(currentSlotId);
          ecpm = calculateActualEcpm(simulatedEcpm);
        }
        
        isAdLoading.value = false;
        isAdReady.value = false;
        
        console.log('✅ 广告成功，返回 ECPM:', ecpm, '广告位ID:', currentSlotId);
        cleanupListeners();
        currentResolve({ ecpm, slotId: currentSlotId });
        
        currentResolve = null;
        currentReject = null;
        isProcessing = false;
        resolveOnce('success');
      };
      
      const onAdFailed = (error: any) => {
        if (!checkSession() || currentAdSuccess || isResolved) return;
        
        console.warn('⚠️ 广告加载失败:', error?.error || error);
        lastError.value = '广告加载失败: ' + (error?.error || error || '未知错误');
        
        if (slotTimeoutId) clearTimeout(slotTimeoutId);
        cleanupListeners();
        resolveOnce('failed');
      };

      const onVideoDownloadSuccess = async () => {
        if (!checkSession() || currentAdSuccess || isResolved) return;
        
        console.log('✅ 视频下载成功，准备显示广告');
        try {
          if (slotTimeoutId) {
            clearTimeout(slotTimeoutId);
            console.log('✅ 清除单层超时定时器');
          }
          
          isAdReady.value = true;
          isAdLoading.value = false;
          
          // 检查广告是否就绪（未过期且缓存成功）
          console.log('🔍 检查广告就绪状态...');
          try {
            const readyStatus = await KuaiShouAd.isReady();
            console.log('📊 广告就绪状态:', readyStatus);
            
            if (!readyStatus.ready) {
              console.warn('⚠️ 广告未就绪（可能已过期或未缓存完成）');
              // 即使isReady返回false，也尝试显示广告，因为广告可能已经加载成功
              console.log('🔄 尝试强制显示广告...');
            }
          } catch (error) {
            console.warn('⚠️ 检查广告就绪状态失败:', error);
            // 检查失败时也尝试显示广告
          }
          
          console.log('✅ 广告位加载成功且已就绪，准备播放');
          await KuaiShouAd.showRewardVideoAd();
          console.log('✅ 广告显示命令已发送');
        } catch (error) {
          console.error('❌ 显示广告失败:', error);
          lastError.value = '显示广告失败: ' + (error?.message || error);
          cleanupListeners();
          resolveOnce('failed');
        }
      };

      const onVideoDownloadFailed = () => {
        if (!checkSession() || currentAdSuccess || isResolved) return;
        
        console.warn('⚠️ 视频下载失败');
        lastError.value = '视频下载失败，可能是广告填充不足';
        
        if (slotTimeoutId) clearTimeout(slotTimeoutId);
        cleanupListeners();
        resolveOnce('failed');
      };
      
      const onAdClose = () => {
        if (!checkSession()) return;
        
        console.log('✅ 广告关闭回调');
        if (slotTimeoutId) clearTimeout(slotTimeoutId);
        isAdReady.value = false;
        isAdLoading.value = false;
        cleanupListeners();
        
        // 如果广告未成功（用户跳过或未获得奖励），标记为失败
        if (!currentAdSuccess) {
          console.log('广告关闭但未获得奖励，标记为失败');
          resolveOnce('failed');
        }
      };
      
      adLoadedListener = onAdLoaded;
      rewardVerifyListener = onRewardVerify;
      adFailedListener = onAdFailed;
      videoDownloadSuccessListener = onVideoDownloadSuccess;
      videoDownloadFailedListener = onVideoDownloadFailed;
      adCloseListener = onAdClose;
      
      KuaiShouAd.addListener('onAdLoaded', onAdLoaded);
      KuaiShouAd.addListener('onRewardVerify', onRewardVerify);
      KuaiShouAd.addListener('onAdFailed', onAdFailed);
      KuaiShouAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
      KuaiShouAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
      KuaiShouAd.addListener('onAdClose', onAdClose);
      
      KuaiShouAd.loadRewardVideoAd({ adId: selectedSlotId })
        .then(() => console.log('✅ 广告加载请求已发送'))
        .catch((err: any) => {
          console.error('❌ 加载广告请求失败:', err);
          if (!isResolved) {
            cleanupListeners();
            resolveOnce('failed');
          }
        });
      
      // 单层超时
      const SLOT_TIMEOUT = 3000;
      slotTimeoutId = setTimeout(() => {
        if (!checkSession() || currentAdSuccess || isResolved) return;
        
        console.warn(`⏱️ 单层广告加载超时（${SLOT_TIMEOUT}ms）`);
        cleanupListeners();
        resolveOnce('failed');
      }, SLOT_TIMEOUT);
    });
  };
  
  const showNativeAd = async (resolve: (value: { ecpm: number; slotId: string }) => void, reject: (reason?: any) => void) => {
    const sessionId = currentSessionId;
    const checkSession = () => sessionId === currentSessionId;
    
    // 前4组并行请求已注释，改为全部串行
    // // 1. 第一组并行请求
    // let result = await tryParallelAdGroup(AD_GROUPS.group1);
    // if (result && checkSession()) {
    //   isAdLoading.value = false;
    //   isAdReady.value = false;
    //   isProcessing = false;
    //   resolve(result);
    //   return;
    // }
    
    // // 组间延迟
    // if (checkSession()) {
    //   console.log(`等待 ${GROUP_DELAY}ms 后尝试下一组...`);
    //   await delay(GROUP_DELAY);
    // }
    
    // // 2. 第二组并行请求
    // result = await tryParallelAdGroup(AD_GROUPS.group2);
    // if (result && checkSession()) {
    //   isAdLoading.value = false;
    //   isAdReady.value = false;
    //   isProcessing = false;
    //   resolve(result);
    //   return;
    // }
    
    // // 组间延迟
    // if (checkSession()) {
    //   console.log(`等待 ${GROUP_DELAY}ms 后尝试下一组...`);
    //   await delay(GROUP_DELAY);
    // }
    
    // // 3. 第三组并行请求
    // result = await tryParallelAdGroup(AD_GROUPS.group3);
    // if (result && checkSession()) {
    //   isAdLoading.value = false;
    //   isAdReady.value = false;
    //   isProcessing = false;
    //   resolve(result);
    //   return;
    // }
    
    // // 组间延迟
    // if (checkSession()) {
    //   console.log(`等待 ${GROUP_DELAY}ms 后尝试下一组...`);
    //   await delay(GROUP_DELAY);
    // }
    
    // // 4. 第四组并行请求
    // result = await tryParallelAdGroup(AD_GROUPS.group4);
    // if (result && checkSession()) {
    //   isAdLoading.value = false;
    //   isAdReady.value = false;
    //   isProcessing = false;
    //   resolve(result);
    //   return;
    // }
    
    // // 组间延迟
    // if (checkSession()) {
    //   console.log(`等待 ${GROUP_DELAY}ms 后尝试下一组...`);
    //   await delay(GROUP_DELAY);
    // }
    
    // 全部串行请求
    let result = await trySerialAdGroup(Object.values(AD_GROUPS).flat(), GROUP5_SLOT_DELAY);
    if (result && checkSession()) {
      isAdLoading.value = false;
      isAdReady.value = false;
      isProcessing = false;
      resolve(result);
      return;
    }
    
    // 所有广告位尝试失败
    isAdLoading.value = false;
    isAdReady.value = false;
    isProcessing = false;
    showNoAdAvailable(reject);
  };

  const showH5Ad = (resolve: (value: { ecpm: number; slotId: string }) => void, reject: (reason?: any) => void) => {
    // 快手广告 SDK 仅提供原生 Android 接入方案，无 H5 版 mobads.js。
    // Web/非原生环境直接降级失败，走无广告分支。
    console.warn('[showH5Ad] 快手广告 H5 环境不可用，仅支持原生 Android APK 环境');
    isAdLoading.value = false;
    isAdReady.value = false;
    isProcessing = false;
    showNoAdAvailable(reject);
  };

  const showNoAdAvailable = (reject: (reason?: any) => void) => {
    console.log('⚠️ 所有广告位都已尝试，暂无合适广告');
    lastError.value = '暂无合适广告匹配，请稍后重试';
    isAdLoading.value = false;
    isAdReady.value = false;
    isProcessing = false;
    currentResolve = null;
    currentReject = null;
    cleanupListeners();
    reject(new Error('暂无合适广告匹配'));
  };

  return {
    isLoaded,
    isAdSdkReady,
    isAdLoading,
    isAdReady,
    lastError,
    preloadAd,
    showRewardVideo: showAd,
    initializeAdSdk,
    preloadNextAd,
    triggerPreloadAfterDelay
  };
}
