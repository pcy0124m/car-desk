# 车机桌面（CarDesk）

一款横屏 Android 车机桌面，三栏布局：**左侧快捷栏 + 中间卡片列 + 右侧高德实时地图**。
核心玩法：**音乐、地图等卡片都可以从已安装应用里自由挑选绑定**，想用网易云就用网易云，想用 QQ 音乐就换 QQ 音乐。

## 功能一览

| 模块 | 说明 |
|---|---|
| 🎵 音乐卡 | 从已装应用挑音乐 App 绑定；点播放键直接启动它；系统在播时显示曲目名/进度条，可控制播放/暂停/切歌（MediaSession） |
| 🗺️ 地图区 | 内嵌高德 3D 地图实时显示车位置；支持定位跟随、缩放 |
| 🧭 快捷导航 | 「导航」可绑定地图应用直接唤起；「回家/去公司」长按设地址，点击用 geo: URI 自动导航 |
| 📦 左侧快捷栏 | 5 个自定义槽位，点"+"绑定任意应用，长按可更换/解除；底部九宫格进应用抽屉 |
| ➕ 添加卡片 | 从已装应用挑一个，动态生成启动卡，长按可换/删 |
| 🚗 车辆卡 | 俯视图占位，预留 OBD 数据接口（`VehicleCardView.setVehicleData()`） |
| ⚡ 悬浮工具栏 | 时钟/WiFi/电量/麦克风/电话/主页/退出；麦克风和电话也可绑定应用 |
| ☀️ 天气卡 | 可绑定任意天气应用，点击唤起 |

## 高德地图 Key 申请（必须，否则地图区显示提示卡）

1. 打开 [高德开放平台](https://lbs.amap.com/) 注册并登录开发者账号
2. 控制台 → 应用管理 → **创建新应用** → 添加 **Android 平台 Key**
3. 需要填两项，**务必正确**：
   - **发布版安全码 SHA1**：给车机签名用的 keystore 指纹（发布时用）
   - **PackageName**：`com.loomy.cardesk`
4. 把拿到的 Key 填到工程根目录 `gradle.properties`：

```properties
AMAP_KEY=REPLACE_WITH_YOUR_AMAP_KEY
```

> 提示：本地调试用 debug 签名，指纹命令：
> `keytool -list -v -keystore ~/.android/debug.keystore -storepass android -alias androiddebugkey`
> 分发到车机用 release 签名，两个 SHA1 都可以在高德后台同时登记。

## 构建 APK

### 方式一：GitHub Actions 云编译（推荐，和悬浮桌面/拦截卫士同一套路）

1. 在 GitHub 新建仓库（如 `car-desk`），推送本工程：
   ```bash
   git init && git add . && git commit -m "init"
   git remote add origin https://github.com/<你的账号>/car-desk.git
   git push -u origin main
   ```
2. Actions 流水线自动编译，产出 **Debug APK**（app-debug.apk），在 Actions 页面下载。

### 方式二：本地编译

需要 JDK 17 + Android SDK。在工程根目录：

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 安装与使用

1. APK 传到车机（U 盘或网盘），允许"未知来源"后安装
2. 打开「车机桌面」，建议先授予**定位权限**（地图跟随用）
3. 点音乐卡「换应用」挑一个音乐 App；点左侧栏"+"绑定快捷应用；「导航」按钮绑地图应用
4. 长按「回家/去公司」输入地址，之后一键导航

## 技术栈

- Kotlin + 原生 View（无 Compose，保持轻量）
- Gradle 8.9 / JDK 17 / compileSdk 34 / minSdk 26（Android 8.0+）
- 高德 3D 地图 `com.amap.api:3dmap:9.8.3` + 定位 `com.amap.api:location:6.4.3`
- 所有配置存 SharedPreferences（`DeskConfig`），无数据库

## 进阶改造方向

- **车辆数据**：接 OBD/CAN 后调用 `vehicleCard.setVehicleData(...)` 填胎压/温度
- **天气实时数据**：接和风/高德天气 API，替换天气卡的"唤起应用"逻辑
- **持续定位**：`MapManager.startLocation` 里去掉 `stopLocation()` 并调大 interval，让地图跟着车实时走
- **替换式 Launcher**：Manifest 加 `<category android:name="android.intent.category.HOME" />`，即按 Home 键进入本桌面（与悬浮桌面冲突，需二选一）
