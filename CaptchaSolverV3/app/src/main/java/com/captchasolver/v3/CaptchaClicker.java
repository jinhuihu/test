package com.captchasolver.v3;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 验证码点击器
 * 负责执行自动点击操作，包括点击图片区域和验证按钮
 */
public class CaptchaClicker {
    
    private static final String TAG = "CaptchaClicker";
    private static final int CLICK_DELAY = 500; // 点击间隔500ms
    
    private Context context;
    private ExecutorService executorService;
    
    public CaptchaClicker(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 点击图片区域
     * @param regions 要点击的区域列表
     * @return 点击操作完成后的Future
     */
    public CompletableFuture<Void> clickImageRegions(List<Rect> regions) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        executorService.execute(() -> {
            try {
                Log.d(TAG, "开始点击 " + regions.size() + " 个图片区域");
                
                for (int i = 0; i < regions.size(); i++) {
                    Rect region = regions.get(i);
                    Log.d(TAG, "点击图片区域 " + (i + 1) + ": " + region);
                    
                    // 执行点击
                    clickRegion(region);
                    
                    // 等待间隔
                    if (i < regions.size() - 1) {
                        Thread.sleep(CLICK_DELAY);
                    }
                }
                
                Log.d(TAG, "所有图片区域点击完成");
                future.complete(null);
                
            } catch (Exception e) {
                Log.e(TAG, "点击图片区域失败", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * 点击验证按钮
     * @param textElements 文本元素列表
     * @return 点击操作完成后的Future
     */
    public CompletableFuture<Void> clickVerifyButton(List<OCRTextRecognizer.TextElement> textElements) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        executorService.execute(() -> {
            try {
                Log.d(TAG, "开始查找验证按钮");
                
                // 查找验证按钮
                OCRTextRecognizer.TextElement verifyButton = findVerifyButton(textElements);
                
                if (verifyButton != null) {
                    Log.d(TAG, "找到验证按钮: " + verifyButton.text + " 位置: " + verifyButton.bounds);
                    
                    // 点击验证按钮
                    clickRegion(verifyButton.bounds);
                    
                    Log.d(TAG, "验证按钮点击完成");
                } else {
                    Log.w(TAG, "未找到验证按钮");
                }
                
                future.complete(null);
                
            } catch (Exception e) {
                Log.e(TAG, "点击验证按钮失败", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * 查找验证按钮
     * @param textElements 文本元素列表
     * @return 验证按钮元素，如果未找到返回null
     */
    private OCRTextRecognizer.TextElement findVerifyButton(List<OCRTextRecognizer.TextElement> textElements) {
        String[] verifyKeywords = {
                "验证", "确认", "提交", "确定", "完成", "下一步",
                "verify", "confirm", "submit", "ok", "done", "next"
        };
        
        for (OCRTextRecognizer.TextElement element : textElements) {
            String text = element.text.toLowerCase().trim();
            
            for (String keyword : verifyKeywords) {
                if (text.contains(keyword.toLowerCase())) {
                    Log.d(TAG, "匹配到验证按钮关键词: " + keyword + " 在文本: " + element.text);
                    return element;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 点击指定区域
     * @param region 要点击的区域
     */
    private void clickRegion(Rect region) {
        try {
            // 计算点击坐标（区域中心）
            int x = region.centerX();
            int y = region.centerY();
            
            Log.d(TAG, "点击坐标: (" + x + ", " + y + ")");
            
            // 使用AccessibilityService执行点击
            if (CaptchaAccessibilityService.getInstance() != null) {
                CaptchaAccessibilityService.getInstance().performClick(x, y);
            } else {
                Log.w(TAG, "AccessibilityService未初始化，无法执行点击");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "点击区域失败", e);
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
