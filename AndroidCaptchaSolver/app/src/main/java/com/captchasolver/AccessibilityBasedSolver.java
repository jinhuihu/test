package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于无障碍服务的验证码识别器（简化版）
 * 不依赖屏幕截图，直接通过节点操作
 */
public class AccessibilityBasedSolver {
    private static final String TAG = "AccessibilityBasedSolver";
    
    private Context context;
    private AccessibilityService accessibilityService;
    private ClickSimulator clickSimulator;
    
    // 验证码相关关键词
    private static final String[] TARGET_KEYWORDS = {
        "飞机", "汽车", "自行车", "摩托车", "公交车", "卡车", 
        "轮船", "火车", "交通灯", "消防栓", "停车", "鸟", "猫", 
        "狗", "马", "羊", "牛", "大象", "熊", "斑马", "长颈鹿"
    };
    
    private static final String[] VERIFY_BUTTON_TEXTS = {
        "验证", "确认", "提交", "确定", "OK", "Submit", "Verify"
    };
    
    public AccessibilityBasedSolver(Context context) {
        this.context = context;
        this.clickSimulator = new ClickSimulator(context);
        Log.d(TAG, "AccessibilityBasedSolver 初始化完成");
    }
    
    public void setAccessibilityService(AccessibilityService service) {
        this.accessibilityService = service;
        this.clickSimulator.setAccessibilityService(service);
    }
    
    /**
     * 处理验证码（基于无障碍服务）
     */
    public void processCaptcha() {
        try {
            Log.d(TAG, "==== 开始验证码识别流程（无障碍服务版本）====");
            logToActivity("==== 开始识别 ====");
            
            if (accessibilityService == null) {
                Log.e(TAG, "AccessibilityService 未设置");
                logToActivity("错误：服务未设置");
                return;
            }
            
            // 第 1 步：获取根节点
            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                logToActivity("错误：无法获取屏幕内容");
                return;
            }
            
            Log.d(TAG, "步骤 1: 成功获取根节点");
            logToActivity("✓ 步骤 1: 获取屏幕内容");
            
            // 第 2 步：查找目标物体文字
            String targetObject = findTargetObject(rootNode);
            if (targetObject == null) {
                Log.w(TAG, "未找到目标物体文字，使用默认策略");
                logToActivity("⚠ 未找到目标物体，使用默认策略");
                // 使用默认策略：点击所有可点击的图片元素
                clickAllClickableImages(rootNode);
            } else {
                Log.d(TAG, "找到目标物体: " + targetObject);
                logToActivity("✓ 步骤 2: 找到目标物体 - " + targetObject);
                // 这里可以基于目标物体进行更智能的选择
                clickAllClickableImages(rootNode);
            }
            
            // 第 3 步：等待一段时间后点击验证按钮
            Thread.sleep(1000);
            
            Log.d(TAG, "步骤 3: 查找并点击验证按钮");
            logToActivity("✓ 步骤 3: 查找验证按钮");
            boolean verified = clickVerifyButton();
            
            if (verified) {
                Log.d(TAG, "==== 验证码处理完成 ====");
                logToActivity("✅ 验证完成！");
            } else {
                Log.w(TAG, "未能点击验证按钮");
                logToActivity("⚠ 未找到验证按钮");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理验证码时出错: " + e.getMessage(), e);
            logToActivity("❌ 错误: " + e.getMessage());
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
     * 查找目标物体文字
     */
    private String findTargetObject(AccessibilityNodeInfo rootNode) {
        try {
            // 遍历所有子节点查找文本
            List<String> allTexts = new ArrayList<String>();
            collectAllTexts(rootNode, allTexts);
            
            Log.d(TAG, "收集到的所有文本: " + allTexts);
            
            // 查找目标关键词
            for (String text : allTexts) {
                for (String keyword : TARGET_KEYWORDS) {
                    if (text.contains(keyword)) {
                        return keyword;
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "查找目标物体时出错: " + e.getMessage(), e);
        }
        
        return null;
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
     * 点击所有可点击的图片元素
     */
    private void clickAllClickableImages(AccessibilityNodeInfo rootNode) {
        try {
            List<AccessibilityNodeInfo> clickableNodes = new ArrayList<AccessibilityNodeInfo>();
            findClickableNodes(rootNode, clickableNodes);
            
            Log.d(TAG, "找到 " + clickableNodes.size() + " 个可点击的节点");
            
            // 点击所有可点击节点（可能是验证码图片）
            int clickCount = 0;
            for (AccessibilityNodeInfo node : clickableNodes) {
                // 过滤掉明显的按钮（包含特定文字）
                boolean isButton = false;
                CharSequence text = node.getText();
                if (text != null) {
                    String textStr = text.toString();
                    for (String btnText : VERIFY_BUTTON_TEXTS) {
                        if (textStr.contains(btnText)) {
                            isButton = true;
                            break;
                        }
                    }
                }
                
                if (!isButton) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    clickCount++;
                    Log.d(TAG, "点击节点 " + clickCount);
                    
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        // 忽略
                    }
                }
            }
            
            Log.d(TAG, "共点击了 " + clickCount + " 个图片节点");
            
        } catch (Exception e) {
            Log.e(TAG, "点击图片元素时出错: " + e.getMessage(), e);
        }
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
            
            // 尝试查找验证按钮
            for (String buttonText : VERIFY_BUTTON_TEXTS) {
                List<AccessibilityNodeInfo> buttons = 
                    rootNode.findAccessibilityNodeInfosByText(buttonText);
                
                if (!buttons.isEmpty()) {
                    AccessibilityNodeInfo button = buttons.get(0);
                    
                    if (button.isClickable()) {
                        button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "成功点击验证按钮: " + buttonText);
                        return true;
                    } else if (button.getParent() != null && button.getParent().isClickable()) {
                        button.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "成功点击验证按钮（父节点）: " + buttonText);
                        return true;
                    }
                }
            }
            
            Log.d(TAG, "未找到验证按钮");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "点击验证按钮时出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "AccessibilityBasedSolver 资源已释放");
    }
}

