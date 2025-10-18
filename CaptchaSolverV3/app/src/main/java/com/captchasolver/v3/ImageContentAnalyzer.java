package com.captchasolver.v3;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 图像内容分析器
 * 使用ML Kit进行图像标签识别，判断图片中是否包含目标物体
 */
public class ImageContentAnalyzer {
    
    private static final String TAG = "ImageContentAnalyzer";
    private static final float MIN_CONFIDENCE = 0.5f; // 最小置信度阈值
    
    /**
     * 分析图像内容，判断是否包含目标物体
     * @param bitmap 要分析的图像
     * @param targetObject 目标物体名称
     * @return 分析结果
     */
    public CompletableFuture<ImageAnalysisResult> analyzeImage(Bitmap bitmap, String targetObject) {
        CompletableFuture<ImageAnalysisResult> future = new CompletableFuture<>();
        
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(labels -> {
                    Log.d(TAG, "图像分析成功，找到 " + labels.size() + " 个标签");
                    
                    ImageAnalysisResult result = analyzeLabels(labels, targetObject);
                    future.complete(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "图像分析失败", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * 分析标签结果
     * @param labels 识别到的标签列表
     * @param targetObject 目标物体
     * @return 分析结果
     */
    private ImageAnalysisResult analyzeLabels(List<ImageLabel> labels, String targetObject) {
        List<String> detectedObjects = new ArrayList<>();
        boolean containsTarget = false;
        float maxConfidence = 0f;
        String bestMatch = null;
        
        // 获取目标物体的英文名称
        String englishTarget = getEnglishName(targetObject);
        
        for (ImageLabel label : labels) {
            String labelText = label.getText().toLowerCase();
            float confidence = label.getConfidence();
            
            Log.d(TAG, "标签: " + labelText + " 置信度: " + confidence);
            
            // 只考虑置信度较高的标签
            if (confidence >= MIN_CONFIDENCE) {
                detectedObjects.add(labelText + " (" + String.format("%.2f", confidence) + ")");
                
                // 检查是否匹配目标物体
                if (matchesTarget(labelText, targetObject, englishTarget)) {
                    containsTarget = true;
                    if (confidence > maxConfidence) {
                        maxConfidence = confidence;
                        bestMatch = labelText;
                    }
                }
            }
        }
        
        return new ImageAnalysisResult(detectedObjects, containsTarget, maxConfidence, bestMatch);
    }
    
    /**
     * 检查标签是否匹配目标物体
     * @param labelText 标签文字
     * @param targetObject 目标物体（中文）
     * @param englishTarget 目标物体（英文）
     * @return 是否匹配
     */
    private boolean matchesTarget(String labelText, String targetObject, String englishTarget) {
        // 直接匹配
        if (labelText.equals(targetObject.toLowerCase()) || labelText.equals(englishTarget.toLowerCase())) {
            return true;
        }
        
        // 包含匹配
        if (labelText.contains(targetObject.toLowerCase()) || labelText.contains(englishTarget.toLowerCase())) {
            return true;
        }
        
        // 同义词匹配
        return matchesSynonyms(labelText, targetObject, englishTarget);
    }
    
    /**
     * 同义词匹配
     * @param labelText 标签文字
     * @param targetObject 目标物体
     * @param englishTarget 英文目标
     * @return 是否匹配同义词
     */
    private boolean matchesSynonyms(String labelText, String targetObject, String englishTarget) {
        // 定义同义词映射
        String[][] synonyms = {
                {"car", "automobile", "vehicle", "汽车"},
                {"airplane", "aircraft", "plane", "jet", "飞机"},
                {"bicycle", "bike", "cycle", "自行车"},
                {"motorcycle", "motorbike", "摩托车"},
                {"bus", "公交车", "公共汽车"},
                {"truck", "lorry", "卡车"},
                {"ship", "boat", "vessel", "轮船", "船"},
                {"train", "locomotive", "火车"},
                {"bird", "avian", "鸟"},
                {"cat", "feline", "猫"},
                {"dog", "canine", "狗"},
                {"horse", "equine", "马"},
                {"tree", "植物", "树"},
                {"flower", "花", "花朵"},
                {"house", "home", "building", "房子", "房屋"}
        };
        
        for (String[] group : synonyms) {
            boolean targetInGroup = false;
            boolean labelInGroup = false;
            
            for (String word : group) {
                if (word.equalsIgnoreCase(targetObject) || word.equalsIgnoreCase(englishTarget)) {
                    targetInGroup = true;
                }
                if (word.equalsIgnoreCase(labelText)) {
                    labelInGroup = true;
                }
            }
            
            if (targetInGroup && labelInGroup) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取目标物体的英文名称
     * @param chineseName 中文名称
     * @return 英文名称
     */
    private String getEnglishName(String chineseName) {
        // 中文到英文的映射
        switch (chineseName) {
            case "飞机": return "airplane";
            case "汽车": return "car";
            case "自行车": return "bicycle";
            case "摩托车": return "motorcycle";
            case "公交车": return "bus";
            case "卡车": return "truck";
            case "轮船": case "船": return "ship";
            case "火车": return "train";
            case "鸟": return "bird";
            case "猫": return "cat";
            case "狗": return "dog";
            case "马": return "horse";
            case "羊": return "sheep";
            case "牛": return "cow";
            case "猪": return "pig";
            case "鸡": return "chicken";
            case "鸭": return "duck";
            case "鹅": return "goose";
            case "大象": return "elephant";
            case "熊": return "bear";
            case "斑马": return "zebra";
            case "长颈鹿": return "giraffe";
            case "椅子": return "chair";
            case "桌子": return "table";
            case "床": return "bed";
            case "沙发": return "sofa";
            case "电视": return "television";
            case "电脑": return "computer";
            case "手机": return "phone";
            case "苹果": return "apple";
            case "香蕉": return "banana";
            case "橙子": return "orange";
            case "葡萄": return "grape";
            case "草莓": return "strawberry";
            case "西瓜": return "watermelon";
            case "树": return "tree";
            case "花": return "flower";
            case "草": return "grass";
            case "山": return "mountain";
            case "海": return "sea";
            case "湖": return "lake";
            case "河": return "river";
            case "云": return "cloud";
            case "太阳": return "sun";
            case "月亮": return "moon";
            case "房子": return "house";
            case "门": return "door";
            case "窗": return "window";
            case "灯": return "light";
            case "钟": return "clock";
            default: return chineseName; // 如果找不到映射，返回原值
        }
    }
    
    /**
     * 图像分析结果类
     */
    public static class ImageAnalysisResult {
        public final List<String> detectedObjects;
        public final boolean containsTarget;
        public final float confidence;
        public final String bestMatch;
        
        public ImageAnalysisResult(List<String> detectedObjects, boolean containsTarget, float confidence, String bestMatch) {
            this.detectedObjects = detectedObjects;
            this.containsTarget = containsTarget;
            this.confidence = confidence;
            this.bestMatch = bestMatch;
        }
        
        @Override
        public String toString() {
            return "ImageAnalysisResult{" +
                    "containsTarget=" + containsTarget +
                    ", confidence=" + confidence +
                    ", bestMatch='" + bestMatch + '\'' +
                    ", detectedObjects=" + detectedObjects +
                    '}';
        }
    }
}
