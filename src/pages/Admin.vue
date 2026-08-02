<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-900 to-black text-white p-4 md:p-8">
    <div class="max-w-6xl mx-auto">
      <!-- 顶部导航 -->
      <div class="flex justify-between items-center mb-8">
        <h1 class="text-2xl font-bold text-amber-400">管理员控制台</h1>
        <button 
          @click="handleLogout"
          class="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg transition-colors"
        >
          退出登录
        </button>
      </div>

      <!-- 功能导航 -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div 
          class="glass-card p-6 rounded-xl hover:scale-[1.02] transition-all cursor-pointer"
          @click="activeTab = 'red-packet'"
        >
          <h3 class="text-lg font-semibold mb-2">红包记录管理</h3>
          <p class="text-gray-400 text-sm">查看所有红包发放记录</p>
        </div>
        <div 
          class="glass-card p-6 rounded-xl hover:scale-[1.02] transition-all cursor-pointer"
          @click="activeTab = 'users'"
        >
          <h3 class="text-lg font-semibold mb-2">用户管理</h3>
          <p class="text-gray-400 text-sm">管理系统用户</p>
        </div>
        <div 
          class="glass-card p-6 rounded-xl hover:scale-[1.02] transition-all cursor-pointer"
          @click="activeTab = 'settings'"
        >
          <h3 class="text-lg font-semibold mb-2">系统设置</h3>
          <p class="text-gray-400 text-sm">配置系统参数</p>
        </div>
      </div>

      <!-- 红包记录管理 -->
      <div v-if="activeTab === 'red-packet'" class="glass-card p-6 rounded-xl">
        <div class="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
          <h2 class="text-xl font-bold">红包发放记录</h2>
          <div class="flex gap-2 w-full md:w-auto">
            <input 
              v-model="employeeIdFilter"
              type="text"
              placeholder="按员工ID筛选"
              class="px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
            <button 
              @click="loadRedPacketRecords"
              class="px-4 py-2 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors"
            >
              搜索
            </button>
          </div>
        </div>

        <!-- 记录列表 -->
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-700">
                <th class="py-3 px-4 text-left text-gray-400">用户ID</th>
                <th class="py-3 px-4 text-left text-gray-400">员工ID</th>
                <th class="py-3 px-4 text-left text-gray-400">红包金额</th>
                <th class="py-3 px-4 text-left text-gray-400">发放后红包池余额</th>
                <th class="py-3 px-4 text-left text-gray-400">发放时间</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="record in redPacketRecords" 
                :key="record._id"
                class="border-b border-gray-800 hover:bg-gray-800/50 transition-colors"
              >
                <td class="py-3 px-4">{{ record.userId }}</td>
                <td class="py-3 px-4">{{ record.employeeId }}</td>
                <td class="py-3 px-4 text-amber-400 font-semibold">{{ record.amount }} 金币</td>
                <td class="py-3 px-4">{{ record.poolBalanceAfter.toFixed(2) }} 金币</td>
                <td class="py-3 px-4 text-gray-400">{{ formatDate(record.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 空状态 -->
        <div v-if="redPacketRecords.length === 0 && !isLoading" class="text-center py-12 text-gray-500">
          暂无红包发放记录
        </div>

        <!-- 加载状态 -->
        <div v-if="isLoading" class="text-center py-12">
          <div class="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-amber-500 mx-auto"></div>
          <p class="mt-2 text-gray-400">加载中...</p>
        </div>

        <!-- 分页 -->
        <div v-if="!isLoading && pagination.total > 0" class="mt-6 flex justify-between items-center">
          <p class="text-gray-400 text-sm">
            共 {{ pagination.total }} 条记录，第 {{ pagination.page }}/{{ pagination.pages }} 页
          </p>
          <div class="flex gap-2">
            <button 
              @click="changePage(1)"
              :disabled="pagination.page === 1"
              class="px-3 py-1 bg-gray-800 hover:bg-gray-700 rounded disabled:opacity-50 disabled:cursor-not-allowed"
            >
              首页
            </button>
            <button 
              @click="changePage(pagination.page - 1)"
              :disabled="pagination.page === 1"
              class="px-3 py-1 bg-gray-800 hover:bg-gray-700 rounded disabled:opacity-50 disabled:cursor-not-allowed"
            >
              上一页
            </button>
            <button 
              @click="changePage(pagination.page + 1)"
              :disabled="pagination.page === pagination.pages"
              class="px-3 py-1 bg-gray-800 hover:bg-gray-700 rounded disabled:opacity-50 disabled:cursor-not-allowed"
            >
              下一页
            </button>
            <button 
              @click="changePage(pagination.pages)"
              :disabled="pagination.page === pagination.pages"
              class="px-3 py-1 bg-gray-800 hover:bg-gray-700 rounded disabled:opacity-50 disabled:cursor-not-allowed"
            >
              末页
            </button>
          </div>
        </div>
      </div>

      <!-- 用户管理 -->
      <div v-else-if="activeTab === 'users'" class="glass-card p-6 rounded-xl">
        <h2 class="text-xl font-bold mb-6">用户管理</h2>
        <p class="text-gray-400">用户管理功能开发中...</p>
      </div>

      <!-- 系统设置 -->
      <div v-else-if="activeTab === 'settings'" class="glass-card p-6 rounded-xl">
        <h2 class="text-xl font-bold mb-6">系统设置</h2>
        <p class="text-gray-400">系统设置功能开发中...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getRedPacketRecords } from '../api/apiService';

const router = useRouter();
const activeTab = ref('red-packet');
const employeeIdFilter = ref('');
const redPacketRecords = ref<any[]>([]);
const isLoading = ref(false);
const pagination = ref({
  total: 0,
  page: 1,
  limit: 20,
  pages: 1
});

// 加载红包记录
const loadRedPacketRecords = async () => {
  isLoading.value = true;
  try {
    const response = await getRedPacketRecords(
      employeeIdFilter.value || undefined,
      pagination.value.page,
      pagination.value.limit
    );
    if (response.success && response.data) {
      redPacketRecords.value = response.data.records;
      pagination.value = response.data.pagination;
    }
  } catch (error) {
    console.error('加载红包记录失败:', error);
  } finally {
    isLoading.value = false;
  }
};

// 切换分页
const changePage = (page: number) => {
  if (page >= 1 && page <= pagination.value.pages) {
    pagination.value.page = page;
    loadRedPacketRecords();
  }
};

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('adminToken');
  router.push('/login');
};

// 检查管理员登录状态
const checkAdminLogin = () => {
  const adminToken = localStorage.getItem('adminToken');
  if (!adminToken) {
    router.push('/login');
  }
};

// 页面挂载时
onMounted(() => {
  checkAdminLogin();
  loadRedPacketRecords();
});
</script>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}
</style>