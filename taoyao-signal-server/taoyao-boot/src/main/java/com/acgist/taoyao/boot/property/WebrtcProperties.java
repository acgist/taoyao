package com.acgist.taoyao.boot.property;

import java.util.List;

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
	 * Mediasoup配置
	 */
	@Schema(title = "Mediasoup配置", description = "Mediasoup配置")
	private List<MediasoupProperties> mediasoupList;
	
}
