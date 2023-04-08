package com.acgist.taoyao.media.client;

import android.os.Handler;

import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.MediaStreamTrack;

/**
 * 房间终端
 * 使用SDK + NDK + Mediasoup实现多人会话
 *
 * @author acgist
 */
public class RoomClient extends Client {

    public RoomClient(String name, String clientId, Handler handler, ITaoyao taoyao) {
        super(name, clientId, handler, taoyao);
    }

    @Override
    public void close() {
        super.close();
    }

}
