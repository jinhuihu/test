package com.captchasolver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * 主活动类 - 验证码识别和自动点击
 * 支持九宫格图片验证码的自动识别和点击
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CaptchaSolver";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private Button btnStart;
    private Button btnStop;
    private Button btnPermissions;
    private TextView tvStatus;
    private TextView tvLog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化UI组件
        initializeViews();
        
        // 设置点击监听器
        setupClickListeners();
        
        // 检查并请求权限
        checkPermissions();
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeViews() {
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnPermissions = findViewById(R.id.btn_permissions);
        tvStatus = findViewById(R.id.tv_status);
        tvLog = findViewById(R.id.tv_log);
        
        // 初始化状态
        updateStatus("服务未启动");
        appendLog("应用已启动，等待用户操作...");
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
     * 检查并请求必要权限
     */
    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // 检查屏幕录制权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        
        // 检查存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            appendLog("所有权限已授予");
        }
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
     * 启动服务
     */
    private void startService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "请先启用无障碍服务", Toast.LENGTH_LONG).show();
            openAccessibilitySettings();
            return;
        }
        
        Intent serviceIntent = new Intent(this, CaptchaService.class);
        startService(serviceIntent);
        
        updateStatus("服务已启动");
        appendLog("验证码识别服务已启动");
        Toast.makeText(this, "验证码识别服务已启动", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 停止服务
     */
    private void stopService() {
        Intent serviceIntent = new Intent(this, CaptchaService.class);
        stopService(serviceIntent);
        
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
        
        // 自动滚动到底部
        tvLog.post(() -> {
            int scrollAmount = tvLog.getLayout().getLineTop(tvLog.getLineCount()) - tvLog.getHeight();
            if (scrollAmount > 0) {
                tvLog.scrollTo(0, scrollAmount);
            } else {
                tvLog.scrollTo(0, 0);
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                appendLog("所有权限已授予");
            } else {
                appendLog("部分权限被拒绝，可能影响功能使用");
                Toast.makeText(this, "需要所有权限才能正常工作", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 检查无障碍服务状态
        if (isAccessibilityServiceEnabled()) {
            updateStatus("无障碍服务已启用");
            appendLog("无障碍服务已启用，可以启动识别服务");
        } else {
            updateStatus("无障碍服务未启用");
            appendLog("无障碍服务未启用，请先启用服务");
        }
    }
}
