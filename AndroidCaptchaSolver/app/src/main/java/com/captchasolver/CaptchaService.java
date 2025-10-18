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
    
    private RealCaptchaSolver captchaSolver;
    private Handler mainHandler;
    
    // 验证码相关常量
    private static final String CAPTCHA_PACKAGE = "com.android.chrome"; // 浏览器包名
    private static final String VERIFY_BUTTON_TEXT = "验证";
    private static final int GRID_SIZE = 3; // 3x3网格
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化 Handler
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化真实的验证码识别器
        captchaSolver = new RealCaptchaSolver(this);
        captchaSolver.setAccessibilityService(this);
        
        Log.d(TAG, "验证码识别服务已创建（使用真实识别器）");
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
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        
        String packageName = event.getPackageName().toString();
        Log.d(TAG, "检测到事件: " + event.getEventType() + " 包名: " + packageName);
        
        // 检查是否在目标应用中
        if (packageName.contains("chrome") || packageName.contains("browser")) {
            // 延迟执行，确保界面完全加载
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    captchaSolver.processCaptcha();
                }
            }, 1000);
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
        Log.d(TAG, "验证码识别服务已销毁");
    }
}
