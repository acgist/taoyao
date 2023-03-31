package com.acgist.taoyao.media;

import java.util.logging.Handler;

/**
 * 房间本地终端
 *
 * @author acgist
 */
public class LocalClient extends RoomClient {

    public LocalClient(String name, String clientId, Handler handler) {
        super(name, clientId, handler);
    }

}
