package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * 验证码识别服务 - 继承自AccessibilityService
 * 负责监听屏幕变化，识别验证码并自动点击
 */
public class CaptchaService extends AccessibilityService {
    private static final String TAG = "CaptchaService";
    
    private static CaptchaService instance;
    
    private SimpleTestSolver captchaSolver;
    private Handler mainHandler;
    
    // 验证码相关常量
    private static final String CAPTCHA_PACKAGE = "com.android.chrome"; // 浏览器包名
    private static final String VERIFY_BUTTON_TEXT = "验证";
    private static final int GRID_SIZE = 3; // 3x3网格
    
    // 防止重复触发
    private boolean isProcessing = false;
    private long lastProcessTime = 0;
    private static final long MIN_PROCESS_INTERVAL = 5000; // 最小处理间隔5秒
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        instance = this;
        
        // 初始化 Handler
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化简单测试识别器（用于排查问题）
        captchaSolver = new SimpleTestSolver(this);
        captchaSolver.setAccessibilityService(this);
        
        Log.d(TAG, "验证码识别服务已创建（使用简单测试识别器）");
        logToActivity("简单测试识别服务已创建");
    }
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        
        // 配置无障碍服务信息
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 100;
        
        setServiceInfo(info);
        Log.d(TAG, "验证码识别服务已连接");
        logToActivity("✓ 无障碍服务已连接");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        
        String packageName = event.getPackageName().toString();
        int eventType = event.getEventType();
        
        // 只处理窗口状态改变事件（避免内容变化事件太频繁）
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        
        // 检查是否正在处理或距离上次处理时间过短
        long currentTime = System.currentTimeMillis();
        if (isProcessing) {
            Log.d(TAG, "正在处理中，跳过此次事件");
            return;
        }
        
        if (currentTime - lastProcessTime < MIN_PROCESS_INTERVAL) {
            Log.d(TAG, "距离上次处理时间过短，跳过此次事件");
            return;
        }
        
        // 记录事件
        Log.d(TAG, "事件: " + eventTypeToString(eventType) + " 包名: " + packageName);
        logToActivity("事件: " + eventTypeToString(eventType) + " 包名: " + packageName);
        
        // 监听所有应用，包括币安APP和Chrome
        Log.d(TAG, "检测到应用: " + packageName + "，准备处理验证码");
        logToActivity("检测到应用: " + packageName);
        
        // 标记为正在处理
        isProcessing = true;
        lastProcessTime = currentTime;
        
        // 延迟执行，确保界面完全加载
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "触发验证码识别...");
                    logToActivity("触发验证码识别...");
                    captchaSolver.processCaptcha();
                } finally {
                    // 处理完成后重置标志
                    isProcessing = false;
                }
            }
        }, 2000); // 2秒延迟，给应用足够时间加载
    }
    
    /**
     * 将事件类型转换为可读字符串
     */
    private String eventTypeToString(int eventType) {
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                return "WINDOW_CONTENT_CHANGED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "WINDOW_STATE_CHANGED";
            default:
                return "TYPE_" + eventType;
        }
    }
    
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "验证码识别服务被中断");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (captchaSolver != null) {
            captchaSolver.release();
        }
        instance = null;
        Log.d(TAG, "验证码识别服务已销毁");
    }
    
    /**
     * 手动触发验证码识别（供 Activity 调用）
     */
    public static void triggerCaptchaRecognition() {
        if (instance != null && instance.captchaSolver != null) {
            Log.d(TAG, "手动触发验证码识别");
            instance.logToActivity("手动触发识别...");
            
            instance.mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    instance.captchaSolver.processCaptcha();
                }
            });
        } else {
            Log.e(TAG, "服务未运行或识别器未初始化");
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
}
