package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ListUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.config.MediaProperties;
import com.acgist.taoyao.media.config.WebrtcProperties;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 视频会话
 * SDK + WebRTC实现视频会话
 *
 * 注意：
 * 2. offer/answer/candidate枚举大小
 * 1. candidate格式安卓和浏览器格式不同
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
        boolean preview,     boolean playAudio,    boolean playVideo,
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
            this.peerConnection    = this.peerConnectionFactory.createPeerConnection(configuration, this.observer);
            this.peerConnection.addStream(this.mediaStream);
            if(this.preview) {
                // 实现预览
            }
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
    public synchronized void offer() {
        final MediaConstraints mediaConstraints = this.mediaManager.buildMediaConstraints();
        this.peerConnection.createOffer(this.sdpObserver(
            "主动Offer",
            sessionDescription -> {
                this.peerConnection.setLocalDescription(this.sdpObserver(
                    "主动OfferExchange",
                    null,
                    () -> this.exchangeSessionDescription(sessionDescription)
                ), sessionDescription);
            },
            null
        ), mediaConstraints);
    }

    private void offer(Message message, Map<String, Object> body) {
        this.init();
        final String sdp  = MapUtils.get(body, "sdp");
        final String type = MapUtils.get(body, "type");
        final SessionDescription.Type sdpType = SessionDescription.Type.valueOf(type.toUpperCase());
        this.peerConnection.setRemoteDescription(this.sdpObserver(
            "被动Offer",
            null,
            () -> this.peerConnection.createAnswer(this.sdpObserver(
                "主动Answer",
                sessionDescription -> {
                    this.peerConnection.setLocalDescription(this.sdpObserver(
                        "主动AnswerExchange",
                        null,
                        () -> this.exchangeSessionDescription(sessionDescription)
                    ), sessionDescription);
                },
                null
            ), this.mediaManager.buildMediaConstraints())
        ), new SessionDescription(sdpType, sdp));
    }

    private void answer(Message message, Map<String, Object> body) {
        final String sdp  = MapUtils.get(body, "sdp");
        final String type = MapUtils.get(body, "type");
        final SessionDescription.Type sdpType = SessionDescription.Type.valueOf(type.toUpperCase());
        this.peerConnection.setRemoteDescription(this.sdpObserver(
            "被动Answer",
            null,
            null
        ), new SessionDescription(sdpType, sdp));
    }

    private void candidate(Message message, Map<String, Object> body) {
        final Map<String, Object> candidate = MapUtils.get(body, "candidate");
        final String  sdp                   = MapUtils.get(candidate, "candidate");
        final String  sdpMid                = MapUtils.get(candidate, "sdpMid");
        final Integer sdpMLineIndex         = MapUtils.getInteger(candidate, "sdpMLineIndex");
        if(sdp == null || sdpMid == null || sdpMLineIndex == null) {
            Log.w(SessionClient.class.getSimpleName(), "无效媒体协商：" + body);
        } else {
            this.peerConnection.addIceCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp));
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

    public void pause(String type) {
        if(MediaStreamTrack.AUDIO_TRACK_KIND.equals(type)) {
            ListUtils.getOnlyOne(this.mediaStream.audioTracks, audioTrack -> {
                audioTrack.setEnabled(false);
                return audioTrack;
            });
        } else if(MediaStreamTrack.VIDEO_TRACK_KIND.equals(type)) {
            ListUtils.getOnlyOne(this.mediaStream.videoTracks, videoTrack -> {
                videoTrack.setEnabled(false);
                return videoTrack;
            });
        } else {
        }
    }

    @Override
    public void resume() {
        super.resume();
        this.resumeAudio();
        this.resumeVideo();
    }

    public void resume(String type) {
        if(MediaStreamTrack.AUDIO_TRACK_KIND.equals(type)) {
            ListUtils.getOnlyOne(this.mediaStream.audioTracks, audioTrack -> {
                audioTrack.setEnabled(true);
                return audioTrack;
            });
        } else if(MediaStreamTrack.VIDEO_TRACK_KIND.equals(type)) {
            ListUtils.getOnlyOne(this.mediaStream.videoTracks, videoTrack -> {
                videoTrack.setEnabled(true);
                return videoTrack;
            });
        } else {
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if(this.close) {
                return;
            }
            super.close();
            try {
                // PeerConnection自动释放
//              if(this.mediaStream != null) {
//                  this.mediaStream.dispose();
//              }
//              if(this.remoteMediaStream != null) {
//                  this.remoteMediaStream.dispose();
//              }
                if(this.peerConnection != null) {
                    this.peerConnection.dispose();
                }
            } catch (Exception e) {
                Log.e(SessionClient.class.getSimpleName(), "释放资源异常", e);
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
                Log.d(SessionClient.class.getSimpleName(), "PC信令状态改变：" + signalingState);
                SessionClient.this.logState();
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(SessionClient.class.getSimpleName(), "PCIce收集状态改变：" + iceGatheringState);
                SessionClient.this.logState();
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(SessionClient.class.getSimpleName(), "PCIce连接状态改变：" + iceConnectionState);
                SessionClient.this.logState();
            }

            @Override
            public void onIceConnectionReceivingChange(boolean result) {
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(SessionClient.class.getSimpleName(), "发送媒体协商：" + SessionClient.this.sessionId);
                final Map<String, Object> candidate = new HashMap<>();
                candidate.put("sdpMid",        iceCandidate.sdpMid);
                candidate.put("candidate",     iceCandidate.sdp);
                candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                SessionClient.this.taoyao.push(SessionClient.this.taoyao.buildMessage(
                    "session::exchange",
                    "type",      "candidate",
                    "candidate", candidate,
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
                if(SessionClient.this.peerConnection.connectionState() == PeerConnection.PeerConnectionState.CONNECTED) {
                // TODO：重新协商
//                  SessionClient.this.offer();
                }
            }

        };
    }

    /**
     * @param tag                   标记
     * @param createSuccessConsumer 创建成功消费者
     * @param setSuccessRunnable    设置成功执行者
     *
     * @return SDP观察者
     */
    private SdpObserver sdpObserver(String tag, Consumer<SessionDescription> createSuccessConsumer, Runnable setSuccessRunnable) {
        return new SdpObserver() {

            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(SessionClient.class.getSimpleName(), "创建" + tag + "SDP成功：" + SessionClient.this.sessionId);
                SessionClient.this.logState();
                if(createSuccessConsumer != null) {
                    createSuccessConsumer.accept(sessionDescription);
                }
            }

            @Override
            public void onSetSuccess() {
                Log.d(SessionClient.class.getSimpleName(), "设置" + tag + "SDP成功：" + SessionClient.this.sessionId);
                SessionClient.this.logState();
                if(setSuccessRunnable != null) {
                    setSuccessRunnable.run();
                }
            }

            @Override
            public void onCreateFailure(String message) {
                Log.w(SessionClient.class.getSimpleName(), "创建" + tag + "SDP失败：" + message);
                SessionClient.this.logState();
            }

            @Override
            public void onSetFailure(String message) {
                Log.w(SessionClient.class.getSimpleName(), "设置" + tag + "SDP失败：" + message);
                SessionClient.this.logState();
            }

        };
    }

    private void exchangeSessionDescription(SessionDescription sessionDescription) {
        if(sessionDescription == null) {
            return;
        }
        final String type = sessionDescription.type.toString().toLowerCase();
        SessionClient.this.taoyao.push(SessionClient.this.taoyao.buildMessage(
            "session::exchange",
            "sdp",       sessionDescription.description,
            "type",      type,
            "sessionId", SessionClient.this.sessionId
        ));
    }

    private void logState() {
        Log.d(SessionClient.class.getSimpleName(), String.format(
            """
            PC信令状态：%s
            PC连接状态：%s
            PCIce收集状态：%s
            PCIce连接状态：%s
            """,
            this.peerConnection.signalingState().name(),
            this.peerConnection.connectionState().name(),
            this.peerConnection.iceGatheringState().name(),
            this.peerConnection.iceConnectionState().name()
        ));
    }

}
