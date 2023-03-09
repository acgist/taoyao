package com.acgist.taoyao.signal.config.camera;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 摄像头配置
 * 
 * 音频：
 * 混音：不用混音
 * 变声：
 * 降噪：
 * 
 * 视频：
 * 录制：
 * 水印：
 * 美颜：
 * AI识别：
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "摄像头配置", description = "摄像头配置")
@ConfigurationProperties(prefix = "taoyao.camera")
public class CameraProperties {

    @Schema(title = "混音", description = "混音")
    private Boolean audioMixer = Boolean.FALSE;
    @Schema(title = "变声", description = "变声")
    private Boolean audioChanger;
    @Schema(title = "降噪", description = "降噪")
    private Boolean audioDenoise;
    @Schema(title = "存储目录", description = "存储目录")
    private String storagePath;
    @Schema(title = "图片存储目录", description = "图片存储目录")
    private String storageImagePath;
    @Schema(title = "视频存储目录", description = "视频存储目录")
    private String storageVideoPath;
    @Schema(title = "AI识别配置", description = "AI识别配置")
    private AiProperties ai;
    @Schema(title = "美颜配置", description = "美颜配置")
    private BeautyProperties beauty;
    @Schema(title = "水印配置", description = "水印配置")
    private WatermarkProperties watermark;
    
}
