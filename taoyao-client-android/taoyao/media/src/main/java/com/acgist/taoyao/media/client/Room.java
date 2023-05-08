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

    private final String name;
    private final String roomId;
    private final String clientId;
    private final String password;
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
    private final Map<String, RemoteClient> remoteClients;
    private final long nativeRoomPointer;
    private LocalClient localClient;
    private PeerConnection.RTCConfiguration rtcConfiguration;
    private PeerConnectionFactory peerConnectionFactory;
    private Object rtpCapabilities;
    private Object sctpCapabilities;

    /**
     * @param name         房间名称
     * @param roomId       房间ID
     * @param clientId     当前终端ID
     * @param password     房间密码
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
    public Room(
        String name, String roomId,
        String clientId, String password,
        ITaoyao taoyao, Handler mainHandler,
        boolean preview, boolean playAudio, boolean playVideo,
        boolean dataConsume, boolean audioConsume, boolean videoConsume,
        boolean dataProduce, boolean audioProduce, boolean videoProduce,
        MediaProperties mediaProperties, WebrtcProperties webrtcProperties
    ) {
        super(taoyao, mainHandler);
        this.name = name;
        this.roomId   = roomId;
        this.clientId = clientId;
        this.password = password;
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
        this.remoteClients = new ConcurrentHashMap<>();
        this.nativeRoomPointer = this.nativeNewRoom(roomId, this);
    }

    public boolean enter() {
        synchronized (this) {
            if (this.init) {
                return true;
            }
            super.init();
            this.peerConnectionFactory = this.mediaManager.newClient();
            this.localClient = new LocalClient(this.name, this.clientId, this.taoyao, this.mainHandler);
            this.localClient.setMediaStream(this.mediaManager.buildLocalMediaStream(this.audioProduce, this.videoProduce));
            if(this.preview) {
                this.localClient.playVideo();
            }
            // STUN | TURN
            final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
            // 不用配置
//          final List<PeerConnection.IceServer> iceServers = this.webrtcProperties.getIceServers();
            this.rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
            return this.taoyao.requestFuture(
                this.taoyao.buildMessage("media::router::rtp::capabilities", "roomId", this.roomId),
                response -> {
                    this.nativeEnterRoom(
                        this.nativeRoomPointer,
                        JSONUtils.toJSON(MapUtils.get(response.body(), "rtpCapabilities")),
                        this.peerConnectionFactory.getNativePeerConnectionFactory(),
                        this.rtcConfiguration
                    );
                    return true;
                },
                response -> {
                    this.close();
                    return false;
                }
            );
        }
    }

    public void mediaProduce() {
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
        if(this.clientId.equals(clientId)) {
            return;
        }
        final Map<String, Object> status = MapUtils.get(body, "status");
        final String name = MapUtils.get(status, "name");
        final RemoteClient remoteClient = new RemoteClient(name, clientId, this.taoyao, this.mainHandler);
        final RemoteClient old = this.remoteClients.put(clientId, remoteClient);
        if(old != null) {
            // 关闭旧的资源
            old.close();
        }
    }

    public void newRemoteClientFromRoomClientList(Map<String, Object> body) {
        final List<Map<String, Object>> clients = MapUtils.get(body, "clients");
        if(CollectionUtils.isEmpty(clients)) {
            return;
        }
        clients.forEach(map -> {
            final String name = MapUtils.get(map, "name");
            final String clientId = MapUtils.get(map, "clientId");
            if(this.clientId.equals(clientId)) {
                return;
            }
            final RemoteClient remoteClient = new RemoteClient(name, clientId, this.taoyao, this.mainHandler);
            final RemoteClient old = this.remoteClients.put(clientId, remoteClient);
            if(old != null) {
                // 关闭旧的资源
                old.close();
            }
        });
    }

    public void closeRemoteClient(String clientId) {
        final RemoteClient remoteClient = this.remoteClients.remove(clientId);
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
            this.nativeCloseRoom(this.nativeRoomPointer);
            // 关闭远程媒体
            this.remoteClients.values().forEach(v -> this.closeRemoteClient(v.clientId));
            this.remoteClients.clear();
            // 关闭本地媒体
            this.localClient.close();
            this.mediaManager.closeClient();
        }
    }

    public void mediaConsumerClose(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::close",
            "roomId", this.roomId,
            "consumerId", consumerId
        ));
    }

    public void mediaConsumerClose(Map<String, Object> body) {
        final String consumerId = MapUtils.get(body, "consumerId");
        this.nativeMediaConsumerClose(this.nativeRoomPointer, consumerId);
        this.remoteClients.values().forEach(v -> v.close(consumerId));
    }

    public void mediaConsumerPause(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::pause",
            "roomId", this.roomId,
            "consumerId", consumerId
        ));
    }

    public void mediaConsumerPause(Map<String, Object> body) {
        this.nativeMediaConsumerPause(this.nativeRoomPointer, MapUtils.get(body, "consumerId"));
    }

    public void mediaConsumerResume(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::resume",
            "roomId", this.roomId,
            "consumerId", consumerId
        ));
    }

    public void mediaConsumerResume(Map<String, Object> body) {
        this.nativeMediaConsumerResume(this.nativeRoomPointer, MapUtils.get(body, "consumerId"));
    }

    public void mediaProducerClose(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::close",
            "roomId", this.roomId,
            "producerId", producerId
        ));
    }

    public void mediaProducerClose(Map<String, Object> body) {
        final String producerId = MapUtils.get(body, "producerId");
        this.nativeMediaProducerClose(this.nativeRoomPointer, producerId);
        this.localClient.close(producerId);
    }

    public void mediaProducerPause(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::pause",
            "roomId", this.roomId,
            "producerId", producerId
        ));
    }

    public void mediaProducerPause(Map<String, Object> body) {
        this.nativeMediaProducerPause(this.nativeRoomPointer, MapUtils.get(body, "producerId"));
    }

    public void mediaProducerResume(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::resume",
            "roomId", this.roomId,
            "producerId", producerId
        ));
    }

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
    public void consumerNewCallback(String message, long consumerPointer, long consumerMediaTrackPointer) {
        final Message response = JSONUtils.toJava(message, Message.class);
        final Map<String, Object> body = response.body();
        final String kind       = MapUtils.get(body, "kind");
        final String sourceId   = MapUtils.get(body, "sourceId");
        final String consumerId = MapUtils.get(body, "consumerId");
        final RemoteClient remoteClient = this.remoteClients.get(sourceId);
        if(remoteClient == null) {
            // TODO：资源释放
            return;
        }
        if(MediaStreamTrack.AUDIO_TRACK_KIND.equals(kind)) {
//          WebRtcAudioTrack
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
            // TODO：资源释放
            return;
        }
        this.taoyao.push(response);
    }

    private native long nativeNewRoom(String roomId, RouterCallback routerCallback);
    private native void nativeEnterRoom(long nativePointer, String rtpCapabilities, long peerConnectionFactoryPointer, PeerConnection.RTCConfiguration rtcConfiguration);
    private native void nativeCloseRoom(long nativePointer);
    private native void nativeCreateSendTransport(long nativeRoomPointer, String body);
    private native void nativeCreateRecvTransport(long nativeRoomPointer, String body);
    private native void nativeMediaProduceAudio(long nativeRoomPointer, long mediaStreamPointer);
    private native void nativeMediaProduceVideo(long nativeRoomPointer, long mediaStreamPointer);
    private native void nativeMediaConsume(long nativeRoomPointer, String message);
    private native void nativeMediaProducerPause(long nativeRoomPointer, String producerId);
    private native void nativeMediaProducerResume(long nativeRoomPointer, String producerId);
    private native void nativeMediaProducerClose(long nativeRoomPointer, String producerId);
    private native void nativeMediaConsumerPause(long nativeRoomPointer, String consumerId);
    private native void nativeMediaConsumerResume(long nativeRoomPointer, String consumerId);
    private native void nativeMediaConsumerClose(long nativeRoomPointer, String consumerId);

}