package com.acgist.taoyao.boot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 安全配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "安全配置", description = "安全配置")
@ConfigurationProperties(prefix = "taoyao.security")
public class SecurityProperties {
	
    @Schema(title = "是否启用", description = "是否启用")
	private Boolean enabled;
    @Schema(title = "安全范围", description = "安全范围")
	private String realm;
    @Schema(title = "公共地址", description = "公共地址")
	private String[] permit;
    @Schema(title = "名称", description = "名称")
	private String username;
    @Schema(title = "密码", description = "密码")
	private String password;
	
}
