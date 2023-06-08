package com.acgist.taoyao.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * FFmpeg配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "FFmpeg配置", description = "FFmpeg配置")
@ConfigurationProperties(prefix = "taoyao.ffmpeg")
public class FfmpegProperties {
    
    @Schema(title = "录像地址", description = "录像地址")
    private String host;
    @Schema(title = "最小端口", description = "最小端口")
    private Integer minPort;
    @Schema(title = "最大端口", description = "最大端口")
    private Integer maxPort;
    @Schema(title = "录像帧率", description = "录像帧率")
    private Integer frameRate;
    @Schema(title = "录像命令", description = "录像命令")
    private String record;
    @Schema(title = "预览命令", description = "预览命令")
    private String preview;
    @Schema(title = "时长命令", description = "时长命令")
    private String duration;
    @Schema(title = "录像SDP", description = "录像SDP")
    private String recordSdp;
    @Schema(title = "预览时间", description = "预览时间")
    private Integer previewTime;
    @Schema(title = "时长提取", description = "时长提取")
    private String durationRegex;
    @Schema(title = "存储目录", description = "存储目录")
    private String storagePath;
    @Schema(title = "图片存储目录", description = "图片存储目录")
    private String storageImagePath;
    @Schema(title = "视频存储目录", description = "视频存储目录")
    private String storageVideoPath;

}
