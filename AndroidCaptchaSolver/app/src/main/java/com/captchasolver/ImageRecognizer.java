package com.captchasolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图像识别模块 - 使用 Google ML Kit
 * 负责识别九宫格中包含目标物体的图片
 */
public class ImageRecognizer {
    private static final String TAG = "ImageRecognizer";
    
    private Context context;
    private ImageLabeler imageLabeler;
    
    // 物体识别的映射表（中文到英文标签）
    private static final Map<String, String[]> OBJECT_MAPPINGS = new HashMap<String, String[]>();
    static {
        // 交通工具
        OBJECT_MAPPINGS.put("飞机", new String[]{"airplane", "aircraft", "plane", "jet"});
        OBJECT_MAPPINGS.put("汽车", new String[]{"car", "automobile", "vehicle", "sedan"});
        OBJECT_MAPPINGS.put("自行车", new String[]{"bicycle", "bike", "cycling"});
        OBJECT_MAPPINGS.put("摩托车", new String[]{"motorcycle", "motorbike", "bike"});
        OBJECT_MAPPINGS.put("公交车", new String[]{"bus", "coach"});
        OBJECT_MAPPINGS.put("卡车", new String[]{"truck", "lorry", "van"});
        OBJECT_MAPPINGS.put("轮船", new String[]{"boat", "ship", "vessel"});
        OBJECT_MAPPINGS.put("火车", new String[]{"train", "locomotive", "railway"});
        
        // 交通设施
        OBJECT_MAPPINGS.put("交通灯", new String[]{"traffic light", "signal", "stoplight"});
        OBJECT_MAPPINGS.put("消防栓", new String[]{"fire hydrant", "hydrant"});
        OBJECT_MAPPINGS.put("停车标志", new String[]{"stop sign", "sign"});
        
        // 动物
        OBJECT_MAPPINGS.put("鸟", new String[]{"bird", "avian"});
        OBJECT_MAPPINGS.put("猫", new String[]{"cat", "feline", "kitty"});
        OBJECT_MAPPINGS.put("狗", new String[]{"dog", "puppy", "canine"});
        OBJECT_MAPPINGS.put("马", new String[]{"horse", "equine"});
        OBJECT_MAPPINGS.put("羊", new String[]{"sheep", "lamb"});
        OBJECT_MAPPINGS.put("牛", new String[]{"cow", "cattle", "bull"});
        OBJECT_MAPPINGS.put("大象", new String[]{"elephant"});
        OBJECT_MAPPINGS.put("熊", new String[]{"bear"});
        OBJECT_MAPPINGS.put("斑马", new String[]{"zebra"});
        OBJECT_MAPPINGS.put("长颈鹿", new String[]{"giraffe"});
    }
    
    // 置信度阈值
    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    
    public ImageRecognizer(Context context) {
        this.context = context;
        
        // 初始化图像标注器
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build();
        this.imageLabeler = ImageLabeling.getClient(options);
        
        Log.d(TAG, "ImageRecognizer 初始化完成");
    }
    
    /**
     * 识别九宫格图片中包含目标物体的图片
     * @param bitmap 完整的验证码图片
     * @param targetObject 目标物体（中文，如"飞机"）
     * @param gridRegions 九宫格的区域列表
     * @return 包含目标物体的图片索引列表
     */
    public List<Integer> findMatchingImages(Bitmap bitmap, String targetObject, List<Rect> gridRegions) {
        List<Integer> matchingIndices = new ArrayList<Integer>();
        
        try {
            Log.d(TAG, "开始识别九宫格图片，目标物体: " + targetObject);
            
            if (bitmap == null || gridRegions == null || gridRegions.isEmpty()) {
                Log.e(TAG, "输入参数无效");
                return matchingIndices;
            }
            
            // 获取目标物体对应的英文标签
            String[] targetLabels = OBJECT_MAPPINGS.get(targetObject);
            if (targetLabels == null || targetLabels.length == 0) {
                Log.e(TAG, "未找到目标物体的映射: " + targetObject);
                return matchingIndices;
            }
            
            Log.d(TAG, "目标标签: " + String.join(", ", targetLabels));
            
            // 遍历每个九宫格图片
            for (int i = 0; i < gridRegions.size(); i++) {
                Rect region = gridRegions.get(i);
                boolean matches = recognizeImageRegion(bitmap, region, targetLabels);
                
                if (matches) {
                    Log.d(TAG, "图片 " + i + " 包含目标物体");
                    matchingIndices.add(Integer.valueOf(i));
                } else {
                    Log.d(TAG, "图片 " + i + " 不包含目标物体");
                }
                
                // 添加短暂延迟，避免请求过快
                Thread.sleep(100);
            }
            
            Log.d(TAG, "识别完成，找到 " + matchingIndices.size() + " 个匹配的图片");
            
        } catch (Exception e) {
            Log.e(TAG, "识别九宫格图片时出错: " + e.getMessage(), e);
        }
        
        return matchingIndices;
    }
    
    /**
     * 识别单个图片区域是否包含目标物体
     * @param bitmap 完整图片
     * @param region 要识别的区域
     * @param targetLabels 目标标签数组
     * @return 是否包含目标物体
     */
    private boolean recognizeImageRegion(Bitmap bitmap, Rect region, String[] targetLabels) {
        try {
            // 裁剪图片区域
            Bitmap croppedBitmap = Bitmap.createBitmap(
                bitmap,
                Math.max(0, region.left),
                Math.max(0, region.top),
                Math.min(region.width(), bitmap.getWidth() - region.left),
                Math.min(region.height(), bitmap.getHeight() - region.top)
            );
            
            if (croppedBitmap == null) {
                Log.e(TAG, "裁剪图片失败");
                return false;
            }
            
            // 创建 InputImage
            InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
            
            // 使用 Tasks.await() 同步等待结果
            List<ImageLabel> labels = Tasks.await(imageLabeler.process(image));
            
            Log.d(TAG, "图像识别成功，标签数量: " + labels.size());
            
            // 检查是否包含目标标签
            for (ImageLabel label : labels) {
                String labelText = label.getText().toLowerCase();
                float confidence = label.getConfidence();
                
                Log.d(TAG, "标签: " + labelText + " 置信度: " + confidence);
                
                // 检查是否匹配目标标签
                for (String targetLabel : targetLabels) {
                    if (labelText.contains(targetLabel.toLowerCase())) {
                        Log.d(TAG, "找到匹配标签: " + labelText);
                        croppedBitmap.recycle();
                        return true;
                    }
                }
            }
            
            // 回收临时 Bitmap
            croppedBitmap.recycle();
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "识别图片区域时出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 检测九宫格的位置
     * @param bitmap 完整的验证码图片
     * @return 九宫格的区域列表
     */
    public List<Rect> detectGridRegions(Bitmap bitmap) {
        List<Rect> gridRegions = new ArrayList<Rect>();
        
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            Log.d(TAG, "图片尺寸: " + width + "x" + height);
            
            // 假设九宫格位于图片的中下部分
            // 这里使用简单的平均分割，实际应用中可能需要更智能的检测
            
            // 九宫格区域估算（根据常见验证码布局）
            int gridStartX = (int) (width * 0.05);  // 左边距 5%
            int gridStartY = (int) (height * 0.2);   // 上边距 20%（为文字留空间）
            int gridWidth = (int) (width * 0.9);     // 宽度 90%
            int gridHeight = (int) (height * 0.7);   // 高度 70%
            
            // 每个小格的尺寸
            int cellWidth = gridWidth / 3;
            int cellHeight = gridHeight / 3;
            
            // 生成九宫格区域
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int left = gridStartX + col * cellWidth;
                    int top = gridStartY + row * cellHeight;
                    int right = left + cellWidth;
                    int bottom = top + cellHeight;
                    
                    Rect rect = new Rect(left, top, right, bottom);
                    gridRegions.add(rect);
                    
                    Log.d(TAG, String.format("格子 [%d,%d]: (%d,%d,%d,%d)", 
                        Integer.valueOf(row), Integer.valueOf(col), 
                        Integer.valueOf(left), Integer.valueOf(top), 
                        Integer.valueOf(right), Integer.valueOf(bottom)));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "检测九宫格位置时出错: " + e.getMessage(), e);
        }
        
        return gridRegions;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (imageLabeler != null) {
            imageLabeler.close();
            Log.d(TAG, "ImageRecognizer 资源已释放");
        }
    }
}
