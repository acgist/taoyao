package com.acgist.taoyao.media;

import android.util.Log;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.VideoTrack;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Handler;

/**
 * 房间终端
 * 使用SDK + NDK + Mediasoup实现多人会话
 *
 * @author acgist
 */
public class RoomClient implements Closeable {

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

    @Override
    public void close() {
        Log.i(Room.class.getSimpleName(), "关闭终端：" + this.clientId);
    }

}
