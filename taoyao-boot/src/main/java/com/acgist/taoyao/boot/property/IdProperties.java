package com.acgist.taoyao.boot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * ID配置
 * 
 * @author acgist
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "taoyao.id")
public class IdProperties {
	
	/**
	 * 机器序号
	 */
	private Integer sn;
	/**
	 * 最大序号
	 */
	private Integer maxIndex;

}
