package com.captchasolver.v3;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 屏幕监控服务
 * 负责持续监控屏幕内容，检测验证码并执行识别和点击操作
 */
public class ScreenMonitorService extends Service {
    
    private static final String TAG = "ScreenMonitorService";
    private static final String CHANNEL_ID = "ScreenMonitorChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SCREEN_CHECK_INTERVAL = 2000; // 2秒检查一次
    
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private WindowManager windowManager;
    private DisplayMetrics displayMetrics;
    
    private Handler mainHandler;
    private ExecutorService executorService;
    private OCRTextRecognizer ocrRecognizer;
    private ImageContentAnalyzer imageAnalyzer;
    private CaptchaClicker captchaClicker;
    
    private boolean isMonitoring = false;
    private boolean isProcessing = false;
    
    // 屏幕录制相关
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    
    // Binder for activity communication
    private final IBinder binder = new ScreenMonitorBinder();
    
    public class ScreenMonitorBinder extends Binder {
        public ScreenMonitorService getService() {
            return ScreenMonitorService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "ScreenMonitorService created");
        
        // 初始化组件
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        
        // 初始化处理器
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newFixedThreadPool(3);
        
        // 初始化识别器
        ocrRecognizer = new OCRTextRecognizer();
        imageAnalyzer = new ImageContentAnalyzer();
        captchaClicker = new CaptchaClicker(this);
        
        // 获取屏幕参数
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        screenDensity = displayMetrics.densityDpi;
        
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ScreenMonitorService started");
        
        startForeground(NOTIFICATION_ID, createNotification());
        
        if (intent != null && intent.getBooleanExtra("start_monitoring", false)) {
            Log.d(TAG, "收到启动监控请求");
            // 获取MediaProjection数据
            Intent mediaProjectionData = intent.getParcelableExtra("media_projection_data");
            if (mediaProjectionData != null) {
                Log.d(TAG, "MediaProjection数据已接收，开始初始化");
                initializeMediaProjection(mediaProjectionData);
            } else {
                Log.w(TAG, "MediaProjection数据为空");
            }
            startMonitoring();
        } else {
            Log.w(TAG, "没有收到启动监控请求或intent为空");
        }
        
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * 初始化MediaProjection
     */
    private void initializeMediaProjection(Intent mediaProjectionData) {
        try {
            mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, mediaProjectionData);
            
            // 注册MediaProjection回调
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.d(TAG, "MediaProjection已停止");
                    isMonitoring = false;
                }
                
                @Override
                public void onCapturedContentResize(int width, int height) {
                    Log.d(TAG, "MediaProjection内容尺寸变化: " + width + "x" + height);
                }
                
                @Override
                public void onCapturedContentVisibilityChanged(boolean isVisible) {
                    Log.d(TAG, "MediaProjection内容可见性变化: " + isVisible);
                }
            }, null);
            
            Log.d(TAG, "MediaProjection初始化成功，回调已注册");
        } catch (Exception e) {
            Log.e(TAG, "MediaProjection初始化失败", e);
        }
    }
    
    /**
     * 开始监控屏幕
     */
    public void startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "监控已在运行中");
            return;
        }
        
        Log.d(TAG, "开始监控屏幕");
        
        // 检查是否有MediaProjection权限
        if (mediaProjection == null) {
            Log.w(TAG, "MediaProjection未初始化，需要用户授予屏幕录制权限");
            // 这里应该通过Intent请求屏幕录制权限，但需要Activity支持
            // 暂时先启动监控，在captureScreen时会提示用户
        }
        
        isMonitoring = true;
        
        // 启动定期检查
        mainHandler.postDelayed(screenCheckRunnable, SCREEN_CHECK_INTERVAL);
    }
    
    /**
     * 停止监控屏幕
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        Log.d(TAG, "停止监控屏幕");
        isMonitoring = false;
        
        // 移除定期检查
        mainHandler.removeCallbacks(screenCheckRunnable);
        
        // 停止屏幕录制
        stopScreenCapture();
    }
    
    /**
     * 屏幕检查任务
     */
    private final Runnable screenCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isMonitoring || isProcessing) {
                // 如果不在监控状态或正在处理，跳过本次检查
                mainHandler.postDelayed(this, SCREEN_CHECK_INTERVAL);
                return;
            }
            
            // 异步检查屏幕内容
            executorService.execute(() -> {
                checkScreenContent();
            });
            
            // 安排下次检查
            mainHandler.postDelayed(this, SCREEN_CHECK_INTERVAL);
        }
    };
    
    /**
     * 检查屏幕内容
     */
    private void checkScreenContent() {
        if (isProcessing) {
            Log.d(TAG, "正在处理中，跳过本次检查");
            return;
        }
        
        Log.d(TAG, "开始检查屏幕内容");
        isProcessing = true;
        
        try {
            // 截取屏幕
            Log.d(TAG, "开始截取屏幕");
            Bitmap screenshot = captureScreen();
            if (screenshot == null) {
                Log.w(TAG, "屏幕截图失败");
                return;
            }
            Log.d(TAG, "屏幕截图成功，尺寸: " + screenshot.getWidth() + "x" + screenshot.getHeight());
            
            // OCR识别文字
            CompletableFuture<OCRTextRecognizer.OCRResult> ocrFuture = ocrRecognizer.recognizeText(screenshot);
            
            ocrFuture.thenAccept(ocrResult -> {
                if (ocrResult.isValid()) {
                    Log.d(TAG, "检测到验证码: " + ocrResult);
                    handleCaptchaDetection(screenshot, ocrResult);
                } else {
                    Log.d(TAG, "未检测到有效验证码");
                }
            }).exceptionally(throwable -> {
                Log.e(TAG, "OCR识别失败", throwable);
                return null;
            }).whenComplete((result, throwable) -> {
                isProcessing = false;
            });
            
        } catch (Exception e) {
            Log.e(TAG, "检查屏幕内容时出错", e);
            isProcessing = false;
        }
    }
    
    /**
     * 处理验证码检测
     * @param screenshot 屏幕截图
     * @param ocrResult OCR识别结果
     */
    private void handleCaptchaDetection(Bitmap screenshot, OCRTextRecognizer.OCRResult ocrResult) {
        Log.d(TAG, "开始处理验证码检测");
        
        String targetObject = ocrResult.targetObject;
        
        // 查找九宫格图片区域
        List<Rect> imageRegions = findImageRegions(ocrResult.elements);
        
        if (imageRegions.isEmpty()) {
            Log.w(TAG, "未找到图片区域");
            return;
        }
        
        Log.d(TAG, "找到 " + imageRegions.size() + " 个图片区域");
        
        // 分析每个图片区域
        List<CompletableFuture<ImageContentAnalyzer.ImageAnalysisResult>> analysisFutures = new ArrayList<>();
        
        for (int i = 0; i < imageRegions.size() && i < 9; i++) { // 最多处理9个图片
            Rect region = imageRegions.get(i);
            Bitmap imageBitmap = Bitmap.createBitmap(screenshot, region.left, region.top, 
                    region.width(), region.height());
            
            CompletableFuture<ImageContentAnalyzer.ImageAnalysisResult> future = 
                    imageAnalyzer.analyzeImage(imageBitmap, targetObject);
            analysisFutures.add(future);
        }
        
        // 等待所有分析完成
        CompletableFuture.allOf(analysisFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    List<Rect> regionsToClick = new ArrayList<>();
                    
                    for (int i = 0; i < analysisFutures.size(); i++) {
                        try {
                            ImageContentAnalyzer.ImageAnalysisResult result = analysisFutures.get(i).get();
                            if (result.containsTarget) {
                                regionsToClick.add(imageRegions.get(i));
                                Log.d(TAG, "图片 " + i + " 包含目标物体: " + result.bestMatch);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "获取图片分析结果失败", e);
                        }
                    }
                    
                    Log.d(TAG, "需要点击 " + regionsToClick.size() + " 个图片区域");
                    
                    // 执行点击操作
                    captchaClicker.clickImageRegions(regionsToClick)
                            .thenRun(() -> {
                                // 查找并点击验证按钮
                                captchaClicker.clickVerifyButton(ocrResult.elements);
                            });
                });
    }
    
    /**
     * 查找图片区域
     * @param textElements 文本元素列表
     * @return 图片区域列表
     */
    private List<Rect> findImageRegions(List<OCRTextRecognizer.TextElement> textElements) {
        List<Rect> imageRegions = new ArrayList<>();
        
        // 基于文本元素的位置推断图片区域
        // 这里使用简化的算法，实际应用中可能需要更复杂的逻辑
        
        // 假设九宫格在屏幕中央偏下位置
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int gridSize = Math.min(screenWidth, screenHeight) / 4; // 九宫格大小
        int spacing = gridSize / 10; // 间距
        
        // 计算九宫格的位置
        int startX = centerX - (gridSize * 3 + spacing * 2) / 2;
        int startY = centerY - (gridSize * 3 + spacing * 2) / 2;
        
        // 生成9个图片区域
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = startX + col * (gridSize + spacing);
                int y = startY + row * (gridSize + spacing);
                imageRegions.add(new Rect(x, y, x + gridSize, y + gridSize));
            }
        }
        
        return imageRegions;
    }
    
    /**
     * 截取屏幕
     * @return 屏幕截图
     */
    private Bitmap captureScreen() {
        try {
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection未初始化");
                return null;
            }
            
            // 创建ImageReader
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);
            
            // 创建VirtualDisplay
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null
            );
            
            // 等待一帧
            Thread.sleep(100);
            
            // 获取最新图像
            Image image = imageReader.acquireLatestImage();
            if (image == null) {
                Log.e(TAG, "无法获取屏幕图像");
                return null;
            }
            
            // 转换为Bitmap
            Bitmap bitmap = imageToBitmap(image);
            image.close();
            
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "截取屏幕失败", e);
            return null;
        }
    }
    
    /**
     * 将Image转换为Bitmap
     * @param image Image对象
     * @return Bitmap对象
     */
    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * screenWidth;
        
        Bitmap bitmap = Bitmap.createBitmap(screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        
        if (rowPadding == 0) {
            return bitmap;
        }
        
        // 裁剪掉多余的padding
        return Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
    }
    
    /**
     * 停止屏幕录制
     */
    private void stopScreenCapture() {
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
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "屏幕监控服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("验证码识别助手后台监控服务");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 创建通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("验证码识别助手")
                .setContentText("正在监控屏幕内容...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        Log.d(TAG, "ScreenMonitorService destroyed");
        
        stopMonitoring();
        
        // 释放资源
        if (ocrRecognizer != null) {
            ocrRecognizer.release();
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
