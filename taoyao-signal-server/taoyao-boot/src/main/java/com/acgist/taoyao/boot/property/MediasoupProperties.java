package com.acgist.taoyao.boot.property;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Mediasoup配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "Mediasoup配置", description = "Mediasoup配置")
public class MediasoupProperties {

	/**
	 * Mediasoup主机
	 */
	@Schema(title = "Mediasoup主机", description = "Mediasoup主机")
	private String host;
	/**
	 * Mediasoup端口
	 */
	@Schema(title = "Mediasoup端口", description = "Mediasoup端口")
	private Integer port;
	/**
	 * Mediasoup协议
	 */
	@Schema(title = "Mediasoup协议", description = "Mediasoup协议")
	private String schema;
	/**
	 * Mediasoup地址
	 */
	@Schema(title = "Mediasoup地址", description = "Mediasoup地址")
	private String websocket;
	/**
	 * Mediasoup用户
	 */
	@Schema(title = "Mediasoup用户", description = "Mediasoup用户")
	@JsonIgnore
	private String username;
	/**
	 * Mediasoup密码
	 */
	@Schema(title = "Mediasoup密码", description = "Mediasoup密码")
	@JsonIgnore
	private String password;
	
	/**
	 * @return 完整Mediasoup地址
	 */
	@Schema(title = "完整Mediasoup地址", description = "完整Mediasoup地址")
	public String getAddress() {
		return this.schema + "://" + this.host + ":" + this.port + this.websocket;
	}
	
}
