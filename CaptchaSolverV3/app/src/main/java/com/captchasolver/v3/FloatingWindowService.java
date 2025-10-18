package com.captchasolver.v3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * 悬浮窗服务
 * 提供可移动的悬浮窗界面，方便用户进行快捷操作
 */
public class FloatingWindowService extends Service {
    
    private static final String TAG = "FloatingWindowService";
    private static final String CHANNEL_ID = "FloatingWindowChannel";
    private static final int NOTIFICATION_ID = 1002;
    
    private WindowManager windowManager;
    private View floatingView;
    private boolean isFloatingWindowShowing = false;
    
    // 悬浮窗组件
    private Button btnStartStop;
    private Button btnManualTrigger;
    private TextView tvStatus;
    private ImageView btnClose;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "FloatingWindowService created");
        
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FloatingWindowService started");
        
        startForeground(NOTIFICATION_ID, createNotification());
        
        if (intent != null && intent.getBooleanExtra("show_floating", true)) {
            showFloatingWindow();
        }
        
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * 显示悬浮窗
     */
    private void showFloatingWindow() {
        if (isFloatingWindowShowing) {
            Log.d(TAG, "悬浮窗已显示");
            return;
        }
        
        try {
            // 创建悬浮窗布局
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            floatingView = inflater.inflate(R.layout.floating_window, null);
            
            initFloatingViewComponents();
            setupFloatingViewListeners();
            
            // 设置悬浮窗参数
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            
            // 设置悬浮窗位置（右上角）
            params.gravity = Gravity.TOP | Gravity.END;
            params.x = 50;
            params.y = 200;
            
            // 添加到窗口管理器
            windowManager.addView(floatingView, params);
            isFloatingWindowShowing = true;
            
            Log.d(TAG, "悬浮窗显示成功");
            
        } catch (Exception e) {
            Log.e(TAG, "显示悬浮窗失败", e);
        }
    }
    
    /**
     * 隐藏悬浮窗
     */
    private void hideFloatingWindow() {
        if (!isFloatingWindowShowing || floatingView == null) {
            return;
        }
        
        try {
            windowManager.removeView(floatingView);
            isFloatingWindowShowing = false;
            floatingView = null;
            
            Log.d(TAG, "悬浮窗隐藏成功");
            
        } catch (Exception e) {
            Log.e(TAG, "隐藏悬浮窗失败", e);
        }
    }
    
    /**
     * 初始化悬浮窗组件
     */
    private void initFloatingViewComponents() {
        btnStartStop = floatingView.findViewById(R.id.btn_start_stop);
        btnManualTrigger = floatingView.findViewById(R.id.btn_manual_trigger);
        tvStatus = floatingView.findViewById(R.id.tv_status);
        btnClose = floatingView.findViewById(R.id.btn_close);
        
        updateFloatingViewStatus();
    }
    
    /**
     * 设置悬浮窗监听器
     */
    private void setupFloatingViewListeners() {
        // 开始/停止监控按钮
        btnStartStop.setOnClickListener(v -> {
            if (btnStartStop.getText().toString().contains("开始")) {
                startMonitoring();
            } else {
                stopMonitoring();
            }
        });
        
        // 手动触发按钮
        btnManualTrigger.setOnClickListener(v -> {
            triggerManualRecognition();
        });
        
        // 关闭按钮
        btnClose.setOnClickListener(v -> {
            hideFloatingWindow();
            stopSelf();
        });
        
        // 设置悬浮窗可拖动
        floatingView.setOnTouchListener(new FloatingWindowTouchListener(windowManager, floatingView));
    }
    
    /**
     * 启动监控
     */
    private void startMonitoring() {
        try {
            // 启动MainActivity并请求屏幕录制权限
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("action", "start_monitoring");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            
            // 更新UI状态
            btnStartStop.setText("停止监控");
            tvStatus.setText("启动中...");
            
            Log.d(TAG, "启动监控请求已发送");
            
        } catch (Exception e) {
            Log.e(TAG, "启动监控失败", e);
            tvStatus.setText("启动失败");
        }
    }
    
    /**
     * 停止监控
     */
    private void stopMonitoring() {
        try {
            // 停止ScreenMonitorService
            Intent serviceIntent = new Intent(this, ScreenMonitorService.class);
            stopService(serviceIntent);
            
            // 更新UI状态
            btnStartStop.setText("开始监控");
            tvStatus.setText("已停止");
            
            Log.d(TAG, "监控已停止");
            
        } catch (Exception e) {
            Log.e(TAG, "停止监控失败", e);
        }
    }
    
    /**
     * 触发手动识别
     */
    private void triggerManualRecognition() {
        try {
            tvStatus.setText("手动识别中...");
            
            // 启动MainActivity并触发手动识别
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("action", "manual_trigger");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            
            Log.d(TAG, "手动识别请求已发送");
            
        } catch (Exception e) {
            Log.e(TAG, "手动识别失败", e);
            tvStatus.setText("识别失败");
        }
    }
    
    /**
     * 更新悬浮窗状态
     */
    private void updateFloatingViewStatus() {
        // 这里应该根据实际服务状态更新UI
        // 简化实现
        btnStartStop.setText("开始监控");
        tvStatus.setText("准备就绪");
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮窗服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("验证码识别助手悬浮窗服务");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 创建通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("验证码识别助手")
                .setContentText("悬浮窗正在运行...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FloatingWindowService destroyed");
        
        hideFloatingWindow();
    }
}
