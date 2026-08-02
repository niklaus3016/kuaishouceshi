<template>
  <div class="min-h-screen bg-gradient-to-b from-zinc-900 via-zinc-800 to-zinc-900 text-white">
    <!-- 头部导航 -->
    <header class="sticky top-0 z-50 bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800">
      <div class="container mx-auto px-4 py-3 flex items-center justify-between">
        <button @click="goBack" class="flex items-center text-zinc-400 hover:text-white transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          返回
        </button>
        <h1 class="text-lg font-bold text-center flex-1">抽奖详情</h1>
        <div class="w-8"></div> <!-- 占位，保持标题居中 -->
      </div>
    </header>

    <main class="container mx-auto px-4 py-6">
      <!-- 今日奖金池 -->
      <section class="mb-8">
        <div class="relative">
          <div class="absolute inset-0 bg-gradient-to-r from-yellow-500 to-amber-500 blur-xl opacity-20 rounded-2xl animate-pulse"></div>
          <div class="relative glass-card rounded-2xl p-5 border border-yellow-500/30">
            <div class="flex flex-col items-center mb-4">
              <div class="flex items-center mb-3">
                <Trophy class="w-6 h-6 text-yellow-500 mr-3" />
                <h2 class="text-lg font-bold text-white">今日奖金池</h2>
              </div>
              <p class="text-3xl font-bold text-white text-center animate-pulse">
                <span class="animate-bounce">₸</span>
                {{ poolStatus.lotteryPool.toLocaleString() }}
                <span class="animate-bounce ml-1">金币</span>
              </p>
            </div>
            <div class="flex items-center justify-center text-[10px] text-zinc-500">
              <span>每晚10点开奖</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 用户奖券 -->
      <section class="mb-8">
        <h3 class="text-lg font-bold mb-4 flex items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          我的奖券
        </h3>
        <div v-if="userTickets.length > 0" class="grid grid-cols-2 gap-3">
          <div v-for="(ticket, index) in userTickets" :key="ticket.id || index" class="glass-card rounded-xl p-4 border border-yellow-500/20">
            <div class="flex justify-between items-start mb-2">
              <span class="text-sm font-medium text-zinc-400">奖券号码</span>
              <span class="text-xs text-yellow-400 bg-yellow-500/10 px-2 py-0.5 rounded">
                {{ ticket.generatedAt || '今日' }}
              </span>
            </div>
            <p class="text-xl font-bold text-white">{{ ticket.ticketNumber || 'N/A' }}</p>
            <p class="text-xs text-zinc-500 mt-2">通过观看广告获得</p>
          </div>
        </div>
        <div v-else class="glass-card rounded-xl p-6 border border-zinc-800 flex flex-col items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-zinc-700 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p class="text-zinc-500 text-sm">暂无奖券</p>
          <p class="text-zinc-600 text-xs mt-2">每观看100个广告可获得1张奖券</p>
        </div>
      </section>

      <!-- 抽奖规则 -->
      <section class="mb-8">
        <h3 class="text-lg font-bold mb-4 flex items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          抽奖规则
        </h3>
        <div class="glass-card rounded-xl p-5 border border-zinc-800">
          <ul class="space-y-3 text-sm text-zinc-300">
            <li class="flex items-start">
              <span class="bg-blue-500/20 text-blue-400 text-xs font-bold px-2 py-1 rounded mr-3 mt-0.5">1</span>
              <span>每观看100个广告可获得1张抽奖券</span>
            </li>
            <li class="flex items-start">
              <span class="bg-blue-500/20 text-blue-400 text-xs font-bold px-2 py-1 rounded mr-3 mt-0.5">2</span>
              <span>每晚10点系统自动开奖</span>
            </li>
            <li class="flex items-start">
              <span class="bg-blue-500/20 text-blue-400 text-xs font-bold px-2 py-1 rounded mr-3 mt-0.5">3</span>
              <span>中奖结果将通过系统通知发送</span>
            </li>
            <li class="flex items-start">
              <span class="bg-blue-500/20 text-blue-400 text-xs font-bold px-2 py-1 rounded mr-3 mt-0.5">4</span>
              <span>中奖金币将直接发放到账户</span>
            </li>
          </ul>
        </div>
      </section>

      <!-- 往期开奖记录 -->
      <section class="mb-8">
        <h3 class="text-lg font-bold mb-4 flex items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          往期开奖记录
        </h3>
        <div v-if="lotteryHistory.length > 0" class="space-y-3">
          <div v-for="(record, index) in lotteryHistory" :key="record.id || index" class="glass-card rounded-xl p-4 border border-zinc-800">
            <div class="flex justify-between items-center mb-3">
              <span class="text-sm font-medium text-zinc-400">{{ record.date || '2024-01-01' }}</span>
              <span class="text-xs text-purple-400 bg-purple-500/10 px-2 py-0.5 rounded">
                已开奖
              </span>
            </div>
            <div class="space-y-2">
              <div class="flex justify-between items-center">
                <span class="text-xs text-zinc-500">一等奖</span>
                <span class="text-sm font-medium text-yellow-400">{{ record.first || 'N/A' }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-xs text-zinc-500">二等奖</span>
                <span class="text-sm font-medium text-zinc-300">{{ record.second || 'N/A' }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-xs text-zinc-500">三等奖</span>
                <span class="text-sm font-medium text-zinc-300">{{ record.third || 'N/A' }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="glass-card rounded-xl p-6 border border-zinc-800 flex flex-col items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-zinc-700 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p class="text-zinc-500 text-sm">暂无开奖记录</p>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Trophy } from 'lucide-vue-next';
import { getPoolStatus, getUserTickets } from '../api/apiService';

const router = useRouter();
const poolStatus = ref({ redPacketPool: 0, lotteryPool: 0 });
const userTickets = ref<any[]>([]);
const lotteryHistory = ref<any[]>([]);
const isLoading = ref(false);

// 返回上一页
const goBack = () => {
  router.back();
};

// 加载奖金池状态
const loadPoolStatus = async () => {
  try {
    const response = await getPoolStatus();
    if (response.success && response.data) {
      poolStatus.value = response.data;
    }
  } catch (error) {
    console.error('加载奖金池状态失败:', error);
  }
};

// 加载用户奖券
const loadUserTickets = async () => {
  try {
    const empId = localStorage.getItem('empId') || '';
    if (empId) {
      const response = await getUserTickets(empId);
      if (response.success && response.data) {
        userTickets.value = response.data;
      }
    }
  } catch (error) {
    console.error('加载用户奖券失败:', error);
  }
};

// 加载开奖记录（模拟数据）
const loadLotteryHistory = () => {
  // 模拟数据
  lotteryHistory.value = [
    {
      date: '2024-01-20',
      first: '888888',
      second: '777777',
      third: '666666'
    },
    {
      date: '2024-01-19',
      first: '555555',
      second: '444444',
      third: '333333'
    },
    {
      date: '2024-01-18',
      first: '222222',
      second: '111111',
      third: '000000'
    }
  ];
};

onMounted(async () => {
  isLoading.value = true;
  await loadPoolStatus();
  await loadUserTickets();
  loadLotteryHistory();
  isLoading.value = false;
});
</script>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
</style>