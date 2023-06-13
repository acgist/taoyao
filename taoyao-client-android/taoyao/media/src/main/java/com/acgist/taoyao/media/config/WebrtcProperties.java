package com.acgist.taoyao.media.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang3.ArrayUtils;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * WebRTC配置
 *
 * 注意：完全拷贝信令模块`WebrtcProperties`代码
 *
 * @author acgist
 */
public class WebrtcProperties {

    /**
     * 是否加密
     */
    private Boolean encrypt;
    /**
     * STUN服务器
     */
    private WebrtcStunProperties[] stun;
    /**
     * TURN服务器
     */
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
