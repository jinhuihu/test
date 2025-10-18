package com.captchasolver.v3;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OCR文字识别器
 * 使用ML Kit进行中文文字识别，专门用于识别验证码中的目标物体文字
 */
public class OCRTextRecognizer {
    
    private static final String TAG = "OCRTextRecognizer";
    
    private final TextRecognizer textRecognizer;
    
    public OCRTextRecognizer() {
        // 使用中文文字识别器，支持中英文混合识别
        textRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build());
    }
    
    /**
     * 识别图片中的文字
     * @param bitmap 要识别的图片
     * @return 识别结果
     */
    public CompletableFuture<OCRResult> recognizeText(Bitmap bitmap) {
        CompletableFuture<OCRResult> future = new CompletableFuture<>();
        
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    Log.d(TAG, "OCR识别成功，找到 " + visionText.getTextBlocks().size() + " 个文本块");
                    
                    OCRResult result = parseOCRResult(visionText);
                    future.complete(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR识别失败", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * 解析OCR识别结果
     * @param visionText ML Kit识别结果
     * @return 解析后的结果
     */
    private OCRResult parseOCRResult(Text visionText) {
        List<TextElement> elements = new ArrayList<>();
        String targetObject = null;
        boolean hasCaptchaPrompt = false;
        
        // 遍历所有文本块
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            String blockText = block.getText();
            Rect blockBounds = block.getBoundingBox();
            
            Log.d(TAG, "文本块: " + blockText + " 位置: " + blockBounds);
            
            // 检查是否是验证码提示文字
            if (isCaptchaPrompt(blockText)) {
                hasCaptchaPrompt = true;
                Log.d(TAG, "检测到验证码提示: " + blockText);
            }
            
            // 检查是否是目标物体文字
            if (isTargetObject(blockText)) {
                targetObject = blockText.trim();
                Log.d(TAG, "检测到目标物体: " + targetObject);
            }
            
            // 保存文本元素
            elements.add(new TextElement(blockText, blockBounds));
        }
        
        return new OCRResult(elements, targetObject, hasCaptchaPrompt);
    }
    
    /**
     * 判断是否是验证码提示文字
     * @param text 要检查的文字
     * @return 是否是验证码提示
     */
    private boolean isCaptchaPrompt(String text) {
        String[] promptKeywords = {
                "请选择", "选择", "包含", "图片", "验证码", "验证", "captcha",
                "select", "choose", "contain", "image", "verify"
        };
        
        String lowerText = text.toLowerCase();
        for (String keyword : promptKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否是目标物体文字
     * @param text 要检查的文字
     * @return 是否是目标物体
     */
    private boolean isTargetObject(String text) {
        String[] targetKeywords = {
                // 交通工具
                "飞机", "汽车", "自行车", "摩托车", "公交车", "卡车", "轮船", "火车", "船", "车",
                "airplane", "car", "bicycle", "motorcycle", "bus", "truck", "ship", "train",
                
                // 动物
                "鸟", "猫", "狗", "马", "羊", "牛", "猪", "鸡", "鸭", "鹅", "大象", "熊", "斑马", "长颈鹿",
                "bird", "cat", "dog", "horse", "sheep", "cow", "pig", "chicken", "duck", "goose", "elephant", "bear", "zebra", "giraffe",
                
                // 物品
                "椅子", "桌子", "床", "沙发", "电视", "电脑", "手机", "书", "杯子", "帽子",
                "chair", "table", "bed", "sofa", "tv", "television", "computer", "phone", "book", "cup", "hat",
                
                // 食物
                "苹果", "香蕉", "橙子", "葡萄", "草莓", "西瓜", "面包", "蛋糕",
                "apple", "banana", "orange", "grape", "strawberry", "watermelon", "bread", "cake",
                
                // 自然
                "树", "花", "草", "山", "海", "湖", "河", "云", "太阳", "月亮",
                "tree", "flower", "grass", "mountain", "sea", "lake", "river", "cloud", "sun", "moon",
                
                // 其他常见物体
                "房子", "门", "窗", "灯", "钟", "钥匙", "包", "鞋", "衣服",
                "house", "door", "window", "light", "clock", "key", "bag", "shoe", "clothes"
        };
        
        String cleanText = text.trim().toLowerCase();
        for (String keyword : targetKeywords) {
            if (cleanText.equals(keyword.toLowerCase()) || cleanText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        textRecognizer.close();
    }
    
    /**
     * OCR识别结果类
     */
    public static class OCRResult {
        public final List<TextElement> elements;
        public final String targetObject;
        public final boolean hasCaptchaPrompt;
        
        public OCRResult(List<TextElement> elements, String targetObject, boolean hasCaptchaPrompt) {
            this.elements = elements;
            this.targetObject = targetObject;
            this.hasCaptchaPrompt = hasCaptchaPrompt;
        }
        
        /**
         * 是否是有效的验证码识别结果
         */
        public boolean isValid() {
            return hasCaptchaPrompt && targetObject != null && !targetObject.isEmpty();
        }
        
        @Override
        public String toString() {
            return "OCRResult{" +
                    "targetObject='" + targetObject + '\'' +
                    ", hasCaptchaPrompt=" + hasCaptchaPrompt +
                    ", elementsCount=" + elements.size() +
                    '}';
        }
    }
    
    /**
     * 文本元素类
     */
    public static class TextElement {
        public final String text;
        public final Rect bounds;
        
        public TextElement(String text, Rect bounds) {
            this.text = text;
            this.bounds = bounds;
        }
        
        @Override
        public String toString() {
            return "TextElement{" +
                    "text='" + text + '\'' +
                    ", bounds=" + bounds +
                    '}';
        }
    }
}
