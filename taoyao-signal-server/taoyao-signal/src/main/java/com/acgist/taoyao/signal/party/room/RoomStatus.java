package com.acgist.taoyao.signal.party.room;

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
@Schema(title = "房间状态", description = "房间状态")
public class RoomStatus {
    
    @Schema(title = "房间ID", description = "房间ID")
    private String roomId;
    @Schema(title = "房间名称", description = "房间名称")
    private String name;
    @Schema(title = "终端数量", description = "终端数量")
    private Long clientSize;
    @Schema(title = "媒体服务标识", description = "媒体服务标识")
    private String mediaClientId;
    
}
