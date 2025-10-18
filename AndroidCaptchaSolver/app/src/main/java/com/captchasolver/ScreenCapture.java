package com.captchasolver;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import java.nio.ByteBuffer;

/**
 * 屏幕截图模块
 * 使用MediaProjection API进行屏幕截图
 * 支持Android 5.0+的屏幕录制功能
 */
public class ScreenCapture {
    private static final String TAG = "ScreenCapture";
    
    private Context context;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    
    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    
    public ScreenCapture(Context context) {
        this.context = context;
        this.projectionManager = (MediaProjectionManager) 
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        // 获取屏幕尺寸
        getScreenSize();
    }
    
    /**
     * 获取屏幕尺寸信息
     */
    private void getScreenSize() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        
        Log.d(TAG, "屏幕尺寸: " + screenWidth + "x" + screenHeight + " 密度: " + screenDensity);
    }
    
    /**
     * 截取屏幕
     * @return 屏幕截图的Bitmap对象
     */
    public Bitmap takeScreenshot() {
        try {
            Log.d(TAG, "开始截取屏幕...");
            
            // 创建ImageReader
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, 
                PixelFormat.RGBA_8888, 1);
            
            // 设置ImageReader的回调
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG, "图片已准备就绪");
                }
            }, new Handler(Looper.getMainLooper()));
            
            // 创建VirtualDisplay
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, null
            );
            
            // 等待截图完成
            Thread.sleep(1000);
            
            // 获取最新的图片
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = imageToBitmap(image);
                image.close();
                return bitmap;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "截取屏幕时出错: " + e.getMessage(), e);
        } finally {
            // 清理资源
            if (virtualDisplay != null) {
                virtualDisplay.release();
            }
            if (imageReader != null) {
                imageReader.close();
            }
        }
        
        return null;
    }
    
    /**
     * 将Image转换为Bitmap
     * @param image Image对象
     * @return Bitmap对象
     */
    private Bitmap imageToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;
            
            Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride, 
                screenHeight, 
                Bitmap.Config.ARGB_8888
            );
            bitmap.copyPixelsFromBuffer(buffer);
            
            // 如果有行填充，需要裁剪
            if (rowPadding > 0) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
            }
            
            Log.d(TAG, "成功创建Bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "转换Image到Bitmap时出错: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 设置MediaProjection（需要在Activity中调用）
     * @param mediaProjection MediaProjection对象
     */
    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        Log.d(TAG, "MediaProjection已设置");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        Log.d(TAG, "ScreenCapture资源已释放");
    }
}
