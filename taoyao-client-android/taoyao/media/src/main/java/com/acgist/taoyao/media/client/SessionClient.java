package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.config.MediaProperties;
import com.acgist.taoyao.media.config.WebrtcProperties;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 视频会话
 * SDK + WebRTC实现视频会话
 *
 * TODO：通过setRemoteDescription设置x-google-start-bitrate、x-google-min-bitrate、x-google-max-bitrate
 *
 * 注意：
 * 1. offer/answer/candidate枚举大小
 * 2. candidate格式安卓和浏览器格式不同
 *
 * @author acgist
 */
public class SessionClient extends Client {

    /**
     * 会话ID
     */
    private final String sessionId;
    /**
     * 是否预览视频
     */
    private final boolean preview;
    /**
     * 是否播放音频
     */
    private final boolean playAudio;
    /**
     * 是否播放视频
     */
    private final boolean playVideo;
    /**
     * 是否消费数据
     */
    private final boolean dataConsume;
    /**
     * 是否消费音频
     */
    private final boolean audioConsume;
    /**
     * 是否消费视频
     */
    private final boolean videoConsume;
    /**
     * 是否生产数据
     */
    private final boolean dataProduce;
    /**
     * 是否生产音频
     */
    private final boolean audioProduce;
    /**
     * 是否生产视频
     */
    private final boolean videoProduce;
    /**
     * 媒体配置
     */
    private final MediaProperties mediaProperties;
    /**
     * WebRTC配置
     */
    private final WebrtcProperties webrtcProperties;
    /**
     * 数据通道
     */
    private DataChannel dataChannel;
    /**
     * 本地媒体
     */
    private MediaStream localMediaStream;
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
     * PeerConnectionFactory
     */
    private PeerConnectionFactory peerConnectionFactory;
    /**
     * 本地视频预览
     */
    private SurfaceViewRenderer localSurfaceViewRenderer;

    /**
     * @param sessionId        会话ID
     * @param name             远程终端名称
     * @param clientId         远程终端ID
     * @param taoyao           信令
     * @param mainHandler      MainHandler
     * @param preview          是否预览视频
     * @param playAudio        是否播放音频
     * @param playVideo        是否播放视频
     * @param dataConsume      是否消费数据
     * @param audioConsume     是否消费音频
     * @param videoConsume     是否消费视频
     * @param dataProduce      是否生产数据
     * @param audioProduce     是否生产音频
     * @param videoProduce     是否生产视频
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
        this.preview   = preview;
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
            this.localMediaStream  = this.mediaManager.buildLocalMediaStream(this.audioProduce, this.videoProduce);
            this.peerConnection    = this.peerConnectionFactory.createPeerConnection(configuration, this.observer);
            this.peerConnection.addStream(this.localMediaStream);
//          final List<String> streamIds = new ArrayList<>();
//          this.localMediaStream.audioTracks.forEach(audioTrack -> {
//              this.peerConnection.addTrack(audioTrack, streamIds);
//          });
//          this.localMediaStream.videoTracks.forEach(videoTrack -> {
//              this.peerConnection.addTrack(videoTrack, streamIds);
//          });
            if(this.preview) {
                this.previewLocalVideo();
            }
        }
    }

    /**
     * 媒体交换
     *
     * @param message 信令消息
     * @param body    消息主体
     */
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
     * 提供本地终端媒体
     */
    public synchronized void offer() {
        this.init();
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
        ), this.mediaManager.buildMediaConstraints());
    }

    /**
     * 远程终端提供媒体
     *
     * @param message 信令消息
     * @param body    消息主体
     */
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

    /**
     * 远程终端响应媒体
     *
     * @param message 信令消息
     * @param body    消息主体
     */
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

    /**
     * 远程终端媒体协商
     *
     * @param message 信令消息
     * @param body    消息主体
     */
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
        this.remoteMediaStream.audioTracks.forEach(audioTrack -> {
            audioTrack.setVolume(Config.DEFAULT_VOLUME);
            audioTrack.setEnabled(true);
        });
    }

    @Override
    public void pauseAudio() {
        super.pauseAudio();
        if(this.remoteMediaStream == null)  {
            return;
        }
        this.remoteMediaStream.audioTracks.forEach(audioTrack -> {
            audioTrack.setEnabled(false);
        });
    }

    @Override
    public void resumeAudio() {
        super.resumeAudio();
        if(this.remoteMediaStream == null)  {
            return;
        }
        this.remoteMediaStream.audioTracks.forEach(audioTrack -> {
            audioTrack.setEnabled(true);
        });
    }

    @Override
    public void playVideo() {
        super.playVideo();
        if(this.remoteMediaStream == null) {
            return;
        }
        this.remoteMediaStream.videoTracks.forEach(videoTrack -> {
            videoTrack.setEnabled(true);
            this.buildSurfaceViewRenderer(Config.WHAT_NEW_REMOTE_VIDEO, videoTrack);
        });
    }

    @Override
    public void pauseVideo() {
        super.pauseVideo();
        if(this.remoteMediaStream == null)  {
            return;
        }
        this.remoteMediaStream.videoTracks.forEach(videoTrack -> {
            videoTrack.setEnabled(false);
        });
    }

    @Override
    public void resumeVideo() {
        super.resumeVideo();
        if(this.remoteMediaStream == null)  {
            return;
        }
        this.remoteMediaStream.videoTracks.forEach(videoTrack -> {
            videoTrack.setEnabled(true);
        });
    }

    /**
     * 暂停本地媒体
     *
     * @param type 媒体类型
     */
    public void pauseLocal(String type) {
        if(this.localMediaStream == null) {
            return;
        }
        if(MediaStreamTrack.AUDIO_TRACK_KIND.equals(type)) {
            this.localMediaStream.audioTracks.forEach(audioTrack -> {
                audioTrack.setEnabled(false);
            });
        } else if(MediaStreamTrack.VIDEO_TRACK_KIND.equals(type)) {
            this.localMediaStream.videoTracks.forEach(videoTrack -> {
                videoTrack.setEnabled(false);
            });
        } else {
        }
    }

    /**
     * 恢复本地媒体
     *
     * @param type 媒体类型
     */
    public void resumeLocal(String type) {
        if(this.localMediaStream == null) {
            return;
        }
        if(MediaStreamTrack.AUDIO_TRACK_KIND.equals(type)) {
            this.localMediaStream.audioTracks.forEach(audioTrack -> {
                audioTrack.setEnabled(true);
            });
        } else if(MediaStreamTrack.VIDEO_TRACK_KIND.equals(type)) {
            this.localMediaStream.videoTracks.forEach(videoTrack -> {
                videoTrack.setEnabled(true);
            });
        } else {
        }
    }

    /**
     * 预览本地视频
     */
    private void previewLocalVideo() {
        if(this.localMediaStream == null) {
            return;
        }
        this.localMediaStream.videoTracks.forEach(videoTrack -> {
            videoTrack.setEnabled(true);
            if(this.localSurfaceViewRenderer == null) {
                this.localSurfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_LOCAL_VIDEO, videoTrack);
            } else {
                Log.w(SessionClient.class.getSimpleName(), "视频预览已经存在");
            }
        });
    }

    /**
     * 释放本地媒体
     */
    private void releaseLocalVideo() {
        // 释放本地视频资源
        if(this.localSurfaceViewRenderer != null) {
            // 释放资源
            this.localSurfaceViewRenderer.release();
            // 移除资源：注意先释放再移除避免报错
            this.mainHandler.obtainMessage(Config.WHAT_REMOVE_VIDEO, this.localSurfaceViewRenderer).sendToTarget();
            // 设置为空
            this.localSurfaceViewRenderer = null;
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if(this.close) {
                return;
            }
            super.close();
            this.releaseLocalVideo();
            try {
                // PeerConnection自动释放：mediaStream、remoteMediaStream
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
     * @return PC观察者
     */
    private PeerConnection.Observer observer() {
        return new PeerConnection.Observer() {

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(SessionClient.class.getSimpleName(), "PCSignalingState改变：" + signalingState);
                SessionClient.this.logState();
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(SessionClient.class.getSimpleName(), "PCIceGatheringState改变：" + iceGatheringState);
                SessionClient.this.logState();
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                // disconnected：暂时连接不上可能自我恢复
                Log.d(SessionClient.class.getSimpleName(), "PCIceConnectionState改变：" + iceConnectionState);
                SessionClient.this.logState();
            }

            @Override
            public void onIceConnectionReceivingChange(boolean result) {
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(SessionClient.class.getSimpleName(), "媒体协商：" + SessionClient.this.sessionId);
                SessionClient.this.exchangeIceCandidate(iceCandidate);
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
                SessionClient.this.remoteMediaStream = null;
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
                    // TODO：验证
                    SessionClient.this.peerConnection.restartIce();
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

    /**
     * @param iceCandidate 媒体协商
     */
    private void exchangeIceCandidate(IceCandidate iceCandidate) {
        if(iceCandidate == null) {
            return;
        }
        final Map<String, Object> candidate = new HashMap<>();
        candidate.put("sdpMid",        iceCandidate.sdpMid);
        candidate.put("candidate",     iceCandidate.sdp);
        candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
        this.taoyao.push(this.taoyao.buildMessage(
            "session::exchange",
            "type",      "candidate",
            "candidate", candidate,
            "sessionId", this.sessionId
        ));
    }

    /**
     * @param sessionDescription SDP
     */
    private void exchangeSessionDescription(SessionDescription sessionDescription) {
        if(sessionDescription == null) {
            return;
        }
        final String type = sessionDescription.type.toString().toLowerCase();
        this.taoyao.push(this.taoyao.buildMessage(
            "session::exchange",
            "sdp",       sessionDescription.description,
            "type",      type,
            "sessionId", this.sessionId
        ));
    }

    /**
     * 记录PC状态
     */
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
