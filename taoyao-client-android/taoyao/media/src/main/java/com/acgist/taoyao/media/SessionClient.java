package com.acgist.taoyao.media;

import android.util.Log;

import java.io.Closeable;

/**
 * P2P终端
 * 使用安卓SDK + WebRTC实现P2P会话
 *
 * @author acgist
 */
public class SessionClient implements Closeable {

    private final String clientId;

    public SessionClient(String clientId) {
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

//    pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
//pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

    @Override
    public void close() {
        Log.i(Room.class.getSimpleName(), "关闭终端：" + this.clientId);
    }

}
