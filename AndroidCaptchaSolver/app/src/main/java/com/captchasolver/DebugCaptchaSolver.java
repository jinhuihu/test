package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 调试版验证码识别器
 * 用于排查识别问题，提供详细的调试信息
 */
public class DebugCaptchaSolver {
    private static final String TAG = "DebugCaptchaSolver";
    
    private Context context;
    private AccessibilityService accessibilityService;
    private ClickSimulator clickSimulator;
    
    public DebugCaptchaSolver(Context context) {
        this.context = context;
        this.clickSimulator = new ClickSimulator(context);
        Log.d(TAG, "DebugCaptchaSolver 初始化完成");
    }
    
    public void setAccessibilityService(AccessibilityService service) {
        this.accessibilityService = service;
        this.clickSimulator.setAccessibilityService(service);
    }
    
    /**
     * 调试处理验证码
     */
    public void processCaptcha() {
        try {
            Log.d(TAG, "==== 开始调试验证码识别（币安APP专用）====");
            logToActivity("==== 开始调试识别（币安APP专用）====");
            
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
            
            // 第 2 步：检查是否为币安APP
            String packageName = rootNode.getPackageName() != null ? 
                rootNode.getPackageName().toString() : "未知";
            Log.d(TAG, "当前应用包名: " + packageName);
            logToActivity("当前应用: " + packageName);
            
            // 第 3 步：查找所有文本
            List<String> allTexts = new ArrayList<String>();
            collectAllTexts(rootNode, allTexts);
            Log.d(TAG, "收集到的所有文本: " + allTexts);
            logToActivity("收集到 " + allTexts.size() + " 个文本元素");
            
            // 第 4 步：检测验证码关键词
            boolean hasCaptchaKeywords = detectCaptchaKeywords(allTexts);
            if (hasCaptchaKeywords) {
                Log.d(TAG, "检测到验证码关键词");
                logToActivity("✓ 检测到验证码关键词");
            } else {
                Log.d(TAG, "未检测到验证码关键词");
                logToActivity("⚠ 未检测到验证码关键词");
            }
            
            // 第 5 步：查找所有可点击节点
            List<AccessibilityNodeInfo> clickableNodes = new ArrayList<AccessibilityNodeInfo>();
            findClickableNodes(rootNode, clickableNodes);
            Log.d(TAG, "找到 " + clickableNodes.size() + " 个可点击节点");
            logToActivity("找到 " + clickableNodes.size() + " 个可点击节点");
            
            // 第 6 步：智能点击策略
            Log.d(TAG, "开始智能点击策略");
            logToActivity("开始智能点击策略");
            
            int clickCount = 0;
            for (AccessibilityNodeInfo node : clickableNodes) {
                try {
                    // 过滤掉明显的按钮和文本节点
                    if (shouldClickNode(node)) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        clickCount++;
                        Log.d(TAG, "点击节点 " + clickCount);
                        
                        // 添加点击间隔
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "点击节点失败: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "智能点击：共点击了 " + clickCount + " 个节点");
            logToActivity("智能点击：点击了 " + clickCount + " 个节点");
            
            // 第 7 步：查找验证按钮
            Thread.sleep(1000);
            boolean verified = clickVerifyButton();
            if (verified) {
                Log.d(TAG, "成功点击验证按钮");
                logToActivity("✓ 点击了验证按钮");
            } else {
                Log.d(TAG, "未找到验证按钮");
                logToActivity("⚠ 未找到验证按钮");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "调试处理验证码时出错: " + e.getMessage(), e);
            logToActivity("❌ 错误: " + e.getMessage());
        }
    }
    
    /**
     * 检测验证码关键词
     */
    private boolean detectCaptchaKeywords(List<String> texts) {
        String[] captchaKeywords = {
            "请选择", "选择", "包含", "图片", "验证码", "验证", "captcha",
            "飞机", "汽车", "自行车", "摩托车", "公交车", "卡车", "轮船", "火车",
            "鸟", "猫", "狗", "马", "羊", "牛", "猪", "鸡", "鸭", "鹅",
            "大象", "熊", "斑马", "长颈鹿", "狮子", "老虎", "豹子",
            "椅子", "桌子", "床", "沙发", "电视", "电脑", "手机",
            "苹果", "香蕉", "橙子", "葡萄", "草莓", "西瓜",
            "树", "花", "草", "山", "海", "湖", "河", "云", "太阳", "月亮"
        };
        
        int keywordCount = 0;
        for (String text : texts) {
            for (String keyword : captchaKeywords) {
                if (text.contains(keyword)) {
                    keywordCount++;
                    Log.d(TAG, "找到关键词: " + keyword + " 在文本: " + text);
                    break;
                }
            }
        }
        
        Log.d(TAG, "关键词匹配数量: " + keywordCount);
        return keywordCount >= 2; // 至少匹配2个关键词
    }
    
    /**
     * 判断是否应该点击该节点
     */
    private boolean shouldClickNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        // 获取节点信息
        CharSequence className = node.getClassName();
        CharSequence text = node.getText();
        CharSequence contentDesc = node.getContentDescription();
        
        // 过滤掉明显的按钮
        if (className != null) {
            String classNameStr = className.toString();
            if (classNameStr.contains("Button") || classNameStr.contains("button")) {
                return false;
            }
        }
        
        // 过滤掉明显的文本按钮
        if (text != null) {
            String textStr = text.toString();
            String[] buttonTexts = {"验证", "确认", "提交", "确定", "OK", "Submit", "Verify", "完成", "取消", "关闭"};
            for (String btnText : buttonTexts) {
                if (textStr.contains(btnText)) {
                    return false;
                }
            }
        }
        
        // 过滤掉内容描述中的按钮
        if (contentDesc != null) {
            String descStr = contentDesc.toString();
            String[] buttonDescs = {"按钮", "button", "点击", "验证", "确认"};
            for (String btnDesc : buttonDescs) {
                if (descStr.contains(btnDesc)) {
                    return false;
                }
            }
        }
        
        // 优先点击没有文本的节点（可能是图片）
        if ((text == null || text.length() == 0) && 
            (contentDesc == null || contentDesc.length() == 0)) {
            return true;
        }
        
        // 点击包含图片相关关键词的节点
        if (text != null || contentDesc != null) {
            String nodeInfo = (text != null ? text.toString() : "") + 
                             (contentDesc != null ? contentDesc.toString() : "");
            String[] imageKeywords = {"图片", "image", "img", "photo"};
            for (String keyword : imageKeywords) {
                if (nodeInfo.contains(keyword)) {
                    return true;
                }
            }
        }
        
        // 默认点击
        return true;
    }
    
    /**
     * 详细分析界面内容
     */
    private void analyzeInterface(AccessibilityNodeInfo rootNode) {
        try {
            Log.d(TAG, "==== 界面分析开始 ====");
            
            // 分析根节点信息
            Log.d(TAG, "根节点类名: " + rootNode.getClassName());
            Log.d(TAG, "根节点包名: " + rootNode.getPackageName());
            Log.d(TAG, "根节点文本: " + rootNode.getText());
            Log.d(TAG, "根节点描述: " + rootNode.getContentDescription());
            Log.d(TAG, "根节点可点击: " + rootNode.isClickable());
            Log.d(TAG, "根节点子节点数: " + rootNode.getChildCount());
            
            // 分析所有子节点
            analyzeNodeRecursively(rootNode, 0);
            
            Log.d(TAG, "==== 界面分析完成 ====");
            
        } catch (Exception e) {
            Log.e(TAG, "分析界面时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 递归分析节点
     */
    private void analyzeNodeRecursively(AccessibilityNodeInfo node, int depth) {
        if (node == null) return;
        
        try {
            String indent = "";
            for (int i = 0; i < depth; i++) {
                indent += "  ";
            }
            
            Log.d(TAG, indent + "节点: " + node.getClassName());
            Log.d(TAG, indent + "  文本: " + node.getText());
            Log.d(TAG, indent + "  描述: " + node.getContentDescription());
            Log.d(TAG, indent + "  可点击: " + node.isClickable());
            Log.d(TAG, indent + "  子节点数: " + node.getChildCount());
            
            // 限制递归深度
            if (depth < 3) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) {
                        analyzeNodeRecursively(child, depth + 1);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "分析节点时出错: " + e.getMessage(), e);
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
            String[] verifyTexts = {"验证", "确认", "提交", "确定", "OK", "Submit", "Verify", "完成"};
            
            for (String text : verifyTexts) {
                List<AccessibilityNodeInfo> buttons = 
                    rootNode.findAccessibilityNodeInfosByText(text);
                
                if (!buttons.isEmpty()) {
                    AccessibilityNodeInfo button = buttons.get(0);
                    
                    if (button.isClickable()) {
                        button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "成功点击验证按钮: " + text);
                        return true;
                    } else if (button.getParent() != null && button.getParent().isClickable()) {
                        button.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "成功点击验证按钮（父节点）: " + text);
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
        Log.d(TAG, "DebugCaptchaSolver 资源已释放");
    }
}
