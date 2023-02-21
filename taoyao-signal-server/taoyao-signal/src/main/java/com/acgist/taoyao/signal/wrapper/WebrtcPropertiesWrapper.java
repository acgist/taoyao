package com.acgist.taoyao.signal.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.config.WebrtcProperties;
import com.acgist.taoyao.boot.config.WebrtcStunProperties;
import com.acgist.taoyao.boot.config.WebrtcTurnProperties;
import com.acgist.taoyao.boot.service.IpService;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;

/**
 * WebRTC配置包装器
 * 
 * @author acgist
 */
@AllArgsConstructor
public class WebrtcPropertiesWrapper extends WebrtcProperties {

    /**
     * 终端IP
     */
    @JsonIgnore
    private String clientIp;
    /**
     * IP服务
     */
    @JsonIgnore
    private IpService ipService;
    /**
     * WebRTC配置
     */
    @JsonIgnore
    private WebrtcProperties webrtcProperties;
    
    @Override
    public WebrtcStunProperties[] getStun() {
        return this.webrtcProperties.getStun();
    }
    
    @Override
    public WebrtcTurnProperties[] getTurn() {
        return this.webrtcProperties.getTurn();
    }
    
    @Override
    public List<Map<String, String>> getIceServers() {
        final List<Map<String, String>> list = new ArrayList<>();
        final WebrtcStunProperties[] stunList = this.webrtcProperties.getStun();
        if(stunList != null) {
            for (WebrtcStunProperties stun : stunList) {
                if(this.ipService.subnetIp(stun.getHost(), this.clientIp)) {
                    final Map<String, String> map = new HashMap<>();
                    map.put(Constant.URLS, stun.getAddress());
                    list.add(map);
                }
            }
        }
        final WebrtcTurnProperties[] turnList = this.webrtcProperties.getTurn();
        if(turnList != null) {
            for (WebrtcTurnProperties turn : turnList) {
                if(this.ipService.subnetIp(turn.getHost(), this.clientIp)) {
                    final Map<String, String> map = new HashMap<>();
                    map.put(Constant.URLS, turn.getAddress());
                    map.put(Constant.USERNAME, turn.getUsername());
                    map.put(Constant.CREDENTIAL, turn.getPassword());
                    list.add(map);
                }
            }
        }
        return list;
    }
    
}
