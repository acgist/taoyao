package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.RouterCallback;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private List<RemoteClient> remoteClients;
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
        this.name     = name;
        this.clientId = clientId;
        this.roomId   = roomId;
        this.password = password;
        this.handler  = handler;
        this.taoyao   = taoyao;
        this.dataConsume  = dataConsume;
        this.audioConsume = audioConsume;
        this.videoConsume = videoConsume;
        this.dataProduce  = dataProduce;
        this.audioProduce = audioProduce;
        this.videoProduce = videoProduce;
        this.nativeRoomPointer = this.nativeNewRoom(roomId);
        this.mediaManager = MediaManager.getInstance();
        this.remoteClients = new CopyOnWriteArrayList<>();
        this.enter = false;
    }

    /**
     * 远程终端列表
     */
    private List<RemoteClient> remoteClientList;

    public synchronized void enter() {
        if(this.enter) {
            return;
        }
        final Message response = this.taoyao.request(this.taoyao.buildMessage("media::router::rtp::capabilities", "roomId", this.roomId));
        if(response == null) {
            Log.w(Room.class.getSimpleName(), "获取通道能力失败");
            return;
        }
        this.localClient = new LocalClient(this.name, this.clientId, this.handler, this.taoyao);
        // STUN | TURN
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        // TODO：读取配置
        final PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);
        this.rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
        this.peerConnectionFactory = this.mediaManager.newClient(MediaManager.Type.BACK);
        final Object rtpCapabilities = MapUtils.get(response.body(), "rtpCapabilities");
        this.nativeLoad(this.nativeRoomPointer, JSONUtils.toJSON(rtpCapabilities), this.peerConnectionFactory.getNativePeerConnectionFactory(), this.rtcConfiguration);
    }

    public void produceMedia() {
        if(this.audioProduce || this.videoProduce) {
            this.createSendTransport();
        }
        if(this.audioConsume || this.videoConsume) {
            this.createRecvTransport();
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
        if(response == null) {
            Log.w(Room.class.getSimpleName(), "创建发送通道失败");
            return;
        }
        final Map<String, Object> body = response.body();
        this.nativeCreateSendTransport(this.nativeRoomPointer, JSONUtils.toJSON(body));
    }

    private void createRecvTransport() {
    }
    
    /**
     * 新增远程终端
     *
     * @param body 消息主体
     */
    public void newRemoteClient(Map<String, Object> body) {
        final String clientId = MapUtils.get(body, "clientId");
        final Map<String, Object> status = MapUtils.get(body, "status");
        final String name = MapUtils.get(status, "name");
        final RemoteClient remoteClient = new RemoteClient(name, clientId, this.handler, this.taoyao);
        this.remoteClients.add(remoteClient);
    }

    @Override
    public void close() {
        Log.i(Room.class.getSimpleName(), "关闭房间：" + this.roomId);
        this.localClient.close();
        this.remoteClientList.forEach(RemoteClient::close);
        this.mediaManager.closeClient();
        this.nativeCloseRoom(this.nativeRoomPointer);
    }

    public void enterCallback(String rtpCapabilities, String sctpCapabilities) {
        this.taoyao.request(this.taoyao.buildMessage(
            "room::enter",
            "roomId",           this.roomId,
            "password",         this.password,
            "rtpCapabilities",  rtpCapabilities,
            "sctpCapabilities", sctpCapabilities
        ));
        this.enter = true;
    }

    private native void nativeLoad(
        long nativePointer,
        String rtpCapabilities,
        long peerConnectionFactoryPointer,
        PeerConnection.RTCConfiguration rtcConfiguration
    );
    private native long nativeNewRoom(String roomId);
    private native void nativeCloseRoom(long nativePointer);
    private native void nativeCreateSendTransport(long nativeRoomPointer, String body);
    private native void nativeCreateRecvTransport(long nativeRoomPointer, String body);
    private native void nativeProduceMedia(long nativeRoomPointer, long mediaStreamPointer);

}
