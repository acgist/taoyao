package com.acgist.taoyao.boot.property;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * KMS配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "KMS配置", description = "KMS配置")
public class KmsProperties {

	/**
	 * KMS主机
	 */
	@Schema(title = "KMS主机", description = "KMS主机")
	private String host;
	/**
	 * KMS端口
	 */
	@Schema(title = "KMS端口", description = "KMS端口")
	private Integer port;
	/**
	 * KMS协议
	 */
	@Schema(title = "KMS协议", description = "KMS协议")
	private String schema;
	/**
	 * KMS地址
	 */
	@Schema(title = "KMS地址", description = "KMS地址")
	private String websocket;
	/**
	 * KMS用户
	 */
	@Schema(title = "KMS用户", description = "KMS用户")
	@JsonIgnore
	private String username;
	/**
	 * KMS密码
	 */
	@Schema(title = "KMS密码", description = "KMS密码")
	@JsonIgnore
	private String password;
	
	/**
	 * @return 完整KMS地址
	 */
	@Schema(title = "完整KMS地址", description = "完整KMS地址")
	public String getAddress() {
		return this.schema + "://" + this.host + ":" + this.port + this.websocket;
	}
	
}
