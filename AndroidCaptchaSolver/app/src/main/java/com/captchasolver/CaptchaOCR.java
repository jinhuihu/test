package com.captchasolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 文字识别模块 - 使用 Google ML Kit
 * 负责识别验证码中的目标物体文字（如"飞机"、"汽车"等）
 */
public class CaptchaOCR {
    private static final String TAG = "CaptchaOCR";
    
    private Context context;
    private TextRecognizer textRecognizer;
    
    // 目标物体关键词（验证码中常见的物体）
    private static final String[] TARGET_OBJECTS = {
        "飞机", "汽车", "自行车", "摩托车", "公交车", "卡车", "轮船",
        "火车", "交通灯", "消防栓", "停车标志", "长凳", "鸟", "猫", "狗",
        "马", "羊", "牛", "大象", "熊", "斑马", "长颈鹿"
    };
    
    public CaptchaOCR(Context context) {
        this.context = context;
        // 初始化中文文字识别器
        this.textRecognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        Log.d(TAG, "CaptchaOCR 初始化完成");
    }
    
    /**
     * 识别图片中的文字，提取目标物体
     * @param bitmap 要识别的图片
     * @return 识别到的目标物体文字
     */
    public String recognizeTargetObject(Bitmap bitmap) {
        try {
            Log.d(TAG, "开始识别目标物体文字...");
            
            if (bitmap == null) {
                Log.e(TAG, "输入图片为空");
                return null;
            }
            
            // 创建 InputImage
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            // 使用 Tasks.await() 同步等待结果
            Text visionText = Tasks.await(textRecognizer.process(image));
            
            Log.d(TAG, "文字识别成功");
            String result = extractTargetFromText(visionText);
            
            if (result != null) {
                Log.d(TAG, "识别到目标物体: " + result);
                return result;
            } else {
                Log.e(TAG, "未找到目标物体");
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "识别目标物体时出错: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从识别的文字中提取目标物体
     * @param visionText ML Kit 识别结果
     * @return 目标物体文字
     */
    private String extractTargetFromText(Text visionText) {
        String fullText = visionText.getText();
        Log.d(TAG, "识别到的完整文字: " + fullText);
        
        // 遍历所有文本块
        for (Text.TextBlock textBlock : visionText.getTextBlocks()) {
            String blockText = textBlock.getText();
            Log.d(TAG, "文本块: " + blockText);
            
            // 遍历所有文本行
            for (Text.Line line : textBlock.getLines()) {
                String lineText = line.getText();
                Log.d(TAG, "文本行: " + lineText);
                
                // 检查是否包含目标物体关键词
                for (String target : TARGET_OBJECTS) {
                    if (lineText.contains(target)) {
                        Log.d(TAG, "找到目标物体: " + target);
                        return target;
                    }
                }
            }
        }
        
        // 如果没有找到目标物体，尝试从完整文字中提取
        for (String target : TARGET_OBJECTS) {
            if (fullText.contains(target)) {
                Log.d(TAG, "从完整文字中找到目标物体: " + target);
                return target;
            }
        }
        
        Log.d(TAG, "未找到目标物体关键词");
        return null;
    }
    
    /**
     * 识别指定区域的文字
     * @param bitmap 完整图片
     * @param region 要识别的区域
     * @return 识别到的文字
     */
    public String recognizeTextInRegion(Bitmap bitmap, Rect region) {
        try {
            // 裁剪指定区域
            Bitmap croppedBitmap = Bitmap.createBitmap(
                bitmap, 
                region.left, 
                region.top, 
                region.width(), 
                region.height()
            );
            
            return recognizeTargetObject(croppedBitmap);
            
        } catch (Exception e) {
            Log.e(TAG, "识别区域文字时出错: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 识别所有文字（用于调试）
     * @param bitmap 要识别的图片
     * @return 所有识别到的文字
     */
    public List<String> recognizeAllText(Bitmap bitmap) {
        try {
            List<String> allText = new ArrayList<String>();
            
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            Text visionText = Tasks.await(textRecognizer.process(image));
            
            for (Text.TextBlock textBlock : visionText.getTextBlocks()) {
                for (Text.Line line : textBlock.getLines()) {
                    allText.add(line.getText());
                }
            }
            
            return allText;
            
        } catch (Exception e) {
            Log.e(TAG, "识别所有文字时出错: " + e.getMessage(), e);
            return new ArrayList<String>();
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (textRecognizer != null) {
            textRecognizer.close();
            Log.d(TAG, "CaptchaOCR 资源已释放");
        }
    }
}
