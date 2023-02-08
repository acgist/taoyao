package com.acgist.taoyao.signal.room;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 房间状态
 * 
 * @author acgist
 */
@Getter
@Setter
public class RoomStatus {

	/**
	 * 房间ID
	 */
	@Schema(title = "房间ID", description = "房间ID")
	private Long id;
	/**
	 * 房间终端数量
	 */
	@Schema(title = "房间终端数量", description = "房间终端数量")
	private Long snSize;
	/**
	 * 房间在线终端数量
	 */
	@Schema(title = "房间在线终端数量", description = "房间在线终端数量")
	private Long onlineSnSize;
	/**
	 * 房间终端列表
	 */
	@Schema(title = "房间终端列表", description = "房间终端列表")
	private List<String> snList;
	/**
	 * 媒体服务名称
	 */
	@Schema(title = "媒体服务名称", description = "媒体服务名称")
	private String mediasoup;
	
}
