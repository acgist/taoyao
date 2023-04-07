package com.acgist.taoyao.media.client;

import android.os.Handler;

import com.acgist.taoyao.media.signal.ITaoyao;

/**
 * 房间远程终端
 *
 * @author acgist
 */
public class RemoteClient extends RoomClient {

    public RemoteClient(String name, String clientId, Handler handler, ITaoyao taoyao) {
        super(name, clientId, handler, taoyao);
    }

    @Override
    public void close() {
        super.close();
    }

}
