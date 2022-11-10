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
	 * 架构类型
	 * 
	 * @author acgist
	 */
	public enum Type {
		
		/**
		 * SFU架构
		 */
		SFU,
		/**
		 * MCU架构
		 */
		MCU,
		/**
		 * MESH架构
		 */
		MESH;
		
	}

	/**
	 * 类型
	 */
	@Schema(name = "架构类型", description = "WebRTC架构类型")
	private Type type;
	/**
	 * stun服务器
	 */
	private String[] stun;
	/**
	 * turn服务器
	 */
	private String[] turn;
	
}
