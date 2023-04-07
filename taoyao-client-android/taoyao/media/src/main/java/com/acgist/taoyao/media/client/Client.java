package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.signal.ITaoyao;

import java.io.Closeable;

/**
 * 终端
 *
 * @author acgist
 */
public abstract class Client implements Closeable {

    /**
     * 终端名称
     */
    protected final String name;
    /**
     * 终端ID
     */
    protected final String clientId;
    /**
     * Handler
     */
    protected final Handler handler;
    /**
     * 信令通道
     */
    protected final ITaoyao taoyao;
    /**
     * 媒体服务
     */
    protected final MediaManager mediaManager;

    public Client(String name, String clientId, Handler handler, ITaoyao taoyao) {
        this.name = name;
        this.clientId = clientId;
        this.taoyao = taoyao;
        this.handler = handler;
        this.mediaManager = MediaManager.getInstance();
    }

    @Override
    public void close() {
        Log.i(this.getClass().getSimpleName(), "关闭终端：" + this.clientId);
    }

}
