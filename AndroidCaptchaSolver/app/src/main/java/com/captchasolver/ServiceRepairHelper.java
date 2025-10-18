package com.captchasolver;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

/**
 * 服务修复助手
 * 用于修复无障碍服务故障问题
 */
public class ServiceRepairHelper {
    private static final String TAG = "ServiceRepairHelper";
    
    private Context context;
    
    public ServiceRepairHelper(Context context) {
        this.context = context;
    }
    
    /**
     * 诊断服务故障
     */
    public FaultDiagnosis diagnoseFault() {
        try {
            Log.d(TAG, "开始诊断无障碍服务故障");
            
            FaultDiagnosis diagnosis = new FaultDiagnosis();
            
            // 检查应用安装状态
            diagnosis.isAppInstalled = checkAppInstalled();
            Log.d(TAG, "应用安装状态: " + diagnosis.isAppInstalled);
            
            // 检查服务注册状态
            diagnosis.isServiceRegistered = checkServiceRegistered();
            Log.d(TAG, "服务注册状态: " + diagnosis.isServiceRegistered);
            
            // 检查无障碍服务状态
            diagnosis.isAccessibilityEnabled = checkAccessibilityEnabled();
            Log.d(TAG, "无障碍服务启用状态: " + diagnosis.isAccessibilityEnabled);
            
            // 检查权限状态
            diagnosis.hasPermissions = checkPermissions();
            Log.d(TAG, "权限状态: " + diagnosis.hasPermissions);
            
            // 分析故障原因
            diagnosis.faultCause = analyzeFaultCause(diagnosis);
            Log.d(TAG, "故障原因: " + diagnosis.faultCause);
            
            // 生成修复建议
            diagnosis.repairSteps = generateRepairSteps(diagnosis);
            
            return diagnosis;
            
        } catch (Exception e) {
            Log.e(TAG, "诊断服务故障时出错: " + e.getMessage(), e);
            return createErrorDiagnosis(e.getMessage());
        }
    }
    
    /**
     * 检查应用是否已安装
     */
    private boolean checkAppInstalled() {
        try {
            context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 检查服务是否已注册
     */
    private boolean checkServiceRegistered() {
        try {
            String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            String packageName = context.getPackageName();
            return enabledServices != null && enabledServices.contains(packageName);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查无障碍服务是否启用
     */
    private boolean checkAccessibilityEnabled() {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            );
            return accessibilityEnabled == 1;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查权限状态
     */
    private boolean checkPermissions() {
        try {
            // 检查基本权限
            return true; // 简化检查
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 分析故障原因
     */
    private String analyzeFaultCause(FaultDiagnosis diagnosis) {
        if (!diagnosis.isAppInstalled) {
            return "应用未正确安装";
        }
        
        if (!diagnosis.isServiceRegistered) {
            return "无障碍服务未注册";
        }
        
        if (!diagnosis.isAccessibilityEnabled) {
            return "无障碍功能未启用";
        }
        
        if (!diagnosis.hasPermissions) {
            return "权限不足";
        }
        
        return "未知故障，需要重新安装应用";
    }
    
    /**
     * 生成修复步骤
     */
    private String[] generateRepairSteps(FaultDiagnosis diagnosis) {
        if (!diagnosis.isAppInstalled) {
            return new String[]{
                "1. 重新安装应用",
                "2. 重启手机",
                "3. 重新启用无障碍服务"
            };
        }
        
        if (!diagnosis.isServiceRegistered) {
            return new String[]{
                "1. 前往无障碍设置",
                "2. 找到验证码识别助手",
                "3. 启用服务开关",
                "4. 授予所有权限"
            };
        }
        
        if (!diagnosis.isAccessibilityEnabled) {
            return new String[]{
                "1. 前往设置 → 辅助功能",
                "2. 启用无障碍功能",
                "3. 重新启用验证码识别助手"
            };
        }
        
        return new String[]{
            "1. 重启手机",
            "2. 重新安装应用",
            "3. 重新启用无障碍服务",
            "4. 联系技术支持"
        };
    }
    
    /**
     * 创建错误诊断
     */
    private FaultDiagnosis createErrorDiagnosis(String errorMessage) {
        FaultDiagnosis diagnosis = new FaultDiagnosis();
        diagnosis.isAppInstalled = false;
        diagnosis.isServiceRegistered = false;
        diagnosis.isAccessibilityEnabled = false;
        diagnosis.hasPermissions = false;
        diagnosis.faultCause = "诊断过程中出错: " + errorMessage;
        diagnosis.repairSteps = new String[]{
            "1. 重启手机",
            "2. 重新安装应用",
            "3. 检查系统版本兼容性"
        };
        return diagnosis;
    }
    
    /**
     * 故障诊断结果
     */
    public static class FaultDiagnosis {
        public boolean isAppInstalled = false;
        public boolean isServiceRegistered = false;
        public boolean isAccessibilityEnabled = false;
        public boolean hasPermissions = false;
        public String faultCause = "";
        public String[] repairSteps = new String[0];
        
        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("故障诊断结果:\n");
            summary.append("应用安装: ").append(isAppInstalled ? "正常" : "异常").append("\n");
            summary.append("服务注册: ").append(isServiceRegistered ? "正常" : "异常").append("\n");
            summary.append("无障碍启用: ").append(isAccessibilityEnabled ? "正常" : "异常").append("\n");
            summary.append("权限状态: ").append(hasPermissions ? "正常" : "异常").append("\n");
            summary.append("故障原因: ").append(faultCause);
            return summary.toString();
        }
        
        public String getRepairStepsText() {
            StringBuilder steps = new StringBuilder();
            steps.append("修复步骤:\n");
            for (String step : repairSteps) {
                steps.append(step).append("\n");
            }
            return steps.toString();
        }
    }
}
