package com.captchasolver.v3;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Service;
import android.content.Context;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证码无障碍服务
 * 提供点击操作和节点查找功能
 */
public class CaptchaAccessibilityService extends AccessibilityService {
    
    private static final String TAG = "CaptchaAccessibilityService";
    
    private static CaptchaAccessibilityService instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "CaptchaAccessibilityService created");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "CaptchaAccessibilityService destroyed");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 这里可以添加事件监听逻辑，但主要功能由ScreenMonitorService处理
        // Log.d(TAG, "AccessibilityEvent: " + event.getEventType());
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted");
    }
    
    /**
     * 获取服务实例
     * @return 服务实例
     */
    public static CaptchaAccessibilityService getInstance() {
        return instance;
    }
    
    /**
     * 检查服务是否已启用
     * @param context 上下文
     * @return 是否已启用
     */
    public static boolean isServiceEnabled(Context context) {
        String enabledServices = android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        
        String serviceName = context.getPackageName() + "/" + CaptchaAccessibilityService.class.getName();
        return enabledServices != null && enabledServices.contains(serviceName);
    }
    
    /**
     * 执行点击操作
     * @param x 点击X坐标
     * @param y 点击Y坐标
     */
    public void performClick(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 使用手势描述执行点击
            Path path = new Path();
            path.moveTo(x, y);
            
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 100);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(stroke)
                    .build();
            
            dispatchGesture(gesture, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d(TAG, "点击完成: (" + x + ", " + y + ")");
                }
                
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    Log.w(TAG, "点击取消: (" + x + ", " + y + ")");
                }
            }, null);
        } else {
            Log.w(TAG, "Android版本过低，无法执行手势点击");
        }
    }
    
    /**
     * 查找包含指定文本的节点
     * @param text 要查找的文本
     * @return 匹配的节点列表
     */
    public List<AccessibilityNodeInfo> findNodesByText(String text) {
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        
        if (getRootInActiveWindow() != null) {
            findNodesByTextRecursive(getRootInActiveWindow(), text, nodes);
        }
        
        return nodes;
    }
    
    /**
     * 递归查找包含指定文本的节点
     * @param node 当前节点
     * @param text 要查找的文本
     * @param result 结果列表
     */
    private void findNodesByTextRecursive(AccessibilityNodeInfo node, String text, List<AccessibilityNodeInfo> result) {
        if (node == null) {
            return;
        }
        
        // 检查当前节点
        if (node.getText() != null && node.getText().toString().contains(text)) {
            result.add(node);
        }
        
        // 检查子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findNodesByTextRecursive(child, text, result);
                child.recycle();
            }
        }
    }
    
    /**
     * 查找可点击的节点
     * @return 可点击的节点列表
     */
    public List<AccessibilityNodeInfo> findClickableNodes() {
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        
        if (getRootInActiveWindow() != null) {
            findClickableNodesRecursive(getRootInActiveWindow(), nodes);
        }
        
        return nodes;
    }
    
    /**
     * 递归查找可点击的节点
     * @param node 当前节点
     * @param result 结果列表
     */
    private void findClickableNodesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        if (node == null) {
            return;
        }
        
        // 检查当前节点是否可点击
        if (node.isClickable()) {
            result.add(node);
        }
        
        // 检查子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findClickableNodesRecursive(child, result);
                child.recycle();
            }
        }
    }
    
    /**
     * 点击节点
     * @param node 要点击的节点
     * @return 是否点击成功
     */
    public boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null || !node.isClickable()) {
            return false;
        }
        
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }
    
    /**
     * 获取节点文本
     * @param node 节点
     * @return 节点文本，如果为空返回空字符串
     */
    public String getNodeText(AccessibilityNodeInfo node) {
        if (node == null || node.getText() == null) {
            return "";
        }
        return node.getText().toString();
    }
    
    /**
     * 获取节点边界
     * @param node 节点
     * @return 节点边界，如果获取失败返回null
     */
    public android.graphics.Rect getNodeBounds(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        
        android.graphics.Rect bounds = new android.graphics.Rect();
        node.getBoundsInScreen(bounds);
        return bounds;
    }
}
