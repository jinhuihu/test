package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能验证码识别器
 * 专门针对九宫格验证码优化
 * 先识别提示文字，再识别图片内容
 */
public class SmartCaptchaSolver {
    private static final String TAG = "SmartCaptchaSolver";
    
    private Context context;
    private AccessibilityService accessibilityService;
    private ClickSimulator clickSimulator;
    
    // 验证码提示文字模式
    private static final Pattern CAPTCHA_PATTERN = Pattern.compile("请选择所有包含以下物体的图片");
    private static final Pattern TARGET_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{1,4})");
    
    // 目标物体关键词（扩展版）
    private static final String[] TARGET_KEYWORDS = {
        // 交通工具
        "飞机", "汽车", "自行车", "摩托车", "公交车", "卡车", "轮船", "火车", "地铁", "高铁",
        "电动车", "滑板车", "三轮车", "拖拉机", "挖掘机", "推土机", "起重机",
        
        // 交通设施
        "交通灯", "红绿灯", "消防栓", "停车", "停车场", "路标", "指示牌",
        
        // 动物
        "鸟", "猫", "狗", "马", "羊", "牛", "猪", "鸡", "鸭", "鹅",
        "大象", "熊", "斑马", "长颈鹿", "狮子", "老虎", "豹子", "狼", "狐狸",
        "兔子", "老鼠", "松鼠", "猴子", "猩猩", "熊猫", "袋鼠", "企鹅",
        
        // 物品
        "椅子", "桌子", "床", "沙发", "电视", "电脑", "手机", "相机", "眼镜",
        "帽子", "鞋子", "衣服", "包", "书", "笔", "杯子", "碗", "盘子",
        
        // 食物
        "苹果", "香蕉", "橙子", "葡萄", "草莓", "西瓜", "面包", "蛋糕", "冰淇淋",
        
        // 建筑
        "房子", "大楼", "桥梁", "塔", "教堂", "学校", "医院", "银行",
        
        // 自然
        "树", "花", "草", "山", "海", "湖", "河", "云", "太阳", "月亮", "星星"
    };
    
    // 验证按钮文字
    private static final String[] VERIFY_BUTTON_TEXTS = {
        "验证", "确认", "提交", "确定", "OK", "Submit", "Verify", "完成"
    };
    
    public SmartCaptchaSolver(Context context) {
        this.context = context;
        this.clickSimulator = new ClickSimulator(context);
        Log.d(TAG, "SmartCaptchaSolver 初始化完成");
    }
    
    public void setAccessibilityService(AccessibilityService service) {
        this.accessibilityService = service;
        this.clickSimulator.setAccessibilityService(service);
    }
    
    /**
     * 智能处理验证码
     */
    public void processCaptcha() {
        try {
            Log.d(TAG, "==== 开始智能验证码识别 ====");
            logToActivity("==== 开始智能识别 ====");
            
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
            
            // 第 2 步：检测是否为验证码界面
            if (!isCaptchaInterface(rootNode)) {
                Log.d(TAG, "不是验证码界面，跳过处理");
                logToActivity("⚠ 未检测到验证码界面");
                return;
            }
            
            Log.d(TAG, "步骤 2: 检测到验证码界面");
            logToActivity("✓ 步骤 2: 检测到验证码界面");
            
            // 第 3 步：识别目标物体
            String targetObject = findTargetObject(rootNode);
            if (targetObject == null) {
                Log.w(TAG, "未找到目标物体，使用默认策略");
                logToActivity("⚠ 未找到目标物体，使用默认策略");
                // 使用默认策略
                clickAllClickableImages(rootNode);
            } else {
                Log.d(TAG, "找到目标物体: " + targetObject);
                logToActivity("✓ 步骤 3: 找到目标物体 - " + targetObject);
                
                // 第 4 步：智能识别图片
                smartClickImages(rootNode, targetObject);
            }
            
            // 第 5 步：点击验证按钮
            Thread.sleep(1000);
            Log.d(TAG, "步骤 4: 查找并点击验证按钮");
            logToActivity("✓ 步骤 4: 查找验证按钮");
            
            boolean verified = clickVerifyButton();
            if (verified) {
                Log.d(TAG, "==== 验证码识别完成 ====");
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
     * 检测是否为验证码界面
     */
    private boolean isCaptchaInterface(AccessibilityNodeInfo rootNode) {
        try {
            List<String> allTexts = new ArrayList<String>();
            collectAllTexts(rootNode, allTexts);
            
            Log.d(TAG, "界面文本: " + allTexts);
            
            // 检查是否包含验证码提示文字
            for (String text : allTexts) {
                if (CAPTCHA_PATTERN.matcher(text).find()) {
                    Log.d(TAG, "检测到验证码提示文字: " + text);
                    return true;
                }
            }
            
            // 检查是否包含目标物体关键词
            int keywordCount = 0;
            for (String text : allTexts) {
                for (String keyword : TARGET_KEYWORDS) {
                    if (text.contains(keyword)) {
                        keywordCount++;
                        break;
                    }
                }
            }
            
            // 如果包含多个目标关键词，可能是验证码界面
            if (keywordCount >= 2) {
                Log.d(TAG, "检测到多个目标关键词，可能是验证码界面");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "检测验证码界面时出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 查找目标物体文字
     */
    private String findTargetObject(AccessibilityNodeInfo rootNode) {
        try {
            List<String> allTexts = new ArrayList<String>();
            collectAllTexts(rootNode, allTexts);
            
            Log.d(TAG, "收集到的所有文本: " + allTexts);
            
            // 优先查找独立显示的目标物体
            for (String text : allTexts) {
                // 清理文本，去除多余字符
                String cleanText = text.trim().replaceAll("[\\s\\n\\r]+", "");
                
                // 检查是否为单个目标物体
                for (String keyword : TARGET_KEYWORDS) {
                    if (cleanText.equals(keyword)) {
                        Log.d(TAG, "找到独立目标物体: " + keyword);
                        return keyword;
                    }
                }
                
                // 检查是否包含目标物体
                for (String keyword : TARGET_KEYWORDS) {
                    if (cleanText.contains(keyword)) {
                        Log.d(TAG, "找到包含的目标物体: " + keyword);
                        return keyword;
                    }
                }
            }
            
            // 使用正则表达式查找
            for (String text : allTexts) {
                Matcher matcher = TARGET_PATTERN.matcher(text);
                while (matcher.find()) {
                    String match = matcher.group(1);
                    for (String keyword : TARGET_KEYWORDS) {
                        if (keyword.contains(match) || match.contains(keyword)) {
                            Log.d(TAG, "正则匹配到目标物体: " + keyword);
                            return keyword;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "查找目标物体时出错: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 智能点击图片（基于目标物体）
     */
    private void smartClickImages(AccessibilityNodeInfo rootNode, String targetObject) {
        try {
            List<AccessibilityNodeInfo> clickableNodes = new ArrayList<AccessibilityNodeInfo>();
            findClickableNodes(rootNode, clickableNodes);
            
            Log.d(TAG, "找到 " + clickableNodes.size() + " 个可点击节点");
            logToActivity("找到 " + clickableNodes.size() + " 个可点击节点");
            
            // 这里可以实现更智能的图片识别
            // 目前使用简化策略：点击所有图片节点
            int clickCount = 0;
            for (AccessibilityNodeInfo node : clickableNodes) {
                // 过滤掉明显的按钮
                if (!isButton(node)) {
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
            logToActivity("✓ 点击了 " + clickCount + " 个图片");
            
        } catch (Exception e) {
            Log.e(TAG, "智能点击图片时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 点击所有可点击的图片元素（默认策略）
     */
    private void clickAllClickableImages(AccessibilityNodeInfo rootNode) {
        try {
            List<AccessibilityNodeInfo> clickableNodes = new ArrayList<AccessibilityNodeInfo>();
            findClickableNodes(rootNode, clickableNodes);
            
            Log.d(TAG, "找到 " + clickableNodes.size() + " 个可点击节点");
            logToActivity("找到 " + clickableNodes.size() + " 个可点击节点");
            
            int clickCount = 0;
            for (AccessibilityNodeInfo node : clickableNodes) {
                // 过滤掉按钮
                if (!isButton(node)) {
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
            logToActivity("✓ 点击了 " + clickCount + " 个图片");
            
        } catch (Exception e) {
            Log.e(TAG, "点击图片元素时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 判断节点是否为按钮
     */
    private boolean isButton(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        CharSequence className = node.getClassName();
        if (className != null) {
            String classNameStr = className.toString();
            if (classNameStr.contains("Button") || classNameStr.contains("button")) {
                return true;
            }
        }
        
        CharSequence text = node.getText();
        if (text != null) {
            String textStr = text.toString();
            for (String btnText : VERIFY_BUTTON_TEXTS) {
                if (textStr.contains(btnText)) {
                    return true;
                }
            }
        }
        
        return false;
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
            
            // 尝试查找验证按钮
            for (String buttonText : VERIFY_BUTTON_TEXTS) {
                List<AccessibilityNodeInfo> buttons = 
                    rootNode.findAccessibilityNodeInfosByText(buttonText);
                
                if (!buttons.isEmpty()) {
                    AccessibilityNodeInfo button = buttons.get(0);
                    
                    if (button.isClickable()) {
                        button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "成功点击验证按钮: " + buttonText);
                        logToActivity("✓ 点击验证按钮: " + buttonText);
                        return true;
                    } else if (button.getParent() != null && button.getParent().isClickable()) {
                        button.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "成功点击验证按钮（父节点）: " + buttonText);
                        logToActivity("✓ 点击验证按钮（父节点）: " + buttonText);
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
        Log.d(TAG, "SmartCaptchaSolver 资源已释放");
    }
}
