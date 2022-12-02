package com.acgist.taoyao.boot.property;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Moon架构配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "Moon架构配置", description = "Moon架构配置")
public class MoonProperties {
	
	/**
	 * 是否混音
	 */
	@Schema(title = "是否混音", description = "是否混音")
	private Boolean audioMix;

}
