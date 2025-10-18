#!/bin/bash

# Android 验证码识别助手 - 快速编译脚本
# 使用现有SDK 36.1.0，避免下载新的Build-Tools
# 使用代理 127.0.0.1:9000 加速下载

echo "=========================================="
echo "Android 验证码识别助手 - 快速编译"
echo "使用现有SDK: 36.1.0"
echo "使用代理: 127.0.0.1:9000"
echo "=========================================="

cd AndroidCaptchaSolver

# 设置环境变量
export ANDROID_HOME=/Users/hujinhui/Library/Android/sdk
export GRADLE_OPTS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=9000 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9000 -Dorg.gradle.daemon=true -Dorg.gradle.parallel=true -Dorg.gradle.configureondemand=true"

echo "1. 清理项目..."
./gradlew clean --quiet

echo "2. 开始编译 (使用现有SDK 36.1.0)..."
./gradlew assembleDebug --parallel --build-cache --quiet

# 检查编译结果
if [ $? -eq 0 ]; then
    echo "✅ 编译成功!"
    
    # 查找生成的APK文件
    APK_FILE=$(find . -name "app-debug.apk" | head -1)
    
    if [ -n "$APK_FILE" ]; then
        echo "📱 APK文件位置: $APK_FILE"
        echo "📦 文件大小: $(ls -lh "$APK_FILE" | awk '{print $5}')"
        
        # 复制到项目根目录
        cp "$APK_FILE" ../验证码识别助手.apk
        echo "📋 APK已复制到: ../验证码识别助手.apk"
        
        echo ""
        echo "🎉 编译完成！"
        echo "📱 安装命令: adb install 验证码识别助手.apk"
        echo "📖 使用说明: 查看 使用指南.md"
    else
        echo "❌ 未找到生成的APK文件"
    fi
else
    echo "❌ 编译失败"
    exit 1
fi
