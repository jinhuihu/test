package com.captchasolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * 图像识别模块
 * 使用Google ML Kit进行物体检测
 * 专门用于识别九宫格中包含特定物体的图片
 */
public class ImageRecognizer {
    private static final String TAG = "ImageRecognizer";
    
    private Context context;
    private com.google.mlkit.vision.objects.ObjectDetector objectDetector;
    
    // 九宫格相关常量
    private static final int GRID_ROWS = 3;
    private static final int GRID_COLS = 3;
    private static final int GRID_SIZE = GRID_ROWS * GRID_COLS;
    
    // 验证码对话框的大致位置（需要根据实际屏幕调整）
    private static final int DIALOG_LEFT_MARGIN = 50;
    private static final int DIALOG_TOP_MARGIN = 200;
    private static final int DIALOG_WIDTH = 300;
    private static final int DIALOG_HEIGHT = 400;
    
    public ImageRecognizer(Context context) {
        this.context = context;
        
        // 配置物体检测器
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build();
            
        this.objectDetector = ObjectDetection.getClient(options);
    }
    
    /**
     * 检测九宫格图片区域
     * @param screenshot 屏幕截图
     * @return 九宫格图片的矩形区域列表
     */
    public List<Rect> detectImageGrid(Bitmap screenshot) {
        List<Rect> imageRects = new ArrayList<>();
        
        try {
            Log.d(TAG, "开始检测九宫格图片区域...");
            
            // 计算九宫格的大致位置
            int dialogWidth = screenshot.getWidth() - 2 * DIALOG_LEFT_MARGIN;
            int dialogHeight = screenshot.getHeight() - DIALOG_TOP_MARGIN - 100;
            
            // 假设九宫格在对话框的中央区域
            int gridStartX = DIALOG_LEFT_MARGIN + 20;
            int gridStartY = DIALOG_TOP_MARGIN + 100;
            int gridWidth = dialogWidth - 40;
            int gridHeight = dialogHeight - 150;
            
            // 计算每个图片的尺寸
            int imageWidth = gridWidth / GRID_COLS;
            int imageHeight = gridHeight / GRID_ROWS;
            
            // 生成九宫格矩形区域
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int left = gridStartX + col * imageWidth;
                    int top = gridStartY + row * imageHeight;
                    int right = left + imageWidth;
                    int bottom = top + imageHeight;
                    
                    Rect imageRect = new Rect(left, top, right, bottom);
                    imageRects.add(imageRect);
                    
                    Log.d(TAG, String.format("图片 %d: (%d, %d, %d, %d)", 
                        row * GRID_COLS + col, left, top, right, bottom));
                }
            }
            
            Log.d(TAG, "检测到 " + imageRects.size() + " 个图片区域");
            
        } catch (Exception e) {
            Log.e(TAG, "检测九宫格时出错: " + e.getMessage(), e);
        }
        
        return imageRects;
    }
    
    /**
     * 识别包含目标物体的图片
     * @param screenshot 屏幕截图
     * @param imageRects 图片区域列表
     * @param targetObject 目标物体名称
     * @return 包含目标物体的图片索引列表
     */
    public List<Integer> identifyTargetImages(Bitmap screenshot, List<Rect> imageRects, String targetObject) {
        List<Integer> correctImages = new ArrayList<>();
        
        try {
            Log.d(TAG, "开始识别包含 " + targetObject + " 的图片...");
            
            for (int i = 0; i < imageRects.size(); i++) {
                Rect imageRect = imageRects.get(i);
                
                // 裁剪图片区域
                Bitmap croppedImage = Bitmap.createBitmap(
                    screenshot,
                    imageRect.left,
                    imageRect.top,
                    imageRect.width(),
                    imageRect.height()
                );
                
                // 检测图片中是否包含目标物体
                if (containsTargetObject(croppedImage, targetObject)) {
                    correctImages.add(i);
                    Log.d(TAG, "图片 " + i + " 包含 " + targetObject);
                }
            }
            
            Log.d(TAG, "找到 " + correctImages.size() + " 个包含目标物体的图片");
            
        } catch (Exception e) {
            Log.e(TAG, "识别目标图片时出错: " + e.getMessage(), e);
        }
        
        return correctImages;
    }
    
    /**
     * 检测图片中是否包含目标物体
     * @param image 要检测的图片
     * @param targetObject 目标物体名称
     * @return 是否包含目标物体
     */
    private boolean containsTargetObject(Bitmap image, String targetObject) {
        try {
            // 将Bitmap转换为InputImage
            InputImage inputImage = InputImage.fromBitmap(image, 0);
            
            // 执行物体检测
            objectDetector.process(inputImage)
                .addOnSuccessListener(detectedObjects -> {
                    Log.d(TAG, "检测到 " + detectedObjects.size() + " 个物体");
                    
                    for (DetectedObject detectedObject : detectedObjects) {
                        for (DetectedObject.Label label : detectedObject.getLabels()) {
                            String labelText = label.getText().toLowerCase();
                            Log.d(TAG, "检测到物体标签: " + labelText + " 置信度: " + label.getConfidence());
                            
                            // 检查是否匹配目标物体
                            if (isTargetObject(labelText, targetObject)) {
                                Log.d(TAG, "找到目标物体: " + labelText);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "物体检测失败: " + e.getMessage(), e);
                });
            
            // 简化版本：基于目标物体名称进行模拟判断
            return simulateObjectDetection(targetObject);
            
        } catch (Exception e) {
            Log.e(TAG, "物体检测过程中出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 模拟物体检测（简化版本）
     * 在实际应用中，这里应该使用真实的ML模型
     */
    private boolean simulateObjectDetection(String targetObject) {
        // 这里应该实现真实的物体检测逻辑
        // 为了演示，我们返回一些模拟结果
        
        // 模拟检测结果：假设某些图片包含飞机
        // 在实际应用中，这里应该使用训练好的模型
        return Math.random() > 0.5; // 随机返回结果用于演示
    }
    
    /**
     * 检查检测到的物体是否匹配目标物体
     * @param detectedLabel 检测到的物体标签
     * @param targetObject 目标物体名称
     * @return 是否匹配
     */
    private boolean isTargetObject(String detectedLabel, String targetObject) {
        // 飞机相关的关键词
        if (targetObject.contains("飞机")) {
            return detectedLabel.contains("airplane") || 
                   detectedLabel.contains("aircraft") || 
                   detectedLabel.contains("plane") ||
                   detectedLabel.contains("jet") ||
                   detectedLabel.contains("helicopter");
        }
        
        // 汽车相关的关键词
        if (targetObject.contains("汽车")) {
            return detectedLabel.contains("car") || 
                   detectedLabel.contains("vehicle") || 
                   detectedLabel.contains("automobile");
        }
        
        // 自行车相关的关键词
        if (targetObject.contains("自行车")) {
            return detectedLabel.contains("bicycle") || 
                   detectedLabel.contains("bike") || 
                   detectedLabel.contains("cycle");
        }
        
        // 其他物体的匹配逻辑...
        
        return false;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (objectDetector != null) {
            objectDetector.close();
        }
    }
}
