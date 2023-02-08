package com.acgist.taoyao.signal.client;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 终端状态
 * 
 * TODO：统计拉流数量
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "终端状态", description = "终端状态")
public class ClientSessionStatus {

	/**
	 * IP
	 */
	public static final String IP = "ip";
	/**
	 * MAC
	 */
	public static final String MAC = "mac";
	/**
	 * 信号强度（0~100）
	 */
	public static final String SIGNAL = "signal";
	/**
	 * 电池电量（0~100）
	 */
	public static final String BATTERY = "battery";
	/**
	 * 是否充电
	 */
	public static final String CHARGING = "charging";
	/**
	 * 媒体服务名称
	 */
	public static final String MEDIASOUP = "mediasoup";
	
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
	 * 电池电量（0~100）
	 */
	@Schema(title = "电池电量（0~100）", description = "电池电量（0~100）")
	private Integer battery = 0;
	/**
	 * 是否充电
	 */
	@Schema(title = "是否充电", description = "是否充电")
	private Boolean charging = false;
	/**
	 * 媒体服务名称
	 */
	@Schema(title = "媒体服务名称", description = "媒体服务名称")
	private String mediasoup;
	/**
	 * 最后心跳时间
	 */
	@Schema(title = "最后心跳时间", description = "最后心跳时间")
	private LocalDateTime lastHeartbeat = LocalDateTime.now();
	
}
