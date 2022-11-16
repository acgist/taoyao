package com.acgist.taoyao.signal.client;

import lombok.Getter;
import lombok.Setter;

/**
 * 终端状态
 * 
 * @author acgist
 */
@Getter
@Setter
public class ClientSessionStatus {

	/**
	 * 终端标识
	 */
	private String sn;
	/**
	 * IP
	 */
	private String ip;
	/**
	 * MAC
	 */
	private String mac;
	/**
	 * 信号强度（0~100）
	 */
	private Integer signal = 0;
	/**
	 * 电量（0~100）
	 */
	private Integer battery = 0;
	
}
