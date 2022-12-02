package com.acgist.taoyao.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 信令配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "信令配置", description = "信令配置")
public class SignalProperties {

	/**
	 * 信令主机
	 */
	@Schema(title = "信令主机", description = "信令主机")
	private String host;
	/**
	 * 信令端口
	 */
	@Schema(title = "信令端口", description = "信令端口")
	private Integer port;
	/**
	 * 信令协议
	 */
	@Schema(title = "信令协议", description = "信令协议")
	private String schema;
	/**
	 * 信令地址
	 */
	@Schema(title = "信令地址", description = "信令地址")
	private String websocket;
	
	/**
	 * @return 完整信令地址
	 */
	@Schema(title = "完整信令地址", description = "完整信令地址")
	public String getAddress() {
		return this.schema + "://" + this.host + ":" + this.port + this.websocket;
	}
	
}
