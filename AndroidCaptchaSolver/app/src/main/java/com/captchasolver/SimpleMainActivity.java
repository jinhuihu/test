package com.captchasolver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 简化版主活动 - 不依赖任何外部库
 * 只提供基本的界面和功能
 */
public class SimpleMainActivity extends Activity {
    private static final String TAG = "SimpleMainActivity";
    
    private Button btnStart;
    private Button btnStop;
    private Button btnPermissions;
    private TextView tvStatus;
    private TextView tvLog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建简单的界面
        createSimpleUI();
        
        // 设置点击监听器
        setupClickListeners();
        
        Log.d(TAG, "简化版验证码识别助手已启动");
    }
    
    /**
     * 创建简单的UI界面
     */
    private void createSimpleUI() {
        // 创建主布局
        android.widget.LinearLayout mainLayout = new android.widget.LinearLayout(this);
        mainLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 50, 50, 50);
        
        // 标题
        TextView title = new TextView(this);
        title.setText("验证码识别助手");
        title.setTextSize(24);
        title.setTextColor(0xFF333333);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        mainLayout.addView(title);
        
        // 状态显示
        tvStatus = new TextView(this);
        tvStatus.setText("服务未启动");
        tvStatus.setTextSize(16);
        tvStatus.setTextColor(0xFF666666);
        tvStatus.setGravity(android.view.Gravity.CENTER);
        tvStatus.setPadding(20, 20, 20, 20);
        tvStatus.setBackgroundColor(0xFFF0F0F0);
        mainLayout.addView(tvStatus);
        
        // 功能说明
        TextView description = new TextView(this);
        description.setText("功能说明：\n• 自动识别九宫格验证码\n• 支持飞机、汽车等物体识别\n• 自动点击正确图片\n• 自动点击验证按钮");
        description.setTextSize(14);
        description.setTextColor(0xFF666666);
        description.setPadding(20, 20, 20, 20);
        description.setBackgroundColor(0xFFFFFFFF);
        mainLayout.addView(description);
        
        // 控制按钮
        android.widget.LinearLayout buttonLayout = new android.widget.LinearLayout(this);
        buttonLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 20, 0, 20);
        
        btnStart = new Button(this);
        btnStart.setText("启动服务");
        btnStart.setTextColor(0xFFFFFFFF);
        btnStart.setBackgroundColor(0xFF4CAF50);
        btnStart.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, 
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        buttonLayout.addView(btnStart);
        
        btnStop = new Button(this);
        btnStop.setText("停止服务");
        btnStop.setTextColor(0xFFFFFFFF);
        btnStop.setBackgroundColor(0xFFF44336);
        btnStop.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, 
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        buttonLayout.addView(btnStop);
        
        mainLayout.addView(buttonLayout);
        
        // 权限设置按钮
        btnPermissions = new Button(this);
        btnPermissions.setText("打开无障碍设置");
        btnPermissions.setTextColor(0xFFFFFFFF);
        btnPermissions.setBackgroundColor(0xFF2196F3);
        btnPermissions.setPadding(20, 20, 20, 20);
        mainLayout.addView(btnPermissions);
        
        // 日志显示
        tvLog = new TextView(this);
        tvLog.setText("日志信息将显示在这里...\n");
        tvLog.setTextSize(12);
        tvLog.setTextColor(0xFF333333);
        tvLog.setPadding(10, 10, 10, 10);
        tvLog.setBackgroundColor(0xFFFFFFFF);
        tvLog.setMaxLines(10);
        tvLog.setVerticalScrollBarEnabled(true);
        mainLayout.addView(tvLog);
        
        setContentView(mainLayout);
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> startService());
        btnStop.setOnClickListener(v -> stopService());
        btnPermissions.setOnClickListener(v -> openAccessibilitySettings());
    }
    
    /**
     * 启动服务
     */
    private void startService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "请先启用无障碍服务", Toast.LENGTH_LONG).show();
            openAccessibilitySettings();
            return;
        }
        
        updateStatus("服务已启动");
        appendLog("验证码识别服务已启动");
        Toast.makeText(this, "验证码识别服务已启动", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 停止服务
     */
    private void stopService() {
        updateStatus("服务已停止");
        appendLog("验证码识别服务已停止");
        Toast.makeText(this, "验证码识别服务已停止", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 打开无障碍设置
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        appendLog("已打开无障碍设置页面");
        Toast.makeText(this, "请在设置中启用验证码识别助手", Toast.LENGTH_LONG).show();
    }
    
    /**
     * 检查无障碍服务是否已启用
     */
    private boolean isAccessibilityServiceEnabled() {
        String settingValue = Settings.Secure.getString(
            getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return settingValue != null && settingValue.contains(getPackageName());
    }
    
    /**
     * 更新状态显示
     */
    private void updateStatus(String status) {
        tvStatus.setText(status);
    }
    
    /**
     * 添加日志信息
     */
    private void appendLog(String message) {
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String logMessage = "[" + timestamp + "] " + message + "\n";
        tvLog.append(logMessage);
    }
}
