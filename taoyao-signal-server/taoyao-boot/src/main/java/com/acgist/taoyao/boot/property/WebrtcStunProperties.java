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
    
    /**
     * 主机
     */
    @Schema(title = "主机", description = "主机")
    protected String host;
    /**
     * 端口
     */
    @Schema(title = "端口", description = "端口")
    protected Integer port;
    
    /**
     * @return 完整地址
     */
    @Schema(title = "完整地址", description = "完整地址")
    public String getAddress() {
        return "stun://" + this.host + ":" + this.port;
    }

}
