<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted, computed } from 'vue';
import { Ticket, Trophy, Sparkles, Coins, Clock, TrendingUp, Smartphone, Gift } from 'lucide-vue-next';
import { useLocalStorage } from '../composables/useLocalStorage';
import { getLotteryPool, getCurrentLotteryTickets, getLotteryHistory, getLotteryResult, generateLotteryTicket, getUserInfo, getLastLotteryTicket, getLotterySettings, getWelfareLotteryInfo } from '../api/apiService';
import { TTSPlugin } from '../plugins/TTSPlugin';
import { Capacitor } from '@capacitor/core';

// 响应式数据
const poolStatus = ref({ currentAmount: 0, totalAmount: 0, lastDrawTime: '' });
const lotteryTickets = ref<any[]>([]);
const previousTickets = ref<any[]>([]);
const showPreviousTickets = ref(false);
const showAllTickets = ref(false);
const displayMode = ref<'current' | 'previous'>('current'); // 'current' 显示本期，'previous' 显示上期
const pastDraws = ref<any[]>([]);
const userInfo = ref({ currentMonthGold: 0 });
const isSpinning = ref(false);
const result = ref<string | null>(null);
const winAmount = ref(0);
const currentTicket = ref<string | null>(null);
const countdown = ref({ h: '00', m: '00', s: '00' });
const lotterySettings = ref({
  adCountThreshold: 100,
  firstPrizePercentage: 0.5,
  secondPrizePercentage: 0.3,
  thirdPrizePercentage: 0.2,
  firstPrizeCount: 1,
  secondPrizeCount: 2,
  thirdPrizeCount: 3
}); // 默认值
const showRulesModal = ref(false); // 控制开奖规则弹窗显示
let timerInterval: any = null;
const isLoading = ref(true);

// 福利抽奖次数
const welfareLotteryChances = ref(0);
const empId = ref(localStorage.getItem('empId') || '');

// 中奖记录
const lotteryWinRecords = ref<any[]>([
  {
    id: 'lottery_win_1711500000000',
    time: '2026-03-27 12:30',
    amount: 5000,
    prizeLevel: '一等奖',
    timestamp: 1711500000000
  },
  {
    id: 'lottery_win_1711413600000',
    time: '2026-03-26 12:00',
    amount: 2000,
    prizeLevel: '二等奖',
    timestamp: 1711413600000
  },
  {
    id: 'lottery_win_1711327200000',
    time: '2026-03-25 12:00',
    amount: 1000,
    prizeLevel: '三等奖',
    timestamp: 1711327200000
  }
]);
const showLotteryWinRecordsModal = ref(false);

// 切换显示所有彩票
const toggleShowAllTickets = () => {
  showAllTickets.value = !showAllTickets.value;
};

// 计算属性
const formattedPoolAmount = computed(() => {
  const amount = poolStatus.value.currentAmount;
  if (isNaN(amount)) return '0.00';
  const fixedAmount = amount.toFixed(2);
  const parts = fixedAmount.split('.');
  parts[0] = parseInt(parts[0]).toLocaleString('zh-CN');
  return parts.join('.');
});

const formattedUserCoins = computed(() => {
  return userInfo.value.currentMonthGold.toLocaleString();
});

// 加载数据
const loadData = async () => {
  isLoading.value = true;
  try {
    const userId = localStorage.getItem('userId');
    const employeeId = localStorage.getItem('employeeId');

    // 并行加载所有独立数据
    const [poolResponse, historyResponse, resultResponse, settingsResponse] = await Promise.all([
      getLotteryPool(),
      getLotteryHistory(),
      getLotteryResult(),
      getLotterySettings()
    ]);

    // 处理奖金池状态
    if (poolResponse.success && poolResponse.data) {
      poolStatus.value = poolResponse.data;
    }

    // 处理开奖历史
    if (historyResponse.data) {
      pastDraws.value = historyResponse.data.history || [];
    }

    // 处理彩票设置
    if (settingsResponse.success && settingsResponse.data) {
      lotterySettings.value = settingsResponse.data;
    }

    // 并行加载需要 userId 的数据
    if (userId) {
      const [ticketsResponse, lastTicketResponse] = await Promise.all([
        getCurrentLotteryTickets(userId),
        getLastLotteryTicket(userId)
      ]);

      // 处理用户奖券
      if (ticketsResponse.success && ticketsResponse.data) {
        lotteryTickets.value = ticketsResponse.data.tickets || [];
      }

      // 处理上一期彩票数据
      if (lastTicketResponse.success && lastTicketResponse.data && lastTicketResponse.data.tickets) {
        const lastTickets = lastTicketResponse.data.tickets;
        previousTickets.value = lastTickets.map((ticket: any) => {
          const isWinner = ticket.status && ticket.status.includes('中奖');
          const prize = ticket.status || '已开奖';
          return {
            ticketNumber: ticket.ticketNumber || '000000',
            createdAt: ticket.createdAt || new Date().toISOString(),
            status: ticket.status || '已开奖',
            isWinner: isWinner,
            prize: prize
          };
        });
      } else {
        previousTickets.value = [];
      }
    } else {
      lotteryTickets.value = [];
      previousTickets.value = [];
    }

    // 并行加载用户信息和福利抽奖次数
    const userInfoPromise = employeeId ? getUserInfo(userId, employeeId) : Promise.resolve(null);
    const welfarePromise = empId.value ? getWelfareLotteryInfo(empId.value) : Promise.resolve(null);

    const [userResponse, welfareResponse] = await Promise.all([userInfoPromise, welfarePromise]);

    // 处理用户信息
    if (userResponse && userResponse.success && userResponse.data) {
      userInfo.value = userResponse.data;
    }

    // 处理福利抽奖次数
    if (welfareResponse && welfareResponse.success && welfareResponse.data) {
      welfareLotteryChances.value = Number(welfareResponse.data.chances) || 0;
    }

    // 加载中奖记录
    await loadLotteryWinRecords();
  } catch (error) {
    console.error('加载数据失败:', error);
  } finally {
    isLoading.value = false;
  }
};

// 从后端API获取中奖记录
const loadLotteryWinRecords = async () => {
  try {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      console.error('❌ 用户ID不存在');
      return;
    }
    
    console.log('🔧 开始从后端获取中奖记录');
    const response = await getLotteryHistory();
    
    if (response.success && response.data) {
      console.log('✅ 后端返回的开奖历史:', response.data);
      
      // 过滤出当前用户的中奖记录
      const userWins = [];
      
      response.data.history.forEach(issue => {
        // 检查一等奖
        if (issue.winners.firstPrize && Array.isArray(issue.winners.firstPrize)) {
          issue.winners.firstPrize.forEach(winner => {
            if (winner.userId === userId) {
              userWins.push({
                id: `lottery_win_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                time: new Date(issue.drawTime).toLocaleString('zh-CN', {
                  year: 'numeric', month: '2-digit', day: '2-digit',
                  hour: '2-digit', minute: '2-digit'
                }),
                amount: winner.amount,
                prizeLevel: '一等奖',
                timestamp: new Date(issue.drawTime).getTime()
              });
            }
          });
        }
        
        // 检查二等奖
        if (issue.winners.secondPrize && Array.isArray(issue.winners.secondPrize)) {
          issue.winners.secondPrize.forEach(winner => {
            if (winner.userId === userId) {
              userWins.push({
                id: `lottery_win_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                time: new Date(issue.drawTime).toLocaleString('zh-CN', {
                  year: 'numeric', month: '2-digit', day: '2-digit',
                  hour: '2-digit', minute: '2-digit'
                }),
                amount: winner.amount,
                prizeLevel: '二等奖',
                timestamp: new Date(issue.drawTime).getTime()
              });
            }
          });
        }
        
        // 检查三等奖
        if (issue.winners.thirdPrize && Array.isArray(issue.winners.thirdPrize)) {
          issue.winners.thirdPrize.forEach(winner => {
            if (winner.userId === userId) {
              userWins.push({
                id: `lottery_win_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                time: new Date(issue.drawTime).toLocaleString('zh-CN', {
                  year: 'numeric', month: '2-digit', day: '2-digit',
                  hour: '2-digit', minute: '2-digit'
                }),
                amount: winner.amount,
                prizeLevel: '三等奖',
                timestamp: new Date(issue.drawTime).getTime()
              });
            }
          });
        }
      });
      
      // 按时间倒序排序
      userWins.sort((a, b) => b.timestamp - a.timestamp);
      
      lotteryWinRecords.value = userWins;
      console.log('✅ 过滤后的用户中奖记录:', userWins);
    } else {
      console.warn('⚠️ 后端返回失败:', response.message);
    }
  } catch (error) {
    console.error('❌ 获取中奖记录失败:', error);
  }
};

// 保存中奖记录到LocalStorage
const saveLotteryWinRecords = () => {
  try {
    localStorage.setItem('lotteryWinRecords', JSON.stringify(lotteryWinRecords.value));
    console.log('✅ 保存中奖记录到LocalStorage');
  } catch (error) {
    console.error('❌ 保存中奖记录失败:', error);
  }
};

// 模拟开奖（实际开奖由后端定时执行）
const drawLottery = async () => {
  if (lotteryTickets.value.length === 0 || isSpinning.value) return;
  
  isSpinning.value = true;
  result.value = null;
  
  // 模拟开奖过程
  setTimeout(async () => {
    const rand = Math.random();
    if (rand > 0.98) {
      winAmount.value = Math.floor(poolStatus.value.totalAmount * 0.5);
      result.value = '一等奖';
    } else if (rand > 0.9) {
      winAmount.value = Math.floor(poolStatus.value.totalAmount * 0.3);
      result.value = '二等奖';
    } else if (rand > 0.7) {
      winAmount.value = Math.floor(poolStatus.value.totalAmount * 0.2);
      result.value = '三等奖';
    } else {
      winAmount.value = 0;
      result.value = '未中奖';
    }
    
    // 更新用户金币
    userInfo.value.currentMonthGold += winAmount.value;
    
    // 播放中奖语音
    if (winAmount.value > 0) {
      await playWinSound(winAmount.value);
      // 显示金币奖励动画
      await showRewardAnimation(winAmount.value);
      // 添加到最近收益记录
      addToRecentRecords(winAmount.value, result.value);
    }
    
    // 添加到开奖历史
    if (winAmount.value > 0) {
      const newDraw = {
        id: Date.now().toString(),
        time: new Date().toLocaleDateString(),
        number: '123456', // 模拟票号
        prize: result.value,
        amount: winAmount.value,
        user: localStorage.getItem('employeeId') || '0000'
      };
      pastDraws.value = [newDraw, ...pastDraws.value].slice(0, 10);
    }
    
    isSpinning.value = false;
  }, 2000);
};

// 显示金币奖励动画
const showRewardAnimation = async (amount: number) => {
  console.log('========== showRewardAnimation 被调用 ==========');
  console.log('金币数量:', amount);
  
  // 这里可以实现一个简单的动画提示
  // 实际项目中可以使用更复杂的动画库
  const rewardElement = document.createElement('div');
  rewardElement.className = 'fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-gradient-to-r from-amber-500 to-orange-500 text-white px-8 py-4 rounded-full shadow-lg z-50 animate-bounce';
  rewardElement.style.fontSize = '1.5rem';
  rewardElement.style.fontWeight = 'bold';
  rewardElement.textContent = `+${amount.toLocaleString()} 金币`;
  document.body.appendChild(rewardElement);
  
  // 2秒后移除
  setTimeout(() => {
    rewardElement.remove();
  }, 2000);
};

// 添加到最近收益记录
const addToRecentRecords = (amount: number, prizeLevel: string) => {
  console.log('========== addToRecentRecords 被调用 ==========');
  console.log('金币数量:', amount);
  console.log('奖项等级:', prizeLevel);
  
  // 通过LocalStorage保存中奖记录，供Home.vue加载
  try {
    const storedRecords = localStorage.getItem('lotteryWinRecords');
    let lotteryWinRecords = [];
    
    if (storedRecords) {
      lotteryWinRecords = JSON.parse(storedRecords);
    }
    
    const newLotteryWinRecord = {
      id: `lottery_win_${Date.now()}`,
      time: new Date().toLocaleString('zh-CN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit'
      }),
      amount: amount,
      prizeLevel: prizeLevel,
      timestamp: Date.now()
    };
    
    lotteryWinRecords.unshift(newLotteryWinRecord);
    localStorage.setItem('lotteryWinRecords', JSON.stringify(lotteryWinRecords));
    console.log('✅ 已添加中奖记录到最近收益列表:', newLotteryWinRecord);
  } catch (error) {
    console.error('❌ 添加中奖记录失败:', error);
  }
};

// 更新倒计时
const updateCountdown = () => {
  const now = new Date();
  const target = new Date();
  target.setHours(22, 0, 0, 0);

  if (now.getTime() >= target.getTime()) {
    target.setDate(target.getDate() + 1);
  }

  const diff = target.getTime() - now.getTime();
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((diff % (1000 * 60)) / 1000);

  const h = String(hours).padStart(2, '0');
  const m = String(minutes).padStart(2, '0');
  const s = String(seconds).padStart(2, '0');

  countdown.value = { h, m, s };
};

// 使用requestAnimationFrame优化的倒计时循环
let animationFrameId: number | null = null;
let lastUpdateTime = 0;

const countdownLoop = (timestamp: number) => {
  // 每秒更新一次，避免频繁更新
  if (timestamp - lastUpdateTime >= 1000) {
    updateCountdown();
    lastUpdateTime = timestamp;
  }
  
  // 继续循环
  animationFrameId = requestAnimationFrame(countdownLoop);
};

// 生命周期钩子
let poolRefreshInterval: any = null;

onMounted(() => {
  loadData();
  updateCountdown();
  
  // 使用requestAnimationFrame代替setInterval，性能更好且页面不可见时自动暂停
  lastUpdateTime = performance.now();
  animationFrameId = requestAnimationFrame(countdownLoop);
  
  // 每30秒自动刷新奖金池数据
  poolRefreshInterval = setInterval(() => {
    // 只刷新奖金池数据，避免频繁加载所有数据
    const poolResponse = getLotteryPool();
    poolResponse.then(response => {
      if (response.success && response.data) {
        poolStatus.value = response.data;
      }
    }).catch(error => {
      console.error('❌ 奖金池刷新失败:', error);
    });
  }, 30000); // 30秒刷新一次
});

onUnmounted(() => {
  // 清理requestAnimationFrame
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId);
    animationFrameId = null;
  }
  // 清理定时器
  if (poolRefreshInterval) {
    clearInterval(poolRefreshInterval);
    poolRefreshInterval = null;
  }
});

// 播放中奖语音
const playWinSound = async (amount: number) => {
  try {
    const gold = Math.floor(amount);
    let message = '';
    
    if (gold >= 5000) {
      message = `恭喜你中了超级大奖，获得${gold}金币！`;
    } else if (gold >= 1000) {
      message = `太棒了！你中了${gold}金币！`;
    } else if (gold >= 500) {
      message = `恭喜你中奖了，获得${gold}金币！`;
    } else {
      message = `恭喜你中奖了，获得${gold}金币！`;
    }
    
    // 检查是否在 Android 平台
    if (Capacitor.getPlatform() === 'android') {
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
      const zhVoice = voices.find(v => v.lang.includes('zh'));
      if (zhVoice) {
        utterance.voice = zhVoice;
      }
      window.speechSynthesis.speak(utterance);
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

// 监听开奖状态
watch(isSpinning, (spinning) => {
  if (!spinning && lotteryTickets.value.length > 0) {
    // 可以在这里添加自动开奖逻辑
  }
});
</script>

<template>
  <div class="min-h-screen bg-[#0a0a0b] text-white pb-32 relative overflow-x-hidden">
    <!-- 背景装饰光晕 -->
    <div class="absolute top-[-10%] left-[-10%] w-[60%] h-[60%] bg-amber-600/10 blur-[120px] rounded-full pointer-events-none" />
    <div class="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] bg-purple-600/10 blur-[120px] rounded-full pointer-events-none" />

    <!-- Header -->
    <header class="bg-black/40 backdrop-blur-xl border-b border-white/5 pt-8 pb-5 px-6 flex justify-between items-center sticky top-0 z-20">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 bg-gradient-to-br from-amber-400 to-orange-500 rounded-xl flex items-center justify-center shadow-lg shadow-amber-500/20">
          <Ticket class="text-white w-5 h-5" />
        </div>
        <div class="flex flex-col">
          <span class="font-bold text-sm tracking-widest uppercase bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent">幸运彩票</span>
          <span class="text-[10px] text-zinc-400 font-bold tracking-wider">超级大奖，福利大派送！</span>
        </div>
      </div>
      <div class="px-4 py-1.5 rounded-full bg-white/5 border border-white/10 flex items-center gap-2">
        <span class="text-xs font-mono text-amber-400">{{ lotteryTickets.length }}</span>
        <Ticket class="w-3 h-3 text-amber-500" />
      </div>
    </header>

    <main class="max-w-md mx-auto px-6 mt-6 space-y-4 relative z-10 pb-8">
      <!-- 奖金池 -->
      <div class="relative group bg-gradient-to-br from-zinc-900 to-black p-8 rounded-[2.5rem] border border-white/5 overflow-hidden shadow-2xl text-center">
        <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-amber-500/50 to-transparent" />
        
        <div class="flex items-center justify-center gap-2 mb-3">
          <p class="text-[10px] uppercase tracking-[0.3em] text-zinc-500">当前奖金池累计</p>
          <Coins class="w-3.5 h-3.5 text-amber-500" />
        </div>
        <div class="flex items-center justify-center">
          <span class="text-4xl font-black tracking-tighter text-amber-500 font-mono animate-breath inline-block">
            {{ formattedPoolAmount }}
          </span>
        </div>

        <div class="mt-6 flex justify-center gap-4">
          <button @click="showRulesModal = true" class="px-3 py-1 rounded-full bg-white/5 border border-white/5 flex items-center gap-2 hover:bg-white/10 transition-colors">
            <div class="w-1.5 h-1.5 bg-amber-500 rounded-full" />
            <span class="text-[9px] text-amber-500 uppercase tracking-widest">开奖规则</span>
            <div class="w-1.5 h-1.5 bg-amber-500 rounded-full" />
          </button>
        </div>
      </div>

      <!-- 下次系统开奖倒计时 -->
      <div class="flex flex-col items-center py-0.5">
        <div class="flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-amber-500/5 border border-amber-500/10 mb-5">
          <Clock class="w-2 h-2 text-amber-500 animate-pulse" />
          <span class="text-[9px] uppercase tracking-[0.3em] text-amber-500 font-bold">开奖倒计时</span>
        </div>
        
        <div class="flex items-center gap-4">
          <!-- 小时 -->
          <div class="flex flex-col items-center gap-1">
            <div class="w-11 h-13 bg-zinc-900/80 rounded-lg border border-white/5 flex items-center justify-center shadow-inner">
              <span class="text-xl font-black font-mono text-emerald-400 tracking-tight">{{ countdown.h }}</span>
            </div>
            <span class="text-[9px] text-zinc-600 font-bold">时</span>
          </div>
          
          <span class="text-zinc-700 font-bold mt-[-16px]">:</span>
          
          <!-- 分钟 -->
          <div class="flex flex-col items-center gap-1">
            <div class="w-11 h-13 bg-zinc-900/80 rounded-lg border border-white/5 flex items-center justify-center shadow-inner">
              <span class="text-xl font-black font-mono text-emerald-400 tracking-tight">{{ countdown.m }}</span>
            </div>
            <span class="text-[9px] text-zinc-600 font-bold">分</span>
          </div>
          
          <span class="text-zinc-700 font-bold mt-[-16px]">:</span>
          
          <!-- 秒 -->
          <div class="flex flex-col items-center gap-1">
            <div class="w-11 h-13 bg-zinc-900/80 rounded-lg border border-white/5 flex items-center justify-center shadow-inner">
              <span class="text-xl font-black font-mono text-emerald-400 tracking-tight">{{ countdown.s }}</span>
            </div>
            <span class="text-[9px] text-zinc-600 font-bold">秒</span>
          </div>
        </div>
        
        <p class="mt-1 text-[8px] text-zinc-700 uppercase tracking-widest">每晚 22:00 准时开奖</p>
      </div>

      <!-- 我的幸运彩票 -->
      <div class="w-full py-6 px-6 rounded-[2rem] bg-white/[0.01] border border-white/[0.03] relative overflow-hidden">
        <div class="flex justify-between items-end mb-4 relative z-10 px-2">
          <div class="flex flex-col gap-1">
              <h3 class="text-[11px] uppercase tracking-[0.3em] text-zinc-500 font-black">我的幸运彩票</h3>
              <div class="flex items-center gap-2">
                <span class="text-xl font-black text-white">{{ displayMode === 'current' ? lotteryTickets.length : previousTickets.length }}</span>
                <span class="text-[9px] text-zinc-600 uppercase tracking-widest">{{ displayMode === 'current' ? '张待开奖' : '张已开奖' }}</span>
              </div>
            </div>
          <div class="flex gap-2">
            <button @click="displayMode = displayMode === 'current' ? 'previous' : 'current'" class="px-3 py-1.5 rounded-full bg-white/5 border border-white/10 text-[9px] text-zinc-400 uppercase tracking-[0.2em] font-bold hover:bg-white/10 transition-colors">
              {{ displayMode === 'current' ? '显示' : '隐藏' }}上期彩票
            </button>
            <div v-if="isSpinning" class="flex items-center gap-2 px-3 py-1.5 rounded-full bg-amber-500/10 border border-amber-500/20 backdrop-blur-sm">
              <div class="w-1.5 h-1.5 bg-amber-500 rounded-full animate-pulse" />
              <span class="text-[9px] text-amber-500 font-bold uppercase tracking-[0.2em]">正在校验中</span>
            </div>
          </div>
        </div>

        <!-- 彩票列表 -->
        <div v-if="(displayMode === 'current' && lotteryTickets.length > 0) || (displayMode === 'previous' && previousTickets.length > 0)" class="grid grid-cols-2 gap-3 relative z-10">
          <div 
            v-for="(ticket, index) in displayMode === 'current' ? (showAllTickets ? lotteryTickets : lotteryTickets.slice(0, 4)) : (showAllTickets ? previousTickets : previousTickets.slice(0, 4))" 
            :key="ticket.ticketNumber" 
            class="ticket-card group relative flex flex-col h-24 cursor-pointer transition-all active:scale-[0.96]"
          >
            <!-- 票面主体 -->
            <div class="relative flex-1 bg-zinc-900/60 border border-white/5 rounded-xl flex flex-col items-center justify-center overflow-hidden">
              <div class="absolute inset-0 bg-gradient-to-br from-amber-500/[0.03] to-transparent pointer-events-none" />
              
              <span class="text-[7px] text-zinc-600 uppercase tracking-[0.2em] font-bold mb-1">Ticket No.</span>
              <span :class="displayMode === 'previous' && ticket.isWinner ? 'text-lg font-mono font-black text-red-500 tracking-widest drop-shadow-[0_0_8px_rgba(239,68,68,0.2)]' : 'text-lg font-mono font-black text-amber-500 tracking-widest drop-shadow-[0_0_8px_rgba(245,158,11,0.2)]'">
                {{ ticket.ticketNumber || '000000' }}
              </span>
              <span class="text-[7px] text-zinc-500 font-mono mt-0.5 opacity-60">{{ new Date(ticket.createdAt || new Date()).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '.') }}</span>
              
              <!-- 装饰虚线 (底部) -->
              <div class="absolute bottom-2 left-2 right-2 border-t border-dashed border-white/5" />
              
              <!-- 流光效果 -->
              <div class="shimmer-effect" />
              
              <!-- 中奖标签 -->
              <div v-if="displayMode === 'previous' && ticket.isWinner" class="absolute top-0 right-0 pointer-events-none">
                <div class="bg-gradient-to-r from-amber-500 to-amber-600 text-white text-[8px] font-black uppercase tracking-widest py-1 px-3 rounded-bl-lg shadow-[0_0_10px_rgba(245,158,11,0.5)] animate-pulse">
                  {{ (ticket.prize || '中奖').replace('中奖（', '').replace('）', '') }}
                </div>
              </div>
              
              <!-- 未中奖标签 -->
              <div v-else-if="displayMode === 'previous' && !ticket.isWinner" class="absolute top-0 right-0 pointer-events-none">
                <div class="bg-gradient-to-r from-zinc-700 to-zinc-800 text-zinc-300 text-[8px] font-bold uppercase tracking-widest py-1 px-3 rounded-bl-lg">
                  未中奖
                </div>
              </div>
            </div>

            <!-- 侧边切口装饰 -->
            <div class="absolute top-1/2 -left-1.5 -translate-y-1/2 w-3 h-3 bg-[#0a0a0b] rounded-full border border-white/5" />
            <div class="absolute top-1/2 -right-1.5 -translate-y-1/2 w-3 h-3 bg-[#0a0a0b] rounded-full border border-white/5" />
            
            <!-- 底部状态栏 -->
            <div class="mt-1 flex items-center justify-center gap-1.5">
              <div class="w-1 h-1 rounded-full" :class="displayMode === 'previous' && !ticket.isWinner ? 'bg-zinc-700/40' : 'bg-amber-500/40'" />
              <span class="text-[7px] text-zinc-500 uppercase tracking-widest font-bold">{{ ticket.status || (displayMode === 'current' ? '待开奖' : '已开奖') }}</span>
            </div>
          </div>
          
          <div v-if="(displayMode === 'current' && lotteryTickets.length > 4) || (displayMode === 'previous' && previousTickets.length > 4)" class="col-span-2 pt-2 text-center">
            <button @click="toggleShowAllTickets" class="text-[9px] text-zinc-600 uppercase tracking-[0.3em] font-bold hover:text-zinc-400 transition-colors">
              {{ showAllTickets ? '收起' : `查看其余 ${displayMode === 'current' ? lotteryTickets.length - 4 : previousTickets.length - 4} 张彩票` }}
            </button>
          </div>
        </div>

        <div v-else class="py-8 flex flex-col items-center justify-center gap-3 opacity-20 relative z-10">
          <div class="w-12 h-12 rounded-full border border-dashed border-zinc-700 flex items-center justify-center">
            <Ticket class="w-6 h-6 text-zinc-600" />
          </div>
          <div class="text-center">
            <p class="text-[11px] uppercase tracking-[0.3em] text-zinc-600 font-black">{{ displayMode === 'current' ? '暂无幸运彩票' : '暂无上期彩票' }}</p>
            <p class="text-[9px] text-zinc-700 mt-1">{{ displayMode === 'current' ? '观看广告即可自动获得幸运彩票' : '上一期没有彩票记录' }}</p>
          </div>
        </div>

        <!-- 开奖结果浮层 -->
        <transition 
          enter-active-class="transition duration-300 ease-out"
          enter-from-class="opacity-0 scale-95"
          enter-to-class="opacity-100 scale-100"
          leave-active-class="transition duration-200 ease-in"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-95"
        >
          <div v-if="result" class="absolute inset-0 z-20 bg-black/80 backdrop-blur-md flex flex-col items-center justify-center text-center p-6">
            <div class="w-16 h-16 rounded-full bg-amber-500/10 flex items-center justify-center mb-4">
              <Trophy v-if="winAmount > 0" class="w-8 h-8 text-amber-400 animate-bounce" />
              <Ticket v-else class="w-8 h-8 text-zinc-600" />
            </div>
            <h4 class="text-2xl font-black tracking-[0.2em] uppercase mb-1" :class="winAmount > 0 ? 'text-amber-400' : 'text-zinc-500'">
              {{ result }}
            </h4>
            <p v-if="winAmount > 0" class="text-lg font-mono text-emerald-400 font-bold">+{{ winAmount.toLocaleString() }} 金币</p>
            <p class="text-[10px] text-zinc-500 font-mono mt-4 tracking-widest">号码: {{ currentTicket }}</p>
            <button @click="result = null" class="mt-6 px-6 py-2 rounded-full bg-white/5 border border-white/10 text-[10px] uppercase tracking-widest text-zinc-400 hover:bg-white/10 transition-colors">
              知道了
            </button>
          </div>
        </transition>

        <!-- 装饰背景 -->
        <div class="absolute -bottom-10 -right-10 w-32 h-32 bg-amber-500/5 blur-3xl rounded-full pointer-events-none" />
      </div>

      <!-- 开奖记录 -->
        <div class="space-y-2">
          <div class="flex justify-between items-center px-2">
            <h3 class="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-bold">往期开奖记录（最近7期）</h3>
            <button 
              @click="showLotteryWinRecordsModal = true"
              class="px-3 py-1 rounded-full bg-white/5 text-[9px] text-amber-500 uppercase tracking-widest hover:bg-white/10 transition-all font-bold border border-amber-500/20"
            >
              我的中奖记录
            </button>
          </div>
        <div class="bg-white/[0.02] rounded-[2rem] border border-white/[0.05] overflow-hidden p-2">
          <div class="divide-y divide-white/[0.03] max-h-[350px] overflow-y-auto scrollbar-thin scrollbar-thumb-white/10 scrollbar-track-transparent">
            <div 
              v-for="(draw, index) in pastDraws.slice(0, 7)" 
              :key="draw.issueNumber || index"
              class="px-6 py-4 flex flex-col items-start hover:bg-white/[0.01] transition-colors"
            >
              <div class="flex justify-between w-full items-center mb-2">
                <span class="text-[13px] font-bold text-amber-400">奖池总金额：{{ (draw.poolAmount || 0).toLocaleString() }} 金币</span>
                <span class="text-[9px] text-zinc-600">{{ draw.drawTime ? new Date(draw.drawTime).toLocaleDateString('zh-CN') : '未知时间' }}</span>
              </div>
              <div class="w-full space-y-1">
                <!-- 处理 winners 是对象的情况 -->
                <div v-if="draw.winners && typeof draw.winners === 'object' && !Array.isArray(draw.winners)">
                  <div v-if="draw.winners.firstPrize && draw.winners.firstPrize.length > 0" class="flex flex-col">
                    <div class="flex items-center gap-0">
                      <span class="text-[12px] text-amber-400 w-12">一等奖：</span>
                      <span class="text-[12px] text-amber-400">{{ (draw.firstPrize || 0).toLocaleString() }} 金币</span>
                    </div>
                    <div v-for="(winner, index) in draw.winners.firstPrize" :key="index" class="flex items-center gap-3 ml-12 mt-1">
                      <span class="text-[10px] text-zinc-400">中奖用户：{{ winner.employeeId || '未知' }}</span>
                      <span class="text-[10px] text-zinc-500">|</span>
                      <span class="text-[10px] text-zinc-400">中奖号码：{{ winner.ticketNumber || '未知' }}</span>
                    </div>
                  </div>
                  <div v-if="draw.winners.secondPrize && draw.winners.secondPrize.length > 0" class="flex flex-col">
                    <div class="flex items-center gap-0">
                      <span class="text-[12px] text-amber-400 w-12">二等奖：</span>
                      <span class="text-[12px] text-amber-400">{{ (draw.secondPrize || 0).toLocaleString() }} 金币</span>
                    </div>
                    <div v-for="(winner, index) in draw.winners.secondPrize" :key="index" class="flex items-center gap-3 ml-12 mt-1">
                      <span class="text-[10px] text-zinc-400">中奖用户：{{ winner.employeeId || '未知' }}</span>
                      <span class="text-[10px] text-zinc-500">|</span>
                      <span class="text-[10px] text-zinc-400">中奖号码：{{ winner.ticketNumber || '未知' }}</span>
                    </div>
                  </div>
                  <div v-if="draw.winners.thirdPrize && draw.winners.thirdPrize.length > 0" class="flex flex-col">
                    <div class="flex items-center gap-0">
                      <span class="text-[12px] text-amber-400 w-12">三等奖：</span>
                      <span class="text-[12px] text-amber-400">{{ (draw.thirdPrize || 0).toLocaleString() }} 金币</span>
                    </div>
                    <div v-for="(winner, index) in draw.winners.thirdPrize" :key="index" class="flex items-center gap-3 ml-12 mt-1">
                      <span class="text-[10px] text-zinc-400">中奖用户：{{ winner.employeeId || '未知' }}</span>
                      <span class="text-[10px] text-zinc-500">|</span>
                      <span class="text-[10px] text-zinc-400">中奖号码：{{ winner.ticketNumber || '未知' }}</span>
                    </div>
                  </div>
                </div>
                <!-- 处理 winners 是数组的情况 -->
                <div v-else-if="draw.winners && Array.isArray(draw.winners)">
                  <div v-for="(winner, index) in draw.winners" :key="index" class="flex flex-col">
                    <div class="flex items-center gap-0">
                      <span class="text-[12px] text-amber-400 w-12">中奖：</span>
                      <span class="text-[12px] text-amber-400">{{ (winner.amount || 0).toLocaleString() }} 金币</span>
                    </div>
                    <div class="flex items-center gap-3 ml-12 mt-1">
                      <span class="text-[10px] text-zinc-400">中奖用户：{{ winner.employeeId || '未知' }}</span>
                      <span class="text-[10px] text-zinc-500">|</span>
                      <span class="text-[10px] text-zinc-400">中奖号码：{{ winner.ticketNumber || '未知' }}</span>
                    </div>
                  </div>
                </div>
                <!-- 没有中奖信息的情况 -->
                <div v-else class="flex items-center gap-0">
                  <span class="text-[12px] text-amber-400 w-12">中奖：</span>
                  <span class="text-[10px] text-zinc-400">暂无中奖信息</span>
                </div>
              </div>
            </div>
            <div v-if="pastDraws.length === 0" class="px-6 py-8 flex justify-center items-center">
              <span class="text-[10px] text-zinc-600 uppercase tracking-widest font-bold">暂无开奖记录</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 开奖规则弹窗 -->
      <transition 
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="opacity-0 scale-95"
        enter-to-class="opacity-100 scale-100"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="opacity-100 scale-100"
        leave-to-class="opacity-0 scale-95"
      >
        <div v-if="showRulesModal" class="fixed inset-0 z-[9999] flex items-center justify-center p-6">
          <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998]" @click="showRulesModal = false" />
          <div class="relative w-full max-w-md bg-[#020205] border border-white/10 rounded-[3rem] overflow-hidden flex flex-col h-[550px] z-[9999] shadow-2xl">
            <!-- 头部 -->
            <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-[#020205] z-10">
              <div class="flex items-center">
                <div class="w-8 h-8 bg-amber-500/20 rounded-full flex items-center justify-center mr-3 border border-amber-500/30">
                  <Trophy class="w-4 h-4 text-amber-400" />
                </div>
                <h3 class="text-sm font-bold uppercase tracking-widest">开奖规则</h3>
              </div>
              <button @click="showRulesModal = false" class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <!-- 内容区域 -->
            <div class="flex-1 overflow-y-auto p-6">
              <div class="space-y-6">
                <!-- 获取规则 -->
                <div class="rounded-xl p-4 bg-white/[0.02] border border-white/5">
                  <h4 class="text-xs font-bold uppercase tracking-wider text-amber-400 mb-3 flex items-center gap-2">
                    <div class="w-2 h-2 bg-amber-500 rounded-full animate-pulse" />
                    彩票获取规则
                  </h4>
                  <ul class="space-y-2 text-xs text-zinc-300">
                    <li class="flex items-start gap-2">
                      <span class="text-blue-400 font-bold">•</span>
                      <span>每观看 <span class="text-amber-400 font-bold">{{ lotterySettings.adCountThreshold }}</span> 个广告自动获得一张幸运彩票</span>
                    </li>
                    <li class="flex items-start gap-2">
                      <span class="text-green-400 font-bold">✓</span>
                      <span>每张彩票包含唯一的 <span class="text-amber-400 font-bold">6</span> 位数字号码</span>
                    </li>
                  </ul>
                </div>
                
                <!-- 开奖规则 -->
                <div class="rounded-xl p-4 bg-white/[0.02] border border-white/5">
                  <h4 class="text-xs font-bold uppercase tracking-wider text-blue-400 mb-3 flex items-center gap-2">
                    <div class="w-2 h-2 bg-blue-500 rounded-full animate-pulse" />
                    开奖规则
                  </h4>
                  <ul class="space-y-2 text-xs text-zinc-300">
                    <li class="flex items-start gap-2">
                      <span class="text-purple-400 font-bold">✦</span>
                      <span>系统每晚 <span class="text-amber-400 font-bold">22:00</span> 自动进行一次开奖</span>
                    </li>
                    <li class="flex items-start gap-2">
                      <span class="text-purple-400 font-bold">✦</span>
                      <span>开奖时处理所有待开奖彩票</span>
                    </li>
                    <li class="flex items-start gap-2">
                      <span class="text-purple-400 font-bold">✦</span>
                      <span>奖池金额由系统设定并持续累积</span>
                    </li>
                  </ul>
                </div>
                
                <!-- 奖金分配 -->
                <div class="rounded-xl p-4 bg-white/[0.02] border border-white/5">
                  <h4 class="text-xs font-bold uppercase tracking-wider text-green-400 mb-3 flex items-center gap-2">
                    <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                    奖金分配规则
                  </h4>
                  <ul class="space-y-2 text-xs text-zinc-300">
                    <li class="flex items-start gap-2">
                      <span class="text-amber-400 font-bold">★</span>
                      <span>一等奖：<span class="text-amber-400 font-bold">{{ Math.round(lotterySettings.firstPrizePercentage * 100) }}%</span>（<span class="text-amber-400 font-bold">{{ lotterySettings.firstPrizeCount }}</span>人，独自分配<span class="text-amber-400 font-bold">{{ Math.round(lotterySettings.firstPrizePercentage * 100) }}%</span>）</span>
                    </li>
                    <li class="flex items-start gap-2">
                      <span class="text-amber-400 font-bold">★</span>
                      <span>二等奖：<span class="text-amber-400 font-bold">{{ Math.round(lotterySettings.secondPrizePercentage * 100) }}%</span>（<span class="text-amber-400 font-bold">{{ lotterySettings.secondPrizeCount }}</span>人，每人分配<span class="text-amber-400 font-bold">{{ Math.round(lotterySettings.secondPrizePercentage * 100 / lotterySettings.secondPrizeCount) }}%</span>）</span>
                    </li>
                    <li class="flex items-start gap-2">
                      <span class="text-amber-400 font-bold">★</span>
                      <span>三等奖：<span class="text-amber-400 font-bold">{{ Math.round(lotterySettings.thirdPrizePercentage * 100) }}%</span>（<span class="text-amber-400 font-bold">{{ lotterySettings.thirdPrizeCount }}</span>人，每人分配<span class="text-amber-400 font-bold">{{ Math.round(lotterySettings.thirdPrizePercentage * 100 / lotterySettings.thirdPrizeCount) }}%</span>）</span>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
            
            <!-- 底部按钮 -->
            <div class="px-8 py-6 border-t border-white/5 bg-[#020205]">
              <button 
                @click="showRulesModal = false"
                class="w-full py-4 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-xs uppercase tracking-widest hover:opacity-90 transition-all active:scale-95 flex items-center justify-center gap-2"
              >
                <Trophy class="w-4 h-4" />
                我知道了
              </button>
            </div>
          </div>
        </div>
      </transition>

      <!-- 中奖记录弹窗 -->
      <transition 
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="opacity-0 scale-95"
        enter-to-class="opacity-100 scale-100"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="opacity-100 scale-100"
        leave-to-class="opacity-0 scale-95"
      >
        <div v-if="showLotteryWinRecordsModal" class="fixed inset-0 z-[9999] flex items-center justify-center p-6">
          <div class="absolute inset-0 bg-black/80 backdrop-blur-md z-[9998]" @click="showLotteryWinRecordsModal = false" />
          <div class="relative w-full max-w-md bg-[#020205] border border-white/10 rounded-[3rem] overflow-hidden flex flex-col h-[550px] z-[9999] shadow-2xl">
            <!-- 头部 -->
            <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-[#020205] z-10">
              <div class="flex items-center">
                <div class="w-8 h-8 bg-amber-500/20 rounded-full flex items-center justify-center mr-3 border border-amber-500/30">
                  <Trophy class="w-4 h-4 text-amber-400" />
                </div>
                <h3 class="text-sm font-bold uppercase tracking-widest">我的中奖记录</h3>
              </div>
              <button @click="showLotteryWinRecordsModal = false" class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <!-- 内容区域 -->
            <div class="flex-1 overflow-y-auto p-6">
              <div v-if="lotteryWinRecords.length === 0" class="py-12 text-center">
                <Trophy class="w-12 h-12 text-zinc-700 mx-auto mb-4" />
                <p class="text-zinc-500 text-sm">暂无中奖记录</p>
                <p class="text-zinc-600 text-xs mt-2">继续努力，下一个中奖的就是你！</p>
              </div>
              <div v-else class="space-y-4">
                <div 
                  v-for="(record, index) in lotteryWinRecords" 
                  :key="record.id"
                  class="rounded-xl p-4 bg-white/[0.02] border border-white/5 hover:bg-white/[0.05] transition-colors"
                >
                  <div class="flex justify-between items-center mb-2">
                    <span class="text-amber-400 font-bold text-lg">+{{ parseFloat(record.amount).toFixed(2) }} 金币</span>
                    <span class="text-zinc-500 text-xs">{{ record.time.split(' ')[0] }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-zinc-400 text-sm">幸运彩票中奖</span>
                    <span class="text-amber-500 text-xs px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 font-bold">{{ record.prizeLevel }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 底部按钮 -->
            <div class="px-8 py-6 border-t border-white/5 bg-[#020205]">
              <button 
                @click="showLotteryWinRecordsModal = false"
                class="w-full py-4 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-xs uppercase tracking-widest hover:opacity-90 transition-all active:scale-95 flex items-center justify-center gap-2"
              >
                <Trophy class="w-4 h-4" />
                我知道了
              </button>
            </div>
          </div>
        </div>
      </transition>
    </main>

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
@keyframes breath {
  0%, 100% {
    transform: scale(1);
    filter: drop-shadow(0 0 0px rgba(245, 158, 11, 0));
  }
  50% {
    transform: scale(1.05);
    filter: drop-shadow(0 0 15px rgba(245, 158, 11, 0.5));
  }
}

@keyframes shimmer {
  0% { transform: translateX(-150%) skewX(-20deg); }
  100% { transform: translateX(150%) skewX(-20deg); }
}

.animate-breath {
  animation: breath 3s ease-in-out infinite;
  display: inline-block;
}

.shimmer-effect {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    to right,
    transparent,
    rgba(255, 255, 255, 0.03),
    rgba(255, 255, 255, 0.08),
    rgba(255, 255, 255, 0.03),
    transparent
  );
  animation: shimmer 3s infinite;
  pointer-events: none;
}

.ticket-card:hover .shimmer-effect {
  animation-duration: 1.5s;
}
</style>