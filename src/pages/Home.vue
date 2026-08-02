<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Coins, History, PlayCircle, LogOut, TrendingUp, Wallet, CreditCard, Trophy, Gift, Ticket, Smartphone, Crown, Medal, Sparkles } from 'lucide-vue-next';
import { getUserInfo, rewardGold, getGoldLogs, getTodayGoldStats, recordLogin, getLoginStats, submitWithdrawRequest, getWithdrawStatus, getWithdrawRecords, getWeeklyBonusProgress, claimWeeklyBonus, recordActivity, getPoolStatus, recordAdView, getUserTickets, getUserRedPacketRecords, claimRedPacket, getDeviceStatus, updateDeviceRecord, getDeviceConfig, getCurrentLotteryTickets, getTodayRanking, getYesterdayRanking, getMonthTopDaily, getWelfareLotteryInfo, type WithdrawRecord } from '../api/apiService';
import { useAdManager } from '../composables/useAdManager';
import { TTSPlugin } from '../plugins/TTSPlugin';
import { RiskCheckPlugin } from '../plugins/RiskCheckPlugin';
import { Capacitor } from '@capacitor/core';

interface Record {
  id: string;
  time: string;
  amount: number;
}

const router = useRouter();
const empId = ref(localStorage.getItem('empId') || '');
const userId = ref(localStorage.getItem('userId') || '');

// 状态管理
const currentMonthGold = ref(0);
const lastMonthGold = ref(0);
const todayCoins = ref(0);
const todayRecordCount = ref(0);
const yesterdayRecordCount = ref(0);
// 周目标任务
const weeklyTarget = ref(100);
const weeklyCompleted = ref(0);
const weeklyStartDate = ref('');
const weeklyEndDate = ref('');
const daysRemaining = ref(0);
const weeklyProgress = computed(() => {
  if (weeklyTarget.value === 0) return 0;
  return Math.min(Math.round((weeklyCompleted.value / weeklyTarget.value) * 100), 100);
});
const bonusGold = ref(0);  // 额外金币奖励
const hasClaimedBonus = ref(false);  // 是否已领取额外金币
const isClaimingBonus = ref(false);  // 是否正在领取额外金币


const records = ref<Record[]>([]);
const isLoading = ref(false);
const isLoadingRecords = ref(false);
const error = ref('');

const isWatching = ref(false);
const showAllRecords = ref(false);

// 福利抽奖次数
const welfareLotteryChances = ref(0);

// 定时同步间隔（用于多设备数据同步）
let syncInterval: ReturnType<typeof setInterval> | null = null;

// 金币奖励弹窗和语音
const showRewardPopup = ref(false);
const rewardAmount = ref(0);
let rewardTimeout: ReturnType<typeof setTimeout> | null = null;

// 红包弹窗
const showRedPacketPopup = ref(false);
const redPacketAmount = ref(0);

// 彩票数量
const lotteryTickets = ref<any[]>([]);
const isRedPacketOpened = ref(false);
let redPacketTimeout: ReturnType<typeof setTimeout> | null = null;

// 红包记录
const redPacketRecords = ref<any[]>([]);
const isLoadingRedPacketRecords = ref(false);
const showRedPacketRecords = ref(false);



// 设备状态管理
const deviceStatus = ref({ isLimited: false, consecutiveLowValueCount: 0 });
const deviceConfig = ref({ consecutiveLimit: 8, goldThreshold: 40 });
const isLoadingDeviceStatus = ref(false);

// 奖金池相关（暂时隐藏，下下个版本上线）
// const poolStatus = ref({ redPacketPool: 0, lotteryPool: 0 });
// const isLoadingPool = ref(false);
// const userTickets = ref<any[]>([]);
// const adViewCount = ref(0);

// 播放金币到账语音
const playRewardSound = async (amount: number) => {
  console.log('========== playRewardSound 被调用 ==========');
  console.log('金币数量:', amount);
  
  try {
    const gold = Math.floor(amount);
    let message = '';
    
    if (gold >= 500) {
      message = `哇塞！太厉害了！恭喜你赚了${gold}金币！`;
    } else if (gold >= 300) {
      message = `太棒了！恭喜你赚了${gold}金币！`;
    } else if (gold >= 100) {
      message = `恭喜你赚了${gold}金币！`;
    } else {
      message = `恭喜你又赚了${gold}金币！`;
    }
    
    console.log('语音内容:', message);
    
    // 检查是否在 Android 平台
    if (Capacitor.getPlatform() === 'android') {
      console.log('使用原生 Android TTS');
      try {
        const result = await TTSPlugin.speak({ text: message });
        console.log('原生 TTS 播放成功:', result);
        // 为了确保语音播放完毕，添加一个小延迟
        await new Promise(resolve => setTimeout(resolve, 1000));
      } catch (err) {
        console.error('原生 TTS 播放失败:', err);
        // 回退到 Web Speech API
        await playWebSpeech(message);
      }
    } else {
      // 在浏览器中使用 Web Speech API
      console.log('使用 Web Speech API');
      await playWebSpeech(message);
    }
  } catch (err) {
    console.error('语音播放失败:', err);
  }
};

// 使用 Web Speech API 播放语音
const playWebSpeech = (message: string): Promise<void> => {
  return new Promise((resolve) => {
    if (typeof window === 'undefined' || !window.speechSynthesis) {
      console.error('浏览器不支持语音合成');
      resolve();
      return;
    }
    
    // 取消之前的语音
    window.speechSynthesis.cancel();
    
    const utterance = new SpeechSynthesisUtterance(message);
    utterance.lang = 'zh-CN';
    utterance.rate = 1.0;
    utterance.pitch = 1.2;
    utterance.volume = 1.0;
    
    // 语音播放完成事件
    utterance.onend = () => {
      console.log('语音播放完成');
      resolve();
    };
    
    // 语音播放错误事件
    utterance.onerror = () => {
      console.error('语音播放错误');
      resolve();
    };
    
    // 等待语音列表加载完成
    const speak = () => {
      const voices = window.speechSynthesis.getVoices();
      console.log('可用语音数量:', voices.length);
      
      const zhVoice = voices.find(v => v.lang.includes('zh'));
      if (zhVoice) {
        utterance.voice = zhVoice;
        console.log('使用中文语音:', zhVoice.name);
      } else {
        console.log('未找到中文语音，使用默认语音');
      }
      
      window.speechSynthesis.speak(utterance);
      console.log('语音播放命令已发送');
    };
    
    // 检查语音列表是否已加载
    if (window.speechSynthesis.getVoices().length > 0) {
      speak();
    } else {
      window.speechSynthesis.onvoiceschanged = () => {
        window.speechSynthesis.onvoiceschanged = null;
        speak();
      };
    }
  });
};

// 显示金币奖励
const showRewardAnimation = async (amount: number) => {
  console.log('========== showRewardAnimation 被调用 ==========');
  console.log('金币数量:', amount);
  
  // 清除之前的定时器
  if (rewardTimeout) {
    clearTimeout(rewardTimeout);
  }
  
  rewardAmount.value = amount;
  showRewardPopup.value = true;
  console.log('showRewardPopup 已设置为 true');
  
  // 播放语音
  await playRewardSound(amount);
  console.log('playRewardSound 已调用');
  
  // 语音播放完成后，等待2秒再隐藏弹窗，确保用户能看到奖励金额
  rewardTimeout = setTimeout(() => {
    showRewardPopup.value = false;
    console.log('showRewardPopup 已设置为 false');
  }, 2000);
};

// 显示红包弹窗
const showRedPacketAnimation = async (amount: number) => {
  console.log('========== showRedPacketAnimation 被调用 ==========');
  console.log('红包金额:', amount);
  
  // 清除之前的定时器
  if (redPacketTimeout) {
    clearTimeout(redPacketTimeout);
  }
  
  redPacketAmount.value = amount;
  showRedPacketPopup.value = true;
  isRedPacketOpened.value = false;
  console.log('showRedPacketPopup 已设置为 true');
  console.log('isRedPacketOpened 已设置为 false');
  
  // 播放红包触发语音提示
  try {
    const message = '哇塞塞，获得幸运红包啦！';
    console.log('红包触发语音内容:', message);
    
    // 检查是否在 Android 平台
    if (Capacitor.getPlatform() === 'android') {
      console.log('使用原生 Android TTS 播放红包触发提示');
      try {
        const result = await TTSPlugin.speak({ text: message });
        console.log('原生 TTS 播放成功:', result);
        // 为了确保语音播放完毕，添加一个小延迟
        await new Promise(resolve => setTimeout(resolve, 1000));
      } catch (err) {
        console.error('原生 TTS 播放失败:', err);
        // 回退到 Web Speech API
        await playWebSpeech(message);
      }
    } else {
      // 在浏览器中使用 Web Speech API
      console.log('使用 Web Speech API 播放红包触发提示');
      await playWebSpeech(message);
    }
  } catch (err) {
    console.error('红包触发语音播放失败:', err);
  }
};

// 显示奖券获得提示
const showTicketAnimation = async (ticketNumber: string) => {
  console.log('========== showTicketAnimation 被调用 ==========');
  console.log('奖券号码:', ticketNumber);
  
  // 播放奖券获得语音提示
  try {
    const message = `恭喜你获得幸运彩票，号码是${ticketNumber}`;
    console.log('奖券获得语音内容:', message);
    
    // 检查是否在 Android 平台
    if (Capacitor.getPlatform() === 'android') {
      console.log('使用原生 Android TTS 播放奖券获得提示');
      try {
        const result = await TTSPlugin.speak({ text: message });
        console.log('原生 TTS 播放成功:', result);
        // 为了确保语音播放完毕，添加一个小延迟
        await new Promise(resolve => setTimeout(resolve, 1000));
      } catch (err) {
        console.error('原生 TTS 播放失败:', err);
        // 回退到 Web Speech API
        await playWebSpeech(message);
      }
    } else {
      // 在浏览器中使用 Web Speech API
      console.log('使用 Web Speech API 播放奖券获得提示');
      await playWebSpeech(message);
    }
  } catch (err) {
    console.error('奖券获得语音播放失败:', err);
  }
  
  // 显示奖券获得弹窗
  showRewardPopup.value = true;
  rewardAmount.value = 0; // 奖券获得提示，不需要显示金币数量
  
  // 1秒后隐藏
  rewardTimeout = setTimeout(() => {
    showRewardPopup.value = false;
    console.log('showRewardPopup 已设置为 false');
  }, 2000);
};

// 打开红包
const openRedPacket = async () => {
  console.log('========== openRedPacket 被调用 ==========');
  isRedPacketOpened.value = true;
  console.log('isRedPacketOpened 已设置为 true');
  
  // 播放语音
  await playRewardSound(redPacketAmount.value);
  console.log('playRewardSound 已调用');
  
  // 调用拆红包确认接口
  if (userId.value && empId.value) {
    console.log('📡 调用拆红包确认接口...');
    console.log('   userId:', userId.value);
    console.log('   employeeId:', empId.value);
    console.log('   redPacketAmount:', redPacketAmount.value);
    
    try {
        const deviceId = getDeviceId();
        const response = await claimRedPacket(userId.value, empId.value, redPacketAmount.value, deviceId);
        console.log('✅ 拆红包确认接口响应:', response);
      
      if (response.success && response.data) {
        console.log('🎁 红包领取成功，金额:', response.data.gold);
        // 更新本地状态
        currentMonthGold.value = response.data.currentMonthGold;
        
        // 手动添加红包记录到redPacketRecords，确保立即显示在最近收益列表中
        const newRedPacketRecord = {
          id: `red_packet_${Date.now()}`,
          time: new Date().toLocaleString('zh-CN', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit'
          }),
          amount: response.data.gold,
          poolBalanceAfter: 0,
          timestamp: Date.now()
        };
        redPacketRecords.value.unshift(newRedPacketRecord);
        console.log('✅ 手动添加红包记录:', newRedPacketRecord);
        
        // 重新加载今日金币统计和收益记录，但不重新加载红包记录（避免清空手动添加的记录）
        await loadTodayGoldStats();
        await loadGoldRecords();
      } else {
        console.warn('⚠️ 红包领取失败:', response.message);
      }
    } catch (err) {
      console.error('❌ 拆红包确认失败:', err);
    }
  }
  
  // 2秒后隐藏
  redPacketTimeout = setTimeout(() => {
    showRedPacketPopup.value = false;
    console.log('showRedPacketPopup 已设置为 false');
  }, 2000);
};

// 模拟触发红包
const simulateRedPacket = async () => {
  console.log('========== 模拟触发红包 ==========');
  const amount = Math.floor(Math.random() * 100) + 50; // 50-150金币
  await showRedPacketAnimation(amount);
};
const showWithdrawModal = ref(false);
const withdrawAmount = ref(0);
const alipayAccount = ref('');
const alipayName = ref('');
const isSubmittingWithdraw = ref(false);
const withdrawSuccess = ref(false);
const withdrawEnabled = ref(false); // 提现开关状态
const showWithdrawRecordsModal = ref(false);
const withdrawRecords = ref<WithdrawRecord[]>([]);
const isLoadingWithdrawRecords = ref(false);

// 排行榜相关
const showLeaderboard = ref(false);
const showWeekendEvent = ref(false);
const dailyLeaderboard = ref<any[]>([]); // 今日排行榜数据
const yesterdayLeaderboard = ref<any[]>([]); // 昨日排行榜数据
const yesterdayDate = ref(''); // 昨日日期
const monthlyTopUser = ref<any>(null);
const isLoadingRanking = ref(false);
const isRankingFirstLoad = ref(true); // 标记是否首次加载（用于控制loading显示）
const rankingTab = ref<'today' | 'yesterday'>('today'); // 排行榜切换：today-今日，yesterday-昨日
const lastRankingLoadTime = ref(0); // 上次加载排行榜数据的时间戳
const RANKING_CACHE_DURATION = 300000; // 排行榜缓存有效期：5分钟（与今日排行榜更新频率一致）

// 登录天数统计
const loginDays = ref(0);

// 获取或生成设备ID
const getDeviceId = (): string => {
  let deviceId = localStorage.getItem('deviceId');
  if (!deviceId) {
    deviceId = 'device_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    localStorage.setItem('deviceId', deviceId);
  }
  return deviceId;
};

// 加载设备状态
const loadDeviceStatus = async () => {
  if (!userId.value) return;
  
  isLoadingDeviceStatus.value = true;
  try {
    const deviceId = getDeviceId();
    console.log('🔄 加载设备状态...');
    const response = await getDeviceStatus(userId.value, deviceId);
    
    if (response.success && response.data) {
      // 确保consecutiveLowValueCount有默认值
      deviceStatus.value = {
        isLimited: response.data.isLimited,
        consecutiveLowValueCount: response.data.consecutiveLowValueCount || 0
      };
      console.log('✅ 设备状态加载成功:', deviceStatus.value);
    } else {
      console.warn('⚠️ 设备状态加载失败，使用默认值');
      // 降级处理：默认设备未被限制
      deviceStatus.value = { isLimited: false, consecutiveLowValueCount: 0 };
    }
  } catch (error) {
    console.error('❌ 加载设备状态异常:', error);
    // 降级处理：默认设备未被限制
    deviceStatus.value = { isLimited: false, consecutiveLowValueCount: 0 };
  } finally {
    isLoadingDeviceStatus.value = false;
  }
};

// 加载福利抽奖次数
const loadWelfareLotteryChances = async () => {
  if (!empId.value) return;
  
  try {
    console.log('🔄 加载福利抽奖次数...');
    const response = await getWelfareLotteryInfo(empId.value);
    
    if (response.success && response.data) {
      welfareLotteryChances.value = Number(response.data.chances) || 0;
      console.log('✅ 福利抽奖次数加载成功:', welfareLotteryChances.value);
    }
  } catch (error) {
    console.error('❌ 加载福利抽奖次数异常:', error);
  }
};

// 加载设备配置
const loadDeviceConfig = async () => {
  try {
    console.log('🔄 加载设备配置...');
    const response = await getDeviceConfig();
    
    if (response.success && response.data) {
      deviceConfig.value = response.data;
      console.log('✅ 设备配置加载成功:', deviceConfig.value);
    } else {
      console.warn('⚠️ 设备配置加载失败，使用默认值');
      // 降级处理：使用默认配置
      deviceConfig.value = { consecutiveLimit: 8, goldThreshold: 40 };
    }
  } catch (error) {
    console.error('❌ 加载设备配置异常:', error);
    // 降级处理：使用默认配置
    deviceConfig.value = { consecutiveLimit: 8, goldThreshold: 40 };
  }
};

// 记录用户活动
const recordUserActivity = async () => {
  if (!userId.value || !empId.value) return;
  
  try {
    const deviceId = getDeviceId();
    await recordActivity(userId.value, empId.value, deviceId);
    console.log('活动记录成功');
  } catch (err) {
    console.error('记录活动失败:', err);
  }
};

// 加载登录统计
const loadLoginStats = async () => {
  if (!userId.value || !empId.value) return;

  try {
    // 检查今天是否已经记录过登录
    const today = new Date().toISOString().split('T')[0];
    const lastLoginDate = localStorage.getItem('lastLoginDate');

    // 如果今天还没有记录过登录，则记录本次登录
    if (lastLoginDate !== today) {
      await recordLogin(userId.value, empId.value);
      localStorage.setItem('lastLoginDate', today);
    }

    // 获取登录统计
    const response = await getLoginStats(userId.value, empId.value);
    if (response.success && response.data) {
      loginDays.value = response.data.totalLoginDays;
    }
  } catch (err) {
    console.error('获取登录统计失败:', err);
  }
};

// 加载提现开关状态
const loadWithdrawStatus = async () => {
  try {
    const response = await getWithdrawStatus();
    console.log('提现状态响应:', response);
    // 后端返回的数据结构: {success: true, enabled: {enabled: true, message: "提现功能已开启"}}
    // enabled可能是布尔值，也可能是对象 {enabled: true, message: "..."}
    if (response.success) {
      const enabledData = (response as any).enabled;
      console.log('提现开关原始值:', enabledData, '类型:', typeof enabledData);
      // 处理enabled可能是对象或布尔值的情况
      let enabledValue: boolean;
      if (typeof enabledData === 'object' && enabledData !== null) {
        enabledValue = enabledData.enabled === true || enabledData.enabled === 'true' || enabledData.enabled === 1 || enabledData.enabled === '1';
        console.log('提现开关（对象解析）:', enabledValue);
      } else {
        enabledValue = enabledData === true || enabledData === 'true' || enabledData === 1 || enabledData === '1';
        console.log('提现开关（直接解析）:', enabledValue);
      }
      withdrawEnabled.value = enabledValue;
      console.log('提现开关最终状态:', withdrawEnabled.value);
    } else {
      console.log('提现状态获取失败');
      withdrawEnabled.value = false;
    }
  } catch (err) {
    console.error('获取提现状态失败:', err);
    withdrawEnabled.value = false;
  }
};

const adConfig = {
  appId: '2882303761520513658',
  slotIds: [
    // group1 - 保价900, 600, 400
    '19361425', // 保价900
    '19361441', // 保价600
    '19361453', // 保价400
    // group2 - 保价300, 230, 200
    '19950604', // 保价300
    '20058408', // 保价230
    '19361461', // 保价200
    // group3 - 保价180, 150, 130
    '20058409', // 保价180
    '19361483', // 保价150
    '19361488', // 保价130
    // group4 - 保价80, 竞价, 保价0
    '19361502', // 保价80
    '19361510', // 竞价
    '19361517'  // 保价0
  ], // 按优先级从高到低排列
};

const { showRewardVideo, triggerPreloadAfterDelay } = useAdManager(adConfig);

// 引用 triggerPreloadAfterDelay 防止被 Vite 删除
if (triggerPreloadAfterDelay) {
  console.log('预加载函数已就绪');
}

// 初始化数据
// 处理页面可见性变化
// 页面可见性变化处理（带防抖）
let visibilityTimeout: ReturnType<typeof setTimeout> | null = null;

const handleVisibilityChange = async () => {
  if (document.visibilityState === 'visible') {
    // 防抖处理，避免频繁切换时重复请求
    if (visibilityTimeout) {
      clearTimeout(visibilityTimeout);
    }
    
    visibilityTimeout = setTimeout(async () => {
      console.log('👁️ 页面重新可见，同步数据看板...');
      await loadUserInfo(false); // 页面聚焦时不显示加载状态
      await loadTodayGoldStats(); // 同步今日金币统计（全局）
      visibilityTimeout = null;
    }, 500); // 500ms防抖
  }
};

onMounted(async () => {
  console.log('🏠 首页初始化 - localStorage:', {
    empId: localStorage.getItem('empId'),
    userId: localStorage.getItem('userId')
  });
  console.log('🏠 首页初始化 - ref values:', {
    empId: empId.value,
    userId: userId.value
  });
  
  if (!empId.value || !userId.value) {
    console.warn('🔴 用户未登录，跳转到登录页');
    router.push('/login');
    return;
  }

  // ========== 数据加载策略优化 ==========
  // 将数据请求分为三个优先级，优先加载关键数据，提升首屏渲染速度
  
  // 【高优先级】关键数据 - 必须先加载完成才能正常使用
  const criticalTasks = [
    loadUserInfo(),           // 用户金币信息（显示余额）
    loadTodayGoldStats(),     // 今日金币统计（显示今日收益）
    loadDeviceStatus(),       // 设备状态（判断是否可看广告）
    loadDeviceConfig()        // 设备配置（广告策略）
  ];
  
  // 【中优先级】重要数据 - 影响部分功能但不阻塞主流程
  const importantTasks = [
    loadLoginStats(),             // 登录统计
    loadWithdrawStatus(),         // 提现状态
    loadWelfareLotteryChances()   // 福利彩票机会
  ];
  
  // 【低优先级】非关键数据 - 可以后台异步加载
  const backgroundTasks = [
    loadGoldRecords(),        // 金币记录列表
    loadRedPacketRecords()    // 红包记录列表
  ];
  
  // 第一步：先加载关键数据，确保页面能快速渲染
  console.log('🔹 开始加载关键数据...');
  await Promise.all(criticalTasks);
  console.log('✅ 关键数据加载完成');
  
  // 第二步：后台并行加载其他数据，不阻塞页面交互
  console.log('🔹 开始后台加载非关键数据...');
  const secondaryPromise = Promise.all([
    ...importantTasks,
    ...backgroundTasks
  ]).then(() => {
    console.log('✅ 非关键数据加载完成');
  }).catch(error => {
    console.error('❌ 后台数据加载失败:', error);
  });
  
  // 排行榜数据也改为后台异步加载
  loadRankingData(false).catch(console.error);
  
  // 不等待非关键数据，立即继续执行后续逻辑
  await Promise.resolve();
  
  // 登录进入首页后触发风控检测
  if (Capacitor.isNativePlatform()) {
    try {
      await RiskCheckPlugin.startRiskCheck();
      console.log('🔒 首页风控检测已触发');
    } catch (e) {
      console.warn('🔒 风控检测调用失败:', e);
    }
  }
  // await loadPoolStatus(); // 加载奖金池状态（暂时隐藏，下下个版本上线）

  // 记录用户活动（进入首页）
  await recordUserActivity();

  // ========== 定时同步机制优化 ==========
  // 延长同步间隔到60秒，并在页面不可见时暂停同步以节省资源
  const SYNC_INTERVAL = 60000; // 60秒同步一次
  
  const performSync = async () => {
    // 页面不可见时跳过同步
    if (document.visibilityState !== 'visible') {
      console.log('⏭️ 页面不可见，跳过定时同步');
      return;
    }
    
    console.log('🔄 定时同步数据看板...');
    try {
      await loadUserInfo(false); // 定时同步时不显示加载状态
      await loadTodayGoldStats(); // 同步今日金币统计（全局）
    } catch (error) {
      console.error('❌ 定时同步失败:', error);
    }
  };
  
  // 使用setInterval执行定时同步
  syncInterval = setInterval(performSync, SYNC_INTERVAL);

  // 监听页面可见性变化，页面重新可见时同步数据
  document.addEventListener('visibilitychange', handleVisibilityChange);
  
  // 调试预估收益显示
  console.log('调试 - 上月累计金币:', lastMonthGold.value);
  console.log('调试 - 上月预估收益:', (lastMonthGold.value / 1000).toFixed(2));
  console.log('调试 - 本月累计金币:', currentMonthGold.value);
  console.log('调试 - 本月预估收益:', (currentMonthGold.value / 1000).toFixed(2));
});

onUnmounted(() => {
  // 清理定时器
  if (syncInterval) {
    clearInterval(syncInterval);
    syncInterval = null;
  }
  // 移除事件监听
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});

// 加载用户金币信息
const loadUserInfo = async (showLoading: boolean = true) => {
  if (!empId.value || !userId.value) return;  
  if (showLoading) {
    isLoading.value = true;
  }
  error.value = '';
  
  try {
    console.log('加载用户信息:', { userId: userId.value, empId: empId.value });
    const response = await getUserInfo(userId.value, empId.value);
    console.log('用户信息响应:', response);
    if (response.success && response.data) {
      currentMonthGold.value = Number(response.data.currentMonthGold) || 0;
      lastMonthGold.value = Number(response.data.lastMonthGold) || 0;
      bonusGold.value = response.data.bonusGold !== undefined && response.data.bonusGold !== null ? Number(response.data.bonusGold) : 0;
      hasClaimedBonus.value = Boolean(response.data.hasClaimedBonus);
    } else {
      error.value = response.message || '获取金币信息失败';
    }
    
    // 加载周目标信息
    await loadWeeklyTargetInfo();
    
    // 获取彩票数量
    const ticketsResponse = await getCurrentLotteryTickets(userId.value);
    console.log('获取彩票数量响应:', ticketsResponse);
    if (ticketsResponse.success && ticketsResponse.data) {
      lotteryTickets.value = ticketsResponse.data.tickets || [];
    }
  } catch (err) {
    console.error('获取金币信息失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    if (showLoading) {
      isLoading.value = false;
    }
  }
};

// 加载周目标信息
const loadWeeklyTargetInfo = async () => {
  try {
    const API_BASE_URL = 'https://wfqmaepvjkdd.sealoshzh.site';
    const token = localStorage.getItem('token');
    
    // 调用周进度接口获取完整信息
    const response = await fetch(`${API_BASE_URL}/api/weeklyBonus/progress`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    
    const result = await response.json();
    console.log('周进度响应:', result);
    
    if (result.success && result.data) {
      // 使用接口返回的targetCount作为周目标值
      weeklyTarget.value = Number(result.data.targetCount) || 100;
      // 使用接口返回的currentCount作为周已完成条数
      weeklyCompleted.value = Number(result.data.currentCount) || 0;
      // 使用接口返回的isClaimed作为是否已领取奖励
      hasClaimedBonus.value = Boolean(result.data.isClaimed);
    }
  } catch (err) {
    console.error('获取周目标进度失败:', err);
  }
};

// 加载奖金池状态（暂时隐藏，下下个版本上线）
// const loadPoolStatus = async () => {
//   isLoadingPool.value = true;
//   try {
//     console.log('🔄 开始加载奖金池状态...');
//     const response = await getPoolStatus();
//     
//     if (response.success && response.data) {
//       poolStatus.value = response.data;
//       console.log('🏆 奖金池状态:', poolStatus.value);
//     } else {
//       console.warn('⚠️ 获取奖金池状态失败:', response.message);
//     }
//   } catch (err) {
//     console.error('❌ 加载奖金池状态失败:', err);
//   } finally {
//     isLoadingPool.value = false;
//   }
// };

// 记录广告观看（用于抽奖券生成）（暂时隐藏，下下个版本上线）
// const recordAdViewCount = async () => {
//   if (!userId.value || !empId.value) return;
//   
//   try {
//     const response = await recordAdView(userId.value, empId.value);
//     
//     if (response.success && response.data) {
//       if (response.data.ticketGenerated) {
//         console.log('🎫 生成抽奖券:', response.data.ticketNumber);
//         // 可以在这里显示抽奖券生成的提示
//       }
//     }
//   } catch (err) {
//     console.error('❌ 记录广告观看失败:', err);
//   }
// };

// 导航到抽奖详情页面（暂时隐藏，下下个版本上线）
// const navigateToLotteryDetail = () => {
//   router.push('/lottery-detail');
// };

// 加载今日金币统计（全局，所有设备）
const loadTodayGoldStats = async () => {
  if (!userId.value) {
    console.log('❌ 加载今日金币统计失败：userId为空');
    return;
  }

  try {
    console.log('🔄 开始加载今日金币统计（全局）...');
    const response = await getTodayGoldStats(userId.value);
    
    if (response.success && response.data) {
    todayCoins.value = Number(response.data.todayCoins) || 0;
    todayRecordCount.value = Number(response.data.todayRecordCount) || 0;
    yesterdayRecordCount.value = Number(response.data.yesterdayRecordCount) || 0;
    console.log('💰 今日金币统计（全局）:', {
      coins: todayCoins.value,
      records: todayRecordCount.value,
      yesterdayRecords: yesterdayRecordCount.value
    });
  } else {
      console.warn('⚠️ 获取今日金币统计失败:', response.message);
    }
  } catch (err) {
    console.error('❌ 加载今日金币统计失败:', err);
  }
};

// 加载红包记录
const loadRedPacketRecords = async () => {
  if (!userId.value) {
    console.log('❌ 加载红包记录失败：userId为空');
    return;
  }

  console.log('🔄 开始加载红包记录...');
  isLoadingRedPacketRecords.value = true;

  try {
    console.log('📡 发送API请求获取红包记录...');
    console.log('   userId:', userId.value);

    // 获取用户的红包记录
    const response = await getUserRedPacketRecords(userId.value, 1, 100);

    console.log('✅ API请求完成，响应:', {
      success: response.success,
      message: response.message,
      dataLength: response.data ? response.data.records.length : 0
    });

    if (response.success && response.data && Array.isArray(response.data.records)) {
      console.log('📊 开始处理数据，原始数据量:', response.data.records.length);

      // 转换并排序记录（按时间倒序，最新的在前面）
      const newRedPacketRecords = response.data.records
        .map((record: any, index: number) => {
          // 安全处理时间字段
          const createdAt = record.createdAt || Date.now();
          const recordTime = new Date(createdAt);

          const redPacketRecord = {
            id: record._id || `red_packet_record-${index}-${Date.now()}`, // 确保ID唯一
            time: recordTime.toLocaleString('zh-CN', {
              year: 'numeric', month: '2-digit', day: '2-digit',
              hour: '2-digit', minute: '2-digit'
            }),
            amount: Number(record.amount) || 0, // 确保金额为数字且有默认值
            poolBalanceAfter: Number(record.poolBalanceAfter) || 0,
            timestamp: recordTime.getTime() // 用于排序
          };

          // 每10条记录打印一次，避免日志过多
          if (index % 10 === 0) {
            console.log(`   处理红包记录 ${index + 1}:`, {
              id: redPacketRecord.id,
              time: redPacketRecord.time,
              amount: redPacketRecord.amount
            });
          }

          return redPacketRecord;
        })
        .sort((a, b) => b.timestamp - a.timestamp); // 按时间倒序排序

      // 只有当后端返回成功且有数据时，才更新redPacketRecords
      if (newRedPacketRecords.length > 0) {
        redPacketRecords.value = newRedPacketRecords;
        console.log('🔧 排序完成，最终红包记录数:', redPacketRecords.value.length);
      } else {
        console.log('⚠️ 后端返回空红包记录，保留本地手动添加的记录');
      }
    } else {
      console.warn('⚠️ API响应数据异常，保留本地手动添加的记录:', response);
    }
  } catch (err) {
    console.error('❌ 获取红包记录失败，保留本地手动添加的记录:', err);
  } finally {
    console.log('✅ 加载红包记录完成');
    isLoadingRedPacketRecords.value = false;
  }
};

// 合并金币记录和红包记录
const combinedRecords = computed(() => {
  // 转换金币记录，添加type字段
  const goldRecords = records.value.map(record => ({
    ...record,
    type: 'gold'
  }));
  
  // 转换红包记录，添加type字段
  const redPacketRecordsWithType = redPacketRecords.value.map(record => ({
    ...record,
    type: 'red-packet'
  }));
  
  // 合并并按时间倒序排序
  const mergedRecords = [...goldRecords, ...redPacketRecordsWithType];
  console.log('🔄 合并记录前 - 金币记录数:', goldRecords.length, '红包记录数:', redPacketRecordsWithType.length);
  
  // 按时间倒序排序
  const sortedRecords = mergedRecords.sort((a, b) => {
    const timestampA = a.timestamp || 0;
    const timestampB = b.timestamp || 0;
    console.log('🔄 排序比较 - 记录A时间戳:', timestampA, '记录B时间戳:', timestampB);
    return timestampB - timestampA;
  });
  
  console.log('🔄 合并记录后 - 总记录数:', sortedRecords.length);
  if (sortedRecords.length > 0) {
    console.log('🔄 前5条记录:', sortedRecords.slice(0, 5).map(r => ({ time: r.time, amount: r.amount, type: r.type, timestamp: r.timestamp })));
  }
  
  return sortedRecords;
});

// 计算单条平均金币
const averageGoldPerAd = computed(() => {
  if (todayRecordCount.value === 0) return '-';
  return todayCoins.value / todayRecordCount.value;
});

// 计算设备评级
const deviceRating = computed(() => {
  if (todayRecordCount.value === 0) return '-';
  const avg = averageGoldPerAd.value;
  if (typeof avg === 'number') {
    if (avg > 100) return '优秀';
    if (avg >= 50) return '正常';
    return '较低';
  }
  return '-';
});

// 获取设备评级对应的颜色
const deviceRatingColor = computed(() => {
  if (deviceRating.value === '-') return 'text-zinc-400';
  switch (deviceRating.value) {
    case '优秀':
      return 'text-emerald-400';
    case '正常':
      return 'text-amber-400';
    case '较低':
      return 'text-red-400';
    default:
      return 'text-zinc-400';
  }
});

// 获取单条平均金币的颜色
const averageGoldColor = computed(() => {
  const avg = averageGoldPerAd.value;
  if (avg === '-') return 'text-zinc-400';
  if (typeof avg === 'number') {
    if (avg > 100) return 'text-emerald-400';
    if (avg >= 50) return 'text-amber-400';
    return 'text-red-400';
  }
  return 'text-zinc-400';
});

// 加载金币记录（仅当前设备，用于最近收益列表）
const loadGoldRecords = async () => {
  if (!userId.value) {
    console.log('❌ 加载金币记录失败：userId为空');
    return;
  }

  console.log('🔄 开始加载金币记录（当前设备）...');
  isLoadingRecords.value = true;

  // 重置数据，避免显示旧数据
  records.value = [];

  try {
    console.log('📡 发送API请求获取金币记录...');
    console.log('   userId:', userId.value);
    console.log('   deviceId:', getDeviceId());
    console.log('   limit:', 10000);

    // 获取当前设备的金币记录（使用较大limit）
    const deviceId = getDeviceId();
    const response = await getGoldLogs(userId.value, deviceId, 10000);

    console.log('✅ API请求完成，响应:', {
      success: response.success,
      message: response.message,
      dataLength: response.data ? response.data.length : 0
    });

    if (response.success && response.data && Array.isArray(response.data)) {
      console.log('📊 开始处理数据，原始数据量:', response.data.length);

      // 只保留最近200条用于显示
      const displayData = response.data.slice(0, 200);
      console.log('📋 显示数据量:', displayData.length);

      // 转换并排序记录（按时间倒序，最新的在前面）
      records.value = displayData
        .map((log: any, index: number) => {
          // 安全处理时间字段
          const createTime = log.createTime || Date.now();
          const recordTime = new Date(createTime);

          const record = {
            id: log._id || `record-${index}-${Date.now()}`, // 确保ID唯一
            time: recordTime.toLocaleString('zh-CN', {
              year: 'numeric', month: '2-digit', day: '2-digit',
              hour: '2-digit', minute: '2-digit'
            }),
            amount: Number(log.gold) || 0, // 确保金额为数字且有默认值
            timestamp: recordTime.getTime() // 用于排序
          };

          // 每10条记录打印一次，避免日志过多
          if (index % 10 === 0) {
            console.log(`   处理记录 ${index + 1}:`, {
              id: record.id,
              time: record.time,
              amount: record.amount
            });
          }

          return record;
        })
        .sort((a, b) => b.timestamp - a.timestamp) // 按时间倒序排序
        .map((record) => ({
          id: record.id,
          time: record.time,
          amount: record.amount
        }));

      console.log('🔧 排序完成，最终记录数:', records.value.length);
    } else {
      console.warn('⚠️ API响应数据异常:', response);
    }
  } catch (err) {
    console.error('❌ 获取金币记录失败:', err);
  } finally {
    console.log('✅ 加载金币记录完成');
    isLoadingRecords.value = false;
  }
};

// 处理广告观看
const handleWatchAd = async () => {
  if (isWatching.value || !empId.value || !userId.value) return;
  
  // 检查设备状态
  await loadDeviceStatus();
  if (deviceStatus.value.isLimited) {
    error.value = '检测到该设备价值过低';
    return;
  }
  
  isWatching.value = true;
  error.value = '';
  
  try {
    // 记录用户活动（观看广告）
    await recordUserActivity();
    
    // 调用广告管理逻辑
    const result = await showRewardVideo();
    console.log('广告观看成功，ECPM:', result.ecpm, '广告位ID:', result.slotId);
    
    // 调用后端发放金币接口，传递ecpm、广告位ID和设备ID
    const deviceId = getDeviceId();
    const rewardResponse = await rewardGold(userId.value, empId.value, result.ecpm, result.slotId, deviceId);
    
    if (rewardResponse.success && rewardResponse.data) {
      const earned = rewardResponse.data.gold;
      console.log('获得金币数量:', earned);
      
      // 确保金币数量是有效的数字
        if (typeof earned === 'number' && earned > 0) {
          // 更新本地状态（关键路径）
          currentMonthGold.value = rewardResponse.data.currentMonthGold;
          // 显示金币奖励动画和语音（关键路径）
          await showRewardAnimation(earned);
          
          // 检查是否有红包信息（关键路径）
          const hasRedPacket = rewardResponse.data.hasRedPacket;
          const redPacketAmount = rewardResponse.data.redPacketAmount;
          
          console.log('红包信息:', {
            hasRedPacket: hasRedPacket,
            redPacketAmount: redPacketAmount
          });
          
          if (hasRedPacket && redPacketAmount > 0) {
            console.log('🎁 后端触发红包，金额：', redPacketAmount);
            // 显示红包动画（关键路径）
            await showRedPacketAnimation(redPacketAmount);
          } else {
            console.log('❌ 未触发红包，原因:', {
              hasRedPacket: hasRedPacket,
              redPacketAmount: redPacketAmount
            });
          }
          
          // 检查是否获得奖券（关键路径）
          const ticketNumber = rewardResponse.data.ticketNumber;
          const issueNumber = rewardResponse.data.issueNumber;
          
          if (ticketNumber && issueNumber) {
            console.log('🎫 获得奖券:', {
              ticketNumber: ticketNumber,
              issueNumber: issueNumber
            });
            // 显示奖券获得提示（关键路径）
            await showTicketAnimation(ticketNumber);
          }
          
          // ========== 非关键操作：后台异步执行 ==========
          // 这些操作不影响用户获得金币的核心流程，可以在后台执行
          const backgroundOperations = async () => {
            try {
              // 更新设备记录
              const deviceId = getDeviceId();
              const deviceRecordResponse = await updateDeviceRecord(userId.value, deviceId, earned);
              if (deviceRecordResponse.success && deviceRecordResponse.data) {
                deviceStatus.value = {
                  isLimited: deviceRecordResponse.data.isLimited,
                  consecutiveLowValueCount: deviceRecordResponse.data.consecutiveLowValueCount || 0
                };
                console.log('✅ 设备记录更新成功:', deviceStatus.value);
              }
            } catch (error) {
              console.error('❌ 更新设备记录失败:', error);
            }
            
            try {
              // 重新加载今日金币统计（全局）、收益记录（当前设备）和红包记录
              await loadTodayGoldStats();
              await loadGoldRecords();
              await loadRedPacketRecords();
            } catch (error) {
              console.error('❌ 后台数据同步失败:', error);
            }
          };
          
          // 后台执行，不阻塞主线程
          backgroundOperations().catch(console.error);
          
        } else {
          console.error('金币数量无效:', earned);
          error.value = '金币发放失败';
        }
    } else {
      error.value = rewardResponse.message || '金币发放失败';
    }
  } catch (err) {
    console.error('广告观看失败:', err);
    error.value = '暂无合适广告匹配，请点击重试';
  } finally {
    isWatching.value = false;
  }
};

// 领取周目标额外金币
const handleClaimBonus = async () => {
  if (!userId.value || !empId.value) return;
  
  isClaimingBonus.value = true;
  
  try {
    const response = await claimWeeklyBonus();
    console.log('领取周奖励响应:', response);
    if (response.success && response.data) {
      // 使用后端返回的bonusCoins字段
      const earned = (response.data as any).bonusCoins || response.data.gold || 0;
      // 检查currentMonthGold是否是有效数字
      if (typeof response.data.currentMonthGold === 'number' && !isNaN(response.data.currentMonthGold)) {
        currentMonthGold.value = response.data.currentMonthGold;
      }
      hasClaimedBonus.value = true;
      // 显示金币奖励动画和语音
      await showRewardAnimation(earned);
      
      // 检查是否获得彩票
      const ticketNumber = (response.data as any).ticketNumber;
      const issueNumber = (response.data as any).issueNumber;
      
      if (ticketNumber && issueNumber && typeof ticketNumber === 'string' && typeof issueNumber === 'string' && ticketNumber.trim() !== '' && issueNumber.trim() !== '') {
        console.log('🎫 周奖励获得彩票:', {
          ticketNumber: ticketNumber,
          issueNumber: issueNumber
        });
        // 显示彩票获得提示
        await showTicketAnimation(ticketNumber);
      }
      
      // 重新加载用户信息（包括金币信息）
      await loadUserInfo(false);
      // 重新加载今日金币统计（全局）和收益记录（当前设备）
      await loadTodayGoldStats();
      await loadGoldRecords();
      // 重新加载周目标信息
      await loadWeeklyTargetInfo();
    } else {
      console.warn('领取周奖励失败:', response.message);
      error.value = response.message || '领取失败';
    }
  } catch (err) {
    console.error('领取额外金币失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    isClaimingBonus.value = false;
  }
};

// 处理登出
const handleLogout = () => {
  localStorage.removeItem('empId');
  localStorage.removeItem('userId');
  localStorage.removeItem('employeeInfo');
  router.push('/login');
};

// 打开提现弹窗
const openWithdrawModal = () => {
  withdrawAmount.value = Number((lastMonthGold.value / 1000).toFixed(2));
  alipayAccount.value = '';
  alipayName.value = '';
  withdrawSuccess.value = false;
  showWithdrawModal.value = true;
};

// 关闭提现弹窗
const closeWithdrawModal = () => {
  showWithdrawModal.value = false;
};

// 打开提现记录弹窗
const openWithdrawRecordsModal = async () => {
  showWithdrawRecordsModal.value = true;
  await loadWithdrawRecords();
};

// 关闭提现记录弹窗
const closeWithdrawRecordsModal = () => {
  showWithdrawRecordsModal.value = false;
};

// 加载提现记录
const loadWithdrawRecords = async () => {
  // 优先使用 empId（员工号）查询，后端接口实际按员工号匹配
  console.log('💡 加载提现记录 - userId:', userId.value, ', empId:', empId.value);
  const queryUserId = empId.value || userId.value;
  if (!queryUserId) {
    console.warn('❌ 没有可用的用户ID，无法查询提现记录');
    return;
  }
  console.log('🔍 正在查询提现记录，参数 userId:', queryUserId);
  isLoadingWithdrawRecords.value = true;
  try {
    const response = await getWithdrawRecords(queryUserId);
    console.log('📥 提现记录响应:', response);
    if (response.success && response.data) {
      withdrawRecords.value = response.data;
      console.log('✅ 成功加载', response.data.length, '条提现记录');
      if (response.data.length > 0) {
        console.log('📋 第一条提现记录:', response.data[0]);
      }
    } else {
      withdrawRecords.value = [];
      console.log('⚠️ 接口返回数据为空或失败');
    }
  } catch (err) {
    console.error('❌ 加载提现记录失败:', err);
    withdrawRecords.value = [];
  } finally {
    isLoadingWithdrawRecords.value = false;
  }
};

// 加载排行榜数据
const loadRankingData = async (showLoading: boolean = true, forceRefresh: boolean = false) => {
  const startTime = Date.now();
  console.log('🔄 开始加载排行榜数据...');
  
  // 检查缓存：如果数据在缓存期内且不是强制刷新，则跳过加载
  const now = Date.now();
  if (!forceRefresh && now - lastRankingLoadTime.value < RANKING_CACHE_DURATION) {
    console.log('📋 排行榜数据仍在缓存期内（5分钟），跳过加载');
    return;
  }
  
  // 只有首次打开弹窗或明确要求显示loading时才显示loading
  if (showLoading && isRankingFirstLoad.value) {
    isLoadingRanking.value = true;
  }
  
  try {
    // 并行请求三个接口
    const apiStartTime = Date.now();
    console.log('🔄 API请求开始时间:', apiStartTime);
    
    const [todayResponse, yesterdayResponse, monthResponse] = await Promise.all([
      getTodayRanking(),
      getYesterdayRanking(),
      getMonthTopDaily()
    ]);
    
    const apiEndTime = Date.now();
    console.log('✅ API请求完成，耗时:', apiEndTime - apiStartTime, 'ms');
    console.log('📊 今日排行榜数据:', todayResponse);
    console.log('📊 昨日排行榜数据:', yesterdayResponse);
    console.log('📊 本月最高数据:', monthResponse);
    
    // 处理今日排行榜数据，添加rank字段
    if (todayResponse.success && todayResponse.data) {
      dailyLeaderboard.value = (todayResponse.data.ranking || []).map((item: any, index: number) => ({
        ...item,
        rank: index + 1,
        empId: item.employeeId,
        totalCoins: Math.round(item.earnings * 1000),
        adCount: item.count,
        avgCoins: item.avgGold
      }));
    }
    
    // 处理昨日排行榜数据，添加rank字段
    if (yesterdayResponse.success && yesterdayResponse.data) {
      yesterdayLeaderboard.value = (yesterdayResponse.data.ranking || []).map((item: any, index: number) => ({
        ...item,
        rank: index + 1,
        empId: item.employeeId,
        totalCoins: Math.round(item.earnings * 1000),
        adCount: item.count,
        avgCoins: item.avgGold
      }));
      yesterdayDate.value = yesterdayResponse.data.date || '';
    }
    
    // 处理本月单日最高数据
    if (monthResponse.success && monthResponse.data && monthResponse.data.topDaily) {
      const top = monthResponse.data.topDaily;
      monthlyTopUser.value = {
        date: top.date,
        empId: top.employeeId,
        totalCoins: Math.round(top.earnings * 1000),
        adCount: top.count,
        avgCoins: top.avgGold
      };
    } else {
      monthlyTopUser.value = null;
    }
    
    // 数据加载完成后，标记首次加载已完成并更新缓存时间
    isRankingFirstLoad.value = false;
    lastRankingLoadTime.value = Date.now(); // 更新缓存时间戳
    
    const endTime = Date.now();
    console.log('✅ 排行榜数据加载完成，总耗时:', endTime - startTime, 'ms');
  } catch (error) {
    console.error('❌ 加载排行榜数据失败:', error);
  } finally {
    isLoadingRanking.value = false;
  }
};

// 监听排行榜弹窗打开，自动加载数据（使用缓存机制）
watch(showLeaderboard, (newValue) => {
  if (newValue) {
    // 打开弹窗时加载数据（受缓存控制，5分钟内不会重复请求）
    loadRankingData(true);
  }
});

// 刷新排行榜数据（供按钮调用，强制刷新）
const refreshRankingData = () => {
  isRankingFirstLoad.value = true; // 重置首次加载标记以显示loading
  loadRankingData(true, true); // 第二个参数为true表示强制刷新
};

// 格式化日期（使用北京时间，UTC+8）
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  // 使用UTC方法直接处理，避免本地时区干扰
  const utcYear = date.getUTCFullYear();
  const utcMonth = date.getUTCMonth() + 1;
  const utcDay = date.getUTCDate();
  const utcHour = date.getUTCHours() + 8; // 转换为北京时间
  
  // 处理跨天情况
  let year = utcYear;
  let month = utcMonth;
  let day = utcDay;
  let hour = utcHour;
  
  if (hour >= 24) {
    hour -= 24;
    day += 1;
    if (day > [31, 28 + (year % 4 === 0 && year % 100 !== 0 || year % 400 === 0 ? 1 : 0), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1]) {
      day = 1;
      month += 1;
      if (month > 12) {
        month = 1;
        year += 1;
      }
    }
  }
  
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(date.getUTCMinutes()).padStart(2, '0')}`;
};

// 获取状态样式
const getStatusStyle = (status: number) => {
  switch (status) {
    case 0:
      return { text: '待打款', class: 'text-amber-400 bg-amber-500/10 border-amber-500/20', amountClass: 'text-amber-400' };
    case 1:
      return { text: '已打款', class: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20', amountClass: 'text-emerald-400' };
    case 2:
      return { text: '已拒绝', class: 'text-red-400 bg-red-500/10 border-red-500/20', amountClass: 'text-red-400' };
    default:
      return { text: '未知状态', class: 'text-zinc-400 bg-zinc-500/10 border-zinc-500/20', amountClass: 'text-zinc-400' };
  }
};

// 提交提现申请
const submitWithdraw = async () => {
  if (!alipayAccount.value.trim() || !alipayName.value.trim()) {
    alert('请填写完整的支付宝信息');
    return;
  }

  isSubmittingWithdraw.value = true;

  try {
    // 调用后端提现接口
    const response = await submitWithdrawRequest({
      userId: userId.value,
      employeeId: empId.value,
      amount: withdrawAmount.value,
      alipayAccount: alipayAccount.value,
      alipayName: alipayName.value,
      goldAmount: lastMonthGold.value
    });

    if (response.success) {
      // 更新剩余金币
      if (response.data && response.data.remainingGold !== undefined) {
        lastMonthGold.value = response.data.remainingGold;
      }
      withdrawSuccess.value = true;
      // 3秒后关闭弹窗
      setTimeout(() => {
        closeWithdrawModal();
      }, 3000);
    } else {
      alert(response.message || '提现申请提交失败');
    }
  } catch (err) {
    console.error('提现申请失败:', err);
    alert('提现申请提交失败，请稍后重试');
  } finally {
    isSubmittingWithdraw.value = false;
  }
};
</script>

<template>
  <div class="min-h-screen bg-[#020205] text-white pb-12 relative overflow-hidden">
    <!-- 背景装饰光晕 -->
    <div class="absolute top-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-600/10 blur-[120px] rounded-full pointer-events-none" />
    <div class="absolute top-[20%] left-[-10%] w-[50%] h-[50%] bg-blue-600/10 blur-[120px] rounded-full pointer-events-none" />
    <div class="absolute bottom-[-10%] left-[20%] w-[40%] h-[40%] bg-emerald-500/10 blur-[120px] rounded-full pointer-events-none" />
    <div class="absolute top-[30%] right-[20%] w-[40%] h-[40%] bg-emerald-500/10 blur-[120px] rounded-full pointer-events-none" />

    <!-- Header -->
    <header class="bg-black/40 backdrop-blur-xl border-b border-white/5 pt-8 pb-5 px-6 flex justify-between items-center sticky top-0 z-20">
      <div class="flex items-center">
        <div class="w-8 h-8 bg-gradient-to-br from-emerald-400 to-blue-500 rounded-xl flex items-center justify-center shadow-lg shadow-emerald-500/20 mr-3">
          <TrendingUp class="text-white w-5 h-5" />
        </div>
        <div class="flex flex-col">
          <span class="font-bold text-sm tracking-widest uppercase bg-gradient-to-r from-emerald-400 to-blue-400 bg-clip-text text-transparent">广告变现系统</span>
          <span class="text-[10px] text-zinc-400 font-bold tracking-wider">员工ID：{{ empId }} · 已登录{{ loginDays }}天</span>
        </div>
      </div>
      <button @click="handleLogout" class="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white hover:bg-white/10 transition-all">
        <LogOut class="w-4 h-4" />
      </button>
    </header>

    <main class="max-w-md mx-auto px-6 mt-6 space-y-6 relative z-10 pb-24">
      <!-- Stats Section -->
      <div class="space-y-3">
        <div class="flex justify-between items-end px-2">
          <h2 class="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-medium">收益看板</h2>
          <div class="flex items-center">
            <div class="w-1 h-1 bg-emerald-500 rounded-full animate-pulse mr-1.5" />
            <span class="text-[10px] text-emerald-500 font-mono">实时同步</span>
          </div>
        </div>
        <!-- 错误信息显示 -->
          <div v-if="error" class="p-4 bg-red-500/10 border border-red-500/20 rounded-lg mb-3">
            <p class="text-red-400 text-sm">{{ error }}</p>
          </div>
          
          <!-- 加载状态 -->
          <div v-if="isLoading" class="grid grid-cols-2 gap-3">
            <div class="bg-white/3 border border-white/5 p-4 rounded-[1.25rem] backdrop-blur-md animate-pulse">
              <div class="h-3 bg-white/10 rounded w-1/2 mb-2"></div>
              <div class="h-6 bg-white/10 rounded w-3/4"></div>
            </div>
            <div class="bg-white/3 border border-white/5 p-4 rounded-[1.25rem] backdrop-blur-md animate-pulse">
              <div class="h-3 bg-white/10 rounded w-1/2 mb-2"></div>
              <div class="h-6 bg-white/10 rounded w-3/4"></div>
            </div>
            <div class="bg-white/3 border border-white/5 p-4 rounded-[1.25rem] backdrop-blur-md animate-pulse">
              <div class="h-3 bg-white/10 rounded w-1/2 mb-2"></div>
              <div class="h-6 bg-white/10 rounded w-3/4"></div>
            </div>
            <div class="bg-white/3 border border-white/5 p-4 rounded-[1.25rem] backdrop-blur-md animate-pulse">
              <div class="h-3 bg-white/10 rounded w-1/2 mb-2"></div>
              <div class="h-6 bg-white/10 rounded w-3/4"></div>
            </div>
          </div>
          
          <!-- 金币统计数据 -->
          <div v-else class="grid grid-cols-2 gap-3">
            <div class="group relative glass-card rounded-[1.25rem] overflow-hidden transition-all hover:bg-white/5">
              <div class="absolute top-0 right-0 w-16 h-16 bg-blue-500/10 blur-2xl rounded-full -mr-8 -mt-8" />
              <div class="p-4">
                <div class="relative">
                  <p class="text-zinc-500 text-[9px] uppercase tracking-wider mb-1">上月累计金币</p>
                  <p class="text-lg font-light tracking-tight text-blue-400 mb-2">{{ Math.floor(lastMonthGold).toLocaleString() }}</p>
                  <div class="flex gap-2">
                    <button 
                      @click="withdrawEnabled ? openWithdrawModal() : null"
                      :disabled="!withdrawEnabled"
                      class="px-2 py-0.5 rounded-md text-[7px] font-bold uppercase tracking-wider transition-all border flex items-center justify-center gap-1 flex-1"
                      :class="withdrawEnabled 
                        ? 'bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 border-blue-500/30 cursor-pointer' 
                        : 'bg-zinc-500/10 text-zinc-600 border-zinc-500/20 cursor-not-allowed'"
                    >
                      <Wallet class="w-2 h-2" />
                      提现
                    </button>
                    <button 
                      @click="openWithdrawRecordsModal"
                      class="px-2 py-0.5 rounded-md text-[7px] font-bold uppercase tracking-wider transition-all border flex items-center justify-center gap-1 flex-1 bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 border-blue-500/30 cursor-pointer"
                    >
                      <CreditCard class="w-2 h-2" />
                      记录
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="group relative bg-gradient-to-br from-emerald-500 to-teal-700 rounded-[1.25rem] shadow-xl shadow-emerald-500/10 overflow-hidden transition-all hover:scale-[1.02]">
              <div class="absolute top-0 right-0 w-16 h-16 bg-white/10 blur-2xl rounded-full -mr-8 -mt-8" />
              <div class="p-4">
                <p class="text-emerald-100/60 text-[9px] uppercase tracking-wider mb-1">本月累计金币</p>
                <p class="text-lg font-bold text-white tracking-tight">{{ Math.floor(currentMonthGold).toLocaleString() }}</p>
                <p class="text-emerald-200/80 text-[8px] mt-2">预估收益≈{{ (currentMonthGold / 1000).toFixed(2) }}元</p>
              </div>
            </div>
            <div class="group relative glass-card rounded-[1.25rem] overflow-hidden transition-all hover:bg-white/5">
              <div class="absolute top-0 right-0 w-16 h-16 bg-purple-500/10 blur-2xl rounded-full -mr-8 -mt-8" />
              <div class="p-4">
                <div 
                  class="absolute top-4 right-4 px-2 py-0.5 rounded-full text-[8px] font-bold tracking-widest border"
                  :class="weeklyTarget > 0 ? (weeklyCompleted >= weeklyTarget ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-red-500/10 text-red-400 border-red-500/20') : 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20'"
                >
                  {{ weeklyTarget > 0 ? (weeklyCompleted >= weeklyTarget ? '已完成' : '未完成') : '未设置' }}
                </div>
                <p class="text-zinc-500 text-[9px] uppercase tracking-wider mb-1">本周目标任务</p>
                <p class="text-lg font-light tracking-tight text-purple-400">{{ weeklyTarget > 0 ? `${weeklyTarget.toLocaleString()} 条` : '未设置' }}</p>
              </div>
            </div>
            <div class="group relative glass-card rounded-[1.25rem] overflow-hidden transition-all hover:bg-white/5">
              <div class="absolute top-0 right-0 w-16 h-16 bg-amber-500/10 blur-2xl rounded-full -mr-8 -mt-8" />
              <div class="p-4">
                <div class="flex items-center gap-2 mb-1">
                  <p class="text-zinc-500 text-[9px] uppercase tracking-wider">今日金币收益</p>
                  <span class="text-[10px] text-zinc-400 bg-zinc-500/10 px-1.5 py-0.5 rounded whitespace-nowrap flex-shrink-0">{{ yesterdayRecordCount }}条</span>
                </div>
                <div class="flex items-center gap-2">
                  <p class="text-lg font-bold text-amber-400 tracking-tight whitespace-nowrap">{{ Math.floor(todayCoins).toLocaleString() }}</p>
                  <span class="text-[10px] text-amber-400 bg-amber-500/10 px-1.5 py-0.5 rounded whitespace-nowrap flex-shrink-0">{{ todayRecordCount }}条</span>
                </div>
              </div>
            </div>
            

          </div>
      </div>

      <!-- 周目标进度条和额外金币奖励 -->
      <div v-if="weeklyTarget >= 0" class="px-4 py-2">
        <div class="flex items-center">
          <!-- 进度条 - 占3/4 -->
          <div class="w-3/4 space-y-2 mr-3">
            <div class="flex justify-between items-center text-[9px]">
              <span class="text-zinc-500 uppercase tracking-wider">本周目标进度</span>
              <span class="text-purple-400 font-bold">{{ weeklyTarget > 0 ? `${Math.min(100, weeklyProgress)}%` : '未设置' }}</span>
            </div>
            <div class="h-2 bg-zinc-800/50 rounded-full overflow-hidden border border-white/5">
              <div 
              class="h-full bg-gradient-to-r from-purple-500 to-emerald-500 rounded-full transition-all duration-500 ease-out"
              :style="{ width: weeklyTarget > 0 ? `${Math.min(100, weeklyProgress)}%` : '0%' }"
            />
            </div>
            <div class="flex justify-between text-[8px] text-zinc-600">
              <span>{{ weeklyCompleted.toLocaleString() }} 条{{ weeklyTarget > 0 && !hasClaimedBonus ? (weeklyCompleted >= weeklyTarget ? '（已达标可领取）' : `（还差 ${Math.floor(weeklyTarget - weeklyCompleted).toLocaleString()} 条）`) : '' }}</span>
              <span>目标 {{ weeklyTarget.toLocaleString() }} 条</span>
            </div>
          </div>

          <!-- 领取额外金币按钮 - 占1/4 -->
          <div class="w-1/4 shrink-0">
            <!-- 已领取提示 -->
            <div
              v-if="hasClaimedBonus"
              class="w-full h-full py-1.5 px-2 rounded-lg text-center text-[8px] font-bold uppercase tracking-widest bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex flex-col items-center justify-center"
            >
              已领取
            </div>

            <!-- 领取按钮 -->
            <button
              v-else-if="bonusGold > 0"
              @click="handleClaimBonus"
              :disabled="isClaimingBonus || weeklyCompleted < weeklyTarget || weeklyTarget === 0"
              class="w-full h-full py-1.5 px-2 rounded-lg font-bold text-[9px] uppercase tracking-widest transition-all border flex flex-col items-center justify-center relative overflow-hidden group"
              :class="[
                    isClaimingBonus || weeklyCompleted < weeklyTarget || weeklyTarget === 0
                      ? 'bg-zinc-800/50 border-zinc-700 text-yellow-400 cursor-not-allowed' 
                      : 'bg-gradient-to-br from-amber-400 via-orange-500 to-red-500 text-white border-amber-300/50 shadow-[0_0_20px_rgba(245,158,11,0.4),0_0_40px_rgba(245,158,11,0.2)] hover:shadow-[0_0_30px_rgba(245,158,11,0.6),0_0_60px_rgba(245,158,11,0.3)] hover:scale-[1.05] active:scale-[0.95]'
                  ]"
            >
              <!-- 动态背景光效 -->
              <div 
                v-if="!isClaimingBonus && weeklyCompleted >= weeklyTarget && weeklyTarget > 0"
                class="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full animate-[shimmer_2s_infinite]"
              />
              <!-- 脉冲光圈 -->
              <div 
                v-if="!isClaimingBonus && weeklyCompleted >= weeklyTarget && weeklyTarget > 0"
                class="absolute inset-0 rounded-lg animate-pulse bg-gradient-to-r from-amber-500/20 via-orange-500/30 to-red-500/20"
              />
              <!-- 灰色脉冲光圈（未完成目标或未设置目标时） -->
              <div 
                v-if="!isClaimingBonus && (weeklyCompleted < weeklyTarget || weeklyTarget === 0)"
                class="absolute inset-0 rounded-lg animate-pulse bg-gradient-to-r from-zinc-500/10 via-zinc-600/20 to-zinc-500/10"
              />
              <!-- 奖励文字 -->
              <span class="text-center leading-tight relative z-10">{{ isClaimingBonus ? '领取中...' : '奖金币' }}</span>
              <span class="text-center leading-tight relative z-10 text-xs">{{ isClaimingBonus ? '' : `${bonusGold.toLocaleString()}` }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Ad Trigger - 大圆形按钮 -->
      <div class="flex flex-col items-center justify-center py-2 relative">
        <!-- 今日奖金池（暂时隐藏，下下个版本上线） -->
      <!-- <div class="relative w-full max-w-xs mb-6">
        <div class="absolute inset-0 bg-gradient-to-r from-yellow-500 to-amber-500 blur-xl opacity-20 rounded-2xl animate-pulse"></div>
        <div class="relative glass-card rounded-2xl p-4 border border-yellow-500/30">
          <div class="flex flex-col items-center mb-3">
            <div class="flex items-center mb-2">
              <Trophy class="w-5 h-5 text-yellow-500 mr-3" />
              <h3 class="text-[10px] uppercase tracking-wider text-zinc-400 font-medium">今日奖金池</h3>
            </div>
            <p class="text-3xl font-bold text-white text-center animate-pulse">
              <span class="animate-bounce">₸</span>
              {{ poolStatus.lotteryPool.toLocaleString() }}
              <span class="animate-bounce ml-1">金币</span>
            </p>
          </div>
          <div class="flex items-center justify-between text-[8px] text-zinc-500">
            <span>每晚10点开奖</span>
            <button 
              @click="navigateToLotteryDetail" 
              class="bg-yellow-500/20 text-yellow-400 text-xs font-bold px-3 py-1 rounded-full hover:bg-yellow-500/30 transition-all"
            >
              点击查看详情
            </button>
          </div>
        </div>
      </div> -->
        
        <div class="relative">
          <!-- 按钮背景光晕 -->
          <div 
            class="absolute inset-0 blur-3xl rounded-full transition-all duration-1000"
            :class="[
              isWatching ? 'bg-blue-500 opacity-60 scale-110' : 'bg-emerald-500 opacity-30 scale-100',
              'animate-pulse'
            ]"
          />
          
          <button
            @click="handleWatchAd"
            :disabled="isWatching || deviceStatus.isLimited"
            class="relative w-48 h-48 rounded-full flex flex-col items-center justify-center transition-all active:scale-90 border-2"
            :class="[
              isWatching 
                ? 'bg-zinc-900/80 border-zinc-800 text-zinc-600 cursor-not-allowed' 
                : deviceStatus.isLimited
                ? 'bg-zinc-900/80 border-red-800/50 text-red-400 cursor-not-allowed' 
                : 'bg-black border-white/10 text-white shadow-[0_20px_50px_rgba(0,0,0,0.5)] hover:border-emerald-500/50'
            ]"
          >
            <div :class="{ 'animate-spin': isWatching }" class="mb-5">
              <PlayCircle class="w-16 h-16" :class="isWatching ? 'text-zinc-700' : deviceStatus.isLimited ? 'text-red-400' : 'text-emerald-400'" />
            </div>
            <div class="text-center">
              <span class="block text-base font-black uppercase tracking-widest leading-none">
                {{ isWatching ? '正在加载' : deviceStatus.isLimited ? '设备价值过低' : '点击赚取金币' }}
              </span>
            </div>
          </button>
        </div>


        <p class="mt-4 text-[10px] text-zinc-500 uppercase tracking-[0.3em] font-medium">
          {{ isWatching ? '正在为您匹配优质广告资源' : deviceStatus.isLimited ? '设备已被限制' : '广告激励已就绪' }}
        </p>
        
        <!-- 设备限制提示 -->
        <div v-if="deviceStatus.isLimited" class="mt-4 p-3 bg-red-900/30 rounded-lg border border-red-800/50">
          <p class="text-red-300 text-sm text-center">
            检测到该设备价值过低，已限制赚金币功能，请重启后解除
          </p>
        </div>
        
        <!-- 设备评级展示 -->
        <div class="mt-4 px-4 py-3 bg-zinc-900/50 rounded-xl border border-zinc-800">
          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <span class="text-[10px] text-zinc-400 uppercase tracking-wider mr-2">单条平均金币：</span>
              <span :class="['text-[11px] font-bold', averageGoldColor]">{{ typeof averageGoldPerAd === 'number' ? averageGoldPerAd.toFixed(1) : averageGoldPerAd }}</span>
            </div>
            <div class="h-4 w-px bg-zinc-700 mx-4"></div>
            <div class="flex items-center">
              <span class="text-[10px] text-zinc-400 uppercase tracking-wider mr-2">评级：</span>
              <span :class="['text-[11px] font-bold', deviceRatingColor]">{{ deviceRating }}</span>
            </div>
          </div>
        </div>
        
        <!-- 金币奖励弹窗 -->
        <transition name="reward-popup">
          <div v-if="showRewardPopup" class="fixed inset-0 flex items-center justify-center z-9999 pointer-events-none">
            <div v-if="rewardAmount > 0" class="bg-gradient-to-r from-amber-400 to-orange-500 text-white px-6 py-4 rounded-xl font-bold shadow-lg flex flex-col items-center justify-center border border-white/30 animate-bounce pointer-events-auto">
              <span class="text-lg">奖励</span>
              <span class="text-xl">+{{ Math.floor(rewardAmount) }} 金币</span>
            </div>
            <div v-else class="bg-gradient-to-r from-purple-400 to-pink-500 text-white px-6 py-3 rounded-xl font-bold shadow-lg flex items-center border border-white/30 animate-bounce pointer-events-auto">
              <Ticket class="w-6 h-6 text-white mr-2" />
              <span class="text-xl">获得幸运彩票</span>
            </div>
          </div>
        </transition>
        
        <!-- 红包弹窗 -->
        <transition name="red-packet-popup">
          <div v-if="showRedPacketPopup" class="fixed inset-0 flex items-center justify-center z-[10000] pointer-events-none">
            <div class="bg-gradient-to-r from-red-500 to-pink-600 text-white px-8 py-6 rounded-2xl font-bold shadow-2xl flex flex-col items-center border border-white/30 pointer-events-auto animate-red-packet-jump z-[10000]">
              <!-- 红包封面 -->
              <div v-if="!isRedPacketOpened" @click="openRedPacket" class="w-full flex flex-col items-center cursor-pointer hover:scale-105 transition-transform">
                <div class="w-20 h-20 bg-white/20 rounded-full flex items-center justify-center mb-6">
                  <Coins class="w-10 h-10 text-white" />
                </div>
                <h3 class="text-2xl font-bold mb-2">恭喜发财</h3>
                <p class="text-lg mb-6">获得随机红包</p>
                <div class="bg-white/20 px-8 py-3 rounded-full text-lg font-bold mb-6">
                  点击拆红包
                </div>
              </div>
              
              <!-- 红包打开后 -->
              <div v-else class="w-full flex flex-col items-center animate-bounce">
                <div class="w-20 h-20 bg-yellow-400/30 rounded-full flex items-center justify-center mb-6">
                  <Coins class="w-12 h-12 text-yellow-300" />
                </div>
                <h3 class="text-2xl font-bold mb-2">恭喜发财</h3>
                <p class="text-lg mb-4">获得红包奖励</p>
                <div class="text-4xl font-extrabold mb-6 text-yellow-300">+{{ Math.floor(redPacketAmount) }} 金币</div>
                <div class="w-full h-1 bg-white/20 rounded-full mb-6"></div>
                <p class="text-sm text-white/80">运气不错！</p>
              </div>
            </div>
          </div>
        </transition>
        
        <!-- 测试按钮（隐藏） -->
        <!-- <div class="flex gap-3 mt-4">
          <button
            @click="showRewardAnimation(100)"
            class="px-6 py-3 bg-blue-500 text-white rounded-full text-sm font-medium hover:bg-blue-600 transition-colors"
          >
            测试奖励弹窗
          </button>
          <button
            @click="simulateRedPacket()"
            class="px-6 py-3 bg-red-500 text-white rounded-full text-sm font-medium hover:bg-red-600 transition-colors"
          >
            测试红包弹窗
          </button>
        </div> -->
      </div>

      <!-- History Section -->
      <div class="space-y-3">
        <div class="flex items-center justify-between px-2">
          <div class="flex items-center">
            <History class="w-3 h-3 text-zinc-500 mr-2" />
            <h2 class="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-medium">最近收益（近50条）</h2>
          </div>
          <div class="flex gap-3">
            <!-- 暂时隐藏周末活动按钮 -->
            <!-- <button 
              @click="showWeekendEvent = true"
              class="px-3 py-1 rounded-full bg-white/5 text-[9px] text-amber-500 uppercase tracking-widest hover:bg-white/10 transition-all font-bold border border-amber-500/20 flex items-center gap-2"
            >
              <Sparkles class="w-3 h-3 text-amber-500" />
              周末活动
            </button> -->
            <button 
              @click="showLeaderboard = true"
              class="px-3 py-1 rounded-full bg-white/5 text-[9px] text-blue-500 uppercase tracking-widest hover:bg-white/10 transition-all font-bold border border-blue-500/20 flex items-center gap-2"
            >
              <Trophy class="w-3 h-3" />
              排行榜
            </button>

            <!-- 暂时隐藏查看全部按钮 -->
            <!-- <button 
              @click="showAllRecords = true"
              class="px-3 py-1 rounded-full bg-white/5 text-[9px] text-emerald-500 uppercase tracking-widest hover:bg-white/10 transition-all font-bold border border-emerald-500/20"
            >
              查看全部
            </button> -->
          </div>
        </div>
        <div class="glass-card rounded-[2rem] overflow-hidden">
          <div class="divide-y divide-white/5 max-h-[400px] overflow-y-auto no-scrollbar">
            <!-- 加载状态 -->
            <div v-if="isLoadingRecords" class="py-16 text-center">
              <div class="w-12 h-12 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4 animate-spin">
                <History class="w-5 h-5 text-zinc-700" />
              </div>
              <p class="text-[10px] text-zinc-600 uppercase tracking-widest">加载中...</p>
            </div>
            <!-- 空状态 -->
            <div v-else-if="records.length === 0 && redPacketRecords.length === 0" class="py-16 text-center">
              <div class="w-12 h-12 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4">
                <History class="w-5 h-5 text-zinc-700" />
              </div>
              <p class="text-[10px] text-zinc-600 uppercase tracking-widest">暂无活动记录</p>
            </div>
            <!-- 记录列表 -->
            <div 
              v-for="record in combinedRecords.slice(0, 50)" 
              :key="record.id" 
              class="px-8 py-5 flex justify-between items-center hover:bg-white/2 transition-colors group"
            >
              <div class="flex flex-col">
                <span class="text-[11px] text-zinc-400 font-mono tracking-tighter">{{ record.time }}</span>
                <span class="text-[9px] text-zinc-600 uppercase tracking-widest mt-0.5">{{ record.type === 'red-packet' ? '幸运红包' : '广告激励成功' }}</span>
              </div>
              <div class="flex items-center">
                <span class="text-sm font-bold font-mono group-hover:scale-110 transition-transform mr-2" :class="record.type === 'red-packet' ? 'text-red-400' : 'text-amber-400'">+{{ Math.floor(record.amount) }}</span>
                <component :is="record.type === 'red-packet' ? 'Gift' : 'Coins'" class="w-4 h-4" :class="record.type === 'red-packet' ? 'text-red-500' : 'text-amber-500'" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- View All Records Modal -->
    <transition name="modal">
      <div v-if="showAllRecords" class="fixed inset-0 z-[9999] flex items-end justify-center sm:items-center p-0 sm:p-6 pointer-events-auto">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998] pointer-events-auto" @click="showAllRecords = false" />
        <div class="relative w-full max-w-md bg-[#020205] border-t sm:border border-white/10 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col h-[90vh] max-h-[600px] z-[9999] shadow-2xl">
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-[#020205] z-10">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-white/5 rounded-full flex items-center justify-center mr-3">
                <History class="w-4 h-4 text-zinc-400" />
              </div>
              <h3 class="text-sm font-bold uppercase tracking-widest">所有收益记录</h3>
            </div>
            <button 
              @click="showAllRecords = false"
              class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white"
            >
              <LogOut class="w-4 h-4 rotate-90" />
            </button>
          </div>
          
          <div class="flex-1 overflow-y-auto no-scrollbar p-4">
            <div class="space-y-2">
              <!-- 加载状态 -->
              <div v-if="isLoadingRecords" class="py-20 text-center">
                <div class="w-12 h-12 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4 animate-spin">
                  <History class="w-5 h-5 text-zinc-700" />
                </div>
                <p class="text-xs text-zinc-600 uppercase tracking-widest">加载中...</p>
              </div>
              <!-- 空状态 -->
              <div v-else-if="records.length === 0" class="py-20 text-center">
                <p class="text-xs text-zinc-600 uppercase tracking-widest">暂无记录</p>
              </div>
              <!-- 记录列表 -->
              <div 
                v-else
                v-for="record in records" 
                :key="record.id" 
                class="px-6 py-4 rounded-2xl glass-card flex justify-between items-center min-h-[60px]"
              >
                <div class="flex flex-col">
                  <span class="text-[11px] text-zinc-400 font-mono tracking-tighter">{{ record.time }}</span>
                  <span class="text-[9px] text-zinc-600 uppercase tracking-widest mt-0.5">激励视频收益</span>
                </div>
                <div class="flex items-center">
                  <span class="text-sm font-bold text-amber-400 font-mono mr-2">+{{ Math.floor(record.amount) }}</span>
                  <Coins class="w-3 h-3 text-amber-500/50" />
                </div>
              </div>
              <!-- 如果记录达到200条，显示提示 -->
              <div v-if="records.length >= 200" class="text-center py-4">
                <p class="text-[10px] text-zinc-600">显示最近200条记录</p>
              </div>
            </div>
          </div>
          
          <div class="p-8 border-t border-white/5 text-center">
            <p class="text-[10px] text-zinc-600 uppercase tracking-[0.2em]">共计 {{ records.length }} 条记录</p>
          </div>
        </div>
      </div>
    </transition>

    <!-- 红包记录弹窗 -->
    <transition name="modal">
      <div v-if="showRedPacketRecords" class="fixed inset-0 z-[9999] flex items-end justify-center sm:items-center p-0 sm:p-6 pointer-events-auto">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998] pointer-events-auto" @click="showRedPacketRecords = false" />
        <div class="relative w-full max-w-md bg-[#020205] border-t sm:border border-white/10 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col h-[90vh] max-h-[600px] z-[9999] shadow-2xl">
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-[#020205] z-10">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-red-500/20 rounded-full flex items-center justify-center mr-3 border border-red-500/30">
                <Gift class="w-4 h-4 text-red-400" />
              </div>
              <h3 class="text-sm font-bold uppercase tracking-widest">红包记录</h3>
            </div>
            <button 
              @click="showRedPacketRecords = false"
              class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white"
            >
              <LogOut class="w-4 h-4 rotate-90" />
            </button>
          </div>
          
          <div class="flex-1 overflow-y-auto no-scrollbar p-4">
            <div class="space-y-2">
              <!-- 加载状态 -->
              <div v-if="isLoadingRedPacketRecords" class="py-20 text-center">
                <div class="w-12 h-12 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4 animate-spin">
                  <Gift class="w-5 h-5 text-zinc-700" />
                </div>
                <p class="text-xs text-zinc-600 uppercase tracking-widest">加载中...</p>
              </div>
              <!-- 空状态 -->
              <div v-else-if="redPacketRecords.length === 0" class="py-20 text-center">
                <p class="text-xs text-zinc-600 uppercase tracking-widest">暂无红包记录</p>
              </div>
              <!-- 红包记录列表 -->
              <div 
                v-else
                v-for="record in redPacketRecords" 
                :key="record.id" 
                class="px-6 py-4 rounded-2xl glass-card flex justify-between items-center min-h-[60px]"
              >
                <div class="flex flex-col">
                  <span class="text-[11px] text-zinc-400 font-mono tracking-tighter">{{ record.time }}</span>
                  <span class="text-[9px] text-zinc-600 uppercase tracking-widest mt-0.5">幸运红包</span>
                </div>
                <div class="flex items-center">
                  <span class="text-sm font-bold text-red-400 font-mono mr-2">+{{ Math.floor(record.amount) }}</span>
                  <Gift class="w-3 h-3 text-red-500/50" />
                </div>
              </div>
            </div>
          </div>
          
          <div class="p-8 border-t border-white/5 text-center">
            <p class="text-[10px] text-zinc-600 uppercase tracking-[0.2em]">共计 {{ redPacketRecords.length }} 条红包记录</p>
          </div>
        </div>
      </div>
    </transition>

    <!-- 排行榜弹窗 -->
    <!-- Leaderboard Modal -->
    <transition name="modal">
      <div v-if="showLeaderboard" class="fixed inset-0 z-[100] flex items-end justify-center sm:items-center p-0 sm:p-6">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" @click="showLeaderboard = false" />
        <div class="relative w-full max-w-md bg-[#0f0f11] border-t sm:border border-white/10 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col max-h-[90vh]">
          
          <!-- 头部标题 -->
          <div class="px-8 py-6 border-b border-white/5 sticky top-0 bg-[#0f0f11] z-10">
            <div class="flex justify-between items-center mb-4">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 bg-amber-500/10 rounded-full flex items-center justify-center">
                  <Trophy class="w-4 h-4 text-amber-500" />
                </div>
                <h3 class="text-sm font-bold uppercase tracking-widest">收益排行榜</h3>
              </div>
              <button 
                @click="showLeaderboard = false" 
                class="flex items-center gap-2 px-3 py-2 rounded-full bg-white/5 text-zinc-500 hover:text-white"
                title="关闭"
              >
                <LogOut class="w-4 h-4 rotate-90" />
                <span class="text-[9px] text-zinc-500 whitespace-nowrap">关闭</span>
              </button>
            </div>
            <!-- 今日/昨日切换按钮 -->
            <div class="flex bg-white/5 rounded-full p-1">
              <button 
                @click="rankingTab = 'today'" 
                class="flex-1 py-2 px-4 rounded-full text-xs font-bold uppercase tracking-wider transition-all duration-300"
                :class="rankingTab === 'today' ? 'bg-amber-500 text-black shadow-lg shadow-amber-500/30' : 'text-zinc-500 hover:text-white'"
              >
                今日
              </button>
              <button 
                @click="rankingTab = 'yesterday'" 
                class="flex-1 py-2 px-4 rounded-full text-xs font-bold uppercase tracking-wider transition-all duration-300"
                :class="rankingTab === 'yesterday' ? 'bg-amber-500 text-black shadow-lg shadow-amber-500/30' : 'text-zinc-500 hover:text-white'"
              >
                昨日
              </button>
            </div>
          </div>

          <!-- 滚动列表内容 -->
          <div class="flex-1 overflow-y-auto no-scrollbar p-6 space-y-8">
            
            <!-- 加载状态 -->
            <div v-if="isLoadingRanking" class="py-12 text-center">
              <div class="w-8 h-8 border-2 border-amber-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
              <p class="text-zinc-500 text-sm">加载排行榜数据中...</p>
            </div>
            
            <template v-else>
              <!-- 本月单日最高收益记录 (置顶卡片) -->
              <div v-if="monthlyTopUser" class="relative group">
                <div class="absolute -inset-0.5 bg-gradient-to-r from-amber-500 to-orange-600 rounded-3xl blur opacity-30 group-hover:opacity-50 transition duration-1000"></div>
                <div class="relative bg-zinc-900 rounded-3xl border border-amber-500/30 overflow-hidden shadow-[0_0_30px_rgba(245,158,11,0.1)]">
                  <div class="bg-amber-500/10 px-6 py-3 flex flex-col items-center justify-center gap-1 border-b border-amber-500/20 relative z-10">
                    <div class="flex items-center justify-center gap-3">
                      <Crown class="w-4 h-4 text-amber-500 opacity-50" />
                      <span class="text-xs text-amber-500 font-black uppercase tracking-[0.3em]">本月单日最高收益记录</span>
                      <Crown class="w-4 h-4 text-amber-500 opacity-50" />
                    </div>
                    <span v-if="monthlyTopUser.date" class="text-[11px] text-amber-500/80 font-black tracking-[0.2em]">{{ monthlyTopUser.date }}</span>
                  </div>
                  <div class="p-6 flex items-center justify-between relative z-10">
                    <div class="flex items-center gap-4">
                      <div class="w-12 h-12 bg-gradient-to-br from-amber-400 to-orange-600 rounded-2xl flex items-center justify-center shadow-lg shadow-amber-500/20">
                        <Medal class="w-7 h-7 text-white" />
                      </div>
                      <div class="flex flex-col">
                        <span class="text-xs text-zinc-500 uppercase font-bold tracking-widest">员工ID</span>
                        <span class="text-base font-black text-white">{{ monthlyTopUser.empId }}</span>
                      </div>
                    </div>
                    <div class="flex flex-col items-end">
                      <span class="text-[9px] text-amber-500 uppercase font-black tracking-widest">单日收益</span>
                      <span class="text-2xl font-black text-amber-400 tabular-nums drop-shadow-[0_0_15px_rgba(245,158,11,0.5)]">¥ {{ (monthlyTopUser.totalCoins / 1000).toFixed(2) }}</span>
                    </div>
                  </div>
                  <div class="grid grid-cols-2 border-t border-white/5 bg-white/[0.02]">
                    <div class="px-6 py-3 border-r border-white/5 flex items-center justify-between">
                      <span class="text-xs text-white uppercase font-bold">总条数</span>
                      <span class="text-xs font-bold text-emerald-400">{{ monthlyTopUser.adCount }} 条</span>
                    </div>
                    <div class="px-6 py-3 flex items-center justify-between">
                      <span class="text-xs text-white uppercase font-bold">平均金币</span>
                      <span class="text-xs font-bold text-emerald-400">{{ Number(monthlyTopUser.avgCoins).toFixed(2) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 收益排行 TOP 10 列表 -->
              <div class="space-y-6 pt-2">
                <!-- 列表副标题 -->
                <div class="flex items-center gap-4 px-4">
                  <div class="h-px flex-1 bg-gradient-to-r from-transparent via-white/20 to-transparent"></div>
                  <h4 class="text-[11px] font-black text-white uppercase tracking-[0.4em] whitespace-nowrap drop-shadow-sm">{{ rankingTab === 'today' ? '今日' : '昨日' }}收益排行 TOP 10</h4>
                  <div class="h-px flex-1 bg-gradient-to-r from-transparent via-white/20 to-transparent"></div>
                </div>

                <!-- TOP 10 列表项循环 -->
                <div v-if="(rankingTab === 'today' ? dailyLeaderboard : yesterdayLeaderboard).length === 0" class="py-12 text-center">
                  <TrendingUp class="w-12 h-12 text-zinc-700 mx-auto mb-4" />
                  <p class="text-zinc-500 text-sm">暂无排行数据</p>
                  <p class="text-zinc-600 text-xs mt-2">{{ rankingTab === 'today' ? '今日数据将在5分钟后更新' : yesterdayDate + ' 暂无数据' }}</p>
                </div>
                <div v-else class="space-y-4">
                  <div v-for="user in (rankingTab === 'today' ? dailyLeaderboard : yesterdayLeaderboard)" :key="user.rank" 
                    class="relative px-6 py-4 rounded-2xl border transition-all hover:scale-[1.01]"
                    :class="[ 
                      user.rank === 1 ? 'bg-gradient-to-r from-amber-500/10 to-amber-600/5 border-amber-500/40 shadow-[0_0_20px_rgba(245,158,11,0.1)]' : 
                      user.rank === 2 ? 'bg-gradient-to-r from-zinc-400/10 to-zinc-500/5 border-zinc-400/40 shadow-[0_0_20px_rgba(161,161,170,0.1)]' : 
                      user.rank === 3 ? 'bg-gradient-to-r from-orange-600/10 to-orange-700/5 border-orange-600/40 shadow-[0_0_20px_rgba(234,88,12,0.1)]' : 
                      'bg-white/[0.02] border-white/[0.05]' 
                    ]"
                  >
                    <div class="flex items-center justify-between w-full">
                      <div class="flex items-center gap-4 flex-1">
                        <!-- 名次徽章 (前三名特殊样式) -->
                        <div class="w-8 h-8 rounded-full flex items-center justify-center font-black text-sm relative" 
                          :class="[ 
                            user.rank === 1 ? 'bg-gradient-to-br from-amber-400 to-amber-600 text-black shadow-lg shadow-amber-500/40 ring-2 ring-amber-500/20' : 
                            user.rank === 2 ? 'bg-gradient-to-br from-zinc-200 to-zinc-400 text-black shadow-lg shadow-zinc-400/40 ring-2 ring-zinc-400/20' : 
                            user.rank === 3 ? 'bg-gradient-to-br from-orange-400 to-orange-600 text-black shadow-lg shadow-orange-500/40 ring-2 ring-orange-500/20' : 
                            'bg-zinc-800 text-zinc-400' 
                          ]"
                        >
                          <!-- 冠军特有小皇冠图标 -->
                          <Crown v-if="user.rank === 1" class="absolute -top-2 -right-1 w-3 h-3 text-amber-500 rotate-[15deg] drop-shadow-[0_0_5px_rgba(245,158,11,1)]" />
                          {{ user.rank }}
                        </div>

                        <!-- 员工ID & 指标数据 -->
                        <div class="flex items-center gap-3 flex-1">
                          <div class="flex flex-col min-w-[60px]">
                            <span class="text-[10px] text-zinc-500 uppercase font-bold tracking-widest">员工ID</span>
                            <span class="text-sm font-black text-white">{{ user.empId }}</span>
                          </div>
                          
                          <!-- 分隔线 & 指标 -->
                          <div class="flex flex-col border-l border-white/5 pl-3 gap-0.5 flex-1">
                            <div class="flex items-baseline gap-1 whitespace-nowrap">
                              <span class="text-[10px] font-bold text-emerald-400/80">{{ user.adCount }}</span>
                              <span class="text-[8px] text-white uppercase font-medium">条数</span>
                            </div>
                            <div class="flex items-baseline gap-1 whitespace-nowrap">
                              <span 
                                class="text-[10px] font-bold tabular-nums" 
                                :class="[
                                  Number(user.avgCoins) > 100 ? 'text-emerald-400/80' :
                                  Number(user.avgCoins) >= 50 ? 'text-amber-400/80' :
                                  'text-red-400/80'
                                ]"
                              >{{ Number(user.avgCoins).toFixed(2) }}</span>
                              <span class="text-[8px] text-white uppercase font-medium">平均</span>
                            </div>
                          </div>
                        </div>
                      </div>

                      <!-- 收益显示区 (绿色字体 + 垂直分隔线) -->
                      <div class="flex items-baseline gap-1 border-l border-white/5 pl-4 ml-4 w-[100px] justify-end">
                        <span class="text-base font-black tabular-nums text-emerald-400">
                          ¥ {{ (user.totalCoins / 1000).toFixed(2) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
          
          <!-- 底部脚注 -->
          <div class="p-8 border-t border-white/5 text-center">
            <p class="text-[10px] text-zinc-600 uppercase tracking-[0.2em]">{{ rankingTab === 'today' ? '排行榜每5分钟自动更新一次' : '昨日数据已缓存，每日00:00更新' }}</p>
          </div>
        </div>
      </div>
    </transition>

    <!-- 周末活动弹窗 -->
    <transition name="modal">
      <div v-if="showWeekendEvent" class="fixed inset-0 z-[100] flex items-end justify-center sm:items-center p-0 sm:p-6">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" @click="showWeekendEvent = false" />
        <div class="relative w-full max-w-md bg-gradient-to-br from-purple-900/90 via-indigo-900/90 to-purple-900/90 border-t sm:border border-purple-500/30 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col z-[101] shadow-2xl">
          
          <!-- 头部装饰 -->
          <div class="relative overflow-hidden">
            <!-- 背景光效 -->
            <div class="absolute -top-20 left-1/2 -translate-x-1/2 w-64 h-64 bg-purple-500/30 rounded-full blur-3xl" />
            <div class="absolute -bottom-10 left-0 w-48 h-48 bg-pink-500/20 rounded-full blur-3xl" />
            <div class="absolute -bottom-10 right-0 w-48 h-48 bg-blue-500/20 rounded-full blur-3xl" />
            
            <!-- 关闭按钮 -->
            <button 
              @click="showWeekendEvent = false"
              class="absolute top-4 right-4 w-8 h-8 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center text-zinc-400 hover:text-white hover:bg-white/20 transition-all z-10"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
            
            <!-- 内容区域 -->
            <div class="relative px-8 py-10 text-center">
              <!-- 闪烁星星 -->
              <div class="relative mb-6">
                <Sparkles class="w-16 h-16 text-purple-400 mx-auto animate-pulse drop-shadow-[0_0_20px_rgba(192,132,252,0.6)]" />
                <!-- 装饰性小火花 -->
                <Sparkles class="absolute -top-2 left-1/2 -translate-x-[70px] w-5 h-5 text-amber-400 animate-pulse" />
                <Sparkles class="absolute -top-1 right-1/2 translate-x-[70px] w-4 h-4 text-pink-400 animate-pulse delay-300" />
              </div>
              
              <!-- 主标题 -->
              <h2 class="text-2xl font-black text-white mb-4">
                <span class="text-purple-400">·</span>周末活动<span class="text-purple-400">·</span>
              </h2>
              
              <!-- 副标题 -->
              <p class="text-lg font-bold text-transparent bg-clip-text bg-gradient-to-r from-purple-300 via-pink-300 to-amber-300 mb-3">
                更多福利，即将来袭！
              </p>
              
              <!-- 时间提示 -->
              <p class="text-sm text-purple-200/80">
                预计6月～7月正式开启
              </p>
            </div>
          </div>
          
          <!-- 装饰线 -->
          <div class="px-8">
            <div class="h-px bg-gradient-to-r from-transparent via-purple-500/50 to-transparent" />
          </div>
          
          <!-- 底部按钮 -->
          <div class="px-8 py-8">
            <button 
              @click="showWeekendEvent = false"
              class="w-full py-4 rounded-2xl bg-gradient-to-r from-purple-600 via-pink-600 to-amber-600 text-white font-bold text-sm uppercase tracking-widest hover:from-purple-500 hover:via-pink-500 hover:to-amber-500 transition-all duration-300 active:scale-95 shadow-lg shadow-purple-500/40 flex items-center justify-center gap-2"
            >
              <Sparkles class="w-4 h-4" />
              非常期待
            </button>
          </div>
        </div>
      </div>
    </transition>


    <footer class="text-center mt-8 px-6 pb-8">
      <div class="inline-flex items-center px-4 py-2 rounded-full bg-white/2 border border-white/5">
        <div class="w-1 h-1 bg-zinc-700 rounded-full mr-2" />
        <p class="text-zinc-600 text-[9px] uppercase tracking-[0.4em]">
          安全加密连接已建立
        </p>
        <div class="w-1 h-1 bg-zinc-700 rounded-full ml-2" />
      </div>
    </footer>

    <!-- 提现弹窗 -->
    <transition name="modal">
      <div v-if="showWithdrawModal" class="fixed inset-0 z-[9999] flex items-end justify-center sm:items-center p-0 sm:p-6 pointer-events-auto">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998] pointer-events-auto" @click="closeWithdrawModal" />
        <div class="relative w-full max-w-md bg-[#020205] border-t sm:border border-white/10 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col max-h-[85vh] z-[9999] shadow-2xl">
          <!-- 弹窗头部 -->
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center sticky top-0 bg-[#020205] z-10">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-blue-500/20 rounded-full flex items-center justify-center border border-blue-500/30 mr-3">
                <Wallet class="w-4 h-4 text-blue-400" />
              </div>
              <h3 class="text-sm font-bold uppercase tracking-widest">申请提现</h3>
            </div>
            <button 
              @click="closeWithdrawModal"
              class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white"
            >
              <LogOut class="w-4 h-4 rotate-90" />
            </button>
          </div>
          
          <!-- 弹窗内容 -->
          <div class="flex-1 overflow-y-auto no-scrollbar p-6">
            <!-- 成功状态 -->
            <div v-if="withdrawSuccess" class="text-center py-12">
              <div class="w-16 h-16 bg-emerald-500/20 rounded-full flex items-center justify-center mx-auto mb-4 border border-emerald-500/30">
                <svg class="w-8 h-8 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h4 class="text-lg font-bold text-white mb-2">提现申请已提交</h4>
              <p class="text-sm text-zinc-500">财务将在3个工作日内处理您的提现申请</p>
            </div>
            
            <!-- 提现表单 -->
            <div v-else class="space-y-6">
              <!-- 提现金额 -->
              <div class="glass-card rounded-2xl p-4">
                <p class="text-zinc-500 text-[10px] uppercase tracking-wider mb-2">可提现金额</p>
                <div class="flex items-baseline">
                  <span class="text-3xl font-bold text-blue-400">¥{{ withdrawAmount }}</span>
                  <span class="text-sm text-zinc-500 ml-1">元</span>
                </div>
                <p class="text-[10px] text-zinc-600 mt-2">
                  {{ Math.floor(lastMonthGold).toLocaleString() }} 金币 ÷ 1000 = ¥{{ withdrawAmount }}
                </p>
              </div>
              
              <!-- 支付宝账号 -->
              <div class="space-y-2">
                <label class="text-[10px] text-amber-400 uppercase tracking-wider">支付宝账号</label>
                <input 
                  v-model="alipayAccount"
                  type="text"
                  placeholder="请输入支付宝账号"
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500/50 transition-all"
                />
              </div>
              
              <!-- 支付宝姓名 -->
              <div class="space-y-2">
                <label class="text-[10px] text-amber-400 uppercase tracking-wider">支付宝姓名</label>
                <input 
                  v-model="alipayName"
                  type="text"
                  placeholder="请输入支付宝实名姓名"
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500/50 transition-all"
                />
              </div>
              
              <!-- 提示信息 -->
              <p class="text-[10px] text-amber-400/80 uppercase tracking-wider mt-4">
                提现申请将在3个工作日内处理，请确保支付宝信息准确无误
              </p>
            </div>
          </div>
          
          <!-- 弹窗底部 -->
          <div class="px-6 py-6 border-t border-white/5 sticky bottom-0 bg-[#020205]">
            <button 
              @click="submitWithdraw"
              :disabled="!alipayAccount || !alipayName || isSubmittingWithdraw || withdrawAmount <= 0"
              class="w-full font-bold py-4 rounded-xl transition-all text-white"
              :class="[!alipayAccount || !alipayName || isSubmittingWithdraw || withdrawAmount <= 0 ? 'bg-zinc-800 cursor-not-allowed' : 'bg-gradient-to-r from-blue-500 to-blue-600']"
            >
              {{ isSubmittingWithdraw ? '提交中...' : '提交提现申请' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 提现记录弹窗 -->
    <transition name="modal">
      <div v-if="showWithdrawRecordsModal" class="fixed inset-0 z-[9999] flex items-end justify-center sm:items-center p-0 sm:p-6 pointer-events-auto">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998] pointer-events-auto" @click="closeWithdrawRecordsModal" />
        <div class="relative w-full max-w-md bg-[#020205] border-t sm:border border-white/10 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col max-h-[85vh] z-[9999] shadow-2xl">
          <!-- 弹窗头部 -->
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center sticky top-0 bg-[#020205] z-10">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-blue-500/20 rounded-full flex items-center justify-center border border-blue-500/30 mr-3">
                <CreditCard class="w-4 h-4 text-blue-400" />
              </div>
              <h3 class="text-sm font-bold uppercase tracking-widest">提现记录</h3>
            </div>
            <button 
              @click="closeWithdrawRecordsModal"
              class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white"
            >
              <LogOut class="w-4 h-4 rotate-90" />
            </button>
          </div>
          
          <!-- 弹窗内容 -->
          <div class="flex-1 overflow-y-auto no-scrollbar p-6">
            <!-- 加载状态 -->
            <div v-if="isLoadingWithdrawRecords" class="py-12 text-center">
              <div class="w-12 h-12 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4 animate-spin">
                <CreditCard class="w-5 h-5 text-zinc-700" />
              </div>
              <p class="text-xs text-zinc-600 uppercase tracking-widest">加载中...</p>
            </div>
            
            <!-- 空状态 -->
            <div v-else-if="withdrawRecords.length === 0" class="py-12 text-center">
              <div class="w-12 h-12 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4">
                <CreditCard class="w-5 h-5 text-zinc-700" />
              </div>
              <p class="text-xs text-zinc-600 uppercase tracking-widest">暂无提现记录</p>
            </div>
            
            <!-- 提现记录列表 -->
            <div v-else class="space-y-3">
              <div 
                v-for="record in withdrawRecords" 
                :key="record._id" 
                class="glass-card rounded-2xl p-5"
              >
                <div class="flex justify-between items-start">
                  <div class="flex-1 space-y-2">
                    <div class="flex items-center">
                      <span class="text-xs text-blue-400 mr-2">支付宝账号：</span>
                      <span class="text-xs text-white">{{ record.alipayAccount }}</span>
                    </div>
                    <div class="flex items-center">
                      <span class="text-xs text-blue-400 mr-2">支付宝姓名：</span>
                      <span class="text-xs text-white">{{ record.alipayName }}</span>
                    </div>
                    <div class="flex items-center">
                      <span class="text-xs text-blue-400 mr-2">提现日期：</span>
                      <span class="text-xs text-white">{{ formatDate(record.createTime) }}</span>
                    </div>
                  </div>
                  <div class="text-right ml-4 space-y-2">
                    <div class="flex items-baseline justify-end">
                      <span class="text-2xl font-bold" :class="getStatusStyle(record.status).amountClass">{{ record.amount }}</span>
                      <span class="text-xs" :class="getStatusStyle(record.status).amountClass">元</span>
                    </div>
                    <div class="flex justify-end">
                      <div 
                        class="px-3 py-1 rounded-full text-[8px] font-bold tracking-wider border"
                        :class="getStatusStyle(record.status).class"
                      >
                        {{ getStatusStyle(record.status).text }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 底部导航栏 -->
    <div class="fixed bottom-0 left-0 right-0 bg-black/40 backdrop-blur-xl border-t border-white/5 py-3 px-6 z-50" style="padding-bottom: calc(3px + var(--safe-area-inset-bottom)); height: calc(60px + var(--safe-area-inset-bottom));">
      <div class="flex items-center justify-around">
        <router-link 
          to="/" 
          class="flex flex-col items-center transition-all duration-300"
          :class="$route.path === '/' ? 'text-emerald-400 scale-105' : 'text-zinc-400 hover:text-zinc-300'"
        >
          <TrendingUp class="w-6 h-6 mb-1" />
          <span class="text-xs font-medium">电子手工</span>
        </router-link>
        <router-link 
          to="/lottery" 
          class="flex flex-col items-center transition-all duration-300 relative"
          :class="$route.path === '/lottery' ? 'text-emerald-400 scale-105' : 'text-zinc-400 hover:text-zinc-300'"
        >
          <Ticket class="w-6 h-6 mb-1" />
          <span class="text-xs font-medium">幸运彩票</span>
          <div v-if="lotteryTickets.length > 0" class="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold rounded-full w-5 h-5 flex items-center justify-center">
            {{ lotteryTickets.length }}
          </div>
        </router-link>
        <router-link 
          to="/welfare-lottery" 
          class="flex flex-col items-center transition-all duration-300 relative"
          :class="$route.path === '/welfare-lottery' ? 'text-emerald-400 scale-105' : 'text-zinc-400 hover:text-zinc-300'"
        >
          <Gift class="w-6 h-6 mb-1" />
          <span class="text-xs font-medium">福利抽奖</span>
          <span 
            v-if="welfareLotteryChances > 0" 
            class="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold rounded-full w-5 h-5 flex items-center justify-center"
          >
            {{ welfareLotteryChances > 99 ? '99+' : welfareLotteryChances }}
          </span>
        </router-link>
        <router-link 
          to="/phone-verification" 
          class="flex flex-col items-center transition-all duration-300"
          :class="$route.path === '/phone-verification' ? 'text-emerald-400 scale-105' : 'text-zinc-400 hover:text-zinc-300'"
        >
          <Smartphone class="w-6 h-6 mb-1" />
          <span class="text-xs font-medium">手机核销</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 自定义样式 */
.no-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

/* 弹窗动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  transform: translateY(30px);
}

/* 奖励弹窗动画 */
.reward-popup-enter-active,
.reward-popup-leave-active {
  transition: all 0.3s ease;
}

.reward-popup-enter-from,
.reward-popup-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

/* 红包弹窗动画 */
.red-packet-popup-enter-active,
.red-packet-popup-leave-active {
  transition: all 0.5s ease;
}

.red-packet-popup-enter-from {
  opacity: 0;
  transform: scale(0.5);
}

.red-packet-popup-leave-to {
  opacity: 0;
  transform: scale(1.2);
}

/* 红包跳动动画 */
@keyframes redPacketJump {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.animate-red-packet-jump {
  animation: redPacketJump 2s ease-in-out infinite;
}

/* 玻璃卡片 */
.glass-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 背景光晕动画 */
@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

/* 自定义滚动条（隐藏） */
::-webkit-scrollbar {
  display: none;
}

/* 确保弹窗覆盖所有内容 */
.modal {
  z-index: 9999 !important;
}

/* 背景遮罩 */
.modal-backdrop {
  z-index: 9998 !important;
  background-color: rgba(0, 0, 0, 0.8) !important;
  backdrop-filter: blur(10px) !important;
}
</style>
