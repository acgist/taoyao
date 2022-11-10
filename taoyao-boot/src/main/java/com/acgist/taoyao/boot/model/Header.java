package com.acgist.taoyao.boot.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 信令头部
 * 
 * @author acgist
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 信令版本
	 */
	private String v;
	/**
	 * 请求标识
	 */
	private Long id;
	/**
	 * 终端标识
	 */
	private String sn;
	/**
	 * 协议标识
	 */
	private Integer pid;
	
}
