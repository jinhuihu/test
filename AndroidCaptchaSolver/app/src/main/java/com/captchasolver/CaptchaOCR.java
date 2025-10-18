package com.captchasolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR文字识别模块
 * 使用Google ML Kit进行中文文字识别
 * 专门用于识别验证码中的目标物体文字
 */
public class CaptchaOCR {
    private static final String TAG = "CaptchaOCR";
    
    private Context context;
    private com.google.mlkit.vision.text.TextRecognizer textRecognizer;
    
    // 常见的目标物体关键词
    private static final String[] TARGET_KEYWORDS = {
        "飞机", "汽车", "自行车", "摩托车", "公交车", "卡车", "火车",
        "船", "直升机", "自行车", "行人", "动物", "狗", "猫", "鸟",
        "树", "花", "草", "山", "水", "桥", "房子", "建筑"
    };
    
    public CaptchaOCR(Context context) {
        this.context = context;
        // 初始化中文文字识别器
        this.textRecognizer = TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build());
    }
    
    /**
     * 识别验证码中的目标物体文字
     * @param screenshot 屏幕截图
     * @return 识别到的目标物体文字，如果未识别到则返回null
     */
    public String recognizeTargetObject(Bitmap screenshot) {
        try {
            Log.d(TAG, "开始OCR识别目标物体...");
            
            // 将Bitmap转换为InputImage
            InputImage image = InputImage.fromBitmap(screenshot, 0);
            
            // 执行文字识别
            textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String recognizedText = processRecognizedText(visionText);
                    if (recognizedText != null) {
                        Log.d(TAG, "OCR识别成功: " + recognizedText);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR识别失败: " + e.getMessage(), e);
                });
            
            // 同步处理（简化版本，实际应该使用回调）
            return processScreenshotSync(screenshot);
            
        } catch (Exception e) {
            Log.e(TAG, "OCR识别过程中出错: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 同步处理截图（简化版本）
     * 在实际应用中，应该使用异步回调
     */
    private String processScreenshotSync(Bitmap screenshot) {
        // 这里应该实现同步OCR识别
        // 为了演示，我们返回一个模拟结果
        Log.d(TAG, "执行同步OCR识别...");
        
        // 在实际实现中，这里应该调用OCR引擎
        // 现在返回模拟的"飞机"结果
        return "飞机";
    }
    
    /**
     * 处理识别到的文字，提取目标物体
     * @param visionText 识别到的文字对象
     * @return 提取的目标物体文字
     */
    private String processRecognizedText(Text visionText) {
        String fullText = visionText.getText();
        Log.d(TAG, "识别到的完整文字: " + fullText);
        
        // 查找包含目标物体的文字
        for (String keyword : TARGET_KEYWORDS) {
            if (fullText.contains(keyword)) {
                Log.d(TAG, "找到目标物体关键词: " + keyword);
                return keyword;
            }
        }
        
        // 使用正则表达式匹配可能的模式
        Pattern pattern = Pattern.compile("请选择所有包含以下物体的图片[：:]\\s*(\\S+)");
        Matcher matcher = pattern.matcher(fullText);
        if (matcher.find()) {
            String target = matcher.group(1);
            Log.d(TAG, "通过正则表达式找到目标: " + target);
            return target;
        }
        
        Log.d(TAG, "未找到目标物体关键词");
        return null;
    }
    
    /**
     * 识别指定区域的文字
     * @param screenshot 屏幕截图
     * @param region 要识别的区域
     * @return 识别到的文字
     */
    public String recognizeTextInRegion(Bitmap screenshot, Rect region) {
        try {
            // 裁剪指定区域
            Bitmap croppedBitmap = Bitmap.createBitmap(
                screenshot, 
                region.left, 
                region.top, 
                region.width(), 
                region.height()
            );
            
            InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
            
            // 这里应该实现同步OCR识别
            // 为了演示，返回模拟结果
            return "飞机";
            
        } catch (Exception e) {
            Log.e(TAG, "区域文字识别失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}
