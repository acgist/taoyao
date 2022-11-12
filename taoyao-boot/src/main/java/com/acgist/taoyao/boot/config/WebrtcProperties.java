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
@Schema(title = "WebRTC配置", description = "WebRTC配置")
@ConfigurationProperties(prefix = "taoyao.webrtc")
public class WebrtcProperties {

	/**
	 * 架构模型
	 * 
	 * @author acgist
	 */
	public enum Model {
		
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
	 * 基础框架
	 * 
	 * @author acgist
	 */
	public enum Framework {
		
		/**
		 * jitsi
		 */
		JITSI,
		/**
		 * kurento
		 */
		KURENTO;
		
	}

	/**
	 * 模型
	 */
	@Schema(title = "架构模型", description = "WebRTC架构模型")
	private Model model;
	/**
	 * 框架
	 */
	@Schema(title = "基础框架", description = "WebRTC基础框架")
	private Framework framework;
	/**
	 * stun服务器
	 */
	@Schema(title = "stun服务器", description = "stun服务器")
	private String[] stun;
	/**
	 * turn服务器
	 */
	@Schema(title = "turn服务器", description = "turn服务器")
	private String[] turn;
	
}
