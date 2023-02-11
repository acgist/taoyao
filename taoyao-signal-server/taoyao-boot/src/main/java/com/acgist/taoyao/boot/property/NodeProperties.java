package com.acgist.taoyao.boot.property;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 节点配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "节点配置", description = "节点配置")
public class NodeProperties {

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
	 * 服务节点ID
	 */
	@Schema(title = "服务节点ID", description = "服务节点ID")
	private String serverId;
	
}
