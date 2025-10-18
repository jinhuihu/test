# Android 验证码识别助手

这是一个运行在Android手机上的验证码识别脚本，可以自动识别九宫格图片验证码并模拟点击，特别适用于抢购商品时的验证过程。

## 功能特点

- ✅ **自动OCR识别** - 识别验证码中的目标物体文字（如"飞机"、"汽车"等）
- ✅ **智能图像识别** - 使用ML Kit识别九宫格中包含目标物体的图片
- ✅ **自动点击模拟** - 自动点击正确的图片和验证按钮
- ✅ **无障碍服务** - 基于AccessibilityService，无需Root权限
- ✅ **多浏览器支持** - 支持Chrome、UC浏览器、QQ浏览器等

## 技术架构

### 核心模块

1. **CaptchaOCR** - OCR文字识别模块
   - 使用Google ML Kit进行中文文字识别
   - 识别验证码中的目标物体文字

2. **ImageRecognizer** - 图像识别模块
   - 使用Google ML Kit进行物体检测
   - 识别九宫格中包含特定物体的图片

3. **ScreenCapture** - 屏幕截图模块
   - 使用MediaProjection API进行屏幕截图
   - 支持Android 5.0+的屏幕录制功能

4. **ClickSimulator** - 点击模拟模块
   - 使用AccessibilityService进行模拟点击
   - 支持精确的坐标点击和节点点击

5. **CaptchaService** - 主服务模块
   - 继承自AccessibilityService
   - 监听屏幕变化，协调各个模块工作

## 安装和使用

### 1. 环境要求

- Android 5.0 (API 21) 或更高版本
- 支持无障碍服务的Android设备
- 至少2GB RAM（用于图像处理）

### 2. 安装步骤

1. **下载APK文件**
   ```bash
   # 编译项目
   cd AndroidCaptchaSolver
   ./gradlew assembleDebug
   ```

2. **安装到手机**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **启用无障碍服务**
   - 打开手机设置 → 辅助功能 → 无障碍
   - 找到"验证码识别助手"
   - 开启服务并授予权限

### 3. 使用方法

1. **启动应用**
   - 打开"验证码识别助手"应用
   - 点击"启动服务"按钮

2. **打开目标网页**
   - 在浏览器中打开需要验证的网页
   - 等待验证码出现

3. **自动识别**
   - 应用会自动检测验证码
   - 识别目标物体文字（如"飞机"）
   - 自动点击包含目标物体的图片
   - 自动点击验证按钮

## 配置说明

### 权限配置

应用需要以下权限：

```xml
<!-- 无障碍服务权限 -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

<!-- 屏幕录制权限 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- 存储权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 支持的浏览器

在`accessibility_service_config.xml`中配置支持的浏览器包名：

```xml
<accessibility-service
    android:packageNames="com.android.chrome,com.android.browser,com.UCMobile,com.tencent.mtt" />
```

### 九宫格位置调整

如果验证码位置不准确，可以修改`ImageRecognizer.java`中的常量：

```java
// 验证码对话框的大致位置
private static final int DIALOG_LEFT_MARGIN = 50;
private static final int DIALOG_TOP_MARGIN = 200;
private static final int DIALOG_WIDTH = 300;
private static final int DIALOG_HEIGHT = 400;
```

## 开发说明

### 项目结构

```
AndroidCaptchaSolver/
├── app/
│   ├── src/main/java/com/captchasolver/
│   │   ├── MainActivity.java          # 主活动
│   │   ├── CaptchaService.java        # 无障碍服务
│   │   ├── CaptchaOCR.java           # OCR识别模块
│   │   ├── ImageRecognizer.java      # 图像识别模块
│   │   ├── ScreenCapture.java        # 屏幕截图模块
│   │   └── ClickSimulator.java       # 点击模拟模块
│   ├── src/main/res/
│   │   ├── layout/activity_main.xml  # 主界面布局
│   │   ├── values/strings.xml        # 字符串资源
│   │   └── xml/accessibility_service_config.xml  # 无障碍服务配置
│   └── build.gradle                  # 应用构建配置
├── build.gradle                      # 项目构建配置
└── settings.gradle                   # 项目设置
```

### 依赖库

```gradle
// Google ML Kit
implementation 'com.google.mlkit:text-recognition:16.0.0'
implementation 'com.google.mlkit:text-recognition-chinese:16.0.0'
implementation 'com.google.mlkit:object-detection:16.2.8'

// 图像处理
implementation 'com.github.bumptech.glide:glide:4.15.1'
```

## 注意事项

### 使用限制

1. **准确性限制** - 图像识别准确率受图片质量影响
2. **网络依赖** - ML Kit需要网络连接进行模型下载
3. **性能影响** - 图像处理会消耗较多CPU和内存
4. **权限要求** - 需要用户手动授予无障碍服务权限

### 安全提醒

1. **仅用于学习** - 本项目仅用于技术学习和研究
2. **遵守法律法规** - 请确保使用符合当地法律法规
3. **不要滥用** - 不要用于恶意目的或商业用途

### 故障排除

1. **服务无法启动**
   - 检查无障碍服务是否已启用
   - 重启应用并重新授权

2. **识别不准确**
   - 确保网络连接正常
   - 检查验证码图片是否清晰
   - 调整九宫格位置参数

3. **点击不生效**
   - 检查无障碍服务权限
   - 确保目标应用在支持列表中

## 更新日志

### v1.0.0 (2024-01-01)
- 初始版本发布
- 支持九宫格验证码识别
- 支持飞机、汽车等物体识别
- 支持自动点击功能

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交 Issue 和 Pull Request 来改进项目。

## 联系方式

如有问题或建议，请通过以下方式联系：

- 邮箱: your-email@example.com
- GitHub: https://github.com/your-username/captcha-solver

---

**免责声明**: 本项目仅供学习和研究使用，请勿用于任何违法用途。使用者需自行承担使用风险。
