package com.acgist.taoyao.media.client;

import android.os.Handler;

import com.acgist.taoyao.media.signal.ITaoyao;

/**
 * 房间终端
 * 使用SDK + NDK + Mediasoup实现多人会话
 *
 * @author acgist
 */
public class RoomClient extends Client {

    public RoomClient(String name, String clientId, ITaoyao taoyao, Handler mainHandler) {
        super(name, clientId, taoyao, mainHandler);
    }

    @Override
    public void close() {
        super.close();
    }

}
