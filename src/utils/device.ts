// 快手系统设备ID：从 localStorage 取或生成，前缀 ks_device_
// 兼容旧 key "deviceId"（device_ 前缀），避免老用户设备ID丢失
export const getDeviceId = (): string => {
  let deviceId = localStorage.getItem('ks_deviceId') || localStorage.getItem('deviceId');
  if (!deviceId) {
    deviceId = 'ks_device_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }
  // 统一写入 ks_deviceId
  localStorage.setItem('ks_deviceId', deviceId);
  return deviceId;
};

// 包名：固定值（秘境星座）
export const getPackageName = (): string => {
  return 'com.mijingxingzuo.app';
};
