import { ref, watch, onUnmounted } from 'vue';

// 防抖延迟时间（毫秒）
const DEBOUNCE_DELAY = 100;

export function useLocalStorage<T>(key: string, defaultValue: T) {
  const storedValue = localStorage.getItem(key);
  const data = ref<T>(storedValue ? JSON.parse(storedValue) : defaultValue);
  
  // 防抖定时器
  let saveTimeout: ReturnType<typeof setTimeout> | null = null;

  watch(data, (newValue) => {
    // 如果有未完成的定时器，先清除
    if (saveTimeout) {
      clearTimeout(saveTimeout);
    }
    
    // 延迟写入，避免频繁IO操作
    saveTimeout = setTimeout(() => {
      localStorage.setItem(key, JSON.stringify(newValue));
      saveTimeout = null;
    }, DEBOUNCE_DELAY);
  }, { deep: true });

  // 组件卸载时清理定时器
  onUnmounted(() => {
    if (saveTimeout) {
      clearTimeout(saveTimeout);
      saveTimeout = null;
    }
  });

  return data;
}
