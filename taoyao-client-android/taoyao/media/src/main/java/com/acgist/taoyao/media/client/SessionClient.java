package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ListUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.media.VideoSourceType;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.config.MediaProperties;
import com.acgist.taoyao.media.config.WebrtcProperties;
import com.acgist.taoyao.media.config.WebrtcStunProperties;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.apache.commons.lang3.ArrayUtils;
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
import java.util.concurrent.CompletableFuture;

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
    private final boolean preview;
    private final boolean playAudio;
    private final boolean playVideo;
    private final boolean dataConsume;
    private final boolean audioConsume;
    private final boolean videoConsume;
    private final boolean dataProduce;
    private final boolean audioProduce;
    private final boolean videoProduce;
    private final MediaProperties mediaProperties;
    private final WebrtcProperties webrtcProperties;
    /**
     * 本地媒体
     */
    private MediaStream mediaStream;
    /**
     * 数据通道
     */
    private DataChannel dataChannel;
    /**
     * 远程媒体
     */
    private MediaStream remoteMediaStream;
    /**
     * OfferSdpObserver
     */
    private SdpObserver offerSdpObserver;
    /**
     * AnswerSdpObserver
     */
    private SdpObserver answerSdpObserver;
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

    /**
     *
     * @param sessionId    会话ID
     * @param name         远程终端名称
     * @param clientId     远程终端ID
     * @param taoyao       信令
     * @param mainHandler  MainHandler
     * @param preview      是否预览视频
     * @param playAudio    是否播放音频
     * @param playVideo    是否播放视频
     * @param dataConsume  是否消费数据
     * @param audioConsume 是否消费音频
     * @param videoConsume 是否消费视频
     * @param dataProduce  是否生产数据
     * @param audioProduce 是否生产音频
     * @param videoProduce 是否生产视频
     * @param mediaProperties  媒体配置
     * @param webrtcProperties WebRTC配置
     */
    public SessionClient(
        String sessionId, String name, String clientId, ITaoyao taoyao, Handler mainHandler,
        boolean preview, boolean playAudio, boolean playVideo,
        boolean dataConsume, boolean audioConsume, boolean videoConsume,
        boolean dataProduce, boolean audioProduce, boolean videoProduce,
        MediaProperties mediaProperties, WebrtcProperties webrtcProperties
    ) {
        super(name, clientId, taoyao, mainHandler);
        this.sessionId = sessionId;
        this.preview  = preview;
        this.playAudio = playAudio;
        this.playVideo = playVideo;
        this.dataConsume  = dataConsume;
        this.audioConsume = audioConsume;
        this.videoConsume = videoConsume;
        this.dataProduce  = dataProduce;
        this.audioProduce = audioProduce;
        this.videoProduce = videoProduce;
        this.mediaProperties = mediaProperties;
        this.webrtcProperties = webrtcProperties;
    }

    @Override
    public void init() {
        synchronized (this) {
            if(this.init) {
                return;
            }
            super.init();
            this.peerConnectionFactory = this.mediaManager.newClient();
            // STUN | TURN
            final List<PeerConnection.IceServer> iceServers = this.webrtcProperties.getIceServers();
            final PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
            this.observer          = this.observer();
            this.mediaStream       = this.mediaManager.buildLocalMediaStream(this.audioProduce, this.videoProduce);
            this.offerSdpObserver  = this.offerSdpObserver();
            this.answerSdpObserver = this.answerSdpObserver();
            this.peerConnection    = this.peerConnectionFactory.createPeerConnection(configuration, this.observer);
            this.peerConnection.addStream(this.mediaStream);
            // 设置streamId同步
//          final List<String> streamIds = new ArrayList<>();
//          ListUtils.getOnlyOne(this.mediaStream.audioTracks, audioTrack -> {
//              this.peerConnection.addTrack(audioTrack, streamIds);
//              return audioTrack;
//          });
//          ListUtils.getOnlyOne(this.mediaStream.videoTracks, videoTrack -> {
//              this.peerConnection.addTrack(videoTrack, streamIds);
//              return videoTrack;
//          });
        }
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

    /**
     * 提供媒体服务
     */
    public void offer() {
        final MediaConstraints mediaConstraints = this.mediaManager.buildMediaConstraints();
        this.peerConnection.createOffer(this.offerSdpObserver, mediaConstraints);
    }

    private void offer(Message message, Map<String, Object> body) {
        final String sdp  = MapUtils.get(body, "sdp");
        final String type = MapUtils.get(body, "type");
        final SessionDescription.Type sdpType = SessionDescription.Type.valueOf(type.toUpperCase());
        final SessionDescription sessionDescription = new SessionDescription(sdpType, sdp);
        this.peerConnection.setRemoteDescription(this.offerSdpObserver, sessionDescription);
        this.answer();
    }

    private void answer() {
        final MediaConstraints mediaConstraints = this.mediaManager.buildMediaConstraints();
        this.peerConnection.createAnswer(this.answerSdpObserver, mediaConstraints);
    }

    private void answer(Message message, Map<String, Object> body) {
        final String sdp  = MapUtils.get(body, "sdp");
        final String type = MapUtils.get(body, "type");
        final SessionDescription.Type sdpType = SessionDescription.Type.valueOf(type.toUpperCase());
        final SessionDescription sessionDescription = new SessionDescription(sdpType, sdp);
        this.peerConnection.setRemoteDescription(this.answerSdpObserver, sessionDescription);
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
    public void playAudio() {
        super.playAudio();
        if(this.remoteMediaStream == null) {
            return;
        }
        ListUtils.getOnlyOne(this.remoteMediaStream.audioTracks, audioTrack -> {
            audioTrack.setEnabled(true);
            return audioTrack;
        });
    }

    @Override
    public void pauseAudio() {
        super.pauseAudio();
        ListUtils.getOnlyOne(this.remoteMediaStream.audioTracks, audioTrack -> {
            audioTrack.setEnabled(false);
            return audioTrack;
        });
    }

    @Override
    public void resumeAudio() {
        super.resumeAudio();
        ListUtils.getOnlyOne(this.remoteMediaStream.audioTracks, audioTrack -> {
            audioTrack.setEnabled(true);
            return audioTrack;
        });
    }

    @Override
    public void playVideo() {
        super.playVideo();
        if(this.remoteMediaStream == null) {
            return;
        }
        ListUtils.getOnlyOne(this.remoteMediaStream.videoTracks, videoTrack -> {
            videoTrack.setEnabled(true);
            if(this.surfaceViewRenderer == null) {
                this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_REMOTE_VIDEO, videoTrack);
            }
            return videoTrack;
        });
    }

    @Override
    public void pauseVideo() {
        super.pauseVideo();
        ListUtils.getOnlyOne(this.remoteMediaStream.videoTracks, videoTrack -> {
            videoTrack.setEnabled(false);
            return videoTrack;
        });
    }

    @Override
    public void resumeVideo() {
        super.resumeVideo();
        ListUtils.getOnlyOne(this.remoteMediaStream.videoTracks, videoTrack -> {
            videoTrack.setEnabled(true);
            return videoTrack;
        });
    }

    @Override
    public void pause() {
        super.pause();
        this.pauseAudio();
        this.pauseVideo();
    }

    @Override
    public void resume() {
        super.resume();
        this.resumeAudio();
        this.resumeVideo();
    }

    @Override
    public void close() {
        synchronized (this) {
            if(this.close) {
                return;
            }
            super.close();
//          if(this.mediaStream != null) {
//              this.mediaStream.dispose();
//          }
            if(this.remoteMediaStream != null) {
                this.remoteMediaStream.dispose();
            }
            if(this.peerConnection != null) {
                this.peerConnection.dispose();
            }
            this.mediaManager.closeClient();
        }
    }

    /**
     * @return 监听
     */
    private PeerConnection.Observer observer() {
        return new PeerConnection.Observer() {

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(SessionClient.class.getSimpleName(), "SignalingState状态改变：" + signalingState);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(SessionClient.class.getSimpleName(), "IceGatheringState状态改变：" + iceGatheringState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(SessionClient.class.getSimpleName(), "IceConnectionState状态改变：" + iceConnectionState);
            }

            @Override
            public void onIceConnectionReceivingChange(boolean result) {
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
                if(SessionClient.this.remoteMediaStream != null) {
                    SessionClient.this.remoteMediaStream.dispose();
                }
                SessionClient.this.remoteMediaStream = mediaStream;
                SessionClient.this.playAudio();
                SessionClient.this.playVideo();
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.i(SessionClient.class.getSimpleName(), "删除远程媒体：" + SessionClient.this.clientId);
                mediaStream.dispose();
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.i(SessionClient.class.getSimpleName(), "添加数据通道：" + SessionClient.this.clientId);
                if(SessionClient.this.dataChannel != null) {
                    SessionClient.this.dataChannel.dispose();
                }
                SessionClient.this.dataChannel = dataChannel;
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(SessionClient.class.getSimpleName(), "重新协商媒体：" + SessionClient.this.sessionId);
                if(peerConnection.connectionState() == PeerConnection.PeerConnectionState.CONNECTED) {
                // TODO：重新协商
//                  SessionClient.this.offer();
                }
            }

        };
    }

    private SdpObserver offerSdpObserver() {
        return new SdpObserver() {

            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(SessionClient.class.getSimpleName(), "创建OfferSDP成功：" + SessionClient.this.sessionId);
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
                Log.d(SessionClient.class.getSimpleName(), "设置OfferSDP成功：" + SessionClient.this.sessionId);
            }

            @Override
            public void onCreateFailure(String message) {
                Log.w(SessionClient.class.getSimpleName(), "创建OfferSDP失败：" + message);
            }

            @Override
            public void onSetFailure(String message) {
                Log.w(SessionClient.class.getSimpleName(), "设置OfferSDP失败：" + message);
            }

        };
    }

    private SdpObserver answerSdpObserver() {
        return new SdpObserver() {

            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(SessionClient.class.getSimpleName(), "创建AnswerSDP成功：" + SessionClient.this.sessionId);
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
                Log.d(SessionClient.class.getSimpleName(), "设置AnswerSDP成功：" + SessionClient.this.sessionId);
            }

            @Override
            public void onCreateFailure(String message) {
                Log.w(SessionClient.class.getSimpleName(), "创建AnswerSDP失败：" + message);
            }

            @Override
            public void onSetFailure(String message) {
                Log.w(SessionClient.class.getSimpleName(), "设置AnswerSDP失败：" + message);
            }

        };
    }

}
