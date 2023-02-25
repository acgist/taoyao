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
