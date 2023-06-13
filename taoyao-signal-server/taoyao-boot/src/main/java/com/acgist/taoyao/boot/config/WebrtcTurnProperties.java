package com.acgist.taoyao.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * WebRTC TURN配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "WebRTC TURN配置", description = "WebRTC TURN配置")
@EqualsAndHashCode(callSuper = true)
public class WebrtcTurnProperties extends WebrtcStunProperties {
    
    @Schema(title = "帐号", description = "帐号")
    private String username;
    @Schema(title = "密码", description = "密码")
    private String password;
    
    @Override
    @Schema(title = "完整地址", description = "完整地址")
    public String getAddress() {
        return "turn:" + this.host + ":" + this.port;
    }
    
}
