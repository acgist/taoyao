package com.acgist.taoyao.boot.property;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WebRTC STUN配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "WebRTC STUN配置", description = "WebRTC STUN配置")
public class WebrtcStunProperties {
    
    @Schema(title = "主机", description = "主机")
    protected String host;
    @Schema(title = "端口", description = "端口")
    protected Integer port;
    
    @Schema(title = "完整地址", description = "完整地址")
    public String getAddress() {
        return "stun://" + this.host + ":" + this.port;
    }

}
