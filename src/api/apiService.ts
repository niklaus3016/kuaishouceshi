// API服务层，封装后端接口调用

const API_BASE_URL = 'https://wfqmaepvjkdd.sealoshzh.site'; // 生产环境后端服务地址
const USE_MOCK_DATA = false; // 使用真实后端API

// 默认超时时间（毫秒）
const DEFAULT_TIMEOUT = 8000;

/**
 * 带超时的fetch包装函数
 * @param url 请求URL
 * @param options 请求选项
 * @param timeout 超时时间（毫秒），默认8秒
 * @returns Promise<Response>
 */
async function fetchWithTimeout(
  url: string,
  options: RequestInit = {},
  timeout: number = DEFAULT_TIMEOUT
): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => {
    console.warn(`⚠️ 请求超时: ${url}`);
    controller.abort();
  }, timeout);

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
    });
    clearTimeout(timeoutId);
    return response;
  } catch (error) {
    clearTimeout(timeoutId);
    if ((error as Error).name === 'AbortError') {
      throw new Error('请求超时');
    }
    throw error;
  }
}

/**
 * API请求封装，带统一的错误处理和超时管理
 * @param url 请求路径（相对于API_BASE_URL）
 * @param options 请求选项
 * @param timeout 超时时间
 * @returns Promise<ApiResponse<T>>
 */
async function apiRequest<T = any>(
  url: string,
  options: RequestInit = {},
  timeout: number = DEFAULT_TIMEOUT
): Promise<ApiResponse<T>> {
  try {
    const fullUrl = url.startsWith('http') ? url : `${API_BASE_URL}${url}`;
    
    const response = await fetchWithTimeout(fullUrl, options, timeout);
    
    if (!response.ok) {
      console.error(`API请求失败: ${fullUrl}, 状态码: ${response.status}`);
      return {
        success: false,
        message: `服务器响应错误: ${response.statusText}`,
      };
    }

    try {
      const data = await response.json();
      return data as ApiResponse<T>;
    } catch (jsonError) {
      console.error('解析响应体失败:', jsonError);
      return {
        success: false,
        message: '服务器返回的数据格式错误',
      };
    }
  } catch (error) {
    console.error('API请求异常:', error);
    const errorMessage = (error as Error).message === '请求超时' 
      ? '请求超时，请稍后重试' 
      : '网络错误，请稍后重试';
    return {
      success: false,
      message: errorMessage,
    };
  }
}

interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
  token?: string;
}

interface EmployeeInfo {
  _id: string;
  userId: string;
  employeeId: string;
  name: string;
  phone: string;
  area: string;
  status: number;
  __v: number;
}

interface UserInfo {
  userId: string;
  employeeId: string;
  currentMonthGold: number;
  lastMonthGold: number;
  todayTarget: number;
  bonusGold: number;
  hasClaimedBonus: boolean;
  _id: string;
  __v: number;
}

interface GoldReward {
  gold: number;
  currentMonthGold: number;
  hasRedPacket?: boolean;
  redPacketAmount?: number;
  ticketNumber?: string;
  issueNumber?: string;
}

interface GoldLog {
  _id: string;
  userId: string;
  employeeId: string;
  deviceId: string;
  ecpm: number;
  gold: number;
  createTime: string;
  __v: number;
}

/**
 * 员工登录校验接口
 * @param employeeId 员工号
 * @returns 员工信息或错误信息
 */
export async function checkEmployee(employeeId: string): Promise<ApiResponse<EmployeeInfo>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      // 模拟网络延迟
      setTimeout(() => {
        // 模拟员工号验证逻辑
        if (employeeId === '8202') {
          // 测试员工，登录成功
          resolve({
            success: true,
            message: '登录成功',
            data: {
              _id: '699c87e89ad7757d16c8b9e1',
              userId: 'test123',
              employeeId: '8202',
              name: '测试员工',
              phone: '13800138000',
              area: '北京',
              status: 1,
              __v: 0
            }
          });
        } else if (employeeId === '1111') {
          // 测试员工，登录成功
          resolve({
            success: true,
            message: '登录成功',
            data: {
              _id: '699c87e89ad7757d16c8b9e2',
              userId: 'user_1111',
              employeeId: '1111',
              name: '测试员工1111',
              phone: '13800138001',
              area: '上海',
              status: 1,
              __v: 0
            },
            token: 'test-token-1111' // 添加token字段
          });
        } else {
          // 员工号不存在
          resolve({
            success: false,
            message: '员工号不存在或已禁用'
          });
        }
      }, 1000);
    });
  }

  // 生产模式下调用真实后端API
  try {
    // 快速检查后端服务是否可用
    const pingController = new AbortController();
    const pingTimeout = setTimeout(() => pingController.abort(), 2000); // 2秒超时
    
    try {
      const pingResponse = await fetch(`${API_BASE_URL}`, {
        method: 'GET',
        signal: pingController.signal,
      });
      clearTimeout(pingTimeout);
      
      if (!pingResponse.ok) {
        throw new Error('后端服务不可用');
      }
    } catch (pingError) {
      clearTimeout(pingTimeout);
      console.error('后端服务检查失败:', pingError);
      return {
        success: false,
        message: '后端服务未运行，请启动后端服务后重试',
      };
    }

    // 添加超时设置
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5秒超时

    const response = await fetch(`${API_BASE_URL}/api/employee/check`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ employeeId }),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    // 检查响应状态
    if (!response.ok) {
      console.error('登录接口响应错误:', response.status, response.statusText);
      return {
        success: false,
        message: `服务器响应错误: ${response.statusText}`,
      };
    }

    // 尝试解析响应体
    try {
      const data = await response.json();
      return data;
    } catch (jsonError) {
      console.error('解析响应体失败:', jsonError);
      return {
        success: false,
        message: '服务器返回的数据格式错误',
      };
    }
  } catch (error) {
    console.error('员工登录校验失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

/**
 * 获取金币信息接口
 * @param userId 用户ID
 * @param employeeId 员工号
 * @returns 用户金币信息
 */
export async function getUserInfo(userId: string, employeeId: string): Promise<ApiResponse<UserInfo>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      // 模拟网络延迟
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取成功',
          data: {
            userId: userId,
            employeeId: employeeId,
            currentMonthGold: 50000,
            lastMonthGold: 120000,
            todayTarget: 100000,
            bonusGold: 5000,
            hasClaimedBonus: false,
            _id: 'mock_user_info',
            __v: 0
          }
        });
      }, 500);
    });
  }

  const url = `/api/user/info?userId=${userId}&employeeId=${employeeId}`;
  return await apiRequest<UserInfo>(url);
}

/**
 * 发放金币接口
 * @param userId 用户ID
 * @param employeeId 员工号
 * @param ecpm 广告ECPM值
 * @param slotId 广告位ID
 * @param deviceId 设备ID
 * @returns 发放的金币数量和当前月金币总数
 */
export async function rewardGold(userId: string, employeeId: string, ecpm: number, slotId: string, deviceId: string): Promise<ApiResponse<GoldReward>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      // 模拟网络延迟
      setTimeout(() => {
        // 根据ecpm计算金币
        const gold = Math.floor(ecpm * 0.1);
        resolve({
          success: true,
          message: '发放成功',
          data: {
            gold: gold,
            currentMonthGold: 50000 + gold
          }
        });
      }, 500);
    });
  }

  return await apiRequest<GoldReward>('/api/employee/reward-gold', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ userId, employeeId, ecpm, slotId, deviceId }),
  });
}

/**
 * 获取金币记录接口
 * @param userId 用户ID
 * @param deviceId 设备ID
 * @param limit 限制数量，默认200
 * @returns 金币发放记录列表
 */
export async function getGoldLogs(userId: string, deviceId: string, limit: number = 200): Promise<ApiResponse<GoldLog[]>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      // 模拟网络延迟
      setTimeout(() => {
        const mockLogs: GoldLog[] = [];
        const now = Date.now();
        
        // 生成模拟数据
        for (let i = 0; i < 50; i++) {
          const timestamp = now - i * 60000; // 每分钟一条记录
          mockLogs.push({
            _id: `mock_log_${i}`,
            userId: userId,
            employeeId: '8202',
            deviceId: deviceId,
            ecpm: Math.floor(Math.random() * 2000) + 50,
            gold: Math.floor((Math.random() * 2000 + 50) * 0.1),
            createTime: new Date(timestamp).toISOString(),
            __v: 0
          });
        }
        
        resolve({
          success: true,
          message: '获取成功',
          data: mockLogs
        });
      }, 500);
    });
  }

  try {
    console.log('🔧 API - getGoldLogs 开始');
    console.log('   URL:', `${API_BASE_URL}/api/gold/log?userId=${userId}&deviceId=${deviceId}&limit=${limit}`);
    
    // 添加超时机制
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      console.warn('⚠️ API请求超时，正在中止...');
      controller.abort();
    }, 10000); // 10秒超时
    
    console.log('   发送请求...');
    const response = await fetch(`${API_BASE_URL}/api/gold/log?userId=${userId}&deviceId=${deviceId}&limit=${limit}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache'
      },
      signal: controller.signal
    });
    
    clearTimeout(timeoutId);
    
    console.log('   收到响应:', {
      status: response.status,
      statusText: response.statusText
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    console.log('   解析响应数据...');
    const data = await response.json();
    console.log('   响应数据:', {
      success: data.success,
      message: data.message,
      dataLength: data.data ? data.data.length : 0
    });
    
    console.log('✅ API - getGoldLogs 完成');
    return data;
  } catch (error) {
    console.error('❌ API - getGoldLogs 失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: [] // 返回空数组，确保前端有默认值
    };
  }
}

// 今日金币统计接口
interface TodayGoldStats {
  todayCoins: number;      // 今日金币总数
  todayRecordCount: number; // 今日记录数
  yesterdayRecordCount: number; // 昨日记录数
}

/**
 * 获取今日金币统计（全局，所有设备）
 * @param userId 用户ID
 * @returns 今日金币统计
 */
export async function getTodayGoldStats(userId: string): Promise<ApiResponse<TodayGoldStats>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      // 模拟网络延迟
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取成功',
          data: {
            todayCoins: 35000,
            todayRecordCount: 120,
            yesterdayRecordCount: 105
          }
        });
      }, 500);
    });
  }

  try {
    console.log('🔧 API - getTodayGoldStats 开始');
    console.log('   URL:', `${API_BASE_URL}/api/gold/today-stats?userId=${userId}`);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      console.warn('⚠️ API请求超时，正在中止...');
      controller.abort();
    }, 10000);

    const response = await fetch(`${API_BASE_URL}/api/gold/today-stats?userId=${userId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache'
      },
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    console.log('✅ API - getTodayGoldStats 完成');
    return data;
  } catch (error) {
    console.error('❌ API - getTodayGoldStats 失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        todayCoins: 0,
        todayRecordCount: 0,
        yesterdayRecordCount: 0
      }
    };
  }
}

/**
 * 添加员工接口（用于测试）
 * @param name 员工姓名
 * @param phone 员工电话
 * @param area 员工地区
 * @returns 新添加的员工信息
 */
export async function addEmployee(name: string, phone: string, area: string): Promise<ApiResponse<EmployeeInfo>> {
  return await apiRequest<EmployeeInfo>('/api/employee/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ name, phone, area }),
  });
}

// 登录统计相关接口
interface LoginRecord {
  loginDays: number;
  todayLogin: boolean;
  isNewLogin: boolean;
  consecutiveDays: number;
}

interface LoginStats {
  totalLoginDays: number;
  firstLoginDate: string;
  lastLoginDate: string;
  consecutiveDays: number;
  loginDates: string[];
}

/**
 * 记录用户登录
 * @param userId 用户ID
 * @param employeeId 员工号
 * @returns 登录记录结果
 */
export async function recordLogin(userId: string, employeeId: string): Promise<ApiResponse<LoginRecord>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/user/login-record`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ userId, employeeId }),
    });
    return await response.json();
  } catch (error) {
    console.error('记录登录失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

/**
 * 获取用户登录统计
 * @param userId 用户ID
 * @param employeeId 员工号
 * @returns 登录统计信息
 */
export async function getLoginStats(userId: string, employeeId: string): Promise<ApiResponse<LoginStats>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/user/login-stats?userId=${userId}&employeeId=${employeeId}`);
    return await response.json();
  } catch (error) {
    console.error('获取登录统计失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

// 提现相关接口
interface WithdrawRequest {
  userId: string;
  employeeId: string;
  amount: number;
  goldAmount: number;
  alipayAccount: string;
  alipayName: string;
}

export interface WithdrawRecord {
  _id: string;
  userId: string;
  employeeId: string;
  amount: number;
  goldAmount: number;
  alipayAccount: string;
  alipayName: string;
  status: number; // 0:提现成功（简化逻辑，提交即成功）
  statusText: string;
  createTime: string;
  remainingGold?: number; // 提现后剩余金币
}

/**
 * 提交提现申请
 * @param data 提现申请数据
 * @returns 提现申请结果
 */
export async function submitWithdrawRequest(data: WithdrawRequest): Promise<ApiResponse<WithdrawRecord>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/withdraw/submit`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });
    return await response.json();
  } catch (error) {
    console.error('提交提现申请失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

/**
 * 获取提现记录列表
 * @param userId 用户ID
 * @returns 提现记录列表
 */
export async function getWithdrawRecords(userId: string): Promise<ApiResponse<WithdrawRecord[]>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/withdraw/list?userId=${userId}`);
    return await response.json();
  } catch (error) {
    console.error('获取提现记录失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

/**
 * 领取今日目标额外金币奖励
 * @param userId 用户ID
 * @param employeeId 员工号
 * @returns 领取结果
 */
export async function claimDailyBonus(userId: string, employeeId: string): Promise<ApiResponse<{ gold: number; currentMonthGold: number }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/daily-bonus/claim`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ userId, employeeId }),
    });
    return await response.json();
  } catch (error) {
    console.error('领取额外金币失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

// 周目标相关接口

interface WeeklyTargetInfo {
  weeklyTarget: number;
  weeklyCompleted: number;
  weeklyStartDate: string;
  weeklyEndDate: string;
  daysRemaining: number;
  progress: number;
  hasClaimedBonus: boolean;
}

/**
 * 获取周目标进度
 * @returns 周目标进度信息
 */
export async function getWeeklyBonusProgress(): Promise<ApiResponse<WeeklyTargetInfo>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/weeklyBonus/progress`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    return await response.json();
  } catch (error) {
    console.error('获取周目标进度失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        weeklyTarget: 0,
        weeklyCompleted: 0,
        weeklyStartDate: '',
        weeklyEndDate: '',
        daysRemaining: 0,
        progress: 0,
        hasClaimedBonus: false
      }
    };
  }
}

/**
 * 领取周奖励
 * @returns 领取结果
 */
export async function claimWeeklyBonus(): Promise<ApiResponse<{ gold: number; currentMonthGold: number }>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/weeklyBonus/claim`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    return await response.json();
  } catch (error) {
    console.error('领取周奖励失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

// 福利抽奖相关接口

interface WelfareLotteryInfo {
  balance: number;
  chances: number;
}

interface WelfareLotteryResult {
  id: string;
  name: string;
  value: number;
  type: string;
}

interface WelfareLotteryRecord {
  id: string;
  time: string;
  name: string;
  value: number;
  type: string;
}

interface WelfareLotteryPrize {
  id: string;
  name: string;
  value: number;
  type: string;
  probability: number;
}

interface AlipayInfo {
  alipayName: string;
  alipayAccount: string;
}

/**
 * 获取福利抽奖信息
 * @param employeeId 员工号
 * @returns 福利抽奖信息
 */
export async function getWelfareLotteryInfo(employeeId: string): Promise<ApiResponse<WelfareLotteryInfo>> {
  try {
    const token = localStorage.getItem('token');
    console.log('getWelfareLotteryInfo - employeeId:', employeeId);
    console.log('getWelfareLotteryInfo - token:', token);
    // 只传递employeeId参数
    const url = `${API_BASE_URL}/api/welfare/lottery/info?employeeId=${employeeId}`;
    console.log('getWelfareLotteryInfo - URL:', url);
    const response = await fetch(url, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    console.log('getWelfareLotteryInfo - Response status:', response.status);
    const data = await response.json();
    console.log('getWelfareLotteryInfo - Response data:', data);
    return data;
  } catch (error) {
    console.error('获取福利抽奖信息失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        balance: 0,
        chances: 0
      }
    };
  }
}

/**
 * 获取福利抽奖奖品列表
 * @returns 奖品列表及其中奖概率
 */
export async function getWelfareLotteryPrizes(): Promise<ApiResponse<{ prizes: WelfareLotteryPrize[] }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/welfare/lottery/prizes`);
    return await response.json();
  } catch (error) {
    console.error('获取福利抽奖奖品列表失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        prizes: []
      }
    };
  }
}

/**
 * 领取福利抽奖
 * @param userId 用户ID
 * @param employeeId 员工号
 * @returns 抽奖结果
 */
export async function claimWelfareLottery(userId: string, employeeId: string): Promise<ApiResponse<{ result: WelfareLotteryResult }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        // 模拟抽奖概率
        const prizes = [
          { id: '1', name: '1克黄金', value: 500, type: 'gold', probability: 2 },
          { id: '2', name: '1.68元', value: 1.68, type: 'cash', probability: 20 },
          { id: '3', name: '88.8元', value: 88.8, type: 'cash', probability: 5 },
          { id: '4', name: '6.88元', value: 6.88, type: 'cash', probability: 15 },
          { id: '5', name: '千元手机', value: 1000, type: 'phone', probability: 1 },
          { id: '6', name: '16.8元', value: 16.8, type: 'cash', probability: 12 },
          { id: '7', name: '66.8元', value: 66.8, type: 'cash', probability: 8 },
          { id: '8', name: '再接再厉', value: 0, type: 'encourage', probability: 37 },
        ];
        
        // 根据概率随机抽取奖品
        const random = Math.random() * 100;
        let cumulative = 0;
        let selectedPrize = prizes[7]; // 默认再接再厉
        
        for (const prize of prizes) {
          cumulative += prize.probability;
          if (random <= cumulative) {
            selectedPrize = prize;
            break;
          }
        }
        
        resolve({
          success: true,
          message: '抽奖成功',
          data: {
            result: selectedPrize
          }
        });
      }, 500);
    });
  }
  
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/welfare/lottery/claim`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({ userId, employeeId }),
    });
    return await response.json();
  } catch (error) {
    console.error('领取福利抽奖失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

/**
 * 获取福利抽奖记录
 * @param employeeId 员工号
 * @returns 抽奖记录
 */
export async function getWelfareLotteryRecords(employeeId: string): Promise<ApiResponse<{ records: WelfareLotteryRecord[] }>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/welfare/lottery/records?employeeId=${employeeId}`, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    return await response.json();
  } catch (error) {
    console.error('获取福利抽奖记录失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        records: []
      }
    };
  }
}

/**
 * 获取福利钱包余额
 * @param employeeId 员工号
 * @returns 钱包余额
 */
export async function getWelfareWalletBalance(employeeId: string): Promise<ApiResponse<{ balance: number }>> {
  try {
    const token = localStorage.getItem('token');
    console.log('getWelfareWalletBalance - employeeId:', employeeId);
    console.log('getWelfareWalletBalance - token:', token);
    // 只传递employeeId参数
    const url = `${API_BASE_URL}/api/welfare/wallet/balance?employeeId=${employeeId}`;
    console.log('getWelfareWalletBalance - URL:', url);
    const response = await fetch(url, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    console.log('getWelfareWalletBalance - Response status:', response.status);
    const data = await response.json();
    console.log('getWelfareWalletBalance - Response data:', data);
    return data;
  } catch (error) {
    console.error('获取福利钱包余额失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        balance: 0
      }
    };
  }
}

/**
 * 福利钱包提现
 * @param employeeId 员工号
 * @param amount 提现金额
 * @param alipayAccount 支付宝账号
 * @param alipayName 支付宝姓名
 * @returns 提现结果
 */
export async function withdrawWelfareFunds(employeeId: string, amount: number, alipayAccount: string, alipayName: string): Promise<ApiResponse<{ success: boolean }>> {
  try {
    console.log('withdrawWelfareFunds - 请求参数:', { employeeId, amount, alipayAccount, alipayName });
    const token = localStorage.getItem('token');
    console.log('withdrawWelfareFunds - token:', token);
    const response = await fetch(`${API_BASE_URL}/api/welfare/withdraw`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({ employeeId, amount, alipayAccount, alipayName }),
    });
    const result = await response.json();
    console.log('withdrawWelfareFunds - 响应:', result);
    return result;
  } catch (error) {
    console.error('福利钱包提现失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

/**
 * 获取福利钱包提现记录
 * @param employeeId 员工号
 * @returns 提现记录列表
 */
export async function getWelfareWithdrawRecords(employeeId: string): Promise<ApiResponse<{ records: any[] }>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/welfare/withdraw/records?employeeId=${employeeId}`, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    return await response.json();
  } catch (error) {
    console.error('获取福利钱包提现记录失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        records: []
      }
    };
  }
}

/**
 * 获取用户抽奖状态（进度条数据）
 * @returns 用户抽奖状态
 */
export interface ThresholdConfig {
  adCount: number;
  giveChances: number;
}

export interface WelfareWalletStatus {
  todayAdCount: number;
  chances: number;
  nextThreshold: {
    adCount: number;
    giveChances: number;
  } | null;
  remaining: number;
  thresholds?: ThresholdConfig[];
}

export async function getWelfareWalletStatus(): Promise<ApiResponse<WelfareWalletStatus>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/welfare/wallet/status`, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    return await response.json();
  } catch (error) {
    console.error('获取用户抽奖状态失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        todayAdCount: 0,
        chances: 0,
        nextThreshold: null,
        remaining: 0
      }
    };
  }
}

/**
 * 绑定支付宝信息
 * @param employeeId 员工号
 * @param alipayName 支付宝姓名
 * @param alipayAccount 支付宝账号
 * @returns 绑定结果
 */
export async function bindAlipay(employeeId: string, alipayName: string, alipayAccount: string): Promise<ApiResponse<AlipayInfo>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/welfare/bind-alipay`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({ employeeId, alipayName, alipayAccount }),
    });
    return await response.json();
  } catch (error) {
    console.error('绑定支付宝信息失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

/**
 * 获取绑定的支付宝信息
 * @param employeeId 员工号
 * @returns 支付宝信息
 */
export async function getAlipayInfo(employeeId: string): Promise<ApiResponse<AlipayInfo>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/welfare/get-alipay?employeeId=${employeeId}`, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    return await response.json();
  } catch (error) {
    console.error('获取绑定的支付宝信息失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

// 提现开关状态
interface WithdrawStatus {
  enabled: boolean;
  message?: string;
}

/**
 * 获取提现开关状态
 * @returns 提现开关状态
 */
export async function getWithdrawStatus(): Promise<ApiResponse<WithdrawStatus>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/settings/withdraw-status`);
    return await response.json();
  } catch (error) {
    console.error('获取提现状态失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

// 活动记录相关接口
interface ActivityRecord {
  userId: string;
  employeeId: string;
  deviceId: string;
  ip: string;
}

/**
 * 记录用户活动
 * @param userId 用户ID
 * @param employeeId 员工号
 * @param deviceId 设备ID
 * @returns 活动记录结果
 */
export async function recordActivity(userId: string, employeeId: string, deviceId: string): Promise<ApiResponse<ActivityRecord>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/activity/record`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ userId, employeeId, deviceId }),
    });
    return await response.json();
  } catch (error) {
    console.error('记录活动失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
    };
  }
}

// 奖金池相关接口
interface PoolStatus {
  redPacketPool: number; // 红包池余额
  lotteryPool: number;   // 每日抽奖池余额
}

interface LotteryTicket {
  id: string;
  ticketNumber: string;
  employeeId: string;
  createdAt: string;
  status: number; // 0: 未开奖, 1: 已中奖, 2: 未中奖
}

interface RedPacketResponse {
  amount: number; // 发放的红包金额
  redPacketPool: number; // 发放后的红包池余额
}

interface LotteryHistory {
  id: string;
  date: string;
  poolAmount: number;
  winners: Array<{
    employeeId: string;
    ticketNumber: string;
    amount: number;
  }>;
}

/**
 * 获取奖金池状态
 * @returns 红包池和抽奖池余额
 */
export async function getPoolStatus(): Promise<ApiResponse<PoolStatus>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取成功',
          data: {
            redPacketPool: 50000,
            lotteryPool: 88888
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/pool/status`);
    return await response.json();
  } catch (error) {
    console.error('获取奖金池状态失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        redPacketPool: 0,
        lotteryPool: 0
      }
    };
  }
}

/**
 * 发放红包
 * @param userId 用户ID
 * @param employeeId 员工号
 * @returns 发放结果
 */
export async function sendRedPacket(userId: string, employeeId: string): Promise<ApiResponse<RedPacketResponse>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '发放成功',
          data: {
            amount: 2500,
            redPacketPool: 47500
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/pool/red-packet/send`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ userId, employeeId }),
    });
    return await response.json();
  } catch (error) {
    console.error('发放红包失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        amount: 0,
        redPacketPool: 0
      }
    };
  }
}

/**
 * 记录广告观看（用于抽奖券生成）
 * @param employeeId 员工号
 * @returns 记录结果
 */
export async function recordAdView(employeeId: string): Promise<ApiResponse<{ ticketGenerated: boolean; ticketNumber?: string }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '记录成功',
          data: {
            ticketGenerated: Math.random() > 0.9, // 10%几率生成抽奖券
            ticketNumber: Math.random() > 0.9 ? `T${Date.now()}` : undefined
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/pool/ad/view-record`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ employeeId }),
    });
    return await response.json();
  } catch (error) {
    console.error('记录广告观看失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        ticketGenerated: false
      }
    };
  }
}

/**
 * 获取用户抽奖券
 * @param employeeId 员工号
 * @returns 抽奖券列表
 */
export async function getUserTickets(employeeId: string): Promise<ApiResponse<LotteryTicket[]>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const tickets: LotteryTicket[] = [];
        for (let i = 0; i < 5; i++) {
          tickets.push({
            id: `ticket_${i}`,
            ticketNumber: `T${Date.now() - i * 10000}`,
            employeeId: employeeId,
            createdAt: new Date(Date.now() - i * 86400000).toISOString(),
            status: 0
          });
        }
        resolve({
          success: true,
          message: '获取成功',
          data: tickets
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/pool/lottery/tickets?employeeId=${employeeId}`);
    return await response.json();
  } catch (error) {
    console.error('获取抽奖券失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: []
    };
  }
}

/**
 * 获取往期开奖记录（旧接口，用于兼容）
 * @param limit 限制数量，默认10
 * @returns 开奖记录列表
 */
export async function getOldLotteryHistory(limit: number = 10): Promise<ApiResponse<LotteryHistory[]>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const history: LotteryHistory[] = [];
        for (let i = 0; i < 3; i++) {
          history.push({
            id: `lottery_${i}`,
            date: new Date(Date.now() - i * 86400000).toISOString().split('T')[0],
            poolAmount: 88888,
            winners: [
              {
                employeeId: '8202',
                ticketNumber: `T${Date.now() - i * 10000}`,
                amount: 44444
              }
            ]
          });
        }
        resolve({
          success: true,
          message: '获取成功',
          data: history
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/pool/lottery/history?limit=${limit}`);
    return await response.json();
  } catch (error) {
    console.error('获取开奖记录失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: []
    };
  }
}

/**
 * 红包记录接口
 */
export interface RedPacketRecord {
  _id: string;
  userId: string;
  employeeId: string;
  amount: number;
  poolBalanceAfter: number;
  createdAt: string;
}

interface RedPacketRecordsResponse {
  records: RedPacketRecord[];
  pagination: {
    total: number;
    page: number;
    limit: number;
    pages: number;
  };
}

/**
 * 获取红包发放记录（管理员接口）
 * @param employeeId 可选，按员工ID筛选
 * @param page 分页页码，默认1
 * @param limit 每页记录数，默认20
 * @returns 红包发放记录列表
 */
export async function getRedPacketRecords(employeeId?: string, page: number = 1, limit: number = 20): Promise<ApiResponse<RedPacketRecordsResponse>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const mockRecords: RedPacketRecord[] = [];
        const now = Date.now();
        
        // 生成模拟数据
        for (let i = 0; i < 11; i++) {
          const timestamp = now - i * 60000; // 每分钟一条记录
          mockRecords.push({
            _id: `mock_red_packet_${i}`,
            userId: 'user123',
            employeeId: '8202',
            amount: Math.floor(Math.random() * 500) + 100,
            poolBalanceAfter: 5000 - i * 100,
            createdAt: new Date(timestamp).toISOString()
          });
        }
        
        resolve({
          success: true,
          message: '获取成功',
          data: {
            records: mockRecords,
            pagination: {
              total: mockRecords.length,
              page: 1,
              limit: 20,
              pages: 1
            }
          }
        });
      }, 500);
    });
  }

  try {
    let url = `${API_BASE_URL}/api/admin/red-packet/records?page=${page}&limit=${limit}`;
    if (employeeId) {
      url += `&employeeId=${employeeId}`;
    }
    
    // 注意：需要管理员登录才能访问此接口
    const token = localStorage.getItem('adminToken');
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    return await response.json();
  } catch (error) {
    console.error('获取红包记录失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        records: [],
        pagination: {
          total: 0,
          page: 1,
          limit: 20,
          pages: 1
        }
      }
    };
  }
}

// 排行榜相关接口
interface RankingRecord {
  employeeId: string;
  earnings: number;
  count: number;
  avgGold: number;
}

interface MonthTopDaily {
  date: string;
  employeeId: string;
  earnings: number;
  count: number;
  avgGold: number;
}

/**
 * 获取今日收益排行榜前10名
 * @returns 今日收益排行榜
 */
export async function getTodayRanking(): Promise<ApiResponse<{ ranking: RankingRecord[] }>> {
  try {
    console.log('🔧 API - getTodayRanking 开始');
    console.log('   URL:', `${API_BASE_URL}/api/ranking/today-ranking`);

    const response = await fetch(`${API_BASE_URL}/api/ranking/today-ranking`);
    const data = await response.json();
    
    console.log('✅ API - getTodayRanking 完成:', data);
    return data;
  } catch (error) {
    console.error('❌ API - getTodayRanking 失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        ranking: []
      }
    };
  }
}

/**
 * 获取昨日收益排行榜前10名
 * @returns 昨日收益排行榜
 */
export async function getYesterdayRanking(): Promise<ApiResponse<{ ranking: RankingRecord[]; date: string }>> {
  try {
    console.log('🔧 API - getYesterdayRanking 开始');
    console.log('   URL:', `${API_BASE_URL}/api/ranking/yesterday-ranking`);

    const response = await fetch(`${API_BASE_URL}/api/ranking/yesterday-ranking`);
    const data = await response.json();
    
    console.log('✅ API - getYesterdayRanking 完成:', data);
    return data;
  } catch (error) {
    console.error('❌ API - getYesterdayRanking 失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        ranking: [],
        date: ''
      }
    };
  }
}

/**
 * 获取本月单日收益最高用户
 * @returns 本月单日收益最高用户
 */
export async function getMonthTopDaily(): Promise<ApiResponse<{ topDaily: MonthTopDaily }>> {
  try {
    console.log('🔧 API - getMonthTopDaily 开始');
    console.log('   URL:', `${API_BASE_URL}/api/ranking/month-top-daily`);

    const response = await fetch(`${API_BASE_URL}/api/ranking/month-top-daily`);
    const data = await response.json();
    
    console.log('✅ API - getMonthTopDaily 完成:', data);
    return data;
  } catch (error) {
    console.error('❌ API - getMonthTopDaily 失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        topDaily: null
      }
    };
  }
}

/**
 * 获取用户个人红包记录
 * @param userId 用户ID
 * @param page 分页页码，默认1
 * @param limit 每页记录数，默认20
 * @returns 红包发放记录列表
 */
export async function getUserRedPacketRecords(userId: string, page: number = 1, limit: number = 20): Promise<ApiResponse<RedPacketRecordsResponse>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const mockRecords: RedPacketRecord[] = [];
        const now = Date.now();
        
        // 生成模拟数据
        for (let i = 0; i < 5; i++) {
          const timestamp = now - i * 60000; // 每分钟一条记录
          mockRecords.push({
            _id: `mock_user_red_packet_${i}`,
            userId: userId,
            employeeId: '8202',
            amount: Math.floor(Math.random() * 500) + 100,
            poolBalanceAfter: 5000 - i * 100,
            createdAt: new Date(timestamp).toISOString()
          });
        }
        
        resolve({
          success: true,
          message: '获取成功',
          data: {
            records: mockRecords,
            pagination: {
              total: mockRecords.length,
              page: 1,
              limit: 20,
              pages: 1
            }
          }
        });
      }, 500);
    });
  }

  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/employee/red-packet/records?userId=${userId}&page=${page}&limit=${limit}`, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    return await response.json();
  } catch (error) {
    console.error('获取用户红包记录失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        records: [],
        pagination: {
          total: 0,
          page: 1,
          limit: 20,
          pages: 1
        }
      }
    };
  }
}

/**
 * 拆红包确认接口
 * @param userId 用户ID
 * @param employeeId 员工号
 * @param redPacketAmount 红包金额
 * @param deviceId 设备ID
 * @returns 红包领取结果
 */
export async function claimRedPacket(userId: string, employeeId: string, redPacketAmount: number, deviceId: string): Promise<ApiResponse<{ gold: number; currentMonthGold: number }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '红包领取成功',
          data: {
            gold: redPacketAmount,
            currentMonthGold: 8860
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/employee/red-packet/claim`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ userId, employeeId, redPacketAmount, deviceId }),
    });
    return await response.json();
  } catch (error) {
    console.error('拆红包确认失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        gold: 0,
        currentMonthGold: 0
      }
    };
  }
}

// 设备状态相关接口

/**
 * 获取设备状态
 * @param userId 用户ID
 * @param deviceId 设备ID
 * @returns 设备状态
 */
export async function getDeviceStatus(userId: string, deviceId: string): Promise<ApiResponse<{ isLimited: boolean; message?: string; consecutiveLowValueCount?: number }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取设备状态成功',
          data: { isLimited: false, consecutiveLowValueCount: 0, message: '设备状态正常' }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/device/status?userId=${userId}&deviceId=${deviceId}`);
    const data = await response.json();
    // 确保返回的数据结构一致
    if (data.success && data.data) {
      // 如果后端返回的数据没有consecutiveLowValueCount字段，添加默认值
      if (data.data.consecutiveLowValueCount === undefined) {
        data.data.consecutiveLowValueCount = 0;
      }
    }
    return data;
  } catch (error) {
    console.error('获取设备状态失败:', error);
    // 降级处理：默认设备未被限制
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: { isLimited: false, consecutiveLowValueCount: 0, message: '设备状态正常' }
    };
  }
}

/**
 * 更新设备记录
 * @param userId 用户ID
 * @param deviceId 设备ID
 * @param gold 获得的金币数
 * @returns 更新后的设备状态
 */
export async function updateDeviceRecord(userId: string, deviceId: string, gold: number): Promise<ApiResponse<{ isLimited: boolean; consecutiveLowValueCount: number }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '更新设备记录成功',
          data: { isLimited: false, consecutiveLowValueCount: 0 }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/device/update`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ userId, deviceId, gold }),
    });
    return await response.json();
  } catch (error) {
    console.error('更新设备记录失败:', error);
    // 降级处理：继续执行，不影响用户获得金币
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: { isLimited: false, consecutiveLowValueCount: 0 }
    };
  }
}

/**
 * 获取设备配置
 * @returns 设备配置
 */
export async function getDeviceConfig(): Promise<ApiResponse<{ consecutiveLimit: number; goldThreshold: number }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取配置成功',
          data: { consecutiveLimit: 8, goldThreshold: 40 }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/device/config`);
    return await response.json();
  } catch (error) {
    console.error('获取设备配置失败:', error);
    // 降级处理：使用默认配置
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: { consecutiveLimit: 8, goldThreshold: 40 }
    };
  }
}

// 手机核销相关接口

interface VerificationRecord {
  id: string;
  amount: number;
  status: string;
  date: string;
  invoiceUrl?: string;
  alipayName?: string;
  alipayAccount?: string;
  rejectReason?: string;
}

interface VerificationResponse {
  success: boolean;
  message: string;
  verificationId: string;
}

interface VerificationRecordsResponse {
  success: boolean;
  data: {
    records: VerificationRecord[];
    total: number;
  };
}

interface VerificationDetailResponse {
  success: boolean;
  data: {
    id: string;
    amount: number;
    status: string;
    date: string;
    invoiceUrl: string;
  };
}

/**
 * 提交核销申请
 * @param amount 核销金额
 * @param invoiceFile 发票文件
 * @returns 核销申请结果
 */
export async function submitVerification(amount: number, invoiceFile: File, alipayName: string, alipayAccount: string): Promise<ApiResponse<{ verificationId: string }>> {
  try {
    const token = localStorage.getItem('token');
    const formData = new FormData();
    formData.append('amount', amount.toString());
    formData.append('invoiceFile', invoiceFile);
    formData.append('alipayName', alipayName);
    formData.append('alipayAccount', alipayAccount);
    
    const response = await fetch(`${API_BASE_URL}/api/verification/submit`, {
      method: 'POST',
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: formData
    });
    
    return await response.json();
  } catch (error) {
    console.error('提交核销申请失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试'
    };
  }
}

/**
 * 获取核销记录
 * @param page 页码
 * @param limit 每页数量
 * @param status 状态筛选
 * @returns 核销记录列表
 */
export async function getVerificationRecords(page: number = 1, limit: number = 10, status?: string): Promise<ApiResponse<{ records: VerificationRecord[]; total: number }>> {
  try {
    const token = localStorage.getItem('token');
    let url = `${API_BASE_URL}/api/verification/records?page=${page}&limit=${limit}`;
    if (status) {
      url += `&status=${status}`;
    }
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    
    return await response.json();
  } catch (error) {
    console.error('获取核销记录失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        records: [],
        total: 0
      }
    };
  }
}

/**
 * 获取核销详情
 * @param id 核销记录ID
 * @returns 核销详情
 */
export async function getVerificationDetail(id: string): Promise<ApiResponse<{ id: string; amount: number; status: string; date: string; invoiceUrl: string }>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/verification/records/${id}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    
    return await response.json();
  } catch (error) {
    console.error('获取核销详情失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试'
    };
  }
}

/**
 * 获取用户金币信息（使用新接口）
 * @returns 用户金币信息
 */
export async function getUserGoldInfo(): Promise<ApiResponse<{ currentMonthGold: number; lastMonthGold: number; totalGold: number }>> {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/user/gold`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    
    return await response.json();
  } catch (error) {
    console.error('获取用户金币信息失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        currentMonthGold: 0,
        lastMonthGold: 0,
        totalGold: 0
      }
    };
  }
}

// 幸运彩票相关接口

/**
 * 获取奖金池状态
 * @returns 奖金池状态
 */
export async function getLotteryPool(): Promise<ApiResponse<{ currentAmount: number; totalAmount: number; lastDrawTime: string }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取奖金池状态成功',
          data: {
            currentAmount: 1500,
            totalAmount: 1500,
            lastDrawTime: new Date().toISOString()
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/lottery/pool`);
    return await response.json();
  } catch (error) {
    console.error('获取奖金池状态失败:', error);
    // 降级处理：返回默认值
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: { currentAmount: 0, totalAmount: 0, lastDrawTime: new Date().toISOString() }
    };
  }
}

/**
 * 获取用户本期奖券列表（未开奖）
 * @param userId 用户ID
 * @returns 用户奖券列表
 */
export async function getCurrentLotteryTickets(userId: string): Promise<ApiResponse<{ tickets: Array<{ ticketNumber: string; issueNumber: string; status: string; validUntil: string; createdAt: string }> }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取奖券列表成功',
          data: {
            tickets: []
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/lottery/tickets/current?userId=${userId}`);
    return await response.json();
  } catch (error) {
    console.error('获取奖券列表失败:', error);
    // 降级处理：返回空数组
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: { tickets: [] }
    };
  }
}

/**
 * 获取用户上一期奖券
 * @param userId 用户ID
 * @returns 上一期奖券信息
 */
export async function getLastLotteryTicket(userId: string): Promise<ApiResponse<{ tickets: Array<{ ticketNumber: string; issueNumber: string; status: string; validUntil: string; createdAt: string }> } | null>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取上一期奖券成功',
          data: {
            tickets: [
              {
                ticketNumber: '123456',
                issueNumber: '2026-03-25-1774450400',
                status: '中奖（一等奖）',
                validUntil: new Date(Date.now() + 86400000).toISOString(),
                createdAt: new Date(Date.now() - 86400000).toISOString()
              }
            ]
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/lottery/tickets/last?userId=${userId}`);
    const data = await response.json();
    console.log('获取上一期奖券响应:', data);
    return data;
  } catch (error) {
    console.error('获取上一期奖券失败:', error);
    // 降级处理：返回null
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: null
    };
  }
}



/**
 * 获取彩票设置
 * @returns 彩票设置
 */
export async function getLotterySettings(): Promise<ApiResponse<{ 
  poolPercentage: number; 
  drawTime: string; 
  adCountThreshold: number; 
  enabled: boolean; 
  firstPrizePercentage: number; 
  secondPrizePercentage: number; 
  thirdPrizePercentage: number; 
  firstPrizeCount: number; 
  secondPrizeCount: number; 
  thirdPrizeCount: number 
}>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取彩票设置成功',
          data: {
            poolPercentage: 0.05,
            drawTime: '22:00',
            adCountThreshold: 2,
            enabled: true,
            firstPrizePercentage: 0.5,
            secondPrizePercentage: 0.3,
            thirdPrizePercentage: 0.2,
            firstPrizeCount: 1,
            secondPrizeCount: 2,
            thirdPrizeCount: 3
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/lottery/settings`);
    return await response.json();
  } catch (error) {
    console.error('获取彩票设置失败:', error);
    // 降级处理：返回默认值
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: {
        poolPercentage: 0.05,
        drawTime: '22:00',
        adCountThreshold: 2,
        enabled: true,
        firstPrizePercentage: 0.5,
        secondPrizePercentage: 0.3,
        thirdPrizePercentage: 0.2,
        firstPrizeCount: 1,
        secondPrizeCount: 2,
        thirdPrizeCount: 3
      }
    };
  }
}

/**
 * 获取最新开奖结果
 * @returns 最新开奖结果
 */
export async function getLotteryResult(): Promise<ApiResponse<any>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取最新开奖结果成功',
          data: {
            issueNumber: '2026-03-25-1774450400',
            drawTime: new Date().toISOString(),
            poolAmount: 1500,
            winners: {
              firstPrize: [],
              secondPrize: [],
              thirdPrize: []
            },
            firstPrize: 750,
            secondPrize: 450,
            thirdPrize: 300
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/lottery/result`);
    return await response.json();
  } catch (error) {
    console.error('获取最新开奖结果失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: null
    };
  }
}

/**
 * 生成奖券
 * @param userId 用户ID
 * @param employeeId 员工ID
 * @returns 生成的奖券信息
 */
export async function generateLotteryTicket(userId: string, employeeId: string): Promise<ApiResponse<{ ticketNumber: string; issueNumber: string }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/lottery/generate-ticket`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ userId, employeeId })
    });
    return await response.json();
  } catch (error) {
    console.error('生成奖券失败:', error);
    return {
      success: false,
      message: '网络错误，请稍后重试',
      data: { ticketNumber: '', issueNumber: '' }
    };
  }
}

/**
 * 获取幸运彩票开奖历史
 * @param page 页码
 * @param limit 每页数量
 * @returns 开奖历史记录
 */
export async function getLotteryHistory(page: number = 1, limit: number = 20): Promise<ApiResponse<{ history: any[]; pagination: any }>> {
  // 开发模式下使用模拟数据
  if (USE_MOCK_DATA) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '获取开奖历史成功',
          data: {
            history: [
              {
                issueNumber: '2026-03-25-1774450400',
                drawTime: new Date().toISOString(),
                poolAmount: 1500,
                firstPrize: 750,
                secondPrize: 450,
                thirdPrize: 300,
                winners: {
                  firstPrize: [
                    {
                      userId: 'user_8202_1772466028893',
                      employeeId: '8202',
                      amount: 750,
                      ticketNumber: '123456'
                    }
                  ],
                  secondPrize: [],
                  thirdPrize: []
                },
                drawType: '随机'
              }
            ],
            pagination: {
              total: 1,
              page: 1,
              limit: 20,
              pages: 1
            }
          }
        });
      }, 500);
    });
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/lottery/history?page=${page}&limit=${limit}`);
    console.log('获取开奖历史响应:', response);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const data = await response.json();
    console.log('获取开奖历史数据:', data);
    return data;
  } catch (error) {
    console.error('获取开奖历史失败:', error);
    // 降级处理：返回空数据
    return {
      success: false,
      message: '获取开奖历史失败，使用空数据',
      data: { history: [], pagination: { total: 0, page: 1, limit: 20, pages: 1 } }
    };
  }
}
