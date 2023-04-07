package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * P2P终端
 * 使用安卓SDK + WebRTC实现P2P会话
 *
 * https://zhuanlan.zhihu.com/p/82446482
 *
 * @author acgist
 */
public class SessionClient extends Client {

    /**
     * 会话ID
     */
    private final String sessionId;
    /**
     * 本地媒体
     */
    private MediaStream mediaStream;
    /**
     * 远程媒体
     */
    private final List<MediaStream> remoteMediaStreams;
    /**
     * SDPObserver
     */
    private SdpObserver sdpObserver;
    /**
     * Peer连接
     */
    private PeerConnection peerConnection;
    /**
     * Peer连接Observer
     */
    private PeerConnection.Observer observer;
    /**
     * Peer连接工厂
     */
    private PeerConnectionFactory peerConnectionFactory;

    public SessionClient(String sessionId, String name, String clientId, Handler handler, ITaoyao taoyao) {
        super(name, clientId, handler, taoyao);
        this.sessionId = sessionId;
        this.remoteMediaStreams = new CopyOnWriteArrayList<>();
    }

    public void init() {
        this.peerConnectionFactory = this.mediaManager.newClient(MediaManager.Type.BACK);
        // STUN | TURN
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        // TODO：读取配置
        final PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);
        final PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
        this.observer       = this.observer();
        this.mediaStream    = this.mediaManager.getMediaStream();
        this.sdpObserver    = this.sdpObserver();
        this.peerConnection = this.peerConnectionFactory.createPeerConnection(configuration, this.observer);
        this.peerConnection.addStream(this.mediaStream);
    }

    public void exchange(Message message, Map<String, Object> body) {
        final String type = MapUtils.get(body, "type");
        switch(type) {
            case "offer"     -> this.offer(message, body);
            case "answer"    -> this.answer(message, body);
            case "candidate" -> this.candidate(message, body);
            default          -> Log.d(SessionClient.class.getSimpleName(), "没有适配的会话指令：" + type);
        }
    }

    public void call(String clientId) {
        final MediaConstraints mediaConstraints = new MediaConstraints();
        this.peerConnection.createOffer(this.sdpObserver, mediaConstraints);
        // TODO：实现主动拉取别人
    }

    /**
     * 提供媒体服务
     */
    public void offer() {
        final MediaConstraints mediaConstraints = new MediaConstraints();
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(1920)));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(1080)));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(15)));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(30)));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//      mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        this.peerConnection.createOffer(this.sdpObserver, mediaConstraints);
    }

    private void offer(Message message, Map<String, Object> body) {
        final String sdp  = MapUtils.get(body, "sdp");
        final String type = MapUtils.get(body, "type");
        final SessionDescription.Type sdpType = SessionDescription.Type.valueOf(type.toUpperCase());
        final SessionDescription sessionDescription = new SessionDescription(sdpType, sdp);
        this.peerConnection.setRemoteDescription(this.sdpObserver, sessionDescription);
        final MediaConstraints mediaConstraints = new MediaConstraints();
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(1920)));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(1080)));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(15)));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(30)));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//      mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        this.peerConnection.createAnswer(this.sdpObserver, mediaConstraints);
    }

    private void answer(Message message, Map<String, Object> body) {
        final String sdp  = MapUtils.get(body, "sdp");
        final String type = MapUtils.get(body, "type");
        final SessionDescription.Type sdpType = SessionDescription.Type.valueOf(type.toUpperCase());
        final SessionDescription sessionDescription = new SessionDescription(sdpType, sdp);
        this.peerConnection.setRemoteDescription(this.sdpObserver, sessionDescription);
    }

    private void candidate(Message message, Map<String, Object> body) {
        final Map<String, Object> candidate = MapUtils.get(body, "candidate");
        final String  sdp           = MapUtils.get(candidate, "candidate");
        final String  sdpMid        = MapUtils.get(candidate, "sdpMid");
        final Integer sdpMLineIndex = MapUtils.getInteger(candidate, "sdpMLineIndex");
        if(sdp == null || sdpMid == null || sdpMLineIndex == null) {
            Log.w(SessionClient.class.getSimpleName(), "无效媒体协商：" + body);
        } else {
            final IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
            this.peerConnection.addIceCandidate(iceCandidate);
        }
    }

    @Override
    public void close() {
        super.close();
        // 本地资源释放
        this.mediaManager.closeClient();
        // 远程资源释放
        this.remoteMediaStreams.forEach(v -> v.dispose());
        this.remoteMediaStreams.clear();
    }

    /**
     * @return 监听
     */
    private PeerConnection.Observer observer() {
        return new PeerConnection.Observer() {

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            }

            @Override
            public void onIceConnectionReceivingChange(boolean result) {
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(SessionClient.class.getSimpleName(), "发送媒体协商：" + SessionClient.this.sessionId);
                SessionClient.this.taoyao.push(SessionClient.this.taoyao.buildMessage(
                    "session::exchange",
                    "type",      "candidate",
                    "candidate", iceCandidate,
                    "sessionId", SessionClient.this.sessionId
                ));
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.i(SessionClient.class.getSimpleName(), "添加远程媒体：" + SessionClient.this.clientId);
                SessionClient.this.mediaStream = mediaStream;
                SessionClient.this.mediaManager.remoteVideoRenderer(mediaStream);
                SessionClient.this.remoteMediaStreams.add(mediaStream);
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                SessionClient.this.remoteMediaStreams.remove(mediaStream);
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(SessionClient.class.getSimpleName(), "重新协商媒体：" + SessionClient.this.sessionId);
                if(peerConnection.connectionState() == PeerConnection.PeerConnectionState.CONNECTED) {
                    // TODO：重写协商
//                SessionClient.this.offer();
                }
            }

        };
    }

    private SdpObserver sdpObserver() {
        return new SdpObserver() {

            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(SessionClient.class.getSimpleName(), "创建SDP成功：" + SessionClient.this.sessionId);
                SessionClient.this.peerConnection.setLocalDescription(this, sessionDescription);
                SessionClient.this.taoyao.push(SessionClient.this.taoyao.buildMessage(
                    "session::exchange",
                    "sdp",       sessionDescription.description,
                    "type",      sessionDescription.type.toString().toLowerCase(),
                    "sessionId", SessionClient.this.sessionId
                ));
            }

            @Override
            public void onSetSuccess() {
                Log.d(SessionClient.class.getSimpleName(), "设置SDP成功：" + SessionClient.this.sessionId);
            }

            @Override
            public void onCreateFailure(String message) {
                Log.w(SessionClient.class.getSimpleName(), "创建SDP失败：" + message);
            }

            @Override
            public void onSetFailure(String message) {
                Log.w(SessionClient.class.getSimpleName(), "设置SDP失败：" + message);
            }

        };
    }

}
