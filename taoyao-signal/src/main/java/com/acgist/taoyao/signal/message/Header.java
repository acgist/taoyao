package com.acgist.taoyao.signal.message;

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
	 * 信令来源
	 */
	private String sn;
	/**
	 * 事件标识
	 */
	private String event;
	/**
	 * 信令版本
	 */
	private String version;

}
