package com.acgist.taoyao.boot.config;

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
    @Schema(title = "最小视频码率", description = "最小视频码率")
    private Integer minBitrate;
    @Schema(title = "最大视频码率", description = "最大视频码率")
    private Integer maxBitrate;
    @Schema(title = "最小视频帧率", description = "最小视频帧率")
    private Integer minFrameRate;
    @Schema(title = "最大视频帧率", description = "最大视频帧率")
    private Integer maxFrameRate;
    @Schema(title = "最小音频采样数", description = "最小音频采样数")
    private Integer minSampleSize;
    @Schema(title = "最大音频采样数", description = "最大音频采样数")
    private Integer maxSampleSize;
    @Schema(title = "最小音频采样率", description = "最小音频采样率")
    private Integer minSampleRate;
    @Schema(title = "最大音频采样率", description = "最大音频采样率")
    private Integer maxSampleRate;
	@Schema(title = "音频默认配置", description = "音频默认配置")
	private MediaAudioProperties audio;
	@Schema(title = "视频默认配置", description = "视频默认配置")
	private MediaVideoProperties video;
	@Schema(title = "4K视频配置", description = "4K视频配置")
	private MediaVideoProperties udVideo;
	@Schema(title = "2K视频配置", description = "2K视频配置")
	private MediaVideoProperties qdVideo;
	@Schema(title = "超清视频配置", description = "超清视频配置")
	private MediaVideoProperties fdVideo;
	@Schema(title = "高清视频配置", description = "高清视频配置")
	private MediaVideoProperties hdVideo;
	@Schema(title = "标清视频配置", description = "标清视频配置")
	private MediaVideoProperties sdVideo;

}
