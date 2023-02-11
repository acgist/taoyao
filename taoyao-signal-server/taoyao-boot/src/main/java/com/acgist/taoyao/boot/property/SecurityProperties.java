package com.acgist.taoyao.boot.property;

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
	 * 是否启用
	 */
	private Boolean enabled;
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
	
}
