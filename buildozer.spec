[app]

# 应用标题
title = 飞行机组执勤时间计算器

# 包名
package.name = flightduty
package.domain = org.flightduty

# 源码目录
source.dir = .

# 需要打包进 APK 的文件扩展名（必须把 ttf 字体包含进去）
source.include_exts = py,png,jpg,kv,atlas,ttf,txt

# 版本号
version = 0.1

# 依赖库
requirements = python3,kivy==2.3.1,kivymd==1.2.0,pillow

# 屏幕方向：竖屏
orientation = portrait

# 是否全屏（0 表示保留状态栏，更适合手机）
fullscreen = 0

# Android API 版本配置
android.api = 33
android.minapi = 21
android.sdk = 33
android.ndk = 25b

# 构建的 CPU 架构
android.archs = arm64-v8a, armeabi-v7a

# 需要的权限
android.permissions = INTERNET

# 应用图标（可选，如果你有 icon.png 可以取消注释）
# icon.filename = %(source.dir)s/icon.png

# 日志级别
log_level = 2

[buildozer]

# 构建目录
build_dir = ./.buildozer

# 打包模式：debug
# 发布时改为 release
# build_mode = release
