package com.acgist.taoyao.signal.client;

import java.time.LocalDateTime;

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
public class ClientStatus {

    @Schema(title = "终端IP", description = "终端IP")
    private String ip;
    @Schema(title = "终端标识", description = "终端标识")
    private String clientId;
	@Schema(title = "信号强度（0~100）", description = "信号强度（0~100）")
	private Integer signal = 0;
	@Schema(title = "电池电量（0~100）", description = "电池电量（0~100）")
	private Integer battery = 0;
	@Schema(title = "是否正在充电", description = "是否正在充电")
	private Boolean charging = false;
	@Schema(title = "媒体服务标识", description = "媒体服务标识")
	private String mediaId;
	@Schema(title = "生产者数量", description = "生产者数量")
	private Integer producerSize;
	@Schema(title = "消费者数量", description = "消费者数量")
	private Integer customerSize;
	@Schema(title = "最后心跳时间", description = "最后心跳时间")
	private LocalDateTime lastHeartbeat;
	
}
