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
	 * 节点主机
	 */
	@Schema(title = "节点主机", description = "节点主机")
	private String host;
	/**
	 * 节点端口
	 */
	@Schema(title = "节点端口", description = "节点端口")
	private Integer port;
	/**
	 * 用户
	 */
	@Schema(title = "节点用户", description = "节点用户")
	@JsonIgnore
	private String username;
	/**
	 * 密码
	 */
	@Schema(title = "节点密码", description = "节点密码")
	@JsonIgnore
	private String password;
	/**
	 * 服务节点ID
	 */
	@Schema(title = "服务节点ID", description = "服务节点ID")
	private String serverId;
	
}
