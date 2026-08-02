<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Smartphone, Upload, AlertCircle, Coins, Wallet, CreditCard, TrendingUp, Ticket, Gift } from 'lucide-vue-next';
import { getUserGoldInfo, submitVerification, getVerificationRecords, getWelfareLotteryInfo, getCurrentLotteryTickets } from '../api/apiService';

const router = useRouter();
const empId = ref(localStorage.getItem('empId') || '');
const userId = ref(localStorage.getItem('userId') || '');

// 状态管理
const currentMonthGold = ref(0);
const lastMonthGold = ref(0);
const isLoading = ref(false);
const error = ref('');

// 福利抽奖次数
const welfareLotteryChances = ref(0);
const lotteryTicketsCount = ref(0); // 幸运彩票数量

// 核销表单
const invoiceAmount = ref('');
const selectedFile = ref<File | null>(null);
const imagePreviewUrl = ref<string | null>(null);
const isSubmitting = ref(false);
const submitSuccess = ref(false);
const alipayName = ref('');
const alipayAccount = ref('');

// 核销记录
const showVerificationRecordsModal = ref(false);
const verificationRecords = ref([
  { id: '1', amount: 1200, status: '已通过', date: '2024-04-10', alipayName: '测试用户', alipayAccount: 'test@example.com', rejectReason: '' },
  { id: '2', amount: 950, status: '处理中', date: '2024-04-05', alipayName: '测试用户', alipayAccount: 'test@example.com', rejectReason: '' },
  { id: '3', amount: 1500, status: '已通过', date: '2024-03-28', alipayName: '测试用户', alipayAccount: 'test@example.com', rejectReason: '' },
]);

// 计算可用核销金币
const availableVerificationGold = computed(() => {
  return currentMonthGold.value + lastMonthGold.value;
});

// 计算可核销金额
const maxVerificationAmount = computed(() => {
  return (availableVerificationGold.value / 1000).toFixed(2);
});

// 检查是否可以核销
const canVerify = computed(() => {
  if (!invoiceAmount.value) return false;
  const amount = parseFloat(invoiceAmount.value);
  if (isNaN(amount) || amount <= 0) return false;
  if (amount > 1500) return false;
  if (!selectedFile.value) return false;
  if (!alipayName.value || alipayName.value.trim() === '') return false;
  if (!alipayAccount.value || alipayAccount.value.trim() === '') return false;
  return amount <= parseFloat(maxVerificationAmount.value);
});

// 加载用户金币信息
const loadUserInfo = async () => {
  isLoading.value = true;
  error.value = '';
  
  try {
    const response = await getUserGoldInfo();
    if (response.success && response.data) {
      currentMonthGold.value = Number(response.data.currentMonthGold) || 0;
      lastMonthGold.value = Number(response.data.lastMonthGold) || 0;
    } else {
      error.value = response.message || '获取金币信息失败';
    }
  } catch (err) {
    console.error('获取金币信息失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 处理文件选择
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    selectedFile.value = target.files[0];
    // 生成预览URL
    if (selectedFile.value.type.startsWith('image/')) {
      imagePreviewUrl.value = URL.createObjectURL(selectedFile.value);
    } else {
      imagePreviewUrl.value = null;
    }
  }
};

// 处理删除文件
const handleRemoveFile = () => {
  // 释放预览URL
  if (imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value);
  }
  selectedFile.value = null;
  imagePreviewUrl.value = null;
  // 重置文件输入
  const fileInput = document.getElementById('invoice-file') as HTMLInputElement;
  if (fileInput) {
    fileInput.value = '';
  }
};

// 打开核销记录模态框
const openVerificationRecordsModal = async () => {
  showVerificationRecordsModal.value = true;
  
  try {
    const response = await getVerificationRecords(1, 20);
    if (response.success && response.data) {
      verificationRecords.value = response.data.records.map(record => ({
        ...record,
        alipayName: record.alipayName || '未知',
        alipayAccount: record.alipayAccount || '未知',
        rejectReason: record.rejectReason || ''
      }));
    }
  } catch (err) {
    console.error('获取核销记录失败:', err);
  }
};



// 处理核销提交
const handleVerifySubmit = async () => {
  if (!canVerify.value || !selectedFile.value) return;
  
  isSubmitting.value = true;
  error.value = '';
  
  try {
    const amount = parseFloat(invoiceAmount.value);
    const response = await submitVerification(amount, selectedFile.value, alipayName.value, alipayAccount.value);
    
    if (response.success) {
      submitSuccess.value = true;
      
      // 3秒后重置表单并重新加载金币信息
      setTimeout(async () => {
        submitSuccess.value = false;
        invoiceAmount.value = '';
        selectedFile.value = null;
        imagePreviewUrl.value = null;
        alipayName.value = '';
        alipayAccount.value = '';
        // 重置文件输入
        const fileInput = document.getElementById('invoice-file') as HTMLInputElement;
        if (fileInput) {
          fileInput.value = '';
        }
        // 重新加载金币信息
        await loadUserInfo();
      }, 3000);
    } else {
      error.value = response.message || '提交失败，请稍后重试';
    }
  } catch (err) {
    console.error('核销提交失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    isSubmitting.value = false;
  }
};

// 复制到剪贴板
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    // 可以添加一个简单的提示，比如显示"复制成功"
    console.log('复制成功:', text);
  } catch (err) {
    console.error('复制失败:', err);
    // 降级处理：使用传统的复制方法
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    try {
      document.execCommand('copy');
      console.log('复制成功:', text);
    } catch (err) {
      console.error('复制失败:', err);
    }
    document.body.removeChild(textarea);
  }
};

// 格式化时间为北京时间
const formatLocalTime = (time: string) => {
  if (!time) return '';
  
  try {
    // 创建Date对象
    const date = new Date(time);
    
    // 检查是否为有效日期
    if (isNaN(date.getTime())) {
      console.error('无效的日期格式:', time);
      return time;
    }
    
    // 直接使用本地时间（浏览器会自动处理时区）
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  } catch (err) {
    console.error('时间格式化失败:', err);
    return time;
  }
};

// 处理登出
const handleLogout = () => {
  localStorage.removeItem('empId');
  localStorage.removeItem('userId');
  localStorage.removeItem('employeeInfo');
  router.push('/login');
};

onMounted(async () => {
  if (!empId.value || !userId.value) {
    router.push('/login');
    return;
  }
  
  await loadUserInfo();
  await loadWelfareLotteryChances();
  await loadLotteryTicketsCount();
});

// 加载福利抽奖次数
const loadWelfareLotteryChances = async () => {
  if (!empId.value) return;
  
  try {
    const response = await getWelfareLotteryInfo(empId.value);
    if (response.success && response.data) {
      welfareLotteryChances.value = Number(response.data.chances) || 0;
    }
  } catch (error) {
    console.error('加载福利抽奖次数失败:', error);
  }
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
</script>

<template>
  <div class="min-h-screen bg-[#020205] text-white pb-12 relative overflow-hidden">
    <!-- 背景装饰光晕 -->
    <div class="absolute top-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-600/20 blur-[120px] rounded-full pointer-events-none" />
    <div class="absolute top-[20%] left-[-10%] w-[50%] h-[50%] bg-pink-600/20 blur-[120px] rounded-full pointer-events-none" />
    <div class="absolute bottom-[-10%] left-[20%] w-[40%] h-[40%] bg-purple-500/10 blur-[120px] rounded-full pointer-events-none" />

    <!-- Header -->
    <header class="bg-black/40 backdrop-blur-xl border-b border-white/5 pt-8 pb-5 px-6 flex justify-between items-center sticky top-0 z-20">
      <div class="flex items-center">
        <div class="w-8 h-8 bg-gradient-to-br from-purple-500 to-pink-600 rounded-xl flex items-center justify-center shadow-lg shadow-purple-500/20 mr-3">
          <Smartphone class="text-white w-5 h-5" />
        </div>
        <div class="flex flex-col">
            <span class="font-bold text-sm tracking-widest uppercase bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">手机核销</span>
            <span class="text-[10px] text-zinc-400 font-bold tracking-wider">提前打款，减轻资金负担</span>
          </div>
      </div>
      <button @click="handleLogout" class="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center text-zinc-500 hover:text-white hover:bg-white/10 transition-all">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
        </svg>
      </button>
    </header>

    <main class="max-w-md mx-auto px-6 mt-6 space-y-6 relative z-10 pb-24">
      <!-- 金币统计 -->
      <div class="space-y-3">
        <div class="flex justify-between items-end px-2">
          <h2 class="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-medium">金币统计</h2>
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
        </div>
        
        <!-- 金币统计数据 -->
        <div v-else class="flex justify-center">
          <div class="group relative bg-gradient-to-br from-purple-500 to-pink-600 rounded-[1.25rem] shadow-xl shadow-purple-500/20 overflow-hidden transition-all hover:scale-[1.02] w-full max-w-xs">
            <div class="absolute top-0 right-0 w-24 h-24 bg-white/10 blur-2xl rounded-full -mr-12 -mt-12" />
            <div class="p-5 text-center">
              <p class="text-purple-100/80 text-[9px] uppercase tracking-wider mb-1">可用核销金币</p>
              <p class="text-2xl font-bold text-white tracking-tight mb-1">{{ Math.floor(availableVerificationGold).toLocaleString() }}</p>
              <p class="text-purple-200/90 text-lg font-semibold">可核销金额≈{{ maxVerificationAmount }}元</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 核销表单 -->
      <div class="space-y-6">
        <div class="flex items-center justify-between px-2">
          <h2 class="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-medium">手机核销</h2>
          <button 
            @click="openVerificationRecordsModal" 
            class="px-2 py-0.5 rounded-md text-[7px] font-bold uppercase tracking-wider transition-all border flex items-center justify-center gap-1 bg-purple-500/20 text-purple-400 hover:bg-purple-500/30 border-purple-500/30 cursor-pointer"
          >
            <CreditCard class="w-2 h-2" />
            核销记录
          </button>
        </div>
        
        <div class="glass-card rounded-[1.25rem] p-6 space-y-6">
          <!-- 发票金额 -->
          <div class="space-y-2">
            <label class="text-[10px] text-amber-400 uppercase tracking-wider">发票金额（元）</label>
            <input 
              v-model="invoiceAmount"
              type="number"
              step="0.01"
              min="0"
              placeholder="请输入发票金额"
              class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500/50 transition-all"
            />
            <p class="text-[10px] text-zinc-600" v-if="invoiceAmount">
              需消耗金币：{{ (parseFloat(invoiceAmount) * 1000).toFixed(0) }}
            </p>
            <p class="text-[10px] text-red-400" v-if="invoiceAmount && parseFloat(invoiceAmount) > 1500">
              发票金额不能超过1500元
            </p>
            <p class="text-[10px] text-red-400" v-if="invoiceAmount && parseFloat(invoiceAmount) <= 1500 && parseFloat(invoiceAmount) > parseFloat(maxVerificationAmount)">
              发票金额超过可核销金额
            </p>
          </div>
          
          <!-- 发票上传 -->
          <div class="space-y-2">
            <label class="text-[10px] text-amber-400 uppercase tracking-wider">上传发票</label>
            <div class="border-2 border-dashed border-white/20 rounded-xl p-6 text-center hover:border-amber-500/50 transition-all cursor-pointer">
              <input 
                type="file"
                accept=".jpg,.jpeg,.png,.pdf"
                class="hidden"
                id="invoice-file"
                @change="handleFileChange"
              />
              <label for="invoice-file" class="cursor-pointer">
                <template v-if="!selectedFile">
                  <Upload class="w-10 h-10 text-zinc-500 mx-auto mb-3" />
                  <p class="text-sm text-zinc-400 mb-2">点击上传发票</p>
                  <p class="text-xs text-zinc-600">支持 JPG、PNG、PDF 格式</p>
                </template>
                <template v-else-if="imagePreviewUrl">
                  <div class="relative w-full max-w-xs mx-auto">
                    <img :src="imagePreviewUrl" class="w-full h-auto rounded-lg object-cover mb-2" />
                    <div class="flex items-center justify-between mt-2">
                      <p class="text-xs text-emerald-400">{{ selectedFile.name }}</p>
                      <button 
                        @click.stop="handleRemoveFile" 
                        class="text-xs text-red-400 hover:text-red-300 transition-colors"
                      >
                        删除
                      </button>
                    </div>
                  </div>
                </template>
                <template v-else-if="selectedFile">
                  <div class="flex items-center justify-center mt-2">
                    <p class="text-xs text-emerald-400 mr-2">{{ selectedFile.name }}</p>
                    <button 
                      @click.stop="handleRemoveFile" 
                      class="text-xs text-red-400 hover:text-red-300 transition-colors"
                    >
                      删除
                    </button>
                  </div>
                </template>
              </label>
            </div>
          </div>
          
          <!-- 核销说明 -->
          <div class="p-4 bg-amber-500/10 border border-amber-500/20 rounded-lg">
            <div class="flex items-start">
              <AlertCircle class="w-4 h-4 text-amber-400 mr-2 mt-0.5 flex-shrink-0" />
              <div class="text-sm text-amber-200 space-y-2">
                <p>1. 发票抬头必须与页面最下方显示的开票信息一致，否则无效</p>
                <p>2. 请确保上传的发票真实有效</p>
                <p>3. 发票金额不能超过1500元</p>
                <p>4. 提交后，财务将在3个工作日内处理</p>
                <p>5. 核销成功后，相应的金币将被扣除</p>
              </div>
            </div>
          </div>
          
          <!-- 支付宝信息 -->
          <div class="space-y-4">
            <!-- 支付宝姓名 -->
            <div class="space-y-2">
              <label class="text-[10px] text-amber-400 uppercase tracking-wider">收款人支付宝姓名</label>
              <input 
                v-model="alipayName"
                type="text"
                placeholder="请输入收款人支付宝姓名"
                class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500/50 transition-all"
              />
            </div>
            
            <!-- 支付宝账号 -->
            <div class="space-y-2">
              <label class="text-[10px] text-amber-400 uppercase tracking-wider">收款人支付宝帐号</label>
              <input 
                v-model="alipayAccount"
                type="text"
                placeholder="请输入收款人支付宝帐号"
                class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500/50 transition-all"
              />
            </div>
          </div>
          
          <!-- 提交按钮 -->
          <button 
            @click="handleVerifySubmit"
            :disabled="!canVerify || isSubmitting"
            class="w-full font-bold py-4 rounded-xl transition-all text-white"
            :class="!canVerify || isSubmitting ? 'bg-zinc-800 cursor-not-allowed' : 'bg-gradient-to-r from-amber-500 to-orange-500 hover:shadow-[0_0_20px_rgba(245,158,11,0.4)]'"
          >
            {{ isSubmitting ? '提交中...' : '申请核销' }}
          </button>
          
          <!-- 成功提示 -->
          <div v-if="submitSuccess" class="p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-center">
            <div class="w-12 h-12 bg-emerald-500/20 rounded-full flex items-center justify-center mx-auto mb-3 border border-emerald-500/30">
              <svg class="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <p class="text-emerald-400 text-sm">核销申请已提交</p>
            <p class="text-zinc-400 text-xs mt-1">财务将在3个工作日内处理</p>
          </div>
        </div>
      </div>

      <!-- 开票信息 -->
      <div class="glass-card rounded-[1.25rem] p-6">
        <div class="text-sm text-amber-400 font-medium mb-3">开票信息</div>
        <div class="text-sm text-zinc-400 space-y-2">
          <div class="flex items-center justify-between">
            <div class="flex items-center flex-1">
              <span class="text-zinc-600 mr-2 w-12">抬头：</span>
              <span class="text-zinc-300">光年跃迁（温州）科技有限公司</span>
            </div>
            <button 
              @click="copyToClipboard('光年跃迁（温州）科技有限公司')"
              class="ml-2 px-2 py-1 bg-white/5 hover:bg-white/10 rounded-lg text-zinc-400 hover:text-white transition-all flex items-center"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
              </svg>
            </button>
          </div>
          <div class="flex items-center justify-between">
            <div class="flex items-center flex-1">
              <span class="text-zinc-600 mr-2 w-12">税号：</span>
              <span class="text-zinc-300 font-mono">91330383MAK7800T0E</span>
            </div>
            <button 
              @click="copyToClipboard('91330383MAK7800T0E')"
              class="ml-2 px-2 py-1 bg-white/5 hover:bg-white/10 rounded-lg text-zinc-400 hover:text-white transition-all flex items-center"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </main>

    <!-- 核销记录模态框 -->
    <div v-if="showVerificationRecordsModal" class="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center z-50 p-4">
      <div class="bg-zinc-900 rounded-2xl w-full max-w-md p-6 border border-white/10">
        <div class="flex justify-between items-center mb-6">
          <h3 class="text-lg font-bold text-white">核销记录</h3>
          <button @click="showVerificationRecordsModal = false" class="text-zinc-400 hover:text-white transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="space-y-3 max-h-[60vh] overflow-y-auto">
          <div v-for="record in verificationRecords" :key="record.id" class="p-4 bg-white/5 rounded-lg border border-white/10 hover:bg-white/10 transition-all duration-300">
            <div class="flex justify-between items-start mb-3">
              <div class="flex flex-col">
                <span class="text-sm font-medium text-white mb-2">{{ formatLocalTime(record.date) }}</span>
                <span 
                  class="text-xs font-bold px-3 py-1 rounded-full w-fit"
                  :class="{
                    'bg-emerald-500/20 text-emerald-400': record.status === '已通过' || record.status === '已打款' || record.status === '已处理' || record.status === 'approved',
                    'bg-amber-500/20 text-amber-400': record.status === '待处理' || record.status === 'pending',
                    'bg-red-500/20 text-red-400': record.status === '已拒绝' || record.status === 'rejected'
                  }"
                >
                  {{ record.status === 'pending' ? '待处理' : record.status === 'rejected' ? '已拒绝' : record.status === '已处理' ? '已打款' : record.status === 'approved' ? '已打款' : record.status }}
                </span>
              </div>
              <div class="text-right">
                <span class="text-zinc-400 text-xs">核销金额</span>
                <p class="text-lg font-bold text-amber-400 mt-1">¥{{ record.amount }}</p>
              </div>
            </div>
            <div class="flex flex-col space-y-1 mt-2">
              <div class="text-xs text-white">
                收款人：{{ record.alipayName || '未知' }}
              </div>
              <div class="text-xs text-white">
                支付宝账号：{{ record.alipayAccount || '未知' }}
              </div>
              <div v-if="record.status === '已拒绝' || record.status === 'rejected'" class="text-xs text-red-400 mt-2">
                拒绝原因：{{ record.rejectReason || '暂无原因' }}
              </div>
            </div>
          </div>
          <div v-if="verificationRecords.length === 0" class="text-center py-8 text-zinc-500 text-sm">
            暂无核销记录
          </div>
        </div>
        <button 
          @click="showVerificationRecordsModal = false" 
          class="mt-6 w-full py-3 rounded-xl bg-zinc-800 text-white font-medium hover:bg-zinc-700 transition-colors"
        >
          关闭
        </button>
      </div>
    </div>

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
.glass-card {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.05);
}
</style>