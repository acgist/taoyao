package com.acgist.taoyao.boot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ID配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "ID配置", description = "ID配置")
@ConfigurationProperties(prefix = "taoyao.id")
public class IdProperties {
	
    @Schema(title = "机器序号", description = "机器序号")
	private Integer index;
    @Schema(title = "最大序号", description = "最大序号")
	private Integer maxIndex;

}
