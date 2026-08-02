<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { Lock, User } from 'lucide-vue-next';
import { checkEmployee } from '../api/apiService';

const empId = ref('');
const error = ref('');
const isLoading = ref(false);
const router = useRouter();

const handleLogin = async (e: Event) => {
  e.preventDefault();
  
  // 前端验证
  if (!/^\d{4}$/.test(empId.value)) {
    error.value = '请输入4位数字员工号';
    return;
  }
  
  // 显示加载状态
  isLoading.value = true;
  error.value = '';
  
  try {
    // 调用后端登录校验接口
    const response = await checkEmployee(empId.value);
    
    if (response.success && response.data) {
      // 登录成功，存储员工信息
      localStorage.setItem('empId', empId.value);
      localStorage.setItem('employeeInfo', JSON.stringify(response.data));
      // 使用后端返回的userId，如果没有则直接使用员工号
      const userId = response.data.userId || empId.value;
      localStorage.setItem('userId', userId);
      // 存储token（如果后端返回了token）
      if (response.token) {
        const token = response.token;
        localStorage.setItem('token', token);
      } else {
        // 如果后端没有返回token，使用默认的测试token
        localStorage.setItem('token', 'test-token-1111');
      }
      // 跳转到首页
      router.push('/');
    } else {
      // 登录失败，显示错误信息
      error.value = response.message || '登录失败，请重试';
    }
  } catch (err) {
    // 网络错误或其他异常
    console.error('登录失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    // 隐藏加载状态
    isLoading.value = false;
  }
};

const onInput = (e: Event) => {
  const target = e.target as HTMLInputElement;
  empId.value = target.value.replace(/\D/g, '');
  error.value = '';
};
</script>

<template>
  <div class="min-h-screen bg-[#020205] flex items-center justify-center p-6 relative overflow-hidden">
    <!-- 背景光晕 -->
    <div class="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-emerald-500/10 blur-[120px] rounded-full" />
    <div class="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-600/10 blur-[120px] rounded-full" />

    <div class="w-full max-w-[320px] z-10 flex flex-col items-center justify-center min-h-[400px]">
      <!-- 登录卡片 -->
      <div class="bg-black/40 backdrop-blur-md rounded-2xl p-8 border border-white/10 w-full">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold tracking-tight mb-2 text-emerald-400">
            广告变现系统
          </h1>
          <p class="text-[12px] text-zinc-500 uppercase tracking-[0.2em] font-medium">
            电子手工 · 勤劳致富
          </p>
        </div>

        <form @submit="handleLogin" class="space-y-6">
          <div class="space-y-2">
            <label class="block text-[11px] uppercase tracking-[0.15em] text-zinc-500 ml-1">
              员工工号
            </label>
            <div class="relative">
              <input
                type="text"
                maxlength="4"
                :value="empId"
                @input="onInput"
                autofocus
                placeholder="0000"
                class="w-full bg-zinc-900/80 border border-zinc-800 text-white rounded-xl px-4 py-4 text-xl tracking-[0.3em] text-center outline-none transition-all duration-500 focus:border-emerald-500/50 placeholder:text-zinc-700"
              />
            </div>
            <p v-if="error" class="text-red-400 text-[11px] mt-2 ml-1">
              {{ error }}
            </p>
          </div>

          <button
            type="submit"
            :class="[
              'w-full py-4 rounded-xl font-semibold text-sm transition-all active:scale-[0.98] flex items-center justify-center gap-2',
              isLoading 
                ? 'bg-zinc-700 text-white cursor-not-allowed' 
                : empId.length === 4 
                ? 'bg-emerald-500 text-white shadow-[0_0_20px_rgba(16,185,129,0.3)]' 
                : 'bg-white text-black'
            ]"
            :disabled="isLoading || empId.length !== 4"
          >
            <div v-if="isLoading" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            {{ isLoading ? '登录中...' : '立即登录' }}
          </button>
        </form>
      </div>


    </div>
  </div>
</template>
