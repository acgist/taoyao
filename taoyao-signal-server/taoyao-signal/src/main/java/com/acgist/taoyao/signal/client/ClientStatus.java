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
public class ClientStatus {

    /**
     * 终端标识
     */
    @Schema(title = "终端标识", description = "终端标识")
    private String clientId;
	/**
	 * 终端IP
	 */
	@Schema(title = "终端IP", description = "终端IP")
	private String ip;
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
	 * 媒体服务标识
	 */
	@Schema(title = "媒体服务标识", description = "媒体服务标识")
	private String mediaId;
	/**
	 * 最后心跳时间
	 */
	@Schema(title = "最后心跳时间", description = "最后心跳时间")
	private LocalDateTime lastHeartbeat;
	
}
