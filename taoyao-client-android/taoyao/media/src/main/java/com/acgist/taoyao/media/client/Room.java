package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.boot.utils.PointerUtils;
import com.acgist.taoyao.media.RouterCallback;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.config.MediaProperties;
import com.acgist.taoyao.media.config.WebrtcProperties;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.apache.commons.collections4.CollectionUtils;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 视频房间
 *
 * @author acgist
 */
public class Room extends CloseableClient implements RouterCallback {

    /**
     * 房间ID
     */
    private final String roomId;
    /**
     * 房间名称
     */
    private final String name;
    /**
     * 房间密码
     */
    private final String password;
    /**
     * 当前终端ID
     */
    private final String clientId;
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
     * 是否使用IceServer
     */
    private final boolean useIceServer;
    /**
     * 是否已经开始生产
     */
    private boolean produce;
    /**
     * 媒体配置
     */
    private final MediaProperties mediaProperties;
    /**
     * WebRTC配置
     */
    private final WebrtcProperties webrtcProperties;
    /**
     * 房间指针
     */
    private volatile long nativeRoomPointer;
    /**
     * 本地终端
     */
    private LocalClient localClient;
    /**
     * 远程终端
     */
    private final Map<String, RemoteClient> remoteClients;
    /**
     * RTC能力
     */
    private Object rtpCapabilities;
    /**
     * SCTP能力
     */
    private Object sctpCapabilities;
    /**
     * RTC配置
     */
    private PeerConnection.RTCConfiguration rtcConfiguration;
    /**
     * PeerConnectionFactory
     */
    private PeerConnectionFactory peerConnectionFactory;

    /**
     * @param roomId           房间ID
     * @param name             终端名称
     * @param clientId         当前终端ID
     * @param password         房间密码
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
     * @param useIceServer     是否使用IceServer
     * @param mediaProperties  媒体配置
     * @param webrtcProperties WebRTC配置
     */
    public Room(
        String roomId,   String name,
        String password, String clientId,
        ITaoyao taoyao,  Handler mainHandler,
        boolean preview,     boolean playAudio,    boolean playVideo,
        boolean dataConsume, boolean audioConsume, boolean videoConsume,
        boolean dataProduce, boolean audioProduce, boolean videoProduce,
        boolean useIceServer,
        MediaProperties mediaProperties, WebrtcProperties webrtcProperties
    ) {
        super(taoyao, mainHandler);
        this.roomId    = roomId;
        this.name      = name;
        this.password  = password;
        this.clientId  = clientId;
        this.preview   = preview;
        this.playAudio = playAudio;
        this.playVideo = playVideo;
        this.dataConsume  = dataConsume;
        this.audioConsume = audioConsume;
        this.videoConsume = videoConsume;
        this.dataProduce  = dataProduce;
        this.audioProduce = audioProduce;
        this.videoProduce = videoProduce;
        this.useIceServer = useIceServer;
        this.produce      = false;
        this.mediaProperties   = mediaProperties;
        this.webrtcProperties  = webrtcProperties;
        this.remoteClients     = new ConcurrentHashMap<>();
        this.nativeRoomPointer = this.nativeNewRoom(roomId, this);
    }

    /**
     * @return 是否成功进入房间
     */
    public boolean enter() {
        synchronized (this) {
            if (this.init) {
                return true;
            }
            Log.i(Room.class.getSimpleName(), "进入房间：" + this.roomId);
            super.init();
            this.peerConnectionFactory = this.mediaManager.newClient();
            this.localClient = new LocalClient(this.name, this.clientId, this.taoyao, this.mainHandler);
            this.localClient.setMediaStream(this.mediaManager.buildLocalMediaStream(this.audioProduce, this.videoProduce));
            if(this.preview) {
                this.localClient.playVideo();
            }
            // STUN | TURN
            final List<PeerConnection.IceServer> iceServers;
            if(this.useIceServer) {
                // 不用配置：正常情况都是能够直接访问媒体服务
                iceServers = this.webrtcProperties.getIceServers();
            } else {
                iceServers = new ArrayList<>();
            }
            this.rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
            // 开始协商
            return this.taoyao.requestFuture(
                this.taoyao.buildMessage("media::router::rtp::capabilities", "roomId", this.roomId),
                // 成功加载Mediasoup房间
                response -> {
                    this.nativeEnterRoom(
                        this.nativeRoomPointer,
                        JSONUtils.toJSON(MapUtils.get(response.body(), "rtpCapabilities")),
                        this.peerConnectionFactory.getNativePeerConnectionFactory(),
                        this.rtcConfiguration
                    );
                    return true;
                },
                // 失败关闭资源
                response -> {
                    this.close();
                    return false;
                }
            );
        }
    }

    /**
     * 生产媒体
     */
    public void mediaProduce() {
        synchronized(this) {
            if(this.produce) {
                return;
            }
            this.produce = true;
        }
        if (this.audioProduce || this.videoProduce) {
            this.createSendTransport();
        }
        if (this.audioConsume || this.videoConsume) {
            this.createRecvTransport();
        }
        final MediaStream mediaStream = this.localClient.getMediaStream();
        final long mediaStreamPointer = PointerUtils.getNativePointer(mediaStream, "nativeStream");
        if (this.audioProduce) {
            this.nativeMediaProduceAudio(this.nativeRoomPointer, mediaStreamPointer);
        }
        if (this.videoProduce) {
            this.nativeMediaProduceVideo(this.nativeRoomPointer, mediaStreamPointer);
        }
    }

    /**
     * 创建发送媒体通道
     */
    private void createSendTransport() {
        this.taoyao.requestFuture(
            this.taoyao.buildMessage(
                "media::transport::webrtc::create",
                "roomId", this.roomId,
                "forceTcp", false,
                "producing", true,
                "consuming", false,
                "sctpCapabilities", this.dataProduce ? this.sctpCapabilities : null
            ),
            response -> {
                this.nativeCreateSendTransport(this.nativeRoomPointer, JSONUtils.toJSON(response.body()));
            },
            response -> {
                Log.w(Room.class.getSimpleName(), "创建发送通道失败");
            }
        );
    }

    /**
     * 创建接收媒体通道
     */
    private void createRecvTransport() {
        this.taoyao.requestFuture(
            this.taoyao.buildMessage(
                "media::transport::webrtc::create",
                "roomId", this.roomId,
                "forceTcp", false,
                "producing", false,
                "consuming", true,
                "sctpCapabilities", this.dataProduce ? this.sctpCapabilities : null
            ),
            response -> {
                this.nativeCreateRecvTransport(this.nativeRoomPointer, JSONUtils.toJSON(response.body()));
            },
            response -> {
                Log.w(Room.class.getSimpleName(), "创建接收通道失败");
            }
        );
    }

    /**
     * 媒体消费
     *
     * @param message 信令消息
     * @param body    消息主体
     */
    public void mediaConsume(Message message, Map<String, Object> body) {
        this.nativeMediaConsume(this.nativeRoomPointer, JSONUtils.toJSON(message));
    }

    /**
     * 新增远程终端
     *
     * @param body 消息主体
     */
    public void newRemoteClientFromRoomEnter(Map<String, Object> body) {
        final String clientId = MapUtils.get(body, "clientId");
        // 忽略自己
        if(this.clientId.equals(clientId)) {
            return;
        }
        final Map<String, Object> status   = MapUtils.get(body, "status");
        final String name                  = MapUtils.get(status, "name");
        final RemoteClient remoteClient    = new RemoteClient(name, clientId, this.taoyao, this.mainHandler);
        final RemoteClient oldRemoteClient = this.remoteClients.put(clientId, remoteClient);
        if(oldRemoteClient != null) {
            // 关闭旧的资源
            this.closeRemoteClient(oldRemoteClient);
        }
    }

    /**
     * 新增远程终端
     *
     * @param body 消息主体
     */
    public void newRemoteClientFromRoomClientList(Map<String, Object> body) {
        final List<Map<String, Object>> clients = MapUtils.get(body, "clients");
        if(CollectionUtils.isEmpty(clients)) {
            return;
        }
        clients.forEach(map -> {
            final String name     = MapUtils.get(map, "name");
            final String clientId = MapUtils.get(map, "clientId");
            // 忽略自己
            if(this.clientId.equals(clientId)) {
                return;
            }
            final RemoteClient remoteClient    = new RemoteClient(name, clientId, this.taoyao, this.mainHandler);
            final RemoteClient oldRemoteClient = this.remoteClients.put(clientId, remoteClient);
            if(oldRemoteClient != null) {
                // 关闭旧的资源
                this.closeRemoteClient(oldRemoteClient);
            }
        });
    }

    /**
     * 关闭远程终端
     *
     * @param clientId 远程终端ID
     */
    public void closeRemoteClient(String clientId) {
        final RemoteClient remoteClient = this.remoteClients.remove(clientId);
        this.closeRemoteClient(remoteClient);
    }

    /**
     * 关闭远程终端
     * 注意：需要自己从列表中删除
     *
     * @param remoteClient 远程终端
     */
    private void closeRemoteClient(RemoteClient remoteClient) {
        if(remoteClient == null) {
            return;
        }
        remoteClient.tracks.keySet().forEach(consumerId -> {
            this.nativeMediaConsumerClose(this.nativeRoomPointer, consumerId);
        });
        remoteClient.close();
    }

    @Override
    public void close() {
        synchronized (this) {
            if(this.close) {
                return;
            }
            Log.i(Room.class.getSimpleName(), "关闭房间：" + this.roomId);
            super.close();
            // 关闭Mediasoup房间
            this.nativeCloseRoom(this.nativeRoomPointer);
            this.nativeRoomPointer = 0L;
            // 关闭远程媒体
            this.remoteClients.values().forEach(this::closeRemoteClient);
            this.remoteClients.clear();
            // 关闭本地媒体
            this.localClient.close();
            // 释放终端
            this.mediaManager.closeClient();
        }
    }

    /**
     * 主动关闭消费者
     *
     * @param consumerId 消费者ID
     */
    public void mediaConsumerClose(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::close",
            "roomId",     this.roomId,
            "consumerId", consumerId
        ));
    }

    /**
     * 关闭消费者回调（信令）
     *
     * @param body 消息主体
     */
    public void mediaConsumerClose(Map<String, Object> body) {
        final String consumerId = MapUtils.get(body, "consumerId");
        this.nativeMediaConsumerClose(this.nativeRoomPointer, consumerId);
        this.remoteClients.values().forEach(v -> v.close(consumerId));
    }

    /**
     * 主动暂停消费者
     *
     * @param consumerId 消费者ID
     */
    public void mediaConsumerPause(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::pause",
            "roomId",     this.roomId,
            "consumerId", consumerId
        ));
    }

    /**
     * 暂停消费者回调（信令）
     *
     * @param body 消息主体
     */
    public void mediaConsumerPause(Map<String, Object> body) {
        this.nativeMediaConsumerPause(this.nativeRoomPointer, MapUtils.get(body, "consumerId"));
    }

    /**
     * 主动恢复消费者
     *
     * @param consumerId 消费者ID
     */
    public void mediaConsumerResume(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::resume",
            "roomId",     this.roomId,
            "consumerId", consumerId
        ));
    }

    /**
     * 恢复消费者回调（信令）
     *
     * @param body 消息主体
     */
    public void mediaConsumerResume(Map<String, Object> body) {
        this.nativeMediaConsumerResume(this.nativeRoomPointer, MapUtils.get(body, "consumerId"));
    }

    /**
     * 主动关闭生产者
     *
     * @param producerId 生产者ID
     */
    public void mediaProducerClose(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::close",
            "roomId",     this.roomId,
            "producerId", producerId
        ));
    }

    /**
     * 关闭生产者回调（信令）
     *
     * @param body 消息主体
     */
    public void mediaProducerClose(Map<String, Object> body) {
        final String producerId = MapUtils.get(body, "producerId");
        this.nativeMediaProducerClose(this.nativeRoomPointer, producerId);
        this.localClient.close(producerId);
    }

    /**
     * 主动暂停生产者
     *
     * @param producerId 生产者ID
     */
    public void mediaProducerPause(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::pause",
            "roomId",     this.roomId,
            "producerId", producerId
        ));
    }

    /**
     * 暂停生产者回调（信令）
     *
     * @param body 消息主体
     */
    public void mediaProducerPause(Map<String, Object> body) {
        this.nativeMediaProducerPause(this.nativeRoomPointer, MapUtils.get(body, "producerId"));
    }

    /**
     * 主动恢复生产者
     *
     * @param producerId 生产者ID
     */
    public void mediaProducerResume(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::resume",
            "roomId",     this.roomId,
            "producerId", producerId
        ));
    }

    /**
     * 恢复生产者回调（信令）
     *
     * @param body 消息主体
     */
    public void mediaProducerResume(Map<String, Object> body) {
        this.nativeMediaProducerResume(this.nativeRoomPointer, MapUtils.get(body, "producerId"));
    }

    @Override
    public void enterRoomCallback(String rtpCapabilities, String sctpCapabilities) {
        this.rtpCapabilities  = JSONUtils.toJava(rtpCapabilities);
        this.sctpCapabilities = JSONUtils.toJava(sctpCapabilities);
        this.taoyao.request(this.taoyao.buildMessage(
            "room::enter",
            "roomId",           this.roomId,
            "password",         this.password,
            "rtpCapabilities",  this.rtpCapabilities,
            "sctpCapabilities", this.sctpCapabilities
        ));
    }

    @Override
    public void closeRoomCallback() {
    }

    @Override
    public void sendTransportConnectCallback(String transportId, String dtlsParameters) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::transport::webrtc::connect",
            "roomId",         this.roomId,
            "transportId",    transportId,
            "dtlsParameters", JSONUtils.toMap(dtlsParameters)
        ));
    }

    @Override
    public void recvTransportConnectCallback(String transportId, String dtlsParameters) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::transport::webrtc::connect",
            "roomId",         this.roomId,
            "transportId",    transportId,
            "dtlsParameters", JSONUtils.toMap(dtlsParameters)
        ));
    }

    @Override
    public String sendTransportProduceCallback(String kind, String transportId, String rtpParameters) {
        return this.taoyao.requestFuture(
            this.taoyao.buildMessage(
                "media::produce",
                "kind",          kind,
                "roomId",        this.roomId,
                "transportId",   transportId,
                "rtpParameters", JSONUtils.toMap(rtpParameters)
            ),
            response -> {
                final Map<String, Object> body = response.body();
                return MapUtils.get(body, "producerId");
            }
        );
    }

    @Override
    public void producerNewCallback(String kind, String producerId, long producerPointer, long producerMediaTrackPointer) {
        this.localClient.tracks.put(producerId, producerMediaTrackPointer);
        if(MediaStreamTrack.AUDIO_TRACK_KIND.equals(kind)) {
            this.localClient.audioProducerPointer = producerPointer;
        } else if(MediaStreamTrack.VIDEO_TRACK_KIND.equals(kind)) {
            this.localClient.videoProducerPointer = producerPointer;
        } else {
        }
    }

    @Override
    public void producerCloseCallback(String producerId) {
    }

    @Override
    public void producerPauseCallback(String producerId) {
    }

    @Override
    public void producerResumeCallback(String producerId) {
    }

    @Override
    public void consumerNewCallback(String message, long consumerPointer, long consumerMediaTrackPointer) {
        final Message response = JSONUtils.toJava(message, Message.class);
        final Map<String, Object> body = response.body();
        final String kind       = MapUtils.get(body, "kind");
        final String sourceId   = MapUtils.get(body, "sourceId");
        final String consumerId = MapUtils.get(body, "consumerId");
        final RemoteClient remoteClient = this.remoteClients.computeIfAbsent(sourceId, key -> {
            // 假如媒体上来时间比进入房间消息快：基本上不可能出现这种情况
            Log.w(Room.class.getSimpleName(), "未知媒体来源：" + sourceId);
            return new RemoteClient(sourceId, sourceId, this.taoyao, this.mainHandler);
        });
        if(MediaStreamTrack.AUDIO_TRACK_KIND.equals(kind)) {
            final AudioTrack audioTrack = new AudioTrack(consumerMediaTrackPointer);
            audioTrack.setVolume(Config.DEFAULT_VOLUME);
            remoteClient.tracks.put(consumerId, audioTrack);
            remoteClient.audioConsumerPointer = consumerPointer;
            remoteClient.playAudio();
        } else if(MediaStreamTrack.VIDEO_TRACK_KIND.equals(kind)) {
            final VideoTrack videoTrack = new VideoTrack(consumerMediaTrackPointer);
            remoteClient.tracks.put(consumerId, videoTrack);
            remoteClient.videoConsumerPointer = consumerPointer;
            remoteClient.playVideo();
        } else {
            Log.w(Room.class.getSimpleName(), "未知媒体类型：" + kind);
            return;
        }
        this.taoyao.push(response);
    }

    @Override
    public void consumerCloseCallback(String consumerId) {
    }

    @Override
    public void consumerPauseCallback(String consumerId) {
    }

    @Override
    public void consumerResumeCallback(String consumerId) {
    }

    /**
     * Mediasoup创建房间
     *
     * @param roomId         房间ID
     * @param routerCallback 路由回调
     *
     * @return Mediasoup房间指针
     */
    private native long nativeNewRoom(String roomId, RouterCallback routerCallback);

    /**
     * Mediasou进入房间
     *
     * @param nativeRoomPointer            房间指针
     * @param rtpCapabilities              RTP能力
     * @param peerConnectionFactoryPointer PeerConnectionFactory指针
     * @param rtcConfiguration             RTC配置
     */
    private native void nativeEnterRoom(long nativeRoomPointer, String rtpCapabilities, long peerConnectionFactoryPointer, PeerConnection.RTCConfiguration rtcConfiguration);

    /**
     * Mediasoup关闭房间
     *
     * @param nativeRoomPointer 房间指针
     */
    private native void nativeCloseRoom(long nativeRoomPointer);

    /**
     * Mediasoup创建发送通道
     *
     * @param nativeRoomPointer 房间指针
     * @param body              消息主体
     */
    private native void nativeCreateSendTransport(long nativeRoomPointer, String body);

    /**
     * Mediasoup创建接收通道
     *
     * @param nativeRoomPointer 房间指针
     * @param body              消息主体
     */
    private native void nativeCreateRecvTransport(long nativeRoomPointer, String body);

    /**
     * Mediasoup生产音频
     *
     * @param nativeRoomPointer  房间指针
     * @param mediaStreamPointer 媒体指针
     */
    private native void nativeMediaProduceAudio(long nativeRoomPointer, long mediaStreamPointer);

    /**
     * Mediasoup生产视频
     *
     * @param nativeRoomPointer  房间指针
     * @param mediaStreamPointer 媒体指针
     */
    private native void nativeMediaProduceVideo(long nativeRoomPointer, long mediaStreamPointer);

    /**
     * Mediasoup消费
     *
     * @param nativeRoomPointer 房间指针
     * @param message           信令消息
     */
    private native void nativeMediaConsume(long nativeRoomPointer, String message);

    /**
     * Mediasoup暂停生产者
     *
     * @param nativeRoomPointer 房间指针
     * @param producerId        生产者ID
     */
    private native void nativeMediaProducerPause(long nativeRoomPointer, String producerId);

    /**
     * Mediasoup恢复生产者
     *
     * @param nativeRoomPointer 房间指针
     * @param producerId        生产者ID
     */
    private native void nativeMediaProducerResume(long nativeRoomPointer, String producerId);

    /**
     * Mediasoup关闭生产者
     *
     * @param nativeRoomPointer 房间指针
     * @param producerId        生产者ID
     */
    private native void nativeMediaProducerClose(long nativeRoomPointer, String producerId);

    /**
     * Mediasoup暂停消费者
     *
     * @param nativeRoomPointer 房间指针
     * @param consumerId        消费者ID
     */
    private native void nativeMediaConsumerPause(long nativeRoomPointer, String consumerId);

    /**
     * Mediasoup恢复消费者
     *
     * @param nativeRoomPointer 房间指针
     * @param consumerId        消费者ID
     */
    private native void nativeMediaConsumerResume(long nativeRoomPointer, String consumerId);

    /**
     * Mediasoup关闭消费者
     *
     * @param nativeRoomPointer 房间指针
     * @param consumerId        消费者ID
     */
    private native void nativeMediaConsumerClose(long nativeRoomPointer, String consumerId);

}
