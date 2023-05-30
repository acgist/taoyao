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
    
    @Schema(title = "SDP模板", description = "SDP模板")
    private String sdp;
    @Schema(title = "媒体录像", description = "媒体录像")
    private String record;
    @Schema(title = "预览截图", description = "预览截图")
    private String preview;
    @Schema(title = "视频时长", description = "视频时长")
    private String duration;
    @Schema(title = "存储目录", description = "存储目录")
    private String storagePath;
    @Schema(title = "图片存储目录", description = "图片存储目录")
    private String storageImagePath;
    @Schema(title = "视频存储目录", description = "视频存储目录")
    private String storageVideoPath;
    @Schema(title = "录像最小端口", description = "录像最小端口")
    private Integer minPort;
    @Schema(title = "录像最大端口", description = "录像最大端口")
    private Integer maxPort;

}
