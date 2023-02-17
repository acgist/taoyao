package com.acgist.taoyao.boot.property;

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

	@Schema(title = "STUN服务器", description = "STUN服务器")
	private WebrtcStunProperties[] stun;
	@Schema(title = "TURN服务器", description = "TURN服务器")
	private WebrtcTurnProperties[] turn;
	
}
