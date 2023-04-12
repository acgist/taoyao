package com.acgist.taoyao.media.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebRTC配置
 * P2P视频监控会用，正常会议不会使用，需要自己搭建`coturn`服务。
 * 
 * @author acgist
 */
public class WebrtcProperties {

    private Boolean encrypt;
	private WebrtcStunProperties[] stun;
	private WebrtcTurnProperties[] turn;
	
    public List<Map<String, String>> getIceServers() {
        final List<Map<String, String>> list = new ArrayList<>();
        if(this.stun != null) {
            for (WebrtcStunProperties stun : this.stun) {
                final Map<String, String> map = new HashMap<>();
                map.put("urls", stun.getAddress());
                list.add(map);
            }
        }
        if(this.turn != null) {
            for (WebrtcTurnProperties turn : this.turn) {
                final Map<String, String> map = new HashMap<>();
                map.put("urls", turn.getAddress());
                map.put("username", turn.getUsername());
                map.put("credential", turn.getPassword());
                list.add(map);
            }
        }
        return list;
    }
	
}
