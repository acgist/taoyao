package com.acgist.taoyao.media;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Handler;

/**
 * 房间远程终端
 *
 * @author acgist
 */
public class RemoteClient extends RoomClient {

    public RemoteClient(String name, String clientId, Handler handler) {
        super(name, clientId, handler);
    }

    @Override
    public void close() {
        super.close();
    }

}
