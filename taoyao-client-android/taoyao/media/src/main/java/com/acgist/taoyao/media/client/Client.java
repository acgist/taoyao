package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.SurfaceViewRenderer;

/**
 * 终端
 *
 * @author acgist
 */
public abstract class Client extends CloseableClient {

    /**
     * 终端名称
     */
    protected final String name;
    /**
     * 终端ID
     */
    protected final String clientId;
    /**
     * 视频预览
     */
    protected SurfaceViewRenderer surfaceViewRenderer;

    public Client(String name, String clientId, ITaoyao taoyao, Handler mainHandler) {
        super(taoyao, mainHandler);
        this.name = name;
        this.clientId = clientId;
    }

    /**
     * 播放音频
     */
    public void playAudio() {
    }

    public void pauseAudio() {
    }

    public void resumeAudio() {
    }

    /**
     * 播放视频
     */
    public void playVideo() {
    }

    public void pauseVideo() {
        if(this.surfaceViewRenderer != null) {
            this.surfaceViewRenderer.pauseVideo();
        }
    }

    public void resumeVideo() {
        if(this.surfaceViewRenderer != null) {
            // TODO：验证是否正确
            this.surfaceViewRenderer.disableFpsReduction();
        }
    }

    public void pause() {
    }

    public void resume() {
    }

    @Override
    public void close() {
        super.close();
        Log.i(this.getClass().getSimpleName(), "关闭终端：" + this.clientId);
        if(this.surfaceViewRenderer != null) {
            // 释放资源
            this.surfaceViewRenderer.release();
            // 移除资源：注意先释放再移除避免报错
            this.mainHandler.obtainMessage(Config.WHAT_REMOVE_VIDEO, this.surfaceViewRenderer).sendToTarget();
            // 设置为空
            this.surfaceViewRenderer = null;
        }
    }

}
