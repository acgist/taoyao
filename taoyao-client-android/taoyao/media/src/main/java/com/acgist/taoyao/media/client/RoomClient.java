package com.acgist.taoyao.media.client;

import android.os.Handler;

import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.MediaStream;

/**
 * 房间终端
 * 使用SDK + NDK + Mediasoup实现多人会话
 *
 * @author acgist
 */
public class RoomClient extends Client {

    protected MediaStream mediaStream;

    public RoomClient(String name, String clientId, Handler handler, ITaoyao taoyao) {
        super(name, clientId, handler, taoyao);
    }

    /**
     * 打开预览
     */
    private void preview() {

    }

    @Override
    public void close() {
        super.close();
    }

    public MediaStream getMediaStream() {
        return mediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

}
