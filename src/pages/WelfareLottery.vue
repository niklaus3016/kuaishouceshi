<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { Gift, Trophy, LogOut, History, Wallet, CreditCard, Sparkles, Zap, ChevronRight, Smartphone, TrendingUp, Ticket, RefreshCw, Handshake } from 'lucide-vue-next';
import { recordAdView, getWelfareLotteryInfo, claimWelfareLottery, getWelfareLotteryRecords, getWelfareWalletBalance, withdrawWelfareFunds, getWelfareLotteryPrizes, bindAlipay, getAlipayInfo, getWelfareWithdrawRecords, getWelfareWalletStatus, getCurrentLotteryTickets } from '../api/apiService';
import { AudioPlugin } from '../plugins/AudioPlugin';
import gold1gImage from '../../gold-1g.png';
import phoneModelImage from '../../phone-model.png';

const empId = ref(localStorage.getItem('empId') || '');
const userId = ref(localStorage.getItem('userId') || '');

// 状态管理
const isLoading = ref(false);
const error = ref('');
const welfareBalance = ref(0);
const lotteryItems = ref([
  { id: '2', name: '1.68元', probability: 20, value: 1.68, type: 'cash' },
  { id: '3', name: '88.8元', probability: 5, value: 88.8, type: 'cash' },
  { id: '4', name: '6.88元', probability: 15, value: 6.88, type: 'cash' },
  { id: '5', name: '千元手机', probability: 1, value: 1000, type: 'phone' },
  { id: '6', name: '16.8元', probability: 12, value: 16.8, type: 'cash' },
  { id: '9', name: '36.8元', probability: 10, value: 36.8, type: 'cash' },
  { id: '8', name: '再接再厉', probability: 37, value: 0, type: 'encourage' },
  { id: '1', name: '1克黄金', probability: 2, value: 500, type: 'gold' }
]);
const isSpinning = ref(false);
const spinResult = ref(null);
const showResultModal = ref(false);
const welfareRecords = ref([]);
const isLoadingRecords = ref(false);
const showRecordsModal = ref(false);
const showWithdrawModal = ref(false);
const showAmountModal = ref(false); // 金额选择弹窗
const withdrawAmount = ref(0);
const alipayAccount = ref('');
const alipayName = ref('');
const withdrawSuccess = ref(false);
const isSubmittingWithdraw = ref(false);
const rotation = ref(0);
const isTransitionEnabled = ref(true); // 控制transition是否启用
const showBindModal = ref(false);
const bindAlipayName = ref('');
const bindAlipayAccount = ref('');
const isSubmittingBind = ref(false);

// 提现次数相关状态
const dailyWithdrawCount = ref(0); // 今日已提现次数
const maxDailyWithdraws = 1; // 每日最大提现次数
const selectedAmount = ref(0); // 已选择的提现金额

// 进度条相关状态
const todayAdCount = ref(0); // 今日广告观看次数
const walletChances = ref(0); // 当前剩余抽奖机会
const nextThreshold = ref<{ adCount: number; giveChances: number } | null>(null); // 下次奖励配置
const remainingToNext = ref(0); // 距离下次奖励还差的广告次数
const thresholds = ref<{ adCount: number; giveChances: number }[]>([
  { adCount: 1000, giveChances: 1 },
  { adCount: 2000, giveChances: 2 },
  { adCount: 3000, giveChances: 3 }
]); // 阈值配置列表（默认值）
const withdrawRecords = ref([]); // 提现记录
const showWithdrawRecordsModal = ref(false); // 提现记录弹窗
const showRulesModal = ref(false); // 抽奖规则弹窗
const lotteryTicketsCount = ref(0); // 幸运彩票数量

// 转盘音效
let spinAudioPlaying = false;
let spinTimeoutId: ReturnType<typeof setTimeout> | null = null;
let resultTimeoutId: ReturnType<typeof setTimeout> | null = null;

// 播放转盘音效
const playSpinSound = async () => {
  try {
    console.log('🎵 开始播放转盘音效');
    await AudioPlugin.play({ 
      filePath: 'gxcjyy', 
      volume: 0.8,
      loop: true  // 循环播放，直到转盘停止
    });
    spinAudioPlaying = true;
    console.log('🎵 转盘音效播放成功');
  } catch (err) {
    console.error('🎵 音效播放失败:', err);
  }
};

// 停止转盘音效
const stopSpinSound = async () => {
  try {
    if (spinAudioPlaying) {
      await AudioPlugin.stop();
      spinAudioPlaying = false;
    }
  } catch (err) {
    console.log('音效停止失败:', err);
  }
};

// 计算属性
const canSpin = computed(() => walletChances.value > 0 && !isSpinning.value);

// 计算剩余提现次数
const remainingWithdraws = computed(() => maxDailyWithdraws - dailyWithdrawCount.value);

// 加载福利抽奖信息
const loadWelfareInfo = async () => {
  console.log('loadWelfareInfo - empId:', empId.value);
  if (!empId.value) {
    console.log('loadWelfareInfo - empId为空');
    return;
  }
  
  isLoading.value = true;
  error.value = '';
  
  try {
    const [infoResponse, prizesResponse, alipayResponse] = await Promise.all([
      getWelfareLotteryInfo(empId.value),
      getWelfareLotteryPrizes(),
      getAlipayInfo(empId.value)
    ]);
    
    console.log('loadWelfareInfo - getWelfareLotteryInfo响应:', infoResponse);
    if (infoResponse.success && infoResponse.data) {
      welfareBalance.value = Number(infoResponse.data.balance) || 0;
      console.log('loadWelfareInfo - 余额:', welfareBalance.value);
    } else {
      error.value = infoResponse.message || '获取福利抽奖信息失败';
      console.log('loadWelfareInfo - 获取失败:', error.value);
    }
    
    console.log('loadWelfareInfo - getWelfareLotteryPrizes响应:', prizesResponse);
    if (prizesResponse.success && prizesResponse.data) {
      lotteryItems.value = prizesResponse.data.prizes;
      console.log('loadWelfareInfo - 奖品列表:', lotteryItems.value);
    }
    
    console.log('loadWelfareInfo - getAlipayInfo响应:', alipayResponse);
    if (alipayResponse.success && alipayResponse.data) {
      bindAlipayName.value = alipayResponse.data.alipayName;
      bindAlipayAccount.value = alipayResponse.data.alipayAccount;
      console.log('loadWelfareInfo - 支付宝信息:', { bindAlipayName: bindAlipayName.value, bindAlipayAccount: bindAlipayAccount.value });
    }
  } catch (err) {
    console.error('获取福利抽奖信息失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 加载抽奖进度状态
const loadWalletStatus = async (retryCount = 0) => {
  try {
    const token = localStorage.getItem('token');
    console.log(`[抽奖状态] 加载中 - empId: ${empId.value}, token存在: ${!!token}, 重试次数: ${retryCount}`);
    
    const response = await getWelfareWalletStatus();
    console.log(`[抽奖状态] 接口返回 - success: ${response.success}, data:`, response.data);
    
    if (response.success && response.data) {
      todayAdCount.value = response.data.todayAdCount;
      walletChances.value = response.data.chances;
      nextThreshold.value = response.data.nextThreshold;
      remainingToNext.value = response.data.remaining;
      // 加载阈值配置，若接口未返回则使用默认值
      thresholds.value = response.data.thresholds || [
        { adCount: 1000, giveChances: 1 },
        { adCount: 2000, giveChances: 2 },
        { adCount: 3000, giveChances: 3 }
      ];
      
      // 记录异常情况：广告次数为0但nextThreshold也为null
      if (response.data.todayAdCount === 0 && response.data.nextThreshold === null) {
        console.warn(`[抽奖状态异常] 用户 ${empId.value}: todayAdCount=0 且 nextThreshold=null`);
      }
    } else {
      console.error(`[抽奖状态] 接口返回失败 - success: ${response.success}, message: ${response.message}`);
      // 尝试重试一次
      if (retryCount < 1) {
        console.log('[抽奖状态] 尝试重试...');
        await new Promise(resolve => setTimeout(resolve, 1000));
        await loadWalletStatus(retryCount + 1);
      }
    }
  } catch (err) {
    console.error('[抽奖状态] 获取抽奖进度状态失败:', err);
    // 使用默认阈值配置
    thresholds.value = [
      { adCount: 1000, giveChances: 1 },
      { adCount: 2000, giveChances: 2 },
      { adCount: 3000, giveChances: 3 }
    ];
  }
};

// 加载福利钱包余额
const loadWelfareBalance = async () => {
  console.log('loadWelfareBalance - empId:', empId.value);
  if (!empId.value) {
    console.log('loadWelfareBalance - empId为空');
    return;
  }
  
  try {
    console.log('loadWelfareBalance - 开始调用getWelfareWalletBalance');
    const response = await getWelfareWalletBalance(empId.value);
    console.log('loadWelfareBalance - getWelfareWalletBalance响应:', response);
    if (response.success && response.data) {
      welfareBalance.value = Number(response.data.balance) || 0;
      console.log('loadWelfareBalance - 余额:', welfareBalance.value);
    }
  } catch (err) {
    console.error('获取福利钱包余额失败:', err);
  }
};

// 加载福利抽奖记录
const loadWelfareRecords = async () => {
  if (!empId.value) return;
  
  isLoadingRecords.value = true;
  
  try {
    const response = await getWelfareLotteryRecords(empId.value);
    if (response.success && response.data) {
      welfareRecords.value = response.data.records || [];
    }
  } catch (err) {
    console.error('获取福利抽奖记录失败:', err);
  } finally {
    isLoadingRecords.value = false;
  }
};

// 加载提现记录
const loadWithdrawRecords = async () => {
  if (!empId.value) return;
  
  isLoading.value = true;
  error.value = '';
  
  try {
    const response = await getWelfareWithdrawRecords(empId.value);
    if (response.success && response.data) {
      withdrawRecords.value = response.data.records;
      
      // 统计今日已提现次数（使用本地时区，和 formatTime 保持一致）
      const now = new Date();
      const todayYear = now.getFullYear();
      const todayMonth = now.getMonth();
      const todayDay = now.getDate();
      
      let hasNonFailedTodayRecord = false;
      for (const record of withdrawRecords.value) {
        if (record.time) {
          const recordDate = new Date(record.time);
          const recordYear = recordDate.getFullYear();
          const recordMonth = recordDate.getMonth();
          const recordDay = recordDate.getDate();
          
          // 只有 statusText 是 "失败" 的记录不计入，其他都算作已提现
          if (recordYear === todayYear && recordMonth === todayMonth && recordDay === todayDay && record.statusText !== '失败') {
            hasNonFailedTodayRecord = true;
            break;
          }
        }
      }
      
      // 今日有非失败状态的记录，算作已提现1次
      dailyWithdrawCount.value = hasNonFailedTodayRecord ? 1 : 0;
    }
  } catch (err) {
    console.error('获取提现记录失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 时间格式化函数
const formatTime = (timeString: string) => {
  const date = new Date(timeString);
  if (isNaN(date.getTime())) {
    return '--';
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
};

// 处理抽奖
const handleSpin = async () => {
  console.log('🎲 handleSpin 被调用');
  console.log('🎲 empId:', empId.value);
  console.log('🎲 walletChances:', walletChances.value);
  console.log('🎲 isSpinning:', isSpinning.value);
  console.log('🎲 canSpin:', canSpin.value);
  
  if (!empId.value) {
    console.error('❌ empId 为空，无法抽奖');
    return;
  }
  
  if (!canSpin.value) {
    console.error('❌ canSpin 为 false，无法抽奖');
    return;
  }
  
  isSpinning.value = true;
  error.value = '';
  
  // 播放转盘音效
  playSpinSound();
  
  try {
    console.log('🎲 开始调用抽奖接口:', { userId: userId.value, empId: empId.value });
    const response = await claimWelfareLottery(userId.value, empId.value);
    console.log('🎲 抽奖接口返回:', response);
    
    if (response.success && response.data && response.data.result) {
      const result = response.data.result;
      console.log('🎲 中奖结果:', result);
      spinResult.value = result;
      
      // 计算旋转角度 - 直接让中奖奖品转到指针位置
      const prizeIndex = lotteryItems.value.findIndex(item => item.id === result.id);
      console.log('🎲 中奖结果:', { resultId: result.id, resultName: result.name, prizeIndex });
      
      // 如果找不到匹配的奖品，使用默认奖品（再接再厉）
      const finalPrizeIndex = prizeIndex >= 0 ? prizeIndex : 6; // 6是再接再厉的索引
      
      // 核心逻辑：让中奖奖品转到指针位置（顶部0度）
      const sectorAngle = 360 / lotteryItems.value.length; // 每个奖品45度
      const targetRotation = 360 * 6 + (360 - finalPrizeIndex * sectorAngle); // 转6圈
      
      // 禁用transition，立即重置到0度
      isTransitionEnabled.value = false;
      rotation.value = 0;
      await nextTick();
      // 重新启用transition，设置目标角度
      isTransitionEnabled.value = true;
      rotation.value = targetRotation;
      
      // 减少抽奖机会
      walletChances.value -= 1;
      
      // 如果中奖，更新余额
      if (result.type !== 'encourage' && result.value > 0) {
        welfareBalance.value += result.value;
      }
      
      // 转盘停止后等待1秒再显示弹窗
      spinTimeoutId = setTimeout(() => {
        isSpinning.value = false;
        // 停止音效
        stopSpinSound();
        // 重新加载福利信息（包括最新的抽奖次数）和中奖记录
        loadWelfareInfo();
        loadWelfareRecords();
        
        // 等待1秒后再显示弹窗，让用户看到转盘停止位置
        resultTimeoutId = setTimeout(() => {
          showResultModal.value = true;
        }, 1000);
      }, 5800);
    } else {
      error.value = response.message || '抽奖失败';
      isSpinning.value = false;
      // 停止音效
      stopSpinSound();
    }
  } catch (err) {
    console.error('抽奖失败:', err);
    error.value = '网络错误，请稍后重试';
    isSpinning.value = false;
    // 停止音效
    stopSpinSound();
  }
};

// 关闭结果弹窗并重置转盘
const closeResultModal = () => {
  showResultModal.value = false;
  // 禁用transition，立即重置转盘到初始位置（瞬间切换）
  isTransitionEnabled.value = false;
  rotation.value = 0;
};

// 打开绑定弹窗
const openBindModal = () => {
  // 从本地存储获取已绑定的支付宝信息
  let savedAlipayName = '';
  let savedAlipayAccount = '';
  
  if (typeof localStorage !== 'undefined') {
    savedAlipayName = localStorage.getItem('alipayName') || '';
    savedAlipayAccount = localStorage.getItem('alipayAccount') || '';
  }
  
  bindAlipayName.value = savedAlipayName;
  bindAlipayAccount.value = savedAlipayAccount;
  
  showBindModal.value = true;
};

// 处理绑定
const handleBind = async () => {
  console.log('handleBind 被调用');
  console.log('empId:', empId.value);
  console.log('bindAlipayName:', bindAlipayName.value);
  console.log('bindAlipayAccount:', bindAlipayAccount.value);
  if (!empId.value || !bindAlipayName.value || !bindAlipayAccount.value) {
    console.log('参数不全，返回');
    return;
  }
  
  isSubmittingBind.value = true;
  error.value = '';
  
  try {
    const response = await bindAlipay(empId.value, bindAlipayName.value, bindAlipayAccount.value);
    if (response.success) {
      // 保存到本地存储
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('alipayName', bindAlipayName.value);
        localStorage.setItem('alipayAccount', bindAlipayAccount.value);
      }
      
      // 关闭绑定弹窗
      showBindModal.value = false;
      
      // 显示成功提示
      showRewardAnimation('绑定成功');
      
      // 重置表单
      bindAlipayName.value = '';
      bindAlipayAccount.value = '';
      
      isSubmittingBind.value = false;
    } else {
      error.value = response.message || '绑定失败';
      isSubmittingBind.value = false;
    }
  } catch (err) {
    console.error('绑定失败:', err);
    error.value = '网络错误，请稍后重试';
    isSubmittingBind.value = false;
  }
};

// 处理提现
const handleWithdraw = async () => {
  console.log('handleWithdraw - 开始提现');
  console.log('handleWithdraw - empId:', empId.value);
  console.log('handleWithdraw - withdrawAmount:', withdrawAmount.value);
  console.log('handleWithdraw - alipayAccount:', alipayAccount.value);
  console.log('handleWithdraw - alipayName:', alipayName.value);
  
  if (!empId.value || !alipayAccount.value || !alipayName.value) {
    console.log('handleWithdraw - 缺少必要参数');
    return;
  }
  
  isSubmittingWithdraw.value = true;
  error.value = '';
  
  try {
    console.log('handleWithdraw - 调用API');
    const response = await withdrawWelfareFunds(empId.value, withdrawAmount.value, alipayAccount.value, alipayName.value);
    console.log('handleWithdraw - API响应:', response);
    if (response.success) {
      withdrawSuccess.value = true;
      welfareBalance.value -= withdrawAmount.value;
      
      // 增加今日提现次数
      dailyWithdrawCount.value += 1;
      
      // 3秒后关闭弹窗
      setTimeout(() => {
        withdrawSuccess.value = false;
        showWithdrawModal.value = false;
        withdrawAmount.value = 0;
        alipayAccount.value = '';
        alipayName.value = '';
      }, 3000);
    } else {
      error.value = response.message || '提现失败';
    }
  } catch (err) {
    console.error('提现失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    isSubmittingWithdraw.value = false;
  }
};

// 显示奖励动画
const showRewardAnimation = (message: string) => {
  // 这里可以实现奖励动画逻辑
  console.log('获得奖励:', message);
};

// 打开提现弹窗
const openWithdrawModal = async () => {
  // 先加载最新的提现记录
  await loadWithdrawRecords();
  // 再打开金额选择弹窗
  showAmountModal.value = true;
};

// 选择提现金额
const selectWithdrawAmount = (amount: number) => {
  // 检查是否达到每日提现次数限制
  if (dailyWithdrawCount.value >= maxDailyWithdraws) {
    showRewardAnimation('今日提现次数已用完');
    return;
  }
  
  selectedAmount.value = amount;
};

// 确认提现金额
const confirmWithdrawAmount = () => {
  if (selectedAmount.value === 0) return;
  
  withdrawAmount.value = selectedAmount.value;
  
  // 清空之前的支付宝信息，让用户手动输入
  alipayName.value = '';
  alipayAccount.value = '';
  
  withdrawSuccess.value = false;
  showAmountModal.value = false;
  showWithdrawModal.value = true;
};

// 关闭提现弹窗
const closeWithdrawModal = () => {
  showWithdrawModal.value = false;
};

// 关闭记录弹窗
const closeRecordsModal = () => {
  showRecordsModal.value = false;
};

// 打开抽奖规则弹窗
const openRulesModal = () => {
  showRulesModal.value = true;
};

// 关闭抽奖规则弹窗
const closeRulesModal = () => {
  showRulesModal.value = false;
};

// 加载幸运彩票数量
const loadLotteryTicketsCount = async () => {
  if (!userId.value) return;
  
  try {
    const response = await getCurrentLotteryTickets(userId.value);
    if (response.success && response.data) {
      lotteryTicketsCount.value = response.data.tickets.length || 0;
    }
  } catch (error) {
    console.error('加载幸运彩票数量失败:', error);
  }
};

// 生命周期
onMounted(async () => {
  Promise.all([
    loadWelfareInfo(),
    loadWelfareBalance(),
    loadWalletStatus(),
    loadLotteryTicketsCount()
  ]).finally(() => {
    loadWelfareRecords();
    loadWithdrawRecords();
  });
});

// 组件卸载时清理
onUnmounted(() => {
  if (spinTimeoutId) {
    clearTimeout(spinTimeoutId);
    spinTimeoutId = null;
  }
  if (resultTimeoutId) {
    clearTimeout(resultTimeoutId);
    resultTimeoutId = null;
  }
  if (spinAudioPlaying) {
    stopSpinSound();
  }
});

// 获取奖品颜色
const getPrizeColor = (type: string) => {
  switch (type) {
    case 'cash':
      return '#10b981';
    case 'phone':
      return '#8b5cf6';
    case 'gold':
      return '#f59e0b';
    default:
      return '#6b7280';
  }
};

// 获取奖品图标
const getPrizeIcon = (type: string) => {
  switch (type) {
    case 'cash':
      return Wallet;
    case 'phone':
      return Smartphone;
    case 'gold':
      return Trophy;
    case 'encourage':
      return Handshake;
    default:
      return Gift;
  }
};

// 提取纯奖品类型名称（去掉金额）
const getPrizeName = (name: string) => {
  // 如果包含"再接再厉"，直接返回
  if (name.includes('再接再厉')) {
    return '再接再厉';
  }
  // 如果包含"黄金"，返回"黄金"
  if (name.includes('黄金')) {
    return '黄金';
  }
  // 如果包含"手机"，返回"手机"
  if (name.includes('手机')) {
    return '手机';
  }
  // 现金奖品，返回"现金"
  return '现金';
};
</script>

<template>
  <div class="min-h-screen bg-[#050505] text-white pb-32 relative overflow-x-hidden font-sans">
    <!-- 沉浸式背景光晕 -->
    <div class="fixed inset-0 overflow-hidden pointer-events-none z-0">
      <div class="absolute top-[-20%] left-[-10%] w-[80%] h-[80%] bg-amber-500/5 blur-[150px] rounded-full" />
      <div class="absolute bottom-[-20%] right-[-10%] w-[70%] h-[70%] bg-blue-600/5 blur-[150px] rounded-full" />
      <!-- 网格底纹 -->
      <div class="absolute inset-0 opacity-[0.03]" style="background-image: radial-gradient(#fff 1px, transparent 1px); background-size: 40px 40px;" />
    </div>

    <!-- 顶部导航栏 -->
    <header class="bg-black/40 backdrop-blur-xl border-b border-white/5 pt-8 pb-5 px-6 flex items-center justify-between sticky top-0 z-50">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20">
          <Gift class="text-white w-5 h-5" />
        </div>
        <div class="flex flex-col">
          <span class="font-bold text-sm tracking-widest uppercase bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">福利抽奖</span>
          <span class="text-[10px] text-zinc-400 font-bold tracking-wider">免费抽奖，好运常相伴！</span>
        </div>
      </div>
      <div class="px-4 py-1.5 rounded-full bg-white/5 border border-white/10 flex items-center gap-2">
        <span class="text-xs font-mono text-blue-400">{{ walletChances }}</span>
        <Sparkles class="w-3 h-3 text-purple-500" />
      </div>
    </header>

    <main class="max-w-md mx-auto px-6 mt-8 space-y-10 relative z-10">
      <!-- 钱包卡片 -->
      <section class="relative">
        <div class="absolute -inset-1 bg-gradient-to-r from-amber-500/20 via-blue-500/20 to-purple-500/20 blur-xl rounded-[2.5rem] opacity-50" />
        <div class="relative bg-gradient-to-br from-zinc-900 to-black border border-white/10 p-6 rounded-[2.5rem] overflow-hidden shadow-2xl">
          <!-- 碳纤维纹理 -->
          <div class="absolute inset-0 opacity-[0.05] pointer-events-none" style="background-image: url('https://www.transparenttextures.com/patterns/carbon-fibre.png');" />
          
          <div class="relative z-10 space-y-4">
            <div>
              <p class="text-sm font-black uppercase tracking-[0.4em] mb-2 bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">福利钱包</p>
              <div class="flex items-baseline gap-2">
                <span class="text-3xl font-light bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">¥</span>
                <span class="text-4xl font-black tracking-tighter bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent tabular-nums">
                  {{ welfareBalance.toFixed(2) }}
                </span>
              </div>
            </div>

            <div class="flex gap-3 items-center pt-2">
              <button 
                @click="openWithdrawModal"
                :disabled="welfareBalance <= 0"
                class="flex-1 py-2 rounded-lg bg-gradient-to-r from-blue-500 to-purple-600 text-white font-black text-xs uppercase tracking-[0.2em] hover:opacity-90 transition-all active:scale-95 disabled:opacity-30 disabled:active:scale-100 flex items-center justify-center gap-2 shadow-[0_5px_10px_rgba(59,130,246,0.2)]"
              >
                <CreditCard class="w-3 h-3" />
                立即提现
              </button>
              <div 
                @click="showWithdrawRecordsModal = true"
                class="flex-1 px-4 py-2 rounded-lg bg-gradient-to-br from-blue-500/20 to-purple-600/20 border border-blue-500/30 text-blue-400 text-xs font-bold uppercase tracking-[0.2em] hover:from-blue-500/30 hover:to-purple-600/30 transition-all flex items-center justify-center gap-2 cursor-pointer"
              >
                <History class="w-3 h-3" />
                提现记录
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 抽奖规则按钮 -->
      <div class="w-full max-w-[200px] mx-auto -mt-6">
        <button 
          @click="openRulesModal"
          class="w-full py-2.5 rounded-full bg-gradient-to-r from-amber-500/10 via-orange-500/10 to-amber-500/10 border border-amber-500/30 shadow-[0_4px_20px_rgba(245,158,11,0.15),inset_0_1px_0_rgba(255,255,255,0.05)] flex items-center justify-center gap-3 hover:from-amber-500/20 hover:via-orange-500/20 hover:to-amber-500/20 hover:border-amber-500/50 hover:shadow-[0_4px_25px_rgba(245,158,11,0.25)] active:scale-[0.98] transition-all duration-300"
        >
          <Sparkles class="w-4 h-4 text-amber-400" />
          <span class="text-xs font-bold tracking-wide text-amber-400/90">查看抽奖规则</span>
          <Sparkles class="w-4 h-4 text-amber-400" />
        </button>
      </div>

      <!-- 实物化大转盘 -->
      <section class="flex flex-col items-center justify-center py-6 space-y-12">
        <div class="relative group">
          <!-- 3D 金属外圈 -->
          <div class="absolute -inset-10 bg-gradient-to-b from-zinc-800 to-black rounded-full shadow-[0_30px_60px_rgba(0,0,0,0.8),inset_0_2px_5px_rgba(255,255,255,0.1)] border-b-4 border-black" />
          
          <!-- LED 跑马灯 -->
          <div class="absolute -inset-8 rounded-full pointer-events-none">
            <div v-for="i in 12" :key="i" 
              class="absolute w-1.5 h-1.5 rounded-full bg-amber-500/40 shadow-[0_0_10px_rgba(245,158,11,0.5)]"
              :style="{ 
                top: '50%', left: '50%', 
                transform: `rotate(${i * 30}deg) translate(0, -145px)`,
                animation: `pulse 2s infinite ${i * 0.1}s`
              }"
            />
          </div>

          <!-- 机械指针 -->
          <div class="absolute -top-12 left-1/2 -translate-x-1/2 z-40">
            <div class="relative">
              <div class="w-10 h-14 bg-gradient-to-b from-zinc-700 to-zinc-900 rounded-b-lg shadow-2xl border-x border-white/5" />
              <div class="absolute bottom-[-10px] left-1/2 -translate-x-1/2 w-4 h-8 bg-red-600 rounded-full shadow-[0_0_15px_rgba(220,38,38,0.5)] border border-red-400/30"
                :class="{ 'animate-wiggle': isSpinning }"
              />
              <!-- 铆钉 -->
              <div class="absolute top-2 left-1/2 -translate-x-1/2 w-3 h-3 bg-zinc-600 rounded-full border border-white/10 shadow-inner" />
            </div>
          </div>

          <!-- 转盘主体 -->
          <div class="relative w-80 h-80 rounded-full p-3 bg-zinc-900 shadow-inner overflow-hidden">
            <div 
              class="w-full h-full rounded-full border-[12px] border-zinc-800 relative overflow-hidden shadow-2xl"
              :class="{ 'transition-transform duration-[5800ms] cubic-bezier(0.15, 0, 0.15, 1)': isTransitionEnabled }"
              :style="{ transform: `rotate(${rotation}deg)` }"
            >
              <!-- 扇区 -->
              <div 
                v-for="(prize, index) in lotteryItems" 
                :key="prize.id"
                class="absolute top-0 left-0 w-full h-full origin-center"
                :style="{ transform: `rotate(${index * 45}deg)` }"
              >
                <div 
                  class="absolute top-0 left-1/2 -translate-x-1/2 w-[120px] h-[150px] origin-bottom flex flex-col items-center pt-4"
                  :style="{
                    backgroundColor: index % 2 === 0 ? '#111' : '#1a1a1a',
                    clipPath: 'polygon(50% 100%, 0 0, 100% 0)',
                    borderRight: '1px solid rgba(255,255,255,0.03)'
                  }"
                >
                  <span class="text-[13px] font-black text-white uppercase tracking-tight text-center px-2 leading-tight drop-shadow-[0_2px_3px_rgba(0,0,0,1)] mb-2">
                    {{ prize.name }}
                  </span>
                  <div class="relative">
                    <div class="absolute inset-0 blur-lg opacity-50" :style="{ backgroundColor: getPrizeColor(prize.type) }" />
                    <img v-if="prize.type === 'gold'" :src="gold1gImage" class="w-9 h-9 relative z-10 object-contain" />
                    <img v-else-if="prize.type === 'phone'" :src="phoneModelImage" class="w-9 h-9 relative z-10 object-contain" />
                    <component v-else :is="getPrizeIcon(prize.type)" class="w-9 h-9 relative z-10" :style="{ color: getPrizeColor(prize.type) }" />
                  </div>
                </div>
              </div>
              
              <!-- 内阴影深度感 -->
              <div class="absolute inset-0 rounded-full shadow-[inset_0_0_60px_rgba(0,0,0,0.8)] pointer-events-none" />
            </div>

            <!-- 中心启动按钮 -->
            <div class="absolute inset-0 m-auto w-24 h-24 z-30">
              <button 
                @click="handleSpin"
                :disabled="isSpinning || walletChances <= 0"
                class="w-full h-full rounded-full border-4 shadow-[0_10px_30px_rgba(0,0,0,0.5),inset_0_2px_5px_rgba(255,255,255,0.1)] flex items-center justify-center active:translate-y-1 active:shadow-inner transition-all disabled:grayscale disabled:opacity-50 disabled:active:translate-y-0 group overflow-hidden"
                :class="{
                  'bg-gradient-to-b from-zinc-700 to-zinc-900 border-zinc-800': walletChances <= 0 || isSpinning,
                  'bg-gradient-to-b from-blue-500 to-purple-600 border-red-600 shadow-[0_10px_30px_rgba(220,38,38,0.4),inset_0_2px_5px_rgba(255,255,255,0.2)]': walletChances > 0 && !isSpinning
                }"
              >
                <!-- 按钮发光 -->
                <div class="absolute inset-0 bg-amber-500/10 opacity-0 group-hover:opacity-100 transition-opacity" />
                <div class="relative z-10 flex flex-col items-center">
                  <Zap class="w-5 h-5 mb-1" :class="{
                    'text-amber-400 animate-bounce': !isSpinning && walletChances > 0,
                    'text-zinc-400': walletChances <= 0 || isSpinning
                  }" />
                  <span class="text-[10px] font-black uppercase tracking-[0.2em]" :class="{
                    'text-amber-400': !isSpinning && walletChances > 0,
                    'text-zinc-400': walletChances <= 0 || isSpinning
                  }">
                    {{ isSpinning ? '抽奖中' : (walletChances > 0 ? '开始抽奖' : '没有机会') }}
                  </span>
                </div>
              </button>
            </div>
          </div>
        </div>

        <!-- 抽奖进度条 -->
        <div class="w-full max-w-[280px]">
          <div class="bg-gradient-to-br from-zinc-800/80 to-zinc-900/80 rounded-xl border border-zinc-700/50 backdrop-blur-xl p-3 shadow-[0_4px_15px_rgba(0,0,0,0.5)]">
            <!-- 奖励标签（进度条上方） -->
            <div class="flex justify-between px-0 mb-2">
              <span class="text-[9px] font-bold text-zinc-500">+0</span>
              <template v-for="(threshold, index) in thresholds" :key="'reward-' + index">
                <span class="flex items-center gap-0.5 text-[10px] font-black text-amber-400 drop-shadow-[0_0_4px_rgba(251,191,36,0.6)]">
                  <Zap class="w-3 h-3" />+{{ threshold.giveChances }}
                </span>
              </template>
            </div>
            
            <!-- 进度条 -->
            <div class="relative h-2.5 bg-zinc-900/80 rounded-full overflow-hidden border border-zinc-700/30">
              <!-- 动态背景刻度线 -->
              <div 
                v-for="(threshold, index) in thresholds.slice(0, -1)" 
                :key="'line-' + index"
                class="absolute inset-y-0 w-[1px] bg-zinc-700/30"
                :style="{ left: `${(threshold.adCount / (thresholds[thresholds.length - 1]?.adCount || 3000)) * 100}%` }"
              />
              
              <!-- 进度条 -->
              <div 
                class="h-full bg-gradient-to-r from-blue-500 via-purple-500 to-amber-500 rounded-full transition-all duration-1000 ease-out relative overflow-hidden"
                :style="{ width: `${Math.min((todayAdCount / (thresholds[thresholds.length - 1]?.adCount || 3000)) * 100, 100)}%` }"
              >
                <!-- 扫光效果 -->
                <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-shimmer" />
              </div>
            </div>
            
            <!-- 刻度标签（进度条下方） -->
            <div class="flex justify-between px-0 mt-2">
              <span class="text-[9px] font-mono font-bold text-white">0</span>
              <template v-for="(threshold, index) in thresholds" :key="'label-' + index">
                <span class="text-[9px] font-mono font-bold text-white">{{ threshold.adCount }}</span>
              </template>
            </div>
            
            <!-- 底部提示（整合广告次数和进度状态） -->
            <div class="mt-3 pt-3 border-t border-zinc-700/30">
              <div class="flex justify-center items-center">
                <template v-if="nextThreshold">
                  <span class="text-[10px] text-zinc-400">
                    <span class="text-white font-bold">{{ todayAdCount }}</span> / {{ thresholds[thresholds.length - 1]?.adCount || 3000 }} 条广告 · 再看 <span class="text-amber-400 font-bold">{{ remainingToNext }}</span> 条得 <span class="text-amber-400 font-bold">{{ nextThreshold.giveChances }}</span> 次
                  </span>
                </template>
                <template v-else-if="todayAdCount >= (thresholds[thresholds.length - 1]?.adCount || 3000)">
                  <span class="text-[10px]">
                    <span class="text-emerald-400 font-bold">{{ todayAdCount }}</span>
                    <span class="text-zinc-500"> / {{ thresholds[thresholds.length - 1]?.adCount || 3000 }} 条广告 · </span>
                    <span class="text-emerald-400 font-bold">今日已满级</span>
                  </span>
                </template>
                <template v-else>
                  <span class="text-[10px] text-zinc-500">
                    <span class="text-white font-bold">{{ todayAdCount }}</span> / {{ thresholds[thresholds.length - 1]?.adCount || 3000 }} 条广告 · 
                    <span class="text-amber-400">加载中...</span>
                  </span>
                </template>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 中奖记录列表 -->
      <section class="space-y-6">
        <div class="flex items-center justify-between px-2">
          <div class="flex items-center gap-3">
            <div class="w-1 h-4 bg-amber-500 rounded-full" />
            <h2 class="text-[10px] uppercase tracking-[0.4em] text-zinc-400 font-black">抽奖记录</h2>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-1.5 h-1.5 bg-emerald-500 rounded-full" />
            <span class="text-[9px] text-emerald-500 font-bold uppercase tracking-widest">实时更新中</span>
          </div>
        </div>
        
        <div class="bg-zinc-900/40 rounded-[2.5rem] border border-white/5 backdrop-blur-xl overflow-hidden shadow-2xl">
          <div v-if="welfareRecords.length === 0" class="py-24 text-center">
            <div class="relative inline-block mb-6">
              <div class="absolute inset-0 bg-zinc-500/10 blur-2xl rounded-full" />
              <Gift class="w-12 h-12 text-zinc-800 relative z-10" />
            </div>
            <p class="text-[10px] text-zinc-600 uppercase tracking-[0.4em] font-bold">暂无中奖记录</p>
          </div>
          <div v-else class="divide-y divide-white/[0.03] max-h-[360px] overflow-y-auto">
            <div 
              v-for="record in welfareRecords" 
              :key="record.id"
              class="px-8 py-5 flex justify-between items-center hover:bg-white/[0.02] transition-colors group"
            >
              <div class="flex items-center gap-4">
                <div 
                  class="w-10 h-10 rounded-xl flex items-center justify-center border border-white/5 shadow-lg transition-transform group-hover:scale-110"
                  :class="{
                    'bg-emerald-500/5 text-emerald-500': record.value > 0,
                    'bg-zinc-500/5 text-zinc-500': record.value === 0
                  }"
                >
                  <Gift class="w-5 h-5" />
                </div>
                <div class="flex flex-col">
                  <span class="text-sm font-black text-white tracking-tight group-hover:text-amber-400 transition-colors">{{ getPrizeName(record.name) }}</span>
                  <span class="text-[9px] text-zinc-500 font-mono tracking-widest mt-0.5">{{ formatTime(record.time) }}</span>
                </div>
              </div>
              <div class="text-right">
                <span 
                  v-if="record.value > 0"
                  class="text-base font-black bg-gradient-to-r from-emerald-400 to-emerald-300 bg-clip-text text-transparent"
                >
                  +{{ record.value }}元
                </span>
                <span 
                  v-else
                  class="text-base font-black text-zinc-500"
                >
                  谢谢参与
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 中奖弹窗 -->
    <transition name="prize">
      <div v-if="showResultModal" class="fixed inset-0 z-[100] flex items-center justify-center p-6">
        <!-- 背景遮罩 -->
        <div class="absolute inset-0 bg-black/95 backdrop-blur-3xl" @click="showResultModal = false" />
        
        <!-- 光晕动画 -->
        <div class="absolute inset-0 overflow-hidden pointer-events-none">
          <div 
            v-if="spinResult?.type !== 'encourage'"
            class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-gradient-radial from-amber-500/20 via-transparent to-transparent animate-ping" 
          />
          <div 
            v-if="spinResult?.type !== 'encourage'"
            class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[400px] h-[400px] bg-gradient-radial from-yellow-400/10 via-transparent to-transparent animate-pulse" 
          />
        </div>

        <!-- 烟花粒子容器 -->
        <div v-if="spinResult?.type !== 'encourage'" class="absolute inset-0 pointer-events-none overflow-hidden">
          <!-- 烟花爆炸效果 -->
          <div class="firework" style="top: 30%; left: 20%;"></div>
          <div class="firework" style="top: 40%; left: 80%; animation-delay: 0.5s;"></div>
          <div class="firework" style="top: 60%; left: 30%; animation-delay: 1s;"></div>
          <div class="firework" style="top: 25%; left: 65%; animation-delay: 1.5s;"></div>
          <div class="firework" style="top: 70%; left: 75%; animation-delay: 2s;"></div>
          
          <!-- 闪烁星星 -->
          <div class="star" style="top: 20%; left: 15%; animation-delay: 0s;"></div>
          <div class="star" style="top: 35%; left: 85%; animation-delay: 0.3s;"></div>
          <div class="star" style="top: 55%; left: 10%; animation-delay: 0.6s;"></div>
          <div class="star" style="top: 75%; left: 90%; animation-delay: 0.9s;"></div>
          <div class="star" style="top: 45%; left: 5%; animation-delay: 1.2s;"></div>
          
          <!-- 飘落彩纸 -->
          <div class="confetti confetti-1" style="left: 10%; animation-delay: 0s;"></div>
          <div class="confetti confetti-2" style="left: 25%; animation-delay: 0.2s;"></div>
          <div class="confetti confetti-3" style="left: 40%; animation-delay: 0.4s;"></div>
          <div class="confetti confetti-4" style="left: 55%; animation-delay: 0.6s;"></div>
          <div class="confetti confetti-5" style="left: 70%; animation-delay: 0.8s;"></div>
          <div class="confetti confetti-6" style="left: 85%; animation-delay: 1s;"></div>
        </div>

        <!-- 主弹窗 -->
        <div 
          :class="[
            'relative p-6 rounded-[2rem] text-center space-y-4 max-w-sm w-full overflow-hidden border transition-all',
            spinResult?.type === 'encourage' 
              ? 'bg-gradient-to-br from-zinc-800 via-zinc-900 to-black border-white/5 shadow-[0_0_100px_rgba(59,130,246,0.25),inset_0_0_40px_rgba(59,130,246,0.05)]'
              : 'bg-gradient-to-br from-zinc-800 via-zinc-900 to-black border-white/5 shadow-[0_0_150px_rgba(245,158,11,0.3),inset_0_0_40px_rgba(245,158,11,0.05)]'
          ]"
        >
          <!-- 边框光效 -->
          <div 
            :class="[
              'absolute inset-[1px] rounded-[calc(2rem-2px)] pointer-events-none',
              spinResult?.type === 'encourage' 
                ? 'bg-gradient-to-r from-blue-500/15 via-blue-400/10 to-blue-500/15'
                : 'bg-gradient-to-r from-amber-500/20 via-yellow-500/10 to-amber-500/20'
            ]"
          />
          
          <!-- 顶部图标 -->
          <div class="relative -mt-12">
            <div 
              :class="[
                'absolute inset-0 w-24 h-24 -left-1/2 -translate-x-1/2 rounded-full blur-xl',
                spinResult?.type === 'encourage' 
                  ? 'bg-gradient-radial from-blue-400/30 to-transparent animate-pulse'
                  : 'bg-gradient-radial from-amber-400/40 to-transparent animate-pulse'
              ]"
            />
            <div 
              :class="[
                'relative w-20 h-20 mx-auto rounded-full flex items-center justify-center shadow-lg border-3 border-zinc-900',
                spinResult?.type === 'encourage' 
                  ? 'bg-gradient-to-br from-blue-500 via-blue-400 to-blue-600 shadow-[0_0_40px_rgba(59,130,246,0.5),0_15px_30px_rgba(0,0,0,0.5)]'
                  : 'bg-gradient-to-br from-amber-400 via-yellow-500 to-orange-500 shadow-[0_0_40px_rgba(245,158,11,0.6),0_15px_30px_rgba(0,0,0,0.5)]'
              ]"
            >
              <div class="absolute inset-1.5 bg-gradient-to-br from-white/20 to-transparent rounded-full" />
              <component 
                :is="spinResult?.type === 'encourage' ? Handshake : Trophy" 
                class="w-10 h-10 text-white drop-shadow-lg" 
              />
            </div>
          </div>
          
          <!-- 标题 -->
          <div class="pt-4">
            <h3 
              :class="[
                'text-2xl font-black uppercase tracking-tight',
                spinResult?.type === 'encourage' 
                  ? 'bg-gradient-to-r from-blue-400 via-blue-300 to-blue-400 bg-clip-text text-transparent'
                  : 'bg-gradient-to-r from-amber-400 via-yellow-300 to-amber-400 bg-clip-text text-transparent'
              ]"
            >
              {{ spinResult?.type === 'encourage' ? '很遗憾!' : '恭喜中奖!' }}
            </h3>
          </div>

          <!-- 奖品展示 -->
          <div 
            :class="[
              'py-4 px-4 rounded-[1.5rem] border relative overflow-hidden',
              spinResult?.type === 'encourage' 
                ? 'bg-black/40 border-white/5'
                : 'bg-black/60 border-white/5'
            ]"
          >
            <!-- 背景光效 -->
            <div 
              v-if="spinResult?.type !== 'encourage'"
              class="absolute inset-0 bg-gradient-to-br from-amber-500/10 via-transparent to-yellow-500/5" 
            />
            
            <!-- 奖品名称 -->
            <p 
              :class="[
                'text-3xl font-black tracking-tight drop-shadow-[0_4px_10px_rgba(0,0,0,0.5)]',
                spinResult?.type === 'encourage' ? 'text-white' : 'text-white'
              ]"
            >
              {{ spinResult?.name }}
            </p>
          </div>

          <!-- 确定按钮 -->
          <button
            @click="closeResultModal"
            :class="[
              'w-full py-4 rounded-xl font-black text-sm uppercase tracking-[0.2em] transition-all duration-300 active:scale-95 relative overflow-hidden group',
              spinResult?.type === 'encourage' 
                ? 'bg-gradient-to-r from-blue-600 via-blue-500 to-blue-600 text-white hover:from-blue-500 hover:via-blue-400 hover:to-blue-500 shadow-[0_8px_20px_rgba(59,130,246,0.3),inset_0_1px_0_rgba(255,255,255,0.2)]'
                : 'bg-gradient-to-r from-amber-500 via-yellow-500 to-amber-500 text-black hover:from-amber-400 hover:via-yellow-400 hover:to-amber-400 shadow-[0_8px_20px_rgba(245,158,11,0.4),inset_0_1px_0_rgba(255,255,255,0.2)]'
            ]"
          >
            <span class="relative z-10">确定</span>
            <div 
              :class="[
                'absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-700'
              ]"
            />
          </button>
        </div>
      </div>
    </transition>

    <!-- 金额选择弹窗 -->
    <transition name="modal">
      <div v-if="showAmountModal" class="fixed inset-0 z-[9999] flex items-end justify-center p-0 pointer-events-auto">
        <div 
          class="fixed inset-0 bg-black/80 backdrop-blur-md transition-opacity"
          @click="showAmountModal = false"
        />
        <div class="relative transform transition-all w-full">
          <div class="bg-zinc-900 border-t border-white/10 rounded-t-[3rem] p-6 shadow-2xl overflow-hidden min-h-[60vh] flex flex-col">
            <!-- 背景渐变 -->
            <div class="absolute top-0 left-0 w-full h-24 bg-gradient-to-b from-blue-500/20 to-transparent pointer-events-none" />
            
            <!-- 碳纤维纹理 -->
            <div class="absolute inset-0 opacity-[0.03] pointer-events-none" style="background-image: url('https://www.transparenttextures.com/patterns/carbon-fibre.png');" />
            
            <!-- 光晕效果 -->
            <div class="absolute -top-20 -right-20 w-40 h-40 bg-blue-500/20 rounded-full blur-3xl pointer-events-none" />
            <div class="absolute -bottom-20 -left-20 w-40 h-40 bg-purple-500/20 rounded-full blur-3xl pointer-events-none" />
            
            <div class="relative z-10 flex flex-col flex-1 justify-between">
              <div class="space-y-8">
                <div class="text-center">
                  <h3 class="text-2xl font-black bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent uppercase tracking-tight">选择提现金额</h3>
                  <p class="text-xs text-zinc-500 font-black uppercase tracking-[0.3em] mt-2">当前余额: <span class="bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent text-sm">¥{{ welfareBalance.toFixed(2) }}</span></p>
                </div>
                
                <div class="grid grid-cols-3 gap-3">
                  <button 
                    v-for="amount in [10, 20, 30, 50, 100, 200]" 
                    :key="amount"
                    @click="selectWithdrawAmount(amount)"
                    :disabled="welfareBalance < amount"
                    class="py-4 rounded-2xl border-2 transition-all active:scale-95 disabled:opacity-30 disabled:border-zinc-800 disabled:text-zinc-700 disabled:bg-zinc-900 disabled:active:scale-100 relative overflow-hidden group"
                    :class="{
                      'border-blue-500 bg-gradient-to-br from-blue-500/10 to-purple-600/10 text-blue-400 hover:from-blue-500/20 hover:to-purple-600/20': welfareBalance >= amount && amount !== selectedAmount,
                      'border-green-500 bg-gradient-to-br from-green-500/20 to-emerald-600/20 text-green-400': welfareBalance >= amount && amount === selectedAmount,
                      'border-zinc-800 text-zinc-700 bg-zinc-900': welfareBalance < amount
                    }"
                  >
                    <!-- 按钮发光效果 -->
                    <div v-if="welfareBalance >= amount" class="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity" />
                    <!-- 按钮边框发光 -->
                    <div v-if="welfareBalance >= amount" class="absolute inset-0 rounded-2xl border-2 border-blue-500/0 group-hover:border-blue-500/50 transition-all" />
                    <span class="text-xl font-black relative z-10">¥{{ amount }}</span>
                  </button>
                </div>
                
                <p class="text-xs text-zinc-400 font-black uppercase tracking-[0.2em] text-center">今日您还可以提现<span class="text-green-400">{{ remainingWithdraws }}次</span></p>
                <p class="text-xs text-zinc-400 font-black uppercase tracking-[0.2em] text-center mt-1">申请提现后，<span class="text-green-400">1～3</span>个工作日打款到账</p>
              </div>
              
              <div class="mt-4" v-if="selectedAmount === 0">
                <button
                  @click="showAmountModal = false"
                  class="w-full py-4 rounded-2xl bg-zinc-800 text-zinc-400 font-black text-xs uppercase tracking-[0.2em] hover:bg-zinc-700 transition-all active:scale-95 relative overflow-hidden group"
                >
                  <!-- 按钮发光效果 -->
                  <div class="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity" />
                  <span class="relative z-10">取消</span>
                </button>
              </div>
              <div class="mt-4 grid grid-cols-2 gap-3" v-else>
                <button
                  @click="showAmountModal = false; selectedAmount = 0"
                  class="py-4 rounded-2xl bg-zinc-800 text-zinc-400 font-black text-xs uppercase tracking-[0.2em] hover:bg-zinc-700 transition-all active:scale-95 relative overflow-hidden group"
                >
                  <!-- 按钮发光效果 -->
                  <div class="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity" />
                  <span class="relative z-10">取消</span>
                </button>
                <button
                  @click="confirmWithdrawAmount"
                  class="py-4 rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600 text-white font-black text-xs uppercase tracking-[0.2em] hover:opacity-90 transition-all active:scale-95 relative overflow-hidden group shadow-[0_10px_20px_rgba(59,130,246,0.2)]"
                >
                  <!-- 按钮发光效果 -->
                  <div class="absolute inset-0 bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity" />
                  <span class="relative z-10">确定</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 提现记录弹窗 -->
    <transition name="modal">
      <div v-if="showWithdrawRecordsModal" class="fixed inset-0 z-[9999] flex items-end justify-center p-0 pointer-events-auto">
        <div 
          class="absolute inset-0 bg-black/80 backdrop-blur-md transition-opacity z-[9998] pointer-events-auto"
          @click="showWithdrawRecordsModal = false"
        />
        <div class="relative w-full bg-zinc-900 border-t border-white/10 rounded-t-[3rem] p-6 shadow-2xl overflow-hidden min-h-[60vh] flex flex-col z-[9999]">
          <!-- 背景渐变 -->
          <div class="absolute top-0 left-0 w-full h-24 bg-gradient-to-b from-blue-500/20 to-transparent pointer-events-none" />
          
          <!-- 碳纤维纹理 -->
          <div class="absolute inset-0 opacity-[0.03] pointer-events-none" style="background-image: url('https://www.transparenttextures.com/patterns/carbon-fibre.png');" />
          
          <!-- 光晕效果 -->
          <div class="absolute -top-20 -right-20 w-40 h-40 bg-blue-500/20 rounded-full blur-3xl pointer-events-none" />
          <div class="absolute -bottom-20 -left-20 w-40 h-40 bg-purple-500/20 rounded-full blur-3xl pointer-events-none" />
          
          <div class="relative z-10 flex flex-col flex-1 justify-between">
            <div class="space-y-6">
              <div class="flex items-center justify-between">
                <h3 class="text-xl font-black text-white uppercase tracking-tight">提现记录</h3>
                <button 
                  @click="showWithdrawRecordsModal = false"
                  class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white"
                >
                  <LogOut class="w-4 h-4 rotate-90" />
                </button>
              </div>
              
              <div class="flex items-center justify-between mb-3">
                <p class="text-xs font-black uppercase tracking-[0.3em] text-zinc-400">历史提现</p>
                <button 
                  @click="loadWithdrawRecords"
                  class="text-xs text-blue-400 font-bold flex items-center gap-1 hover:text-blue-300 transition-colors"
                >
                  <RefreshCw class="w-3 h-3" />
                  刷新
                </button>
              </div>
              
              <div class="bg-zinc-900/60 rounded-xl border border-white/5 p-3 max-h-64 overflow-y-auto no-scrollbar">
                <div v-if="withdrawRecords.length === 0" class="text-center py-8">
                  <div class="relative inline-block mb-4">
                    <div class="absolute inset-0 bg-zinc-500/10 blur-2xl rounded-full" />
                    <History class="w-10 h-10 text-zinc-800 relative z-10" />
                  </div>
                  <p class="text-[10px] text-zinc-600 uppercase tracking-[0.2em] font-bold">暂无提现记录</p>
                </div>
                <div v-else class="space-y-4">
                  <div v-for="record in withdrawRecords" :key="record.id" class="flex justify-between items-center pb-3 border-b border-white/5 last:border-0 last:pb-0">
                    <div class="flex flex-col">
                      <span class="text-sm font-black text-white tracking-tight">{{ formatTime(record.time) }}</span>
                      <span :class="['text-xs font-bold mt-1', record.statusColor]">{{ record.statusText }}</span>
                    </div>
                    <span class="text-lg font-black text-white">¥{{ record.amount }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="mt-6">
              <button
                @click="showWithdrawRecordsModal = false"
                class="w-full py-4 rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600 text-white font-black text-xs uppercase tracking-[0.2em] hover:opacity-90 transition-all active:scale-95 relative overflow-hidden group shadow-[0_10px_20px_rgba(59,130,246,0.2)]"
              >
                <!-- 按钮发光效果 -->
                <div class="absolute inset-0 bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity" />
                <span class="relative z-10">关闭</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 提现弹窗 -->
    <transition name="modal">
      <div v-if="showWithdrawModal" class="fixed inset-0 z-[9999] flex items-end justify-center sm:items-center p-0 sm:p-6 pointer-events-auto">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998] pointer-events-auto" @click="closeWithdrawModal" />
        <div class="relative w-full max-w-md bg-[#020205] border-t sm:border border-white/10 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col h-[90vh] max-h-[600px] z-[9999] shadow-2xl">
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-[#020205] z-10">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-purple-500/20 rounded-full flex items-center justify-center mr-3 border border-purple-500/30">
                <Wallet class="w-4 h-4 text-purple-400" />
              </div>
              <h3 class="text-sm font-bold uppercase tracking-widest">福利提现</h3>
            </div>
            <button 
              @click="closeWithdrawModal"
              class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white"
            >
              <LogOut class="w-4 h-4 rotate-90" />
            </button>
          </div>
          
          <div class="flex-1 overflow-y-auto no-scrollbar p-8">
            <div class="space-y-6">
              <!-- 可提现金额 -->
              <div class="glass-card rounded-xl p-4">
                <p class="bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent text-xs uppercase tracking-wider mb-2">可提现金额</p>
                <p class="text-2xl font-bold text-white">¥{{ welfareBalance.toFixed(2) }}</p>
              </div>
              
              <!-- 提现金额输入 -->
              <div>
                <label class="block bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent text-xs uppercase tracking-wider mb-2">提现金额</label>
                <input 
                  v-model="withdrawAmount" 
                  type="number" 
                  min="0" 
                  :max="welfareBalance" 
                  step="0.01"
                  readonly
                  class="w-full px-4 py-3 rounded-xl bg-zinc-800/50 border border-zinc-700 text-white focus:outline-none focus:border-purple-500 transition-all"
                  placeholder="请输入提现金额"
                />
              </div>
              
              <!-- 支付宝账号 -->
              <div>
                <label class="block bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent text-xs uppercase tracking-wider mb-2">支付宝账号</label>
                <input 
                  v-model="alipayAccount" 
                  type="text"
                  class="w-full px-4 py-3 rounded-xl bg-zinc-800/50 border border-zinc-700 text-white focus:outline-none focus:border-purple-500 transition-all"
                  placeholder="请输入支付宝账号"
                />
              </div>
              
              <!-- 支付宝姓名 -->
              <div>
                <label class="block bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent text-xs uppercase tracking-wider mb-2">支付宝姓名</label>
                <input 
                  v-model="alipayName" 
                  type="text"
                  class="w-full px-4 py-3 rounded-xl bg-zinc-800/50 border border-zinc-700 text-white focus:outline-none focus:border-purple-500 transition-all"
                  placeholder="请输入支付宝姓名"
                />
              </div>
            </div>
          </div>
          
          <div class="p-8 border-t border-white/5">
            <button 
              @click="handleWithdraw"
              :disabled="isSubmittingWithdraw || !withdrawAmount || !alipayAccount || !alipayName"
              class="w-full py-3 rounded-xl font-bold text-sm uppercase tracking-wider transition-all"
              :class="isSubmittingWithdraw || !withdrawAmount || !alipayAccount || !alipayName ? 'bg-zinc-800 text-zinc-400 cursor-not-allowed' : 'bg-gradient-to-r from-purple-500 to-pink-500 text-white hover:opacity-90'"
            >
              {{ isSubmittingWithdraw ? '提交中...' : '确认提现' }}
            </button>
            
            <!-- 提现成功提示 -->
            <div v-if="withdrawSuccess" class="mt-4 p-3 bg-emerald-900/30 rounded-lg border border-emerald-800/50">
              <p class="text-emerald-300 text-sm text-center">提现申请已提交，等待处理</p>
            </div>
            
            <!-- 错误提示 -->
            <div v-if="error" class="mt-4 p-3 bg-red-900/30 rounded-lg border border-red-800/50">
              <p class="text-red-300 text-sm text-center">{{ error }}</p>
            </div>
          </div>
        </div>
      </div>
    </transition>
    
    <!-- 绑定弹窗 -->
    <transition name="modal">
      <div v-if="showBindModal" class="fixed inset-0 z-[9999] flex items-end justify-center sm:items-center p-0 sm:p-6 pointer-events-auto">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998] pointer-events-auto" @click="showBindModal = false" />
        <div class="relative w-full max-w-md bg-[#020205] border-t sm:border border-white/10 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col h-[90vh] max-h-[600px] z-[9999] shadow-2xl">
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-[#020205] z-10">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-blue-500/20 rounded-full flex items-center justify-center mr-3 border border-blue-500/30">
                <Wallet class="w-4 h-4 text-blue-400" />
              </div>
              <h3 class="text-sm font-bold uppercase tracking-widest">绑定支付宝</h3>
            </div>
            <button 
              @click="showBindModal = false"
              class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white"
            >
              <LogOut class="w-4 h-4 rotate-90" />
            </button>
          </div>
          
          <div class="flex-1 overflow-y-auto no-scrollbar p-8">
            <div class="space-y-6">
              <!-- 绑定说明 -->
              <div class="glass-card rounded-xl p-4 text-xs text-zinc-500 space-y-2">
                <p>• 绑定支付宝账号后，提现时将自动填充账号信息</p>
                <p>• 请确保支付宝账号信息正确，否则可能导致提现失败</p>
                <p>• 首次绑定后再次点击可修改绑定信息</p>
              </div>
              
              <!-- 收款人支付宝姓名 -->
              <div>
                <label class="block text-zinc-500 text-xs uppercase tracking-wider mb-2">支付宝姓名</label>
                <input 
                  v-model="bindAlipayName" 
                  type="text"
                  class="w-full px-4 py-3 rounded-xl bg-zinc-800/50 border border-zinc-700 text-white focus:outline-none focus:border-blue-500 transition-all"
                  placeholder="请输入支付宝姓名"
                />
              </div>
              
              <!-- 收款人支付宝帐号 -->
              <div>
                <label class="block text-zinc-500 text-xs uppercase tracking-wider mb-2">支付宝帐号</label>
                <input 
                  v-model="bindAlipayAccount" 
                  type="text"
                  class="w-full px-4 py-3 rounded-xl bg-zinc-800/50 border border-zinc-700 text-white focus:outline-none focus:border-blue-500 transition-all"
                  placeholder="请输入支付宝帐号"
                />
              </div>
            </div>
          </div>
          
          <div class="px-8 py-6 border-t border-white/5 bg-[#020205] z-10">
            <button 
              @click="handleBind"
              :disabled="isSubmittingBind || !bindAlipayName || !bindAlipayAccount"
              class="w-full py-4 rounded-xl bg-gradient-to-r from-blue-500 to-purple-600 text-white font-bold text-xs uppercase tracking-widest hover:opacity-90 transition-all active:scale-95 disabled:opacity-30 disabled:active:scale-100 flex items-center justify-center gap-2"
            >
              <Wallet class="w-4 h-4" />
              确认绑定
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 抽奖规则弹窗 -->
    <transition name="modal">
      <div v-if="showRulesModal" class="fixed inset-0 z-[9999] flex items-center justify-center p-6 pointer-events-auto">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998] pointer-events-auto" @click="closeRulesModal" />
        <div class="relative w-full max-w-md bg-[#020205] border border-white/10 rounded-[3rem] overflow-hidden flex flex-col h-[600px] z-[9999] shadow-2xl">
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-[#020205] z-10">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-amber-500/20 rounded-full flex items-center justify-center mr-3 border border-amber-500/30">
                <Sparkles class="w-4 h-4 text-amber-400" />
              </div>
              <h3 class="text-sm font-bold uppercase tracking-widest">抽奖规则</h3>
            </div>
            <button 
              @click="closeRulesModal"
              class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white"
            >
              <LogOut class="w-4 h-4 rotate-90" />
            </button>
          </div>
          
          <div class="flex-1 overflow-y-auto no-scrollbar p-6">
            <div class="space-y-6">
              <!-- 抽奖机会规则 -->
              <div class="glass-card rounded-xl p-4">
                <h4 class="text-xs font-bold uppercase tracking-wider text-amber-400 mb-3 flex items-center gap-2">
                  <div class="w-2 h-2 bg-amber-500 rounded-full animate-pulse" />
                  抽奖机会获取规则
                </h4>
                <ul class="space-y-2 text-xs text-zinc-300">
                  <li class="flex items-start gap-2">
                    <span class="text-blue-400 font-bold">•</span>
                    <span>抽奖机会采用阶梯制，每日0点重置</span>
                  </li>
                  <template v-for="(threshold, index) in thresholds" :key="'rule-' + index">
                    <li class="flex items-start gap-2">
                      <span class="text-green-400 font-bold">✓</span>
                      <span>当日广告次数达<span class="text-amber-400 font-bold">{{ threshold.adCount }}</span>次，
                        <template v-if="index === 0">得</template>
                        <template v-else>额外得</template>
                        <span class="text-amber-400 font-bold">{{ threshold.giveChances }}</span>次抽奖机会
                        <template v-if="index === thresholds.length - 1">（封顶）</template>
                      </span>
                    </li>
                  </template>
                  <li class="flex items-start gap-2">
                    <span class="text-amber-400 font-bold">★</span>
                    <span>每个用户每日最多可获得<span class="text-amber-400 font-bold">{{ thresholds.reduce((sum, t) => sum + t.giveChances, 0) }}</span>次抽奖机会</span>
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-purple-400 font-bold">✦</span>
                    <span>本抽奖活动综合中奖率为<span class="text-amber-400 font-bold">92%</span></span>
                  </li>
                </ul>
              </div>
              
              <!-- 现金奖励规则 -->
              <div class="glass-card rounded-xl p-4">
                <h4 class="text-xs font-bold uppercase tracking-wider text-green-400 mb-3 flex items-center gap-2">
                  <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                  现金奖励规则
                </h4>
                <p class="text-xs text-zinc-300 leading-relaxed">
                  现金奖励中奖后，获得的奖励会直接发放至福利钱包，可直接提现至支付宝，1～3个工作日到账。
                </p>
              </div>
              
              <!-- 实物奖励规则 -->
              <div class="glass-card rounded-xl p-4">
                <h4 class="text-xs font-bold uppercase tracking-wider text-purple-400 mb-3 flex items-center gap-2">
                  <div class="w-2 h-2 bg-purple-500 rounded-full animate-pulse" />
                  实物奖励规则
                </h4>
                <p class="text-xs text-zinc-300 leading-relaxed">
                  实物奖励中奖后，工作人员会主动联系中奖用户，对接收货地址，进行奖品寄送。
                </p>
              </div>
            </div>
          </div>
          
          <div class="px-8 py-4 border-t border-white/5 bg-[#020205] z-10">
            <button 
              @click="closeRulesModal"
              class="w-full py-3 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 text-black font-bold text-xs uppercase tracking-widest hover:opacity-90 transition-all active:scale-95"
            >
              我知道了
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 记录弹窗 -->
    <transition name="modal">
      <div v-if="showRecordsModal" class="fixed inset-0 z-[9999] flex items-end justify-center sm:items-center p-0 sm:p-6 pointer-events-auto">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998] pointer-events-auto" @click="closeRecordsModal" />
        <div class="relative w-full max-w-md bg-[#020205] border-t sm:border border-white/10 rounded-t-[3rem] sm:rounded-[3rem] overflow-hidden flex flex-col h-[90vh] max-h-[600px] z-[9999] shadow-2xl">
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-[#020205] z-10">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-purple-500/20 rounded-full flex items-center justify-center mr-3 border border-purple-500/30">
                <History class="w-4 h-4 text-purple-400" />
              </div>
              <h3 class="text-sm font-bold uppercase tracking-widest">抽奖记录</h3>
            </div>
            <button 
              @click="closeRecordsModal"
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
              <div v-else-if="welfareRecords.length === 0" class="py-20 text-center">
                <p class="text-xs text-zinc-600 uppercase tracking-widest">暂无抽奖记录</p>
              </div>
              <!-- 记录列表 -->
              <div 
                v-else
                v-for="record in welfareRecords" 
                :key="record.id" 
                class="px-6 py-4 rounded-2xl glass-card flex justify-between items-center min-h-[60px]"
              >
                <div class="flex flex-col">
                  <span class="text-[11px] text-zinc-400 font-mono tracking-tighter">{{ record.time }}</span>
                  <span class="text-[9px] text-zinc-600 uppercase tracking-widest mt-0.5">{{ record.name }}</span>
                </div>
                <div class="flex items-center">
                  <span class="text-sm font-bold" :class="record.value > 0 ? 'text-green-400' : 'text-zinc-400'">{{ record.value > 0 ? `+¥${record.value.toFixed(2)}` : record.name }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="p-8 border-t border-white/5 text-center">
            <p class="text-[10px] text-zinc-600 uppercase tracking-[0.2em]">共计 {{ welfareRecords.length }} 条记录</p>
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
          <!-- 幸运彩票红点标记 -->
          <span 
            v-if="lotteryTicketsCount > 0"
            class="absolute -top-1 -right-1 min-w-[18px] h-[18px] bg-red-500 rounded-full text-[10px] font-bold text-white flex items-center justify-center px-1 shadow-[0_0_10px_rgba(239,68,68,0.6)] animate-pulse"
          >
            {{ lotteryTicketsCount > 99 ? '99+' : lotteryTicketsCount }}
          </span>
        </router-link>
        <router-link 
          to="/welfare-lottery" 
          class="flex flex-col items-center transition-all duration-300 relative"
          :class="$route.path === '/welfare-lottery' ? 'text-emerald-400 scale-105' : 'text-zinc-400 hover:text-zinc-300'"
        >
          <Gift class="w-6 h-6 mb-1" />
          <span class="text-xs font-medium">福利抽奖</span>
          <!-- 抽奖机会红点标记 -->
          <span 
            v-if="walletChances > 0"
            class="absolute -top-1 -right-1 min-w-[18px] h-[18px] bg-red-500 rounded-full text-[10px] font-bold text-white flex items-center justify-center px-1 shadow-[0_0_10px_rgba(239,68,68,0.6)] animate-pulse"
          >
            {{ walletChances > 99 ? '99+' : walletChances }}
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
.glass-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
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

/* 动画效果 */
@keyframes pulse {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.5); }
}

@keyframes wiggle {
  0%, 100% { transform: translateX(-50%) rotate(0deg); }
  25% { transform: translateX(-50%) rotate(-5deg); }
  75% { transform: translateX(-50%) rotate(5deg); }
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.animate-wiggle {
  animation: wiggle 0.2s infinite;
}

.animate-shimmer {
  animation: shimmer 2s infinite;
}

.prize-enter-active, .prize-leave-active {
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.prize-enter-from {
  opacity: 0;
  transform: scale(0.7) translateY(40px);
}
.prize-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 字体平滑处理 */
.font-sans {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>