package com.acgist.taoyao.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 媒体服务配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "媒体服务配置", description = "媒体服务配置")
public class MediaServerProperties {

	@Schema(title = "媒体服务标识", description = "媒体服务标识")
	private String mediaId;
	@Schema(title = "是否启用", description = "是否启用")
	private Boolean enabled;
	@Schema(title = "是否启用重写本地IP", description = "内外网多网卡环境重写IP地址")
	private Boolean rewriteIp;
	@Schema(title = "主机", description = "主机")
	private String host;
	@Schema(title = "端口", description = "端口")
	private Integer port;
	@Schema(title = "协议", description = "协议")
	private String schema;
	@Schema(title = "用户", description = "用户")
	private String username;
	@Schema(title = "密码", description = "密码")
	private String password;
	
	@Schema(title = "完整地址", description = "完整地址")
	public String getAddress() {
		return this.schema + "://" + this.host + ":" + this.port;
	}
	
}
