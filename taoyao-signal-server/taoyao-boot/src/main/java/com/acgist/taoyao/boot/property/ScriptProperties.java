package com.acgist.taoyao.boot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 脚本配置
 * 
 * @author acgist
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "taoyao.script")
public class ScriptProperties {

	/**
	 * 是否启用
	 */
	private Boolean enabled;
	/**
	 * 重启媒体服务
	 */
	private String mediaReboot;
	/**
	 * 关闭媒体服务
	 */
	private String mediaShutdown;
	/**
	 * 重启系统
	 */
	private String systemReboot;
	/**
	 * 关闭系统
	 */
	private String systemShutdown;
	/**
	 * 重启平台
	 */
	private String platformReboot;
	/**
	 * 关闭平台
	 */
	private String platformShutdown;
	
}
