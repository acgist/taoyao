package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.boot.utils.PointerUtils;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.RouterCallback;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoTrack;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间
 *
 * @author acgist
 */
public class Room implements Closeable, RouterCallback {

    private final String name;
    private final String clientId;
    private final String roomId;
    private final String password;
    private final Handler handler;
    private final ITaoyao taoyao;
    private final boolean dataConsume;
    private final boolean audioConsume;
    private final boolean videoConsume;
    private final boolean dataProduce;
    private final boolean audioProduce;
    private final boolean videoProduce;
    private final long nativeRoomPointer;
    private final MediaManager mediaManager;
    private volatile boolean enter;
    private LocalClient localClient;
    private Map<String, RemoteClient> remoteClients;
    private PeerConnection.RTCConfiguration rtcConfiguration;
    private PeerConnectionFactory peerConnectionFactory;
    private String sctpCapabilities;

    public Room(
        String name, String clientId,
        String roomId, String password,
        Handler handler, ITaoyao taoyao,
        boolean dataConsume, boolean audioConsume, boolean videoConsume,
        boolean dataProduce, boolean audioProduce, boolean videoProduce
    ) {
        this.name = name;
        this.clientId = clientId;
        this.roomId = roomId;
        this.password = password;
        this.handler = handler;
        this.taoyao = taoyao;
        this.dataConsume = dataConsume;
        this.audioConsume = audioConsume;
        this.videoConsume = videoConsume;
        this.dataProduce = dataProduce;
        this.audioProduce = audioProduce;
        this.videoProduce = videoProduce;
        this.nativeRoomPointer = this.nativeNewRoom(roomId, this);
        this.mediaManager = MediaManager.getInstance();
        this.remoteClients = new ConcurrentHashMap<>();
        this.enter = false;
    }

    public synchronized void enter() {
        if (this.enter) {
            return;
        }
        final Message response = this.taoyao.request(this.taoyao.buildMessage("media::router::rtp::capabilities", "roomId", this.roomId));
        if (response == null) {
            Log.w(Room.class.getSimpleName(), "获取通道能力失败");
            return;
        }
        this.localClient = new LocalClient(this.name, this.clientId, this.handler, this.taoyao);
        this.localClient.setMediaStream(this.mediaManager.getMediaStream());
        // STUN | TURN
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        // TODO：读取配置
        final PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);
        this.rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
        this.peerConnectionFactory = this.mediaManager.newClient(MediaManager.Type.BACK);
        final Object rtpCapabilities = MapUtils.get(response.body(), "rtpCapabilities");
        this.nativeEnter(this.nativeRoomPointer, JSONUtils.toJSON(rtpCapabilities), this.peerConnectionFactory.getNativePeerConnectionFactory(), this.rtcConfiguration);
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
        final Message response = this.taoyao.request(this.taoyao.buildMessage(
            "media::transport::webrtc::create",
            "roomId", this.roomId,
            "forceTcp", false,
            "producing", true,
            "consuming", false,
            "sctpCapabilities", this.dataProduce ? this.sctpCapabilities : null
        ));
        if (response == null) {
            Log.w(Room.class.getSimpleName(), "创建发送通道失败");
            return;
        }
        this.nativeCreateSendTransport(this.nativeRoomPointer, JSONUtils.toJSON(response.body()));
    }

    private void createRecvTransport() {
        final Message response = this.taoyao.request(this.taoyao.buildMessage(
            "media::transport::webrtc::create",
            "forceTcp", false,
            "producing", false,
            "consuming", true,
            "sctpCapabilities", this.dataProduce ? this.sctpCapabilities : null
        ));
        if (response == null) {
            Log.w(Room.class.getSimpleName(), "创建接收通道失败");
            return;
        }
        this.nativeCreateRecvTransport(this.nativeRoomPointer, JSONUtils.toJSON(response.body()));
    }

    public void mediaConsume(Message message, Map<String, Object> body) {
        this.nativeMediaConsume(this.nativeRoomPointer, JSONUtils.toJSON(message));
    }

    /**
     * 新增远程终端
     *
     * @param body 消息主体
     */
    public void newRemoteClient(Map<String, Object> body) {
        synchronized (this.remoteClients) {
            final String clientId = MapUtils.get(body, "clientId");
            final Map<String, Object> status = MapUtils.get(body, "status");
            final String name = MapUtils.get(status, "name");
            final RemoteClient remoteClient = new RemoteClient(name, clientId, this.handler, this.taoyao);
            final RemoteClient old = this.remoteClients.put(clientId, remoteClient);
            if(old != null) {
                // 关闭旧的资源
                old.close();
            }
        }
    }

    public void closeRemoteClient(String clientId) {
        synchronized (this.remoteClients) {
            final RemoteClient remoteClient = this.remoteClients.get(clientId);
            if(remoteClient == null) {
                return;
            }
            remoteClient.close();
        }
    }

    @Override
    public void close() {
        Log.i(Room.class.getSimpleName(), "关闭房间：" + this.roomId);
        this.localClient.close();
        this.remoteClients.values().forEach(v -> this.closeRemoteClient(v.clientId));
        this.mediaManager.closeClient();
        this.nativeCloseRoom(this.nativeRoomPointer);
    }

    public void mediaConsumerClose(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::close",
            "roomId", this.roomId,
            "consumerId", consumerId
        ));
    }

    public void mediaConsumerPause(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::pause",
            "roomId", this.roomId,
            "consumerId", consumerId
        ));
    }

    public void mediaConsumerResume(String consumerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::consumer::resume",
            "roomId", this.roomId,
            "consumerId", consumerId
        ));
    }

    public void mediaProducerClose(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::close",
            "roomId", this.roomId,
            "producerId", producerId
        ));
    }

    public void mediaProducerPause(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::pause",
            "roomId", this.roomId,
            "producerId", producerId
        ));
    }

    public void mediaProducerResume(String producerId) {
        this.taoyao.push(this.taoyao.buildMessage(
            "media::producer::resume",
            "roomId", this.roomId,
            "producerId", producerId
        ));
    }

    @Override
    public void enterCallback(String rtpCapabilities, String sctpCapabilities) {
        this.taoyao.request(this.taoyao.buildMessage(
            "room::enter",
            "roomId", this.roomId,
            "password", this.password,
            "rtpCapabilities", rtpCapabilities,
            "sctpCapabilities", sctpCapabilities
        ));
        this.enter = true;
    }

    @Override
    public void sendTransportConnectCallback(String transportId, String dtlsParameters) {
        this.taoyao.request(this.taoyao.buildMessage(
            "media::transport::webrtc::connect",
            "roomId", this.roomId,
            "transportId", transportId,
            "dtlsParameters", JSONUtils.toMap(dtlsParameters)
        ));
    }

    @Override
    public String sendTransportProduceCallback(String kind, String transportId, String rtpParameters) {
        final Message response = this.taoyao.request(this.taoyao.buildMessage(
            "media::produce",
            "kind", kind,
            "roomId", this.roomId,
            "transportId", transportId,
            "rtpParameters", JSONUtils.toMap(rtpParameters)
        ));
        final Map<String, Object> body = response.body();
        return MapUtils.get(body, "producerId");
    }

    @Override
    public void recvTransportConnectCallback(String transportId, String dtlsParameters) {
        this.taoyao.request(this.taoyao.buildMessage(
            "media::transport::webrtc::connect",
            "roomId", this.roomId,
            "transportId", transportId,
            "dtlsParameters", JSONUtils.toMap(dtlsParameters)
        ));
    }

    @Override
    public void consumerNewCallback(String message, long consumerMediaTrackPointer) {
        final Message response = JSONUtils.toJava(message, Message.class);
        final Map<String, Object> body = response.body();
        final String kind       = MapUtils.get(body, "kind");
        final String clientId   = MapUtils.get(body, "clientId");
        final String consumerId = MapUtils.get(body, "consumerId");
        final RemoteClient remoteClient = this.remoteClients.get(clientId);
        if(remoteClient == null) {
            // TODO：资源释放
            return;
        }
        if(MediaStreamTrack.AUDIO_TRACK_KIND.equals(kind)) {
//          WebRtcAudioTrack
            final AudioTrack audioTrack = new AudioTrack(consumerMediaTrackPointer);
            remoteClient.tracks.put(consumerId, audioTrack);
        } else if(MediaStreamTrack.VIDEO_TRACK_KIND.equals(kind)) {
            final VideoTrack videoTrack = new VideoTrack(consumerMediaTrackPointer);
            remoteClient.tracks.put(consumerId, videoTrack);
            remoteClient.playVideo();
        } else {
            Log.w(Room.class.getSimpleName(), "未知媒体类型：" + kind);
            // TODO：资源释放
            return;
        }
        this.taoyao.push(response);
    }

    private native void nativeEnter(long nativePointer, String rtpCapabilities, long peerConnectionFactoryPointer, PeerConnection.RTCConfiguration rtcConfiguration);
    private native long nativeNewRoom(String roomId, RouterCallback routerCallback);
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
