package com.acgist.taoyao.media;

import java.util.logging.Handler;

/**
 * 房间本地终端
 *
 * @author acgist
 */
public class LocalClient extends RoomClient {

    /**
     * 传输类型
     *
     * @author acgist
     */
    public enum TransportType {

        /**
         * RTP
         */
        RTP,
        /**
         * WebRTC
         */
        WEBRTC;

    }

    public LocalClient(String name, String clientId, Handler handler) {
        super(name, clientId, handler);
    }

    @Override
    public void close() {
        super.close();
    }

}
