package com.acgist.taoyao.boot.property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WebRTC配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "WebRTC配置", description = "WebRTC配置")
@ConfigurationProperties(prefix = "taoyao.webrtc")
public class WebrtcProperties {

	@Schema(title = "STUN服务器", description = "STUN服务器")
	private WebrtcStunProperties[] stun;
	@Schema(title = "TURN服务器", description = "TURN服务器")
	private WebrtcTurnProperties[] turn;
	
    @Schema(title = "IceServers", description = "IceServers")
    public List<Map<String, String>> getIceServers() {
        final List<Map<String, String>> list = new ArrayList<>();
        if(this.stun != null) {
            for (WebrtcStunProperties stun : this.stun) {
                final Map<String, String> map = new HashMap<>();
                map.put(Constant.URLS, stun.getAddress());
                list.add(map);
            }
        }
        if(this.turn != null) {
            for (WebrtcTurnProperties turn : this.turn) {
                final Map<String, String> map = new HashMap<>();
                map.put(Constant.URLS, turn.getAddress());
                map.put(Constant.USERNAME, turn.getUsername());
                map.put(Constant.CREDENTIAL, turn.getPassword());
                list.add(map);
            }
        }
        return list;
    }
	
}
