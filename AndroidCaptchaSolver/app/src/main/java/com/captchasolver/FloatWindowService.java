package com.captchasolver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 悬浮窗服务
 * 提供便捷的验证码识别操作界面
 */
public class FloatWindowService extends Service {
    private static final String TAG = "FloatWindowService";
    
    private WindowManager windowManager;
    private View floatView;
    private boolean isFloatViewShowing = false;
    
    // 悬浮窗组件
    private Button btnTrigger;
    private Button btnClose;
    private Button btnSettings;
    private TextView tvStatus;
    private LinearLayout layout;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "FloatWindowService 已创建");
        
        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "没有悬浮窗权限");
            return;
        }
        
        createFloatView();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("show".equals(action)) {
                showFloatView();
            } else if ("hide".equals(action)) {
                hideFloatView();
            } else if ("toggle".equals(action)) {
                toggleFloatView();
            }
        }
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        hideFloatView();
        Log.d(TAG, "FloatWindowService 已销毁");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * 创建悬浮窗视图
     */
    private void createFloatView() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        
        // 创建悬浮窗布局
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xCC000000); // 半透明黑色背景
        layout.setPadding(20, 20, 20, 20);
        
        // 状态显示
        tvStatus = new TextView(this);
        tvStatus.setText("验证码识别助手");
        tvStatus.setTextColor(0xFFFFFFFF);
        tvStatus.setTextSize(14);
        tvStatus.setGravity(Gravity.CENTER);
        layout.addView(tvStatus);
        
        // 触发按钮
        btnTrigger = new Button(this);
        btnTrigger.setText("识别验证码");
        btnTrigger.setTextColor(0xFFFFFFFF);
        btnTrigger.setBackgroundColor(0xFF4CAF50);
        btnTrigger.setPadding(20, 10, 20, 10);
        btnTrigger.setOnClickListener(v -> triggerRecognition());
        layout.addView(btnTrigger);
        
        // 设置按钮
        btnSettings = new Button(this);
        btnSettings.setText("打开设置");
        btnSettings.setTextColor(0xFFFFFFFF);
        btnSettings.setBackgroundColor(0xFF2196F3);
        btnSettings.setPadding(20, 10, 20, 10);
        btnSettings.setOnClickListener(v -> openSettings());
        layout.addView(btnSettings);
        
        // 关闭按钮
        btnClose = new Button(this);
        btnClose.setText("关闭浮窗");
        btnClose.setTextColor(0xFFFFFFFF);
        btnClose.setBackgroundColor(0xFFF44336);
        btnClose.setPadding(20, 10, 20, 10);
        btnClose.setOnClickListener(v -> hideFloatView());
        layout.addView(btnClose);
        
        // 设置悬浮窗参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        
        params.format = PixelFormat.TRANSLUCENT;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;
        
        floatView = layout;
        floatView.setTag("FloatWindow");
        
        Log.d(TAG, "悬浮窗视图已创建");
    }
    
    /**
     * 显示悬浮窗
     */
    private void showFloatView() {
        if (!isFloatViewShowing && floatView != null) {
            try {
                windowManager.addView(floatView, getFloatViewParams());
                isFloatViewShowing = true;
                updateStatus("悬浮窗已显示");
                Log.d(TAG, "悬浮窗已显示");
            } catch (Exception e) {
                Log.e(TAG, "显示悬浮窗失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 隐藏悬浮窗
     */
    private void hideFloatView() {
        if (isFloatViewShowing && floatView != null) {
            try {
                windowManager.removeView(floatView);
                isFloatViewShowing = false;
                Log.d(TAG, "悬浮窗已隐藏");
            } catch (Exception e) {
                Log.e(TAG, "隐藏悬浮窗失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 切换悬浮窗显示状态
     */
    private void toggleFloatView() {
        if (isFloatViewShowing) {
            hideFloatView();
        } else {
            showFloatView();
        }
    }
    
    /**
     * 触发验证码识别
     */
    private void triggerRecognition() {
        Log.d(TAG, "悬浮窗触发验证码识别");
        updateStatus("正在识别...");
        
        // 通知 CaptchaService 触发识别
        CaptchaService.triggerCaptchaRecognition();
        
        // 延迟更新状态
        btnTrigger.postDelayed(() -> updateStatus("识别完成"), 3000);
    }
    
    /**
     * 打开设置
     */
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        updateStatus("已打开无障碍设置");
    }
    
    /**
     * 更新状态显示
     */
    private void updateStatus(String status) {
        if (tvStatus != null) {
            tvStatus.setText(status);
        }
    }
    
    /**
     * 获取悬浮窗参数
     */
    private WindowManager.LayoutParams getFloatViewParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        
        params.format = PixelFormat.TRANSLUCENT;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;
        
        return params;
    }
    
    /**
     * 启动悬浮窗服务
     */
    public static void startFloatWindow(Context context) {
        Intent intent = new Intent(context, FloatWindowService.class);
        intent.putExtra("action", "show");
        context.startService(intent);
    }
    
    /**
     * 隐藏悬浮窗服务
     */
    public static void hideFloatWindow(Context context) {
        Intent intent = new Intent(context, FloatWindowService.class);
        intent.putExtra("action", "hide");
        context.startService(intent);
    }
    
    /**
     * 切换悬浮窗显示
     */
    public static void toggleFloatWindow(Context context) {
        Intent intent = new Intent(context, FloatWindowService.class);
        intent.putExtra("action", "toggle");
        context.startService(intent);
    }
}
