#!/bin/bash

# GitHub Actions 在线编译 - 快速设置脚本

echo "=========================================="
echo "GitHub Actions 在线编译 - 设置向导"
echo "=========================================="
echo ""

# 检查是否已经有 GitHub Actions 配置
if [ -f ".github/workflows/android-build.yml" ]; then
    echo "✅ GitHub Actions 配置文件已存在"
else
    echo "❌ 缺少 GitHub Actions 配置文件"
    echo "请确保 .github/workflows/ 目录下有配置文件"
    exit 1
fi

echo ""
echo "📋 使用步骤："
echo ""
echo "1️⃣  创建 GitHub 仓库"
echo "   访问: https://github.com/new"
echo "   创建一个新的仓库（公开或私有）"
echo ""

read -p "2️⃣  输入你的 GitHub 仓库地址（例如：https://github.com/username/repo.git）: " GITHUB_REPO

if [ -z "$GITHUB_REPO" ]; then
    echo "❌ 未输入仓库地址，退出"
    exit 1
fi

echo ""
echo "3️⃣  配置 Git 远程仓库..."

# 检查是否已有 github 远程仓库
if git remote | grep -q "^github$"; then
    echo "   已存在 github 远程仓库，更新地址..."
    git remote set-url github "$GITHUB_REPO"
else
    echo "   添加新的 github 远程仓库..."
    git remote add github "$GITHUB_REPO"
fi

echo "   ✅ 远程仓库配置完成"
echo ""

# 显示当前的远程仓库
echo "📊 当前的远程仓库："
git remote -v | grep github
echo ""

read -p "4️⃣  是否要提交并推送代码到 GitHub？(y/n): " PUSH_CHOICE

if [ "$PUSH_CHOICE" = "y" ] || [ "$PUSH_CHOICE" = "Y" ]; then
    echo ""
    echo "5️⃣  提交代码..."
    
    # 添加所有文件
    git add .
    
    # 提交
    git commit -m "添加 GitHub Actions 在线编译配置" || echo "   无新的更改或已提交"
    
    echo ""
    echo "6️⃣  推送到 GitHub..."
    echo "   (可能需要输入 GitHub 用户名和密码/Token)"
    
    # 推送到 GitHub
    git push github master || git push github main
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "🎉 推送成功！"
        echo ""
        echo "=========================================="
        echo "下一步操作："
        echo "=========================================="
        echo ""
        echo "1. 访问你的 GitHub 仓库"
        echo "2. 点击顶部的 'Actions' 标签"
        echo "3. 如果看到提示，点击 'I understand my workflows, go ahead and enable them'"
        echo "4. 选择 'Android CI - 编译 APK' 工作流"
        echo "5. 点击右侧的 'Run workflow' 按钮"
        echo "6. 等待编译完成（约 5-10 分钟）"
        echo "7. 在工作流运行页面底部的 'Artifacts' 部分下载 APK"
        echo ""
        echo "📱 仓库地址: $GITHUB_REPO"
        echo "🔗 快速链接: ${GITHUB_REPO%.git}/actions"
        echo ""
        echo "=========================================="
    else
        echo ""
        echo "❌ 推送失败"
        echo ""
        echo "可能的原因："
        echo "1. 需要先在 GitHub 创建仓库"
        echo "2. 需要配置 GitHub 认证（用户名密码或 Token）"
        echo "3. 分支名称可能是 'main' 而不是 'master'"
        echo ""
        echo "💡 建议："
        echo "   使用 GitHub Desktop 或 Git 凭据管理器来简化认证"
        echo ""
    fi
else
    echo ""
    echo "📝 手动推送步骤："
    echo ""
    echo "git add ."
    echo "git commit -m '添加 GitHub Actions 配置'"
    echo "git push github master"
    echo ""
    echo "或者使用你喜欢的 Git 客户端进行推送"
    echo ""
fi

echo ""
echo "=========================================="
echo "💡 提示"
echo "=========================================="
echo ""
echo "如果 GitHub Actions 编译失败（SDK 36 问题）："
echo "1. 访问 Actions 页面"
echo "2. 选择 'Android CI - 使用 SDK 33 编译' 工作流"
echo "3. 手动触发该工作流"
echo ""
echo "详细说明请查看: 在线编译指南.md"
echo ""



