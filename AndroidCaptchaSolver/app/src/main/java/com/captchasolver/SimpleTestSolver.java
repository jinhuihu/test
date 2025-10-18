package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单测试识别器
 * 用于排查基本问题，提供最详细的调试信息
 */
public class SimpleTestSolver {
    private static final String TAG = "SimpleTestSolver";
    
    private Context context;
    private AccessibilityService accessibilityService;
    private ClickSimulator clickSimulator;
    
    public SimpleTestSolver(Context context) {
        this.context = context;
        this.clickSimulator = new ClickSimulator(context);
        Log.d(TAG, "SimpleTestSolver 初始化完成");
    }
    
    public void setAccessibilityService(AccessibilityService service) {
        this.accessibilityService = service;
        this.clickSimulator.setAccessibilityService(service);
        Log.d(TAG, "AccessibilityService 已设置");
    }
    
    /**
     * 简单测试处理
     */
    public void processCaptcha() {
        try {
            Log.d(TAG, "==== 开始简单测试 ====");
            logToActivity("==== 开始简单测试 ====");
            
            // 第 1 步：检查服务状态
            if (accessibilityService == null) {
                Log.e(TAG, "AccessibilityService 未设置");
                logToActivity("❌ 错误：服务未设置");
                return;
            }
            Log.d(TAG, "✓ AccessibilityService 已设置");
            logToActivity("✓ 服务状态正常");
            
            // 第 2 步：获取根节点
            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                logToActivity("❌ 错误：无法获取屏幕内容");
                return;
            }
            Log.d(TAG, "✓ 成功获取根节点");
            logToActivity("✓ 成功获取屏幕内容");
            
            // 第 3 步：获取基本信息
            String packageName = rootNode.getPackageName() != null ? 
                rootNode.getPackageName().toString() : "未知";
            String className = rootNode.getClassName() != null ? 
                rootNode.getClassName().toString() : "未知";
            
            Log.d(TAG, "当前应用包名: " + packageName);
            Log.d(TAG, "根节点类名: " + className);
            logToActivity("当前应用: " + packageName);
            logToActivity("根节点: " + className);
            
            // 第 4 步：收集所有文本
            List<String> allTexts = new ArrayList<String>();
            collectAllTexts(rootNode, allTexts);
            Log.d(TAG, "收集到的所有文本: " + allTexts);
            logToActivity("收集到 " + allTexts.size() + " 个文本");
            
            // 第 5 步：查找所有可点击节点
            List<AccessibilityNodeInfo> clickableNodes = new ArrayList<AccessibilityNodeInfo>();
            findClickableNodes(rootNode, clickableNodes);
            Log.d(TAG, "找到 " + clickableNodes.size() + " 个可点击节点");
            logToActivity("找到 " + clickableNodes.size() + " 个可点击节点");
            
            // 第 6 步：详细分析每个可点击节点
            analyzeClickableNodes(clickableNodes);
            
            // 第 7 步：尝试点击所有可点击节点
            Log.d(TAG, "开始点击所有可点击节点");
            logToActivity("开始点击所有节点");
            
            int clickCount = 0;
            for (int i = 0; i < clickableNodes.size(); i++) {
                AccessibilityNodeInfo node = clickableNodes.get(i);
                try {
                    Log.d(TAG, "尝试点击节点 " + (i + 1));
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    clickCount++;
                    logToActivity("✓ 点击节点 " + (i + 1));
                    
                    // 添加点击间隔
                    Thread.sleep(1000); // 1秒间隔，便于观察
                } catch (Exception e) {
                    Log.e(TAG, "点击节点 " + (i + 1) + " 失败: " + e.getMessage());
                    logToActivity("❌ 点击节点 " + (i + 1) + " 失败");
                }
            }
            
            Log.d(TAG, "共点击了 " + clickCount + " 个节点");
            logToActivity("✓ 共点击了 " + clickCount + " 个节点");
            
            // 第 8 步：查找验证按钮
            Thread.sleep(2000);
            Log.d(TAG, "开始查找验证按钮");
            logToActivity("开始查找验证按钮");
            
            boolean verified = clickVerifyButton();
            if (verified) {
                Log.d(TAG, "成功点击验证按钮");
                logToActivity("✓ 成功点击验证按钮");
            } else {
                Log.d(TAG, "未找到验证按钮");
                logToActivity("⚠ 未找到验证按钮");
            }
            
            Log.d(TAG, "==== 简单测试完成 ====");
            logToActivity("==== 简单测试完成 ====");
            
        } catch (Exception e) {
            Log.e(TAG, "简单测试时出错: " + e.getMessage(), e);
            logToActivity("❌ 错误: " + e.getMessage());
        }
    }
    
    /**
     * 详细分析可点击节点
     */
    private void analyzeClickableNodes(List<AccessibilityNodeInfo> nodes) {
        Log.d(TAG, "==== 开始分析可点击节点 ====");
        logToActivity("分析可点击节点...");
        
        for (int i = 0; i < nodes.size(); i++) {
            AccessibilityNodeInfo node = nodes.get(i);
            try {
                String className = node.getClassName() != null ? 
                    node.getClassName().toString() : "未知";
                String text = node.getText() != null ? 
                    node.getText().toString() : "无文本";
                String contentDesc = node.getContentDescription() != null ? 
                    node.getContentDescription().toString() : "无描述";
                
                Log.d(TAG, "节点 " + (i + 1) + ": " + className + 
                    " | 文本: " + text + " | 描述: " + contentDesc);
                logToActivity("节点 " + (i + 1) + ": " + className);
                
            } catch (Exception e) {
                Log.e(TAG, "分析节点 " + (i + 1) + " 时出错: " + e.getMessage());
            }
        }
        
        Log.d(TAG, "==== 可点击节点分析完成 ====");
    }
    
    /**
     * 查找所有可点击的节点
     */
    private void findClickableNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> clickableNodes) {
        if (node == null) return;
        
        if (node.isClickable()) {
            clickableNodes.add(node);
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findClickableNodes(child, clickableNodes);
            }
        }
    }
    
    /**
     * 收集所有文本
     */
    private void collectAllTexts(AccessibilityNodeInfo node, List<String> texts) {
        if (node == null) return;
        
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            texts.add(text.toString());
        }
        
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null && contentDesc.length() > 0) {
            texts.add(contentDesc.toString());
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectAllTexts(child, texts);
            }
        }
    }
    
    /**
     * 点击验证按钮
     */
    private boolean clickVerifyButton() {
        try {
            if (accessibilityService == null) {
                return false;
            }
            
            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                return false;
            }
            
            // 查找验证按钮
            String[] verifyTexts = {"验证", "确认", "提交", "确定", "OK", "Submit", "Verify", "完成", "开始", "继续"};
            
            for (String text : verifyTexts) {
                List<AccessibilityNodeInfo> buttons = 
                    rootNode.findAccessibilityNodeInfosByText(text);
                
                if (!buttons.isEmpty()) {
                    AccessibilityNodeInfo button = buttons.get(0);
                    
                    if (button.isClickable()) {
                        button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "成功点击验证按钮: " + text);
                        logToActivity("✓ 点击验证按钮: " + text);
                        return true;
                    } else if (button.getParent() != null && button.getParent().isClickable()) {
                        button.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "成功点击验证按钮（父节点）: " + text);
                        logToActivity("✓ 点击验证按钮（父节点）: " + text);
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "点击验证按钮时出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 输出日志到 Activity
     */
    private void logToActivity(String message) {
        SimpleMainActivity activity = SimpleMainActivity.getInstance();
        if (activity != null) {
            activity.updateLog(message);
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "SimpleTestSolver 资源已释放");
    }
}
