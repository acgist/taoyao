package com.acgist.taoyao.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 系统配置
 * 
 * @author acgist
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "taoyao")
public class TaoyaoProperties {
	
	/**
	 * 地址
	 */
	private String url;
	/**
	 * 名称
	 */
	private String name;
	/**
	 * 超时时间
	 */
	private Long timeout;
	/**
	 * 版本
	 */
	private String version;
	/**
	 * 描述
	 */
	private String description;


}
