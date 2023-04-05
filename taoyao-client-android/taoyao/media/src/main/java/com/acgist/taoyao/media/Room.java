package com.acgist.taoyao.media;

import android.provider.MediaStore;
import android.util.Log;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.ITaoyao;

import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * 房间
 *
 * @author acgist
 */
public class Room implements Closeable {

    private final String roomId;
    private final String password;
    private final long nativePointer;
    private final ITaoyao taoyao;
    private final MediaManager mediaManager;
    private volatile boolean enter;
    private PeerConnection.RTCConfiguration rtcConfiguration;
    private PeerConnectionFactory peerConnectionFactory;

    public Room(
        String roomId, String password,
        boolean audioConsume, boolean videoConsume,
        boolean audioProduce, boolean videoProduce,
        long nativePointer, ITaoyao taoyao
    ) {
        this.roomId = roomId;
        this.password = password;
        this.nativePointer = nativePointer;
        this.taoyao = taoyao;
        this.mediaManager = MediaManager.getInstance();
        this.enter = false;
    }

    /**
     * 远程终端列表
     */
    private List<RemoteClient> remoteClientList;

    @Override
    public void close() {
        Log.i(Room.class.getSimpleName(), "关闭房间：" + this.roomId);
        this.mediaManager.closeClient();
        this.remoteClientList.forEach(RemoteClient::close);
    }

    public synchronized void enter() {
        if(this.enter) {
            return;
        }
        final Message response = this.taoyao.request(this.taoyao.buildMessage("media::router::rtp::capabilities", "roomId", this.roomId));
        if(response == null) {
            return;
        }
        // STUN | TURN
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        // TODO：读取配置
        final PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);
        this.rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);

        this.rtcConfiguration.screencastMinBitrate = 100;
        this.rtcConfiguration.enableDtlsSrtp = true;

        this.peerConnectionFactory = this.mediaManager.newClient(MediaManager.Type.BACK);
        final Object rtpCapabilities = MapUtils.get(response.body(), "rtpCapabilities");
        this.nativeLoad(this.nativePointer, JSONUtils.toJSON(rtpCapabilities), this.peerConnectionFactory.getNativePeerConnectionFactory(), this.rtcConfiguration);
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

    public void produceMedia() {
    }

    private native void nativeLoad(long nativePointer, String rtpCapabilities, long peerConnectionFactory, PeerConnection.RTCConfiguration rtcConfiguration);
    private native void nativeNewClient();
    private native void nativeCloseClient();
    private native void nativeCloseRoom(long nativePointer);

}
