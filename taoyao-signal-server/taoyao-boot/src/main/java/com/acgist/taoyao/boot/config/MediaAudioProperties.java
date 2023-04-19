package com.acgist.taoyao.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 音频配置
 * 
 * 比特率 = 采样率 * 采样位数 * 声道数 / 8 / 1024
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
        // G711A
        PCMA,
        // G711U
        PCMU,
        OPUS;
		
	}
	
	@Schema(title = "格式", description = "格式", example = "G722|PCMA|PCMU|OPUS")
	private Format format;
	@Schema(title = "比特率", description = "比特率", example = "96|128|256")
	private Integer bitrate;
	@Schema(title = "采样位数", description = "采样位数", example = "8|16|32")
	private Integer sampleSize;
	@Schema(title = "采样率", description = "采样率", example = "8000|16000|32000|48000")
	private Integer sampleRate;
	
}
