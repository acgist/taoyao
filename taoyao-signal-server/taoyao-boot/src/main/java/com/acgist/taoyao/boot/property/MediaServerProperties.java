package com.acgist.taoyao.boot.property;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

	/**
	 * 名称
	 */
	@Schema(title = "名称", description = "名称")
	private String name;
	/**
	 * 是否启用
	 */
	@Schema(title = "是否启用", description = "是否启用")
	private Boolean enabled;
	/**
	 * 主机
	 */
	@Schema(title = "主机", description = "主机")
	private String host;
	/**
	 * 端口
	 */
	@Schema(title = "端口", description = "端口")
	private Integer port;
	/**
	 * 协议
	 */
	@Schema(title = "协议", description = "协议")
	private String schema;
	/**
	 * 用户
	 */
	@Schema(title = "用户", description = "用户")
	@JsonIgnore
	private String username;
	/**
	 * 密码
	 */
	@Schema(title = "密码", description = "密码")
	@JsonIgnore
	private String password;
	
	/**
	 * @return 完整地址
	 */
	@Schema(title = "完整地址", description = "完整地址")
	public String getAddress() {
		return this.schema + "://" + this.host + ":" + this.port;
	}
	
}
