#!/bin/bash

# Android 验证码识别助手 - 编译和安装脚本
# 使用方法: ./编译和安装.sh

echo "=========================================="
echo "Android 验证码识别助手 - 编译和安装脚本"
echo "=========================================="

# 检查是否在正确的目录
if [ ! -f "AndroidCaptchaSolver/build.gradle" ]; then
    echo "错误: 请在项目根目录运行此脚本"
    exit 1
fi

# 进入项目目录
cd AndroidCaptchaSolver

echo "1. 清理项目..."
./gradlew clean

echo "2. 编译Debug版本..."
./gradlew assembleDebug

# 检查编译是否成功
if [ $? -eq 0 ]; then
    echo "✅ 编译成功!"
    
    # 查找生成的APK文件
    APK_FILE=$(find . -name "app-debug.apk" | head -1)
    
    if [ -n "$APK_FILE" ]; then
        echo "📱 APK文件位置: $APK_FILE"
        
        # 检查是否有连接的设备
        echo "3. 检查连接的设备..."
        adb devices
        
        # 询问是否安装到设备
        read -p "是否要安装到连接的Android设备? (y/n): " install_choice
        
        if [ "$install_choice" = "y" ] || [ "$install_choice" = "Y" ]; then
            echo "4. 安装APK到设备..."
            adb install -r "$APK_FILE"
            
            if [ $? -eq 0 ]; then
                echo "✅ 安装成功!"
                echo ""
                echo "📋 接下来的步骤:"
                echo "1. 在手机上打开 '验证码识别助手' 应用"
                echo "2. 点击 '启动服务' 按钮"
                echo "3. 在设置中启用无障碍服务权限"
                echo "4. 打开浏览器，访问需要验证的网页"
                echo "5. 等待自动识别和点击验证码"
                echo ""
                echo "📖 详细使用说明请查看: 使用指南.md"
            else
                echo "❌ 安装失败，请检查设备连接和权限"
            fi
        else
            echo "📱 APK文件已生成，可以手动安装:"
            echo "   $APK_FILE"
        fi
    else
        echo "❌ 未找到生成的APK文件"
    fi
else
    echo "❌ 编译失败，请检查错误信息"
    exit 1
fi

echo ""
echo "=========================================="
echo "脚本执行完成"
echo "=========================================="
