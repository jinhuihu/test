package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * 点击模拟模块
 * 使用AccessibilityService进行模拟点击
 * 支持精确的坐标点击和节点点击
 */
public class ClickSimulator {
    private static final String TAG = "ClickSimulator";
    
    private Context context;
    private AccessibilityService accessibilityService;
    
    public ClickSimulator(Context context) {
        this.context = context;
    }
    
    /**
     * 设置AccessibilityService引用
     * @param service AccessibilityService实例
     */
    public void setAccessibilityService(AccessibilityService service) {
        this.accessibilityService = service;
    }
    
    /**
     * 在指定坐标进行点击
     * @param x X坐标
     * @param y Y坐标
     */
    public void click(int x, int y) {
        try {
            Log.d(TAG, "点击坐标: (" + x + ", " + y + ")");
            
            if (accessibilityService == null) {
                Log.e(TAG, "AccessibilityService未设置");
                return;
            }
            
            // 使用GestureDescription进行点击（Android 7.0+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                clickWithGesture(x, y);
            } else {
                // 使用AccessibilityNodeInfo进行点击（兼容旧版本）
                clickWithNodeInfo(x, y);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "点击时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 使用GestureDescription进行点击（Android 7.0+）
     * @param x X坐标
     * @param y Y坐标
     */
    private void clickWithGesture(int x, int y) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 创建点击手势
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(x, y);
                android.accessibilityservice.GestureDescription.StrokeDescription stroke = 
                    new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100);
                
                android.accessibilityservice.GestureDescription gesture = 
                    new android.accessibilityservice.GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();
                
                // 执行手势
                accessibilityService.dispatchGesture(gesture, null, null);
                Log.d(TAG, "手势点击执行完成");
            }
        } catch (Exception e) {
            Log.e(TAG, "手势点击失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 使用AccessibilityNodeInfo进行点击（兼容旧版本）
     * @param x X坐标
     * @param y Y坐标
     */
    private void clickWithNodeInfo(int x, int y) {
        try {
            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                return;
            }
            
            // 查找包含指定坐标的节点
            AccessibilityNodeInfo targetNode = findNodeAtCoordinates(rootNode, x, y);
            if (targetNode != null && targetNode.isClickable()) {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "节点点击执行完成");
            } else {
                Log.d(TAG, "未找到可点击的节点");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "节点点击失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查找指定坐标处的节点
     * @param rootNode 根节点
     * @param x X坐标
     * @param y Y坐标
     * @return 找到的节点
     */
    private AccessibilityNodeInfo findNodeAtCoordinates(AccessibilityNodeInfo rootNode, int x, int y) {
        if (rootNode == null) return null;
        
        Rect bounds = new Rect();
        rootNode.getBoundsInScreen(bounds);
        
        // 检查当前节点是否包含指定坐标
        if (bounds.contains(x, y)) {
            // 递归查找子节点
            for (int i = 0; i < rootNode.getChildCount(); i++) {
                AccessibilityNodeInfo child = rootNode.getChild(i);
                if (child != null) {
                    AccessibilityNodeInfo found = findNodeAtCoordinates(child, x, y);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return rootNode;
        }
        
        return null;
    }
    
    /**
     * 点击指定文本的节点
     * @param text 要点击的文本
     * @return 是否点击成功
     */
    public boolean clickByText(String text) {
        try {
            Log.d(TAG, "查找并点击文本: " + text);
            
            if (accessibilityService == null) {
                Log.e(TAG, "AccessibilityService未设置");
                return false;
            }
            
            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                return false;
            }
            
            // 查找包含指定文本的节点
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
            
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "成功点击文本: " + text);
                    return true;
                }
            }
            
            Log.d(TAG, "未找到可点击的文本: " + text);
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "按文本点击时出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 长按指定坐标
     * @param x X坐标
     * @param y Y坐标
     * @param duration 长按持续时间（毫秒）
     */
    public void longClick(int x, int y, int duration) {
        try {
            Log.d(TAG, "长按坐标: (" + x + ", " + y + ") 持续时间: " + duration + "ms");
            
            if (accessibilityService == null) {
                Log.e(TAG, "AccessibilityService未设置");
                return;
            }
            
            // 使用GestureDescription进行长按
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(x, y);
                
                android.accessibilityservice.GestureDescription.StrokeDescription stroke = 
                    new android.accessibilityservice.GestureDescription.StrokeDescription(
                        path, 0, duration);
                
                android.accessibilityservice.GestureDescription gesture = 
                    new android.accessibilityservice.GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();
                
                accessibilityService.dispatchGesture(gesture, null, null);
                Log.d(TAG, "长按手势执行完成");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "长按时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 滑动操作
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 结束X坐标
     * @param endY 结束Y坐标
     * @param duration 滑动持续时间（毫秒）
     */
    public void swipe(int startX, int startY, int endX, int endY, int duration) {
        try {
            Log.d(TAG, String.format("滑动: (%d, %d) -> (%d, %d) 持续时间: %dms", 
                startX, startY, endX, endY, duration));
            
            if (accessibilityService == null) {
                Log.e(TAG, "AccessibilityService未设置");
                return;
            }
            
            // 使用GestureDescription进行滑动
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(startX, startY);
                path.lineTo(endX, endY);
                
                android.accessibilityservice.GestureDescription.StrokeDescription stroke = 
                    new android.accessibilityservice.GestureDescription.StrokeDescription(
                        path, 0, duration);
                
                android.accessibilityservice.GestureDescription gesture = 
                    new android.accessibilityservice.GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();
                
                accessibilityService.dispatchGesture(gesture, null, null);
                Log.d(TAG, "滑动手势执行完成");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "滑动时出错: " + e.getMessage(), e);
        }
    }
}
