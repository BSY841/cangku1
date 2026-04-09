# 打包成 Android APK 说明

由于 Kivy/Android 打包工具 **Buildozer** 只能在 **Linux（Ubuntu）** 上运行，而当前电脑是 Windows，因此提供两种零成本、最便捷的远程打包方案：

1. **GitHub Actions 自动打包**（推荐，一次配置，以后推送代码自动出 APK）
2. **Google Colab 在线打包**（最快，5~10 分钟拿到 APK）

---

## 方案一：GitHub Actions 自动打包（推荐）

项目根目录已自带 `.github/workflows/build-apk.yml`，只要把整个项目推送到 GitHub，Actions 就会自动帮你编译 APK。

### 步骤

1. **在 GitHub 创建仓库**
   - 登录 https://github.com/new
   - 仓库名填 `flight-duty-calculator`
   - 创建后不要勾选初始化 README（本地已有）

2. **把代码推上去**
   在项目根目录执行：
   ```bash
   git init
   git add .
   git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/你的用户名/flight-duty-calculator.git
   git push -u origin main
   ```

3. **等待自动编译**
   - 打开 GitHub 仓库 → Actions 标签页
   - 会看到 "Build Android APK" 工作流正在运行
   - 首次编译需要下载 Android SDK/NDK，耗时约 **15~25 分钟**
   - 编译完成后，在 Actions 页面点击最新运行记录 → Artifacts → 下载 `flightduty-apk.zip`
   - 解压后即可得到 `flightduty-0.1-arm64-v8a_armeabi-v7a-debug.apk`

4. **以后更新**
   只要修改代码再 `git push`，Actions 会自动重新打包，直接去 Artifacts 下载最新 APK 即可。

---

## 方案二：Google Colab 在线打包（最快）

不用装任何环境，打开浏览器就能打包。

### 步骤

1. 打开 https://colab.research.google.com
2. 新建一个笔记本，在第一个代码单元格粘贴以下内容并运行：

```python
# 挂载 Google Drive（可选，方便保存 APK）
from google.colab import drive
drive.mount('/content/drive')

# 安装依赖
!apt-get update -qq
!apt-get install -y -qq git zip unzip openjdk-17-jdk python3-pip autoconf libtool pkg-config zlib1g-dev libncurses5-dev libncursesw5-dev liblzma-dev cmake

# 安装 buildozer
!pip install -q buildozer cython virtualenv
```

3. **上传项目文件**
   在左侧文件栏点击 "上传"，把以下文件上传到 `/content/` 目录：
   - `main.py`
   - `simhei.ttf`
   - `materialdesignicons-webfont.ttf`
   - `buildozer.spec`

4. **开始打包**
   新建一个代码单元格，运行：

```python
!buildozer android debug
```

5. **下载 APK**
   - 打包完成后，在左侧文件栏找到 `bin/` 文件夹
   - 里面会有一个 `flightduty-0.1-...-debug.apk`
   - 右键下载到本地即可

> 提示：首次打包约 15~20 分钟，Colab 免费 GPU/CPU 会话有 12 小时限制，足够完成。

---

## 安装到手机

1. 把 APK 发送到手机（微信、QQ、邮件、数据线均可）
2. 在手机上点击安装
3. 如果系统提示"未知来源应用"，请允许安装
4. 安装完成后即可使用

---

## 已包含的字体文件

- `simhei.ttf` — 应用主字体（中文显示必需）
- `materialdesignicons-webfont.ttf` — KivyMD 图标字体（避免图标方框）

`buildozer.spec` 已配置把 `.ttf` 文件自动打包进 APK，因此安装到手机后中文和图标都能正常显示。
