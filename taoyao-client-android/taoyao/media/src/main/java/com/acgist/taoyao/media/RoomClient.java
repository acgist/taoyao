package com.acgist.taoyao.media;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.VideoTrack;

import java.util.logging.Handler;

/**
 * 房间终端
 * 使用NDK + Mediasoup实现多人会话
 *
 * @author acgist
 */
public class RoomClient {

    protected final String name;
    protected final String clientId;
    protected final Handler handler;
    protected AudioTrack audioTrack;
    protected VideoTrack videoTrack;
    protected MediaStream mediaStream;

    public RoomClient(String name, String clientId, Handler handler) {
        this.name = name;
        this.clientId = clientId;
        this.handler = handler;
    }

    /**
     * 打开预览
     */
    private void preview() {

    }

}
