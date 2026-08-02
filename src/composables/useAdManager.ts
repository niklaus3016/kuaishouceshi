import { ref, onMounted, onUnmounted } from 'vue';
import BaiduAd from '../plugins/BaiduAdPlugin';
import { sendRedPacket, recordAdView, getPoolStatus, getUserTickets } from '../api/apiService';

declare global {
  interface Window {
    baidu?: any;
    _baidu?: any;
  }
}

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
  
  // 广告位分组配置（月序星座）
  const AD_GROUPS = {
    group1: [
      '19361425', // 保价900
      '19361441', // 保价600
      '19361453'  // 保价400
    ],
    group2: [
      '19950604', // 保价300
      '20058408', // 保价230
      '19361461'  // 保价200
    ],
    group3: [
      '20058409', // 保价180
      '19361483', // 保价150
      '19361488'  // 保价130
    ],
    group4: [
      '19361502', // 保价80
      '19361510', // 竞价
      '19361517'  // 保价0
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
  
  // 获取或生成设备ID
  const getDeviceId = (): string => {
    let deviceId = localStorage.getItem('deviceId');
    if (!deviceId) {
      deviceId = 'device_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
      localStorage.setItem('deviceId', deviceId);
    }
    return deviceId;
  };
  
  // 从本地存储获取设备的激励池状态
  const getEcpmPool = (deviceId: string): number => {
    const key = `ecpm_pool_${deviceId}`;
    const stored = localStorage.getItem(key);
    return stored ? parseFloat(stored) : 0;
  };
  
  // 保存设备的激励池状态到本地存储
  const saveEcpmPool = (deviceId: string, pool: number): void => {
    const key = `ecpm_pool_${deviceId}`;
    localStorage.setItem(key, pool.toString());
  };
  
  // 计算实际传输的eCPM值（核心算法）
  const calculateActualEcpm = (simulatedEcpm: number): number => {
    try {
      // 参数校验
      if (simulatedEcpm < 0) {
        console.warn('⚠️ 模拟eCPM值为负数，设置为0');
        simulatedEcpm = 0;
      }
      
      const deviceId = getDeviceId();
      const previousPool = getEcpmPool(deviceId);
      
      // 配置参数
      const ECPM_THRESHOLD = 700;  // 分界线
      const HIGH_VALUE_RATIO = 0.7;   // 高值传输比例（70%传输，30%留存）
      const RELEASE_RATIO = 0.3;     // 激励池释放比例（30%）
      const ROLL_OVER_RATIO = 0.7;   // 激励池滚存比例（70%）
      
      // 计算基础传输值和留存额度
      let baseTransmitAmount: number;
      let currentRetainAmount: number;
      
      if (simulatedEcpm > ECPM_THRESHOLD) {
        // 高值eCPM (>700)：70%传输，30%留存
        baseTransmitAmount = simulatedEcpm * HIGH_VALUE_RATIO;
        currentRetainAmount = simulatedEcpm * (1 - HIGH_VALUE_RATIO);
      } else {
        // 低值eCPM (≤700)：100%传输，0留存
        baseTransmitAmount = simulatedEcpm;
        currentRetainAmount = 0;
      }
      
      // 计算当期激励释放额
      const currentReleaseAmount = previousPool * RELEASE_RATIO;
      
      // 计算实际传输值
      const actualEcpm = baseTransmitAmount + currentReleaseAmount;
      
      // 更新激励池总额
      let newPool = previousPool * ROLL_OVER_RATIO + currentRetainAmount;
      
      // 激励池下限控制
      const MIN_POOL = 0;      // 最小激励池
      newPool = Math.max(MIN_POOL, newPool);
      
      // 保存到本地存储
      saveEcpmPool(deviceId, newPool);
      
      // 日志输出
      console.log(`💰 eCPM算法计算:`);
      console.log(`   模拟eCPM: ${simulatedEcpm}`);
      console.log(`   类型: ${simulatedEcpm > ECPM_THRESHOLD ? '高值' : '低值'}`);
      console.log(`   基础传输值: ${baseTransmitAmount.toFixed(2)}`);
      console.log(`   当期留存额度: ${currentRetainAmount.toFixed(2)}`);
      console.log(`   上一期激励池: ${previousPool.toFixed(2)}`);
      console.log(`   当期激励释放额: ${currentReleaseAmount.toFixed(2)}`);
      console.log(`   实际传输值: ${actualEcpm.toFixed(2)}`);
      console.log(`   新激励池总额: ${newPool.toFixed(2)}`);
      
      return actualEcpm;
    } catch (error) {
      console.error('❌ eCPM算法计算失败:', error);
      // 异常情况下返回原始模拟值
      return simulatedEcpm;
    }
  };

  const generateSimulatedEcpm = (slotId: string): number => {
    const ecpmRanges: { [key: string]: [number, number] } = {
      // group1 - 保价900, 600, 400
      '19361425': [810, 900],    // 保价900
      '19361441': [540, 600],    // 保价600
      '19361453': [360, 400],    // 保价400
      // group2 - 保价300, 230, 200
      '19950604': [270, 300],    // 保价300
      '20058408': [207, 230],    // 保价230
      '19361461': [180, 200],    // 保价200
      // group3 - 保价180, 150, 130
      '20058409': [162, 180],    // 保价180
      '19361483': [135, 150],    // 保价150
      '19361488': [117, 130],    // 保价130
      // group4 - 保价80, 竞价, 保价0
      '19361502': [72, 80],      // 保价80
      '19361510': [20, 30],      // 竞价
      '19361517': [20, 30]       // 保价0
    };

    const range = ecpmRanges[slotId];
    if (!range) return 0;
    return Math.floor(Math.random() * (range[1] - range[0] + 1)) + range[0];
  };

  const isBiddingSlot = (slotId: string): boolean => {
    const biddingSlots = ['19361510'];
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
              const readyStatus = await BaiduAd.isReady();
              console.log(`📊 广告就绪状态 (${slotId}):`, readyStatus);
              
              if (!readyStatus.ready) {
                console.warn(`⚠️ 广告未就绪 (${slotId})，尝试强制显示...`);
              }
            } catch (error) {
              console.warn(`⚠️ 检查广告就绪状态失败 (${slotId}):`, error);
            }
            
            console.log(`✅ 广告位加载成功且已就绪 (${slotId})，准备播放`);
            
            // 显示广告
            BaiduAd.showRewardVideoAd();
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
        BaiduAd.addListener('onRewardVerify', onRewardVerify);
        BaiduAd.addListener('onAdFailed', onAdFailed);
        BaiduAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
        BaiduAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
        BaiduAd.addListener('onAdClose', onAdClose);
        
        // 清理监听器的函数
        const cleanupSlotListeners = () => {
          try {
            BaiduAd.removeListener('onRewardVerify', onRewardVerify);
            BaiduAd.removeListener('onAdFailed', onAdFailed);
            BaiduAd.removeListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
            BaiduAd.removeListener('onVideoDownloadFailed', onVideoDownloadFailed);
            BaiduAd.removeListener('onAdClose', onAdClose);
          } catch (e) {
            console.warn(`清理监听器失败 (${slotId}):`, e);
          }
        };
        
        // 加载广告
        BaiduAd.loadRewardVideoAd({ adId: slotId })
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
            BaiduAd.removeListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
            BaiduAd.removeListener('onVideoDownloadFailed', onVideoDownloadFailed);
            BaiduAd.removeListener('onAdFailed', onAdFailed);
          } catch (e) {
            // 忽略清理错误
          }
        };
        
        listeners.push({ slotId, cleanup: cleanupSlot });
        
        // 注册监听器
        BaiduAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
        BaiduAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
        BaiduAd.addListener('onAdFailed', onAdFailed);
        
        // 设置超时（2秒）
        setTimeout(() => {
          if (!isSlotResolved && !resolved) {
            isSlotResolved = true;
            console.log(`⏱️ 并行预加载超时: ${slotId}`);
            cleanupSlotListeners(slotId);
          }
        }, 2000);
        
        // 发起请求
        BaiduAd.loadRewardVideoAd({ adId: slotId }).catch((error) => {
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
  const preloadSingleSlot = (slotId: string): Promise<boolean> => {
    return new Promise((resolve) => {
      let isResolved = false;
      
      const onVideoDownloadSuccess = () => {
        if (!isResolved) {
          isResolved = true;
          console.log(`✅ 串行预加载成功: ${slotId}`);
          cleanupListeners();
          resolve(true);
        }
      };
      
      const onVideoDownloadFailed = () => {
        if (!isResolved) {
          isResolved = true;
          console.log(`❌ 串行预加载失败: ${slotId} (视频下载失败)`);
          cleanupListeners();
          resolve(false);
        }
      };
      
      const onAdFailed = (error: any) => {
        if (!isResolved) {
          isResolved = true;
          console.log(`❌ 串行预加载失败: ${slotId} (广告加载失败)`, error);
          cleanupListeners();
          resolve(false);
        }
      };
      
      const cleanupListeners = () => {
        try {
          BaiduAd.removeListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
          BaiduAd.removeListener('onVideoDownloadFailed', onVideoDownloadFailed);
          BaiduAd.removeListener('onAdFailed', onAdFailed);
        } catch (e) {
          // 忽略清理错误
        }
      };
      
      // 注册监听器
      BaiduAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
      BaiduAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
      BaiduAd.addListener('onAdFailed', onAdFailed);
      
      // 设置超时（2秒）
      setTimeout(() => {
        if (!isResolved) {
          isResolved = true;
          console.log(`⏱️ 串行预加载超时: ${slotId}`);
          cleanupListeners();
          resolve(false);
        }
      }, 2000);
      
      // 调用loadRewardVideoAd()加载广告
      BaiduAd.loadRewardVideoAd({ adId: slotId }).catch((error) => {
        if (!isResolved) {
          isResolved = true;
          console.log(`❌ 串行预加载请求失败: ${slotId}`, error);
          cleanupListeners();
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
              const readyStatus = await BaiduAd.isReady();
              console.log(`📊 广告就绪状态 (${slotId}):`, readyStatus);
              
              if (!readyStatus.ready) {
                console.warn(`⚠️ 广告未就绪 (${slotId})，尝试强制显示...`);
              }
            } catch (error) {
              console.warn(`⚠️ 检查广告就绪状态失败 (${slotId}):`, error);
            }
            
            console.log(`✅ 广告位加载成功且已就绪 (${slotId})，准备播放`);
            await BaiduAd.showRewardVideoAd();
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
        BaiduAd.addListener('onRewardVerify', onRewardVerify);
        BaiduAd.addListener('onAdFailed', onAdFailed);
        BaiduAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
        BaiduAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
        BaiduAd.addListener('onAdClose', onAdClose);
        
        // 清理监听器的函数
        const cleanupSlotListeners = () => {
          try {
            BaiduAd.removeListener('onRewardVerify', onRewardVerify);
            BaiduAd.removeListener('onAdFailed', onAdFailed);
            BaiduAd.removeListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
            BaiduAd.removeListener('onVideoDownloadFailed', onVideoDownloadFailed);
            BaiduAd.removeListener('onAdClose', onAdClose);
          } catch (e) {
            console.warn(`清理监听器失败 (${slotId}):`, e);
          }
        };
        
        // 加载广告
        BaiduAd.loadRewardVideoAd({ adId: slotId })
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
          BaiduAd.removeListener(name, handler);
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
        console.log('原生 Android 环境，使用百度原生 SDK');
        isAdSdkReady.value = true;
        isLoaded.value = true;
        preloadAd.value = true;
        
        // SDK 加载成功后 500ms 触发预加载
        setTimeout(() => {
          console.log('📱 原生环境 SDK 就绪，开始预加载广告');
          preloadNextAd();
        }, 500);
        
        return;
      }

      if (window.baidu?.mobads) {
        console.log('百度 H5 广告 SDK 已加载');
        isAdSdkReady.value = true;
        isLoaded.value = true;
        preloadAd.value = true;
        
        // SDK 已加载，500ms 后触发预加载
        setTimeout(() => {
          console.log('🌐 H5 SDK 已就绪，开始预加载广告');
          preloadNextAd();
        }, 500);
        
        return;
      }

      console.log('尝试加载百度 H5 广告 SDK');
      const script = document.createElement('script');
      script.src = 'https://mobads.baidu.com/js/mobads.js';
      script.async = true;
      script.onload = () => {
        console.log('百度 H5 广告 SDK 加载成功');
        isAdSdkReady.value = true;
        isLoaded.value = true;
        preloadAd.value = true;
        window.baidu?.mobads?.setAppId?.(config.appId);
        
        // SDK 加载成功后 500ms 触发预加载
        setTimeout(() => {
          console.log('🌐 H5 SDK 加载成功，开始预加载广告');
          preloadNextAd();
        }, 500);
      };
      script.onerror = () => {
        console.error('百度 H5 广告 SDK 加载失败');
        isLoaded.value = true;
        isAdSdkReady.value = false;
        preloadAd.value = true;
      };
      document.head.appendChild(script);
    } catch (error) {
      console.error('初始化广告 SDK 失败:', error);
      isLoaded.value = true;
      isAdSdkReady.value = false;
      preloadAd.value = true;
    }
  };
  
  // 显示预加载的广告
  const showPreloadedAd = async (resolve: (value: { ecpm: number; slotId: string }) => void, reject: (reason?: any) => void) => {
    if (!preloadedAd || !preloadedAd.isReady) {
      console.log('预加载广告未就绪，开始正常加载');
      reject(new Error('预加载广告未就绪'));
      return;
    }
    
    const slotId = preloadedAd.slotId;
    console.log(`🚀 使用预加载的广告位: ${slotId}`);
    
    // 清除预加载状态
    preloadedAd = null;
    
    // 设置广告显示标志
    hasShownAd = true;
    
    // 注册监听器
    let isResolved = false;
    let currentAdSuccess = false;
    
    const resolveOnce = (result: { ecpm: number; slotId: string } | null) => {
      if (!isResolved) {
        isResolved = true;
        cleanupSlotListeners();
        if (result) {
          resolve(result);
        } else {
          reject(new Error('广告显示失败'));
        }
      }
    };
    
    const onRewardVerify = (result: any) => {
      if (currentAdSuccess || isResolved) return;
      
      console.log(`========== 预加载广告奖励回调 (${slotId}) ==========`);
      console.log('结果:', result);
      
      currentAdSuccess = true;
      
      // 所有广告位都使用模拟 ECPM 值
      console.log('使用模拟 ECPM 值');
      const simulatedEcpm = generateSimulatedEcpm(slotId);
      const ecpm = calculateActualEcpm(simulatedEcpm);
      
      console.log(`✅ 预加载广告成功 (${slotId})，返回 ECPM:`, ecpm);
      
      resolveOnce({ ecpm, slotId });
    };
    
    const onAdShow = () => {
      console.log(`📺 预加载广告页面已打开 (${slotId})，智能触发预加载`);
      smartPreload();
    };
    
    const onAdClose = () => {
      console.log(`✅ 预加载广告关闭回调 (${slotId})`);
      cleanupSlotListeners();
      if (!currentAdSuccess) {
        console.log(`预加载广告关闭但未获得奖励 (${slotId})，标记为失败`);
        resolveOnce(null);
      }
    };
    
    const cleanupSlotListeners = () => {
      try {
        BaiduAd.removeListener('onRewardVerify', onRewardVerify);
        BaiduAd.removeListener('onAdClose', onAdClose);
        BaiduAd.removeListener('onAdShow', onAdShow);
      } catch (e) {
        console.warn(`清理预加载广告监听器失败 (${slotId}):`, e);
      }
    };
    
    // 注册监听器
    BaiduAd.addListener('onRewardVerify', onRewardVerify);
    BaiduAd.addListener('onAdClose', onAdClose);
    BaiduAd.addListener('onAdShow', onAdShow);
    
    try {
      // 显示广告
      await BaiduAd.showRewardVideoAd();
      console.log(`✅ 预加载广告显示命令已发送 (${slotId})`);
    } catch (error) {
      console.error(`❌ 显示预加载广告失败 (${slotId}):`, error);
      cleanupSlotListeners();
      resolveOnce(null);
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
            const readyStatus = await BaiduAd.isReady();
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
          await BaiduAd.showRewardVideoAd();
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
      
      BaiduAd.addListener('onAdLoaded', onAdLoaded);
      BaiduAd.addListener('onRewardVerify', onRewardVerify);
      BaiduAd.addListener('onAdFailed', onAdFailed);
      BaiduAd.addListener('onVideoDownloadSuccess', onVideoDownloadSuccess);
      BaiduAd.addListener('onVideoDownloadFailed', onVideoDownloadFailed);
      BaiduAd.addListener('onAdClose', onAdClose);
      
      BaiduAd.loadRewardVideoAd({ adId: selectedSlotId })
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
    isAdLoading.value = true;

    try {
      const selectedSlotId = getNextSlotId();
      console.log('选择的H5广告位:', selectedSlotId);
      
      const rewardVideoAd = window.baidu.mobads.RewardVideoAd({
        slotId: selectedSlotId,
        appId: config.appId,
        onAdLoaded: async () => {
          console.log('H5 广告加载成功');
          isAdReady.value = true;
          isAdLoading.value = false;
          
          // 检查广告是否就绪（未过期且缓存成功）
          console.log('🔍 检查 H5 广告就绪状态...');
          const isAdReadyToShow = rewardVideoAd.isReady ? rewardVideoAd.isReady() : true;
          console.log('📊 H5 广告就绪状态:', isAdReadyToShow);
          
          if (!isAdReadyToShow) {
            console.warn('⚠️ H5 广告未就绪（可能已过期或未缓存完成）');
            isAdReady.value = false;
            isProcessing = false;
            showNoAdAvailable(reject);
            return;
          }
          
          console.log('✅ H5 广告已就绪，准备播放');
          rewardVideoAd.show();
        },
        onAdFailed: (error: any) => {
          console.error('H5 广告加载失败:', error);
          isAdReady.value = false;
          isAdLoading.value = false;
          isProcessing = false;
          showNoAdAvailable(reject);
        },
        onAdShow: () => console.log('H5 广告开始播放'),
        onAdClose: () => {
          console.log('H5 广告关闭');
          isProcessing = false;
        },
        onAdReward: (reward: any) => {
          console.log('获得 H5 广告奖励:', reward);
          let ecpm = reward?.ecpm || reward?.amount || 0;
          
          if (selectedSlotId === '19188427') {
            console.log('H5 竞价位广告，使用模拟 ECPM');
            const simulatedEcpm = generateSimulatedEcpm(selectedSlotId);
            ecpm = calculateActualEcpm(simulatedEcpm);
          } else if (ecpm === 0) {
            console.log('H5 保价位广告 ECPM 为 0，生成模拟 ECPM');
            const simulatedEcpm = generateSimulatedEcpm(selectedSlotId);
            ecpm = calculateActualEcpm(simulatedEcpm);
          }
          
          isAdReady.value = false;
          isProcessing = false;
          if (ecpm > 0) {
            resolve({ ecpm, slotId: selectedSlotId });
          } else {
            showNoAdAvailable(reject);
          }
        },
        onAdClick: () => console.log('用户点击了 H5 广告')
      });

      rewardVideoAd.load();
    } catch (error) {
      console.error('H5 广告初始化失败:', error);
      isAdReady.value = false;
      isAdLoading.value = false;
      isProcessing = false;
      showNoAdAvailable(reject);
    }
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
