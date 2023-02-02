package com.acgist.taoyao.boot.model;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 请求响应头部
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema( title = "请求响应头部", description = "请求响应头部")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 请求响应版本
	 */
	@Schema(title = "请求响应版本", description = "请求响应版本")
	private String v;
	/**
	 * 请求响应标识
	 */
	@Schema(title = "请求响应标识", description = "请求响应标识")
	private String id;
	/**
	 * 终端标识
	 */
	@Schema(title = "终端标识", description = "终端标识")
	private String sn;
	/**
	 * 协议标识
	 */
	@Schema(title = "协议标识", description = "协议标识")
	private Integer pid;
	
}
