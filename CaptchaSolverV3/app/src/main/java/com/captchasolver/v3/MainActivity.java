package com.captchasolver.v3;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 主Activity - 验证码识别助手V3
 * 提供权限管理、服务启动、悬浮窗控制等功能
 */
public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final int REQUEST_CODE_ACCESSIBILITY = 1002;
    private static final int REQUEST_CODE_OVERLAY = 1003;
    private static final int REQUEST_CODE_MEDIA_PROJECTION = 1004;
    
    private Button btnStartMonitoring;
    private Button btnStopMonitoring;
    private Button btnShowFloating;
    private Button btnHideFloating;
    private Button btnEnableAccessibility;
    private Button btnGrantPermissions;
    private TextView tvStatus;
    private TextView tvLog;
    private ScrollView scrollLog;
    
    private boolean isMonitoring = false;
    private FloatingWindowService floatingService;
    private MediaProjectionManager projectionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
        initMediaProjection();
        updateUI();
        
        // 处理从悬浮窗发来的请求
        handleIntentAction(getIntent());
    }
    
    /**
     * 初始化媒体投影管理器
     */
    private void initMediaProjection() {
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }
    
    /**
     * 处理Intent动作
     */
    private void handleIntentAction(Intent intent) {
        if (intent != null && intent.hasExtra("action")) {
            String action = intent.getStringExtra("action");
            switch (action) {
                case "start_monitoring":
                    // 延迟一下确保UI完全加载
                    new android.os.Handler().postDelayed(() -> {
                        startMonitoring();
                    }, 500);
                    break;
                case "manual_trigger":
                    // 手动触发识别
                    triggerManualRecognition();
                    break;
            }
        }
    }
    
    /**
     * 手动触发识别
     */
    private void triggerManualRecognition() {
        if (isMonitoring) {
            addLog("手动触发验证码识别");
            // 这里可以触发一次性的识别，不需要完整的监控流程
            Toast.makeText(this, "手动识别已触发", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先启动监控", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        btnStartMonitoring = findViewById(R.id.btn_start_monitoring);
        btnStopMonitoring = findViewById(R.id.btn_stop_monitoring);
        btnShowFloating = findViewById(R.id.btn_show_floating);
        btnHideFloating = findViewById(R.id.btn_hide_floating);
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        btnGrantPermissions = findViewById(R.id.btn_grant_permissions);
        tvStatus = findViewById(R.id.tv_status);
        tvLog = findViewById(R.id.tv_log);
        scrollLog = findViewById(R.id.scroll_log);
    }
    
    /**
     * 设置按钮监听器
     */
    private void setupListeners() {
        btnStartMonitoring.setOnClickListener(v -> startMonitoring());
        btnStopMonitoring.setOnClickListener(v -> stopMonitoring());
        btnShowFloating.setOnClickListener(v -> showFloatingWindow());
        btnHideFloating.setOnClickListener(v -> hideFloatingWindow());
        btnEnableAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnGrantPermissions.setOnClickListener(v -> requestPermissions());
    }
    
    /**
     * 更新UI状态
     */
    private void updateUI() {
        boolean hasPermissions = checkPermissions();
        boolean accessibilityEnabled = CaptchaAccessibilityService.isServiceEnabled(this);
        
        btnStartMonitoring.setEnabled(hasPermissions && accessibilityEnabled && !isMonitoring);
        btnStopMonitoring.setEnabled(isMonitoring);
        btnShowFloating.setEnabled(hasPermissions && !isMonitoring);
        btnHideFloating.setEnabled(hasPermissions && isMonitoring);
        btnEnableAccessibility.setEnabled(!accessibilityEnabled);
        btnGrantPermissions.setEnabled(!hasPermissions);
        
        // 更新状态显示
        if (!hasPermissions) {
            tvStatus.setText("需要授予权限");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else if (!accessibilityEnabled) {
            tvStatus.setText("需要启用无障碍服务");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else if (isMonitoring) {
            tvStatus.setText("正在监控中...");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            tvStatus.setText("准备就绪");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
        }
    }
    
    /**
     * 检查所需权限
     */
    private boolean checkPermissions() {
        // 检查悬浮窗权限
        boolean overlayPermission = Settings.canDrawOverlays(this);
        
        // 检查Android 13+媒体权限
        boolean mediaPermissions = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaPermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12及以下检查存储权限
            mediaPermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        
        // 检查其他必要权限
        boolean otherPermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        
        return overlayPermission && mediaPermissions && otherPermissions;
    }
    
    /**
     * 请求权限
     */
    private void requestPermissions() {
        // 根据Android版本请求不同的权限
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+请求媒体权限
            permissions = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.RECORD_AUDIO
            };
        } else {
            // Android 12及以下请求存储权限
            permissions = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.RECORD_AUDIO
            };
        }
        
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS);
        
        // 请求悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY);
        }
    }
    
    /**
     * 打开无障碍设置
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "请在无障碍设置中找到'验证码识别助手V3'并启用", Toast.LENGTH_LONG).show();
    }
    
    /**
     * 开始监控
     */
    private void startMonitoring() {
        if (!checkPermissions()) {
            Toast.makeText(this, "请先授予所需权限", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!CaptchaAccessibilityService.isServiceEnabled(this)) {
            Toast.makeText(this, "请先启用无障碍服务", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 请求屏幕录制权限
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE_MEDIA_PROJECTION);
    }
    
    /**
     * 停止监控
     */
    private void stopMonitoring() {
        Intent serviceIntent = new Intent(this, ScreenMonitorService.class);
        stopService(serviceIntent);
        
        isMonitoring = false;
        updateUI();
        addLog("验证码监控已停止");
    }
    
    /**
     * 显示悬浮窗
     */
    private void showFloatingWindow() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show();
            requestPermissions();
            return;
        }
        
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        addLog("悬浮窗已显示");
    }
    
    /**
     * 隐藏悬浮窗
     */
    private void hideFloatingWindow() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        stopService(serviceIntent);
        
        addLog("悬浮窗已隐藏");
    }
    
    /**
     * 添加日志信息
     */
    private void addLog(String message) {
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";
        
        runOnUiThread(() -> {
            tvLog.append(logEntry);
            scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentAction(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "权限授予成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "部分权限被拒绝，功能可能受限", Toast.LENGTH_SHORT).show();
            }
            
            updateUI();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show();
            }
            updateUI();
        } else if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                // 屏幕录制权限已授予，启动监控服务
                startScreenMonitorService(data);
            } else {
                Toast.makeText(this, "屏幕录制权限被拒绝", Toast.LENGTH_SHORT).show();
                addLog("屏幕录制权限被拒绝，无法进行监控");
            }
        }
    }
    
    /**
     * 启动屏幕监控服务
     */
    private void startScreenMonitorService(Intent mediaProjectionData) {
        try {
            Intent serviceIntent = new Intent(this, ScreenMonitorService.class);
            serviceIntent.putExtra("start_monitoring", true);
            serviceIntent.putExtra("media_projection_data", mediaProjectionData);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            isMonitoring = true;
            updateUI();
            addLog("验证码监控已启动");
            addLog("屏幕录制权限已授予，开始监控屏幕内容");
            addLog("调试：服务启动请求已发送");
            
        } catch (Exception e) {
            addLog("错误：启动监控服务失败 - " + e.getMessage());
            Log.e("MainActivity", "启动监控服务失败", e);
        }
    }
}
