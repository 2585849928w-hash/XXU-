# XXU 抢座 Android 工程

这是从所给 Python 脚本移植出的原生 Android 应用工程。应用本身没有预置账号、密码、楼层或座位号；密码只用于本次登录，不会被应用保存。

## 功能

- 使用学校 CAS 登录并获取座位系统会话；
- 选择楼层、一个或多个优先座位、预约日期和开抢时间；
- 在开抢时查询 08:00–21:45 时段，并按填写顺序循环尝试座位，最多三分钟；
- 可直接在 Android Studio 打开并构建 APK。最低 Android 8.0。

## 构建

1. 用 Android Studio 打开本目录。
2. 让 Android Studio 安装 Android SDK Platform 35 和 Build Tools。
3. 选择 `Build > Build APK(s)`，在 `app/build/outputs/apk/debug/app-debug.apk` 取得调试 APK。
4. 将 APK 传到 Android 手机上，允许“安装未知应用”后安装。

## 使用注意

- 定时模式依赖应用处于运行状态；开抢前保持应用在前台、网络通畅，并把该应用设为“不受电池优化限制”。
- 请只在学校制度允许的范围内使用，并以学校座位系统的实际规则为准。
- 学校接口或登录页面变动后，应用可能需要相应更新。
