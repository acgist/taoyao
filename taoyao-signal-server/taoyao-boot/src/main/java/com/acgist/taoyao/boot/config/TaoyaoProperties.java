package com.acgist.taoyao.boot.config;

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
@Schema(title = "平台配置", description = "平台配置")
@ConfigurationProperties(prefix = "taoyao")
public class TaoyaoProperties {
	
	@Schema(title = "项目地址", description = "项目地址")
	private String url;
	@Schema(title = "项目名称", description = "项目名称")
	private String name;
	@Schema(title = "项目版本", description = "项目版本")
	private String version;
	@Schema(title = "项目描述", description = "项目描述")
	private String description;
	@Schema(title = "超时时间", description = "超时时间")
	private Long timeout;

}
