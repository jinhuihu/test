package com.captchasolver.v3;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * 悬浮窗触摸监听器
 * 实现悬浮窗的拖动功能
 */
public class FloatingWindowTouchListener implements View.OnTouchListener {
    
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    
    public FloatingWindowTouchListener(WindowManager windowManager, View floatingView) {
        this.windowManager = windowManager;
        this.floatingView = floatingView;
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录初始位置
                params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                // 计算移动距离
                int deltaX = (int) (event.getRawX() - initialTouchX);
                int deltaY = (int) (event.getRawY() - initialTouchY);
                
                // 更新位置
                params.x = initialX + deltaX;
                params.y = initialY + deltaY;
                
                // 边界检查
                int screenWidth = windowManager.getDefaultDisplay().getWidth();
                int screenHeight = windowManager.getDefaultDisplay().getHeight();
                
                if (params.x < 0) params.x = 0;
                if (params.y < 0) params.y = 0;
                if (params.x > screenWidth - floatingView.getWidth()) {
                    params.x = screenWidth - floatingView.getWidth();
                }
                if (params.y > screenHeight - floatingView.getHeight()) {
                    params.y = screenHeight - floatingView.getHeight();
                }
                
                // 更新悬浮窗位置
                windowManager.updateViewLayout(floatingView, params);
                return true;
                
            case MotionEvent.ACTION_UP:
                // 可以在这里添加点击事件处理
                return false;
        }
        
        return false;
    }
}
