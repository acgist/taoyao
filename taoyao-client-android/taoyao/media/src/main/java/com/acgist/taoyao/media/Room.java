package com.acgist.taoyao.media;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * 房间
 *
 * @author acgist
 */
public class Room implements Closeable {

    private final String id;

    public Room(String id) {
        this.id = id;
    }

    /**
     * 远程终端列表
     */
    private List<RemoteClient> remoteClientList;

    @Override
    public void close() {
        Log.i(Room.class.getSimpleName(), "关闭房间：" + this.id);
        this.remoteClientList.forEach(RemoteClient::close);
    }

}
