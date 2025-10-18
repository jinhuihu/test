package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

/**
 * 服务健康检查器
 * 用于检查和修复无障碍服务状态
 */
public class ServiceHealthChecker {
    private static final String TAG = "ServiceHealthChecker";
    
    private Context context;
    
    public ServiceHealthChecker(Context context) {
        this.context = context;
    }
    
    /**
     * 检查无障碍服务状态
     */
    public ServiceStatus checkAccessibilityServiceStatus() {
        try {
            String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            Log.d(TAG, "当前启用的无障碍服务: " + enabledServices);
            
            String packageName = context.getPackageName();
            String serviceName = packageName + "/" + packageName + ".CaptchaService";
            
            boolean isEnabled = enabledServices != null && enabledServices.contains(packageName);
            
            Log.d(TAG, "验证码识别助手服务状态: " + (isEnabled ? "已启用" : "未启用"));
            Log.d(TAG, "期望的服务名称: " + serviceName);
            
            if (isEnabled) {
                return ServiceStatus.ENABLED;
            } else {
                return ServiceStatus.DISABLED;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "检查无障碍服务状态时出错: " + e.getMessage(), e);
            return ServiceStatus.ERROR;
        }
    }
    
    /**
     * 检查无障碍服务是否可用
     */
    public boolean isAccessibilityServiceAvailable() {
        try {
            return Settings.canDrawOverlays(context);
        } catch (Exception e) {
            Log.e(TAG, "检查无障碍服务可用性时出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取服务状态描述
     */
    public String getServiceStatusDescription() {
        ServiceStatus status = checkAccessibilityServiceStatus();
        
        switch (status) {
            case ENABLED:
                return "无障碍服务已启用";
            case DISABLED:
                return "无障碍服务未启用，请前往设置启用";
            case ERROR:
                return "检查服务状态时出错";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 获取修复建议
     */
    public String getFixSuggestion() {
        ServiceStatus status = checkAccessibilityServiceStatus();
        
        switch (status) {
            case ENABLED:
                return "服务已正常，如果仍有问题请重启应用";
            case DISABLED:
                return "请按以下步骤修复：\n1. 点击'打开无障碍设置'\n2. 找到'验证码识别助手'\n3. 打开开关\n4. 授予所有权限";
            case ERROR:
                return "请重新安装应用或重启手机";
            default:
                return "请重新启用无障碍服务";
        }
    }
    
    /**
     * 服务状态枚举
     */
    public enum ServiceStatus {
        ENABLED,    // 已启用
        DISABLED,   // 未启用
        ERROR       // 错误
    }
}
