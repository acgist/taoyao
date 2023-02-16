package com.acgist.taoyao.signal.media;

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
	 * ID
	 */
	@Schema(title = "ID", description = "ID")
	private Long id;
	/**
	 * 名称
	 */
	@Schema(title = "名称", description = "名称")
	private String name;
	/**
	 * 媒体服务标识
	 */
	@Schema(title = "媒体服务标识", description = "媒体服务标识")
	private String mediaId;
	/**
	 * 终端数量
	 */
	@Schema(title = "终端数量", description = "终端数量")
	private Long clientSize;
	
}
