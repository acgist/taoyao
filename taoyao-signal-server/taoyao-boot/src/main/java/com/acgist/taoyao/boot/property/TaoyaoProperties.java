package com.acgist.taoyao.boot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台配置
 * 
 * @author acgist
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "taoyao")
public class TaoyaoProperties {
	
	/**
	 * 项目地址
	 */
	@Schema(title = "项目地址", description = "项目地址")
	private String url;
	/**
	 * 项目名称
	 */
	@Schema(title = "项目名称", description = "项目名称")
	private String name;
	/**
	 * 项目版本
	 */
	@Schema(title = "项目版本", description = "项目版本")
	private String version;
	/**
	 * 项目描述
	 */
	@Schema(title = "项目描述", description = "项目描述")
	private String description;
	/**
	 * 超时时间
	 */
	@Schema(title = "超时时间", description = "超时时间")
	private Long timeout;
	/**
	 * 最大超时时间
	 */
	@Schema(title = "最大超时时间", description = "最大超时时间")
	private Long maxTimeout;

}
