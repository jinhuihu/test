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
        textRecognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
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
        Rect promptBounds = null;
        
        // 第一遍：查找验证码提示和所有文本
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            String blockText = block.getText();
            Rect blockBounds = block.getBoundingBox();
            
            Log.d(TAG, "文本块: " + blockText + " 位置: " + blockBounds);
            
            // 检查是否是验证码提示文字
            if (isCaptchaPrompt(blockText)) {
                hasCaptchaPrompt = true;
                promptBounds = blockBounds;
                Log.d(TAG, "✅ 检测到验证码提示: " + blockText);
            }
            
            // 保存文本元素
            elements.add(new TextElement(blockText, blockBounds));
        }
        
        // 第二遍：基于位置查找目标物体（必须在提示文字下方且接近）
        if (hasCaptchaPrompt && promptBounds != null) {
            int promptBottom = promptBounds.bottom;
            int promptCenterX = (promptBounds.left + promptBounds.right) / 2;
            
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                String blockText = block.getText();
                Rect blockBounds = block.getBoundingBox();
                
                // 跳过验证码提示本身
                if (isCaptchaPrompt(blockText)) {
                    continue;
                }
                
                // 目标物体应该在提示下方100像素范围内，且水平居中
                int verticalDistance = blockBounds.top - promptBottom;
                int horizontalDistance = Math.abs((blockBounds.left + blockBounds.right) / 2 - promptCenterX);
                
                if (verticalDistance > 0 && verticalDistance < 150 && 
                    horizontalDistance < 400 && 
                    isTargetObject(blockText)) {
                    targetObject = blockText.trim();
                    Log.d(TAG, "✅ 检测到目标物体: " + targetObject + " (距离提示: " + verticalDistance + "px)");
                    break; // 找到第一个符合条件的就停止
                }
            }
        }
        
        Log.d(TAG, "解析结果 - hasCaptchaPrompt: " + hasCaptchaPrompt + ", targetObject: " + targetObject);
        
        return new OCRResult(elements, targetObject, hasCaptchaPrompt);
    }
    
    /**
     * 判断是否是验证码提示文字
     * @param text 要检查的文字
     * @return 是否是验证码提示
     */
    private boolean isCaptchaPrompt(String text) {
        String cleanText = text.trim();
        
        // 检查是否包含关键短语：'请选择所有' 和 '图片'
        boolean hasStart = cleanText.contains("请选择所有") || cleanText.contains("请选择");
        boolean hasEnd = cleanText.contains("图片");
        
        // 如果同时包含这两个关键词，则认为是验证码提示
        if (hasStart && hasEnd) {
            Log.d(TAG, "✅ 匹配验证码提示模式: " + cleanText);
            return true;
        }
        
        // 备用检查：包含其他常见验证码关键词
        String[] promptKeywords = {
                "验证码", "验证", "captcha", "select", "choose", "contain", "image", "verify"
        };
        
        String lowerText = cleanText.toLowerCase();
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
        String cleanText = text.trim();
        
        // 排除一些明显不是目标物体的文字
        String[] excludeKeywords = {
                "请选择", "选择", "包含", "图片", "验证码", "验证", "的", "以下", "所有", "物体"
        };
        
        for (String exclude : excludeKeywords) {
            if (cleanText.contains(exclude)) {
                return false;
            }
        }
        
        // 如果文字太短或太长，可能不是目标物体
        if (cleanText.length() < 1 || cleanText.length() > 10) {
            return false;
        }
        
        // 常见目标物体关键词
        String[] targetKeywords = {
                // 交通工具
                "飞机", "汽车", "自行车", "摩托车", "公交车", "卡车", "轮船", "火车", "船", "车", "直升机",
                "airplane", "car", "bicycle", "motorcycle", "bus", "truck", "ship", "train",
                
                // 动物
                "鸟", "猫", "狗", "马", "羊", "牛", "猪", "鸡", "鸭", "鹅", "大象", "熊", "斑马", "长颈鹿", "老虎", "狮子",
                "bird", "cat", "dog", "horse", "sheep", "cow", "pig", "chicken", "duck", "goose", "elephant", "bear", "zebra", "giraffe",
                
                // 物品
                "椅子", "桌子", "床", "沙发", "电视", "电脑", "手机", "书", "杯子", "帽子", "包", "鞋", "衣服",
                "chair", "table", "bed", "sofa", "tv", "television", "computer", "phone", "book", "cup", "hat",
                
                // 食物
                "苹果", "香蕉", "橙子", "葡萄", "草莓", "西瓜", "面包", "蛋糕", "米饭", "面条",
                "apple", "banana", "orange", "grape", "strawberry", "watermelon", "bread", "cake",
                
                // 自然
                "树", "花", "草", "山", "海", "湖", "河", "云", "太阳", "月亮", "星星",
                "tree", "flower", "grass", "mountain", "sea", "lake", "river", "cloud", "sun", "moon",
                
                // 其他常见物体
                "房子", "门", "窗", "灯", "钟", "钥匙", "球", "玩具", "乐器",
                "house", "door", "window", "light", "clock", "key", "ball", "toy"
        };
        
        String lowerText = cleanText.toLowerCase();
        for (String keyword : targetKeywords) {
            if (lowerText.equals(keyword.toLowerCase()) || lowerText.contains(keyword.toLowerCase())) {
                Log.d(TAG, "✅ 匹配目标物体关键词: " + cleanText + " -> " + keyword);
                return true;
            }
        }
        
        // 如果包含中文字符且长度合理，也可能是目标物体
        if (cleanText.matches("[\\u4e00-\\u9fa5]+") && cleanText.length() <= 4) {
            Log.d(TAG, "✅ 可能是中文目标物体: " + cleanText);
            return true;
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
