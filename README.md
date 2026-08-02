# 月序星座 - 广告变现系统

![Build Status](https://github.com/niklaus3016/admaster-data/workflows/Build%20Project/badge.svg)
![Deploy Status](https://github.com/niklaus3016/admaster-data/workflows/Deploy%20to%20GitHub%20Pages/badge.svg)
![APK Build Status](https://github.com/niklaus3016/admaster-data/workflows/Build%20Android%20APK/badge.svg)

基于Vue3 + TypeScript + Vite开发的月序星座广告变现系统前端应用。

## 功能特性

- ✅ 员工登录校验（4位数字员工号）
- ✅ 金币信息展示（上月/本月累计金币）
- ✅ 看广告领金币功能
- ✅ 金币收益记录列表
- ✅ 本地数据持久化
- ✅ 响应式设计（移动端适配）
- ✅ 苹果官网风格UI设计
- ✅ Android APK支持
- ✅ 幸运彩票功能
- ✅ 奖券状态显示（未开奖、中奖、作废）
- ✅ 底栏红点通知（显示未开奖奖券数量）
- ✅ 福利抽奖功能（暂未正式开放）
- ✅ 百度广告集成
- ✅ 智能广告预加载机制

## 技术栈

- **前端框架**：Vue 3.5.28
- **构建工具**：Vite 6.2.0
- **路由管理**：Vue Router 5.0.3
- **类型检查**：TypeScript 5.8.2
- **样式框架**：Tailwind CSS 4.1.14
- **图标库**：Lucide Vue Next 0.575.0
- **广告SDK**：百度广告SDK
- **移动端打包**：Capacitor 6.x

## 项目结构

```
project/
├── src/
│   ├── api/              # API服务层
│   │   └── apiService.ts
│   ├── composables/       # 组合式函数
│   │   ├── useAdManager.ts
│   │   └── useLocalStorage.ts
│   ├── pages/            # 页面组件
│   │   ├── Login.vue
│   │   ├── Home.vue
│   │   ├── Lottery.vue
│   │   ├── LotteryDetail.vue
│   │   └── WelfareLottery.vue
│   ├── plugins/           # 插件
│   │   ├── BaiduAdPlugin.ts
│   │   └── BaiduAdPluginWeb.ts
│   ├── router/            # 路由配置
│   │   └── index.ts
│   ├── App.vue            # 根组件
│   ├── main.ts            # 应用入口
│   └── index.css          # 全局样式
├── android/               # Android项目
│   ├── app/               # Android应用代码
│   ├── gradle/            # Gradle配置
│   └── gradlew            # Gradle包装器
├── .github/workflows/   # GitHub Actions工作流
│   ├── build.yml          # 构建工作流
│   ├── deploy.yml         # 部署工作流
│   └── build-apk.yml     # APK构建工作流
├── public/              # 静态资源
├── capacitor.config.ts   # Capacitor配置
├── index.html            # HTML模板
├── package.json          # 项目配置
├── tsconfig.json         # TypeScript配置
└── vite.config.ts        # Vite配置
```

## 开发指南

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0

### 安装依赖

```bash
npm install
```

### 本地开发

```bash
npm run dev
```

前端服务将在 `http://localhost:3002` 启动。

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist/` 目录。

### 类型检查

```bash
npm run lint
```

### 清理构建产物

```bash
npm run clean
```

## Android APK构建

### 自动构建

项目配置了GitHub Actions自动构建APK，每次推送到master分支时会自动：

1. 构建Vue3项目
2. 同步到Android平台
3. 构建Debug APK
4. 上传APK作为构建产物

### 手动构建

```bash
npm run build
npx cap sync android
cd android
./gradlew assembleDebug
```

APK文件将生成在 `android/app/build/outputs/apk/debug/app-debug.apk`。

## 部署

### GitHub Pages

项目配置了GitHub Actions自动构建和部署，每次推送到master分支时会自动：

1. 构建项目
2. 上传构建产物
3. 部署到GitHub Pages

### 手动部署

```bash
npm run build
# 将dist目录上传到您的服务器
```

## API配置

### 开发环境

在 `src/api/apiService.ts` 中配置：

```typescript
const API_BASE_URL = 'https://xevbnmgazudl.sealoshzh.site';
const USE_MOCK_DATA = false;
```

### 生产环境

修改 `API_BASE_URL` 为生产环境的后端地址。

## 测试账号

- **8202**：测试员工（北京）
- **1111**：测试员工1111（上海）
- **3048**：测试员工2（上海）

## 浏览器支持

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！

## 作者

niklaus3016

## 更新日志

### v1.1.0 (2026-06-13) - 月序星座

- ✅ 应用重命名为「月序星座」
- ✅ 更新应用包名为 `com.yuexuxingzuo.app`
- ✅ 更新广告位配置（15个广告位）
- ✅ 更新百度广告AppId
- ✅ 更新应用图标
- ✅ 调整eCPM高值传输比例为30%

### v1.0.1 (2026-04-14) - 月序星座广告正式版

- ✅ 应用重命名为「月序星座」
- ✅ 更新应用包名为 `com.yuexuxingzuo.app`
- ✅ 集成百度广告SDK
- ✅ 更新广告位配置（12个广告位）
- ✅ 实现智能广告预加载机制
- ✅ 添加福利抽奖功能（暂未正式开放）
- ✅ 更新应用图标
- ✅ 优化广告加载逻辑
- ✅ 修复所有类型错误和警告

### v1.0.0 (2026-02-24)

- ✅ 初始版本发布
- ✅ 员工登录功能
- ✅ 金币管理功能
- ✅ 广告集成准备
- ✅ GitHub Actions配置
- ✅ 响应式设计
- ✅ Android APK支持