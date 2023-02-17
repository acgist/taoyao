package com.acgist.taoyao.boot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 脚本配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "脚本配置", description = "脚本配置")
@ConfigurationProperties(prefix = "taoyao.script")
public class ScriptProperties {

    @Schema(title = "是否启用", description = "是否启用")
	private Boolean enabled;
    @Schema(title = "重启媒体服务", description = "重启媒体服务")
	private String mediaReboot;
    @Schema(title = "关闭媒体服务", description = "关闭媒体服务")
	private String mediaShutdown;
    @Schema(title = "重启系统", description = "重启系统")
	private String systemReboot;
    @Schema(title = "关闭系统", description = "关闭系统")
	private String systemShutdown;
    @Schema(title = "重启平台", description = "重启平台")
	private String platformReboot;
    @Schema(title = "关闭平台", description = "关闭平台")
	private String platformShutdown;
	
}
