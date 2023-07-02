package com.acgist.taoyao.signal.party.media;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 房间终端ID
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "房间终端ID", description = "房间终端ID")
public class RoomClientId {

    @Schema(title = "房间ID", description = "房间ID")
    private String roomId;
    @Schema(title = "终端ID", description = "终端ID")
    private String clientId;
    @Schema(title = "数据生产者ID", description = "数据生产者ID")
    private List<String> dataProducers;
    @Schema(title = "数据消费者ID", description = "数据消费者ID")
    private List<String> dataConsumers;
    @Schema(title = "音频生产者ID", description = "音频生产者ID")
    private List<String> audioProducers;
    @Schema(title = "视频生产者ID", description = "视频生产者ID")
    private List<String> videoProducers;
    @Schema(title = "音频消费者ID", description = "音频消费者ID")
    private List<String> audioConsumers;
    @Schema(title = "视频消费者ID", description = "视频消费者ID")
    private List<String> videoConsumers;
    
}
