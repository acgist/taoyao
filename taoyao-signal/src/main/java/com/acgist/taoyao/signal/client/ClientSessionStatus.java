package com.acgist.taoyao.signal.client;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 终端状态
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "终端状态", description = "终端状态")
public class ClientSessionStatus {

	public static final String IP = "ip";
	public static final String MAC = "mac";
	public static final String SIGNAL = "signal";
	public static final String BATTERY = "battery";
	
	/**
	 * 终端标识
	 */
	@Schema(title = "终端标识", description = "终端标识")
	private String sn;
	/**
	 * IP
	 */
	@Schema(title = "IP", description = "IP")
	private String ip;
	/**
	 * MAC
	 */
	@Schema(title = "MAC", description = "MAC")
	private String mac;
	/**
	 * 信号强度（0~100）
	 */
	@Schema(title = "信号强度（0~100）", description = "信号强度（0~100）")
	private Integer signal = 0;
	/**
	 * 电量（0~100）
	 */
	@Schema(title = "电量（0~100）", description = "电量（0~100）")
	private Integer battery = 0;
	/**
	 * 最后心跳时间
	 */
	@JsonIgnore
	private LocalDateTime lastHeartbeat = LocalDateTime.now();
	
}
