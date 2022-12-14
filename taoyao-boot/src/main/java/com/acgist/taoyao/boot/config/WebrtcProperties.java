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
	public enum Framework {
		
		/**
		 * MESH架构
		 */
		MESH,
		/**
		 * MOON架构
		 */
		MOON;
		
	}

	/**
	 * 模型
	 */
	@Schema(title = "架构模型", description = "WebRTC架构模型")
	private Framework framework;
	/**
	 * 媒体最小端口
	 */
	@Schema(title = "媒体最小端口", description = "媒体最小端口")
	private Integer minPort;
	/**
	 * 媒体最大端口
	 */
	@Schema(title = "媒体最大端口", description = "媒体最大端口")
	private Integer maxPort;
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
	/**
	 * KMS配置
	 */
	@Schema(title = "KMS配置", description = "KMS配置")
	private KmsProperties kms;
	/**
	 * 信令配置
	 */
	@Schema(title = "信令配置", description = "信令配置")
	private SignalProperties signal;
	
}
