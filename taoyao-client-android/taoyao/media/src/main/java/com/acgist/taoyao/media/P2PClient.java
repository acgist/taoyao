package com.acgist.taoyao.media;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

/**
 * P2P终端
 * 使用安卓SDK + WebRTC实现P2P会话
 *
 * @author acgist
 */
public class P2PClient implements Closeable {

    private final String clientId;

    public P2PClient(String clientId) {
        this.clientId = clientId;
    }

    // 配置STUN穿透服务器  转发服务器
//    iceServers = new ArrayList<>();
//    PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(Constant.STUN).createIceServer();
//            iceServers.add(iceServer);
//    streamList = new ArrayList<>();
//    PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
//    PeerConnectionObserver connectionObserver = getObserver();
//    peerConnection = peerConnectionFactory.createPeerConnection(configuration, connectionObserver);

    @Override
    public void close() {
        Log.i(Room.class.getSimpleName(), "关闭终端：" + this.clientId);
    }

}
