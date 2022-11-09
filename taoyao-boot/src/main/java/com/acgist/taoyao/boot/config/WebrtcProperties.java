package com.acgist.taoyao.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WebRTC配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(name = "WebRTC配置")
@ConfigurationProperties(prefix = "taoyao.webrtc")
public class WebrtcProperties {

	/**
	 * 类型
	 * 
	 * @author acgist
	 */
	public enum Type {
		
		SFU,
		MCU,
		MESH;
		
	}

	/**
	 * 类型
	 */
	@Schema(name = "类型", description = "WebRTC媒体架构")
	private Type type;
	
}
