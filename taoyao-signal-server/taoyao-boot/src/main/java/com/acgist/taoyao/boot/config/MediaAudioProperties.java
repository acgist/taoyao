package com.acgist.taoyao.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 音频配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "音频配置", description = "音频配置")
public class MediaAudioProperties {

	/**
	 * 音频格式
	 * 
	 * @author acgist
	 */
	public enum Format {
		
	    G722,
		PCMA,
		PCMU,
		OPUS;
		
	}
	
	@Schema(title = "格式", description = "格式", example = "G722|PCMA|PCMU|OPUS")
	private Format format;
	@Schema(title = "采样数", description = "采样数", example = "16")
	private Integer sampleSize;
	@Schema(title = "采样率", description = "采样率", example = "8000|16000|32000|48000")
	private Integer sampleRate;
	
}
