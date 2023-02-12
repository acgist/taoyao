package com.acgist.taoyao.boot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Socket信令配置
 * 
 * @author acgist
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "taoyao.socket")
public class SocketProperties {

	/**
	 * 是否启用
	 */
	private Boolean enabled;
	/**
	 * 监听地址
	 */
	private String host;
	/**
	 * 监听端口
	 */
	private Integer port;
	/**
	 * 线程队列长度
	 */
	private Integer queueSize;
	/**
	 * 最小线程数量
	 */
	private Integer threadMin;
	/**
	 * 最大线程数量
	 */
	private Integer threadMax;
	/**
	 * 线程池的前缀
	 */
	private String threadNamePrefix;
	/**
	 * 线程销毁时间
	 */
	private Integer keepAliveTime;
	
	
}
