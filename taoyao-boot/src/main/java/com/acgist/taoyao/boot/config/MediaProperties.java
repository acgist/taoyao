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
	
	/**
	 * 音频配置
	 */
	private MediaAudioProperties audio;
	/**
	 * 视频配置
	 */
	private MediaVideoProperties video;

}
