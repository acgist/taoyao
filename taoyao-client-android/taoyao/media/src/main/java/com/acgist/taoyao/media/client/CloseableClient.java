package com.acgist.taoyao.media.client;

import android.os.Handler;

import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.PeerConnectionFactory;

import java.io.Closeable;

/**
 * 需要关闭终端
 *
 * @author acgist
 */
public abstract class CloseableClient implements Closeable {

    /**
     * 是否加载
     * 防止重复加载
     */
    protected volatile boolean init;
    /**
     * 是否关闭
     */
    protected volatile boolean close;
    /**
     * 信令通道
     */
    protected final ITaoyao taoyao;
    /**
     * Handler
     */
    protected final Handler handler;
    /**
     * 媒体服务
     */
    protected final MediaManager mediaManager;

    public CloseableClient(ITaoyao taoyao, Handler handler) {
        this.init = false;
        this.close = false;
        this.taoyao = taoyao;
        this.handler = handler;
        this.mediaManager = MediaManager.getInstance();
    }

    /**
     * 加载
     */
    protected void init() {
        this.init = true;
    }


    @Override
    public void close() {
        this.close = true;
    }
}
