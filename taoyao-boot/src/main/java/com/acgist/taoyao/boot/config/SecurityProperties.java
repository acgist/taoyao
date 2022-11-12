package com.acgist.taoyao.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 安全配置
 * 
 * @author acgist
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "taoyao.security")
public class SecurityProperties {
	
	/**
	 * Basic认证
	 */
	public static final String BASIC = "Basic";

	/**
	 * 范围
	 */
	private String realm;
	/**
	 * 公共地址
	 */
	private String[] permit;
	/**
	 * 名称
	 */
	private String username;
	/**
	 * 密码
	 */
	private String password;
	
	/**
	 * @return 授权信息
	 */
	public String getAuthorization() {
		return this.username + ":" + this.password;
	}
	
}
