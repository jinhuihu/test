package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * 真实的验证码识别器
 * 整合 OCR、图像识别和点击模拟，实现完整的验证码识别流程
 */
public class RealCaptchaSolver {
    private static final String TAG = "RealCaptchaSolver";
    
    private Context context;
    private AccessibilityService accessibilityService;
    
    // 核心组件
    private CaptchaOCR captchaOCR;
    private ImageRecognizer imageRecognizer;
    private ClickSimulator clickSimulator;
    private ScreenCapture screenCapture;
    
    // 验证码相关常量
    private static final String VERIFY_BUTTON_TEXT = "验证";
    
    public RealCaptchaSolver(Context context) {
        this.context = context;
        
        // 初始化核心组件
        this.captchaOCR = new CaptchaOCR(context);
        this.imageRecognizer = new ImageRecognizer(context);
        this.clickSimulator = new ClickSimulator(context);
        this.screenCapture = new ScreenCapture(context);
        
        Log.d(TAG, "RealCaptchaSolver 初始化完成");
    }
    
    /**
     * 设置 AccessibilityService 引用
     */
    public void setAccessibilityService(AccessibilityService service) {
        this.accessibilityService = service;
        this.clickSimulator.setAccessibilityService(service);
        Log.d(TAG, "AccessibilityService 已设置");
    }
    
    /**
     * 处理验证码识别和点击（完整流程）
     */
    public void processCaptcha() {
        try {
            Log.d(TAG, "==== 开始验证码识别流程 ====");
            
            // 第 1 步：截取屏幕
            Log.d(TAG, "步骤 1: 截取屏幕");
            Bitmap screenshot = screenCapture.takeScreenshot();
            
            if (screenshot == null) {
                Log.e(TAG, "截屏失败，可能需要屏幕录制权限");
                // 尝试使用无障碍服务获取屏幕内容
                screenshot = captureScreenWithAccessibility();
            }
            
            if (screenshot == null) {
                Log.e(TAG, "无法获取屏幕内容");
                return;
            }
            
            Log.d(TAG, "截屏成功: " + screenshot.getWidth() + "x" + screenshot.getHeight());
            
            // 第 2 步：识别目标物体文字（左上角）
            Log.d(TAG, "步骤 2: 识别目标物体文字");
            
            // 裁剪左上角区域（目标文字通常在这里）
            int textRegionWidth = screenshot.getWidth() / 3;
            int textRegionHeight = screenshot.getHeight() / 5;
            Rect textRegion = new Rect(0, 0, textRegionWidth, textRegionHeight);
            
            String targetObject = captchaOCR.recognizeTextInRegion(screenshot, textRegion);
            
            if (targetObject == null || targetObject.isEmpty()) {
                Log.e(TAG, "未能识别目标物体文字");
                screenshot.recycle();
                return;
            }
            
            Log.d(TAG, "识别到目标物体: " + targetObject);
            
            // 第 3 步：检测九宫格位置
            Log.d(TAG, "步骤 3: 检测九宫格位置");
            List<Rect> gridRegions = imageRecognizer.detectGridRegions(screenshot);
            
            if (gridRegions.isEmpty()) {
                Log.e(TAG, "未能检测到九宫格");
                screenshot.recycle();
                return;
            }
            
            Log.d(TAG, "检测到 " + gridRegions.size() + " 个九宫格");
            
            // 第 4 步：识别包含目标物体的图片
            Log.d(TAG, "步骤 4: 识别包含目标物体的图片");
            List<Integer> matchingImages = imageRecognizer.findMatchingImages(
                screenshot, targetObject, gridRegions);
            
            if (matchingImages.isEmpty()) {
                Log.e(TAG, "未找到包含目标物体的图片");
                screenshot.recycle();
                return;
            }
            
            Log.d(TAG, "找到 " + matchingImages.size() + " 个匹配的图片: " + matchingImages);
            
            // 第 5 步：点击匹配的图片
            Log.d(TAG, "步骤 5: 点击匹配的图片");
            for (int index : matchingImages) {
                Rect region = gridRegions.get(index);
                int centerX = region.centerX();
                int centerY = region.centerY();
                
                Log.d(TAG, "点击图片 " + index + " 位置: (" + centerX + ", " + centerY + ")");
                clickSimulator.click(centerX, centerY);
                
                // 添加点击间隔
                Thread.sleep(300);
            }
            
            // 第 6 步：点击验证按钮
            Log.d(TAG, "步骤 6: 点击验证按钮");
            Thread.sleep(500);
            boolean verified = clickVerifyButton();
            
            if (verified) {
                Log.d(TAG, "==== 验证码识别完成！====");
            } else {
                Log.w(TAG, "未找到验证按钮");
            }
            
            // 释放截图资源
            screenshot.recycle();
            
        } catch (Exception e) {
            Log.e(TAG, "处理验证码时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 使用无障碍服务捕获屏幕内容（备用方案）
     */
    private Bitmap captureScreenWithAccessibility() {
        try {
            if (accessibilityService == null) {
                return null;
            }
            
            Log.d(TAG, "尝试使用无障碍服务获取屏幕内容...");
            
            // 这里可以通过无障碍服务获取界面节点信息
            // 但无法直接获取截图，需要其他方案
            
            // 返回 null，提示用户需要屏幕录制权限
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "使用无障碍服务捕获屏幕失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 点击验证按钮
     */
    private boolean clickVerifyButton() {
        try {
            if (accessibilityService == null) {
                Log.e(TAG, "AccessibilityService 未设置");
                return false;
            }
            
            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                return false;
            }
            
            // 查找包含"验证"文字的按钮
            List<AccessibilityNodeInfo> verifyButtons = 
                rootNode.findAccessibilityNodeInfosByText(VERIFY_BUTTON_TEXT);
            
            if (verifyButtons.isEmpty()) {
                Log.d(TAG, "未找到验证按钮");
                
                // 尝试查找其他可能的验证按钮文字
                String[] alternativeTexts = {"确认", "提交", "确定", "OK", "Submit"};
                for (String text : alternativeTexts) {
                    verifyButtons = rootNode.findAccessibilityNodeInfosByText(text);
                    if (!verifyButtons.isEmpty()) {
                        Log.d(TAG, "找到替代验证按钮: " + text);
                        break;
                    }
                }
            }
            
            if (!verifyButtons.isEmpty()) {
                AccessibilityNodeInfo button = verifyButtons.get(0);
                
                if (button.isClickable()) {
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "已点击验证按钮");
                    return true;
                } else {
                    // 尝试点击父节点
                    AccessibilityNodeInfo parent = button.getParent();
                    if (parent != null && parent.isClickable()) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "已点击验证按钮（父节点）");
                        return true;
                    }
                }
            }
            
            Log.d(TAG, "验证按钮不可点击，尝试使用坐标点击");
            return clickSimulator.clickByText(VERIFY_BUTTON_TEXT);
            
        } catch (Exception e) {
            Log.e(TAG, "点击验证按钮时出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (captchaOCR != null) {
            captchaOCR.release();
        }
        if (imageRecognizer != null) {
            imageRecognizer.release();
        }
        if (screenCapture != null) {
            screenCapture.release();
        }
        Log.d(TAG, "RealCaptchaSolver 资源已释放");
    }
}

