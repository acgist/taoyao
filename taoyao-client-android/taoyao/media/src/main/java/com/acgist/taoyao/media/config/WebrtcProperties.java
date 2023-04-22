package com.acgist.taoyao.media.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang3.ArrayUtils;
import org.webrtc.PeerConnection;

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

    @JsonIgnore
    public List<PeerConnection.IceServer> getIceServers() {
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        if(ArrayUtils.isNotEmpty(this.stun)) {
            for (WebrtcStunProperties stun : this.stun) {
                final PeerConnection.IceServer iceServer = PeerConnection.IceServer
                    .builder(stun.getAddress())
                    .createIceServer();
                iceServers.add(iceServer);
            }
        }
        if(ArrayUtils.isNotEmpty(this.turn)) {
            for (WebrtcTurnProperties turn : this.turn) {
                final PeerConnection.IceServer iceServer = PeerConnection.IceServer
                    .builder(turn.getAddress())
                    .setUsername(turn.getUsername())
                    .setPassword(turn.getPassword())
                    .createIceServer();
                iceServers.add(iceServer);
            }
        }
        return iceServers;
    }

    public Boolean getEncrypt() {
        return this.encrypt;
    }

    public void setEncrypt(Boolean encrypt) {
        this.encrypt = encrypt;
    }

    public WebrtcStunProperties[] getStun() {
        return this.stun;
    }

    public void setStun(WebrtcStunProperties[] stun) {
        this.stun = stun;
    }

    public WebrtcTurnProperties[] getTurn() {
        return this.turn;
    }

    public void setTurn(WebrtcTurnProperties[] turn) {
        this.turn = turn;
    }

}
