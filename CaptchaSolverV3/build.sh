#!/bin/bash

# 验证码识别助手V3 - 快速编译脚本

echo "🚀 开始编译验证码识别助手V3..."

# 检查是否在正确的目录
if [ ! -f "build.gradle" ]; then
    echo "❌ 错误：请在项目根目录运行此脚本"
    exit 1
fi

# 清理之前的构建
echo "🧹 清理之前的构建..."
./gradlew clean

# 编译项目
echo "🔨 开始编译..."
./gradlew assembleDebug

# 检查编译结果
if [ $? -eq 0 ]; then
    echo "✅ 编译成功！"
    echo "📱 APK文件位置: app/build/outputs/apk/debug/app-debug.apk"
    
    # 检查APK是否存在
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        echo "📊 APK文件大小: $(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)"
        echo "🎉 编译完成！可以使用以下命令安装："
        echo "   adb install -r app/build/outputs/apk/debug/app-debug.apk"
    else
        echo "⚠️  警告：APK文件未找到"
    fi
else
    echo "❌ 编译失败！请检查错误信息"
    exit 1
fi
