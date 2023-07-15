package com.acgist.taoyao.boot.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 媒体配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "媒体配置", description = "媒体配置")
@ConfigurationProperties(prefix = "taoyao.media")
public class MediaProperties {
    
    @Schema(title = "最小视频宽度", description = "最小视频宽度")
    private Integer minWidth;
    @Schema(title = "最大视频宽度", description = "最大视频宽度")
    private Integer maxWidth;
    @Schema(title = "最小视频高度", description = "最小视频高度")
    private Integer minHeight;
    @Schema(title = "最大视频高度", description = "最大视频高度")
    private Integer maxHeight;
    @Schema(title = "最小视频帧率", description = "最小视频帧率")
    private Integer minFrameRate;
    @Schema(title = "最大视频帧率", description = "最大视频帧率")
    private Integer maxFrameRate;
    @Schema(title = "最小视频码率", description = "最小视频码率")
    private Integer minVideoBitrate;
    @Schema(title = "最大视频码率", description = "最大视频码率")
    private Integer maxVideoBitrate;
    @Schema(title = "最小音频采样位数", description = "最小音频采样位数")
    private Integer minSampleSize;
    @Schema(title = "最大音频采样位数", description = "最大音频采样位数")
    private Integer maxSampleSize;
    @Schema(title = "最小音频采样率", description = "最小音频采样率")
    private Integer minSampleRate;
    @Schema(title = "最大音频采样率", description = "最大音频采样率")
    private Integer maxSampleRate;
    @Schema(title = "最小音频码率", description = "最小音频码率")
    private Integer minAudioBitrate;
    @Schema(title = "最大音频码率", description = "最大音频码率")
    private Integer maxAudioBitrate;
    @Schema(title = "音频默认配置", description = "音频默认配置")
    private MediaAudioProperties audio;
    @Schema(title = "视频默认配置", description = "视频默认配置")
    private MediaVideoProperties video;
    @Schema(title = "音频配置", description = "音频配置")
    private Map<String, MediaAudioProperties> audios;
    @Schema(title = "视频配置", description = "视频配置")
    private Map<String, MediaVideoProperties> videos;

}
