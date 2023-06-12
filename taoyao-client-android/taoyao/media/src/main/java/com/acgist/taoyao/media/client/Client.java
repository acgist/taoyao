package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

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

    /**
     * @param name        终端名称
     * @param clientId    终端ID
     * @param taoyao      信令
     * @param mainHandler MainHandler
     */
    public Client(String name, String clientId, ITaoyao taoyao, Handler mainHandler) {
        super(taoyao, mainHandler);
        this.name     = name;
        this.clientId = clientId;
    }

    /**
     * 播放音频
     */
    public void playAudio() {
        Log.i(Client.class.getSimpleName(), "播放音频：" + this.clientId);
    }

    /**
     * 暂停音频
     */
    public void pauseAudio() {
        Log.i(Client.class.getSimpleName(), "暂停音频：" + this.clientId);
    }

    /**
     * 恢复音频
     */
    public void resumeAudio() {
        Log.i(Client.class.getSimpleName(), "恢复音频：" + this.clientId);
    }

    /**
     * 播放视频
     */
    public void playVideo() {
        Log.i(Client.class.getSimpleName(), "播放视频：" + this.clientId);
    }

    /**
     * 暂停视频
     */
    public void pauseVideo() {
        Log.i(Client.class.getSimpleName(), "暂停视频：" + this.clientId);
    }

    /**
     * 恢复视频
     */
    public void resumeVideo() {
        Log.i(Client.class.getSimpleName(), "恢复视频：" + this.clientId);
    }

    /**
     * 暂停音视频
     */
    public void pause() {
        this.pauseAudio();
        this.pauseVideo();
    }

    /**
     * 恢复音视频
     */
    public void resume() {
        this.resumeAudio();
        this.resumeVideo();
    }

    /**
     * 创建视频预览
     *
     * @param flag       Config.WHAT_*
     * @param videoTrack 视频媒体流Track
     */
    protected void buildSurfaceViewRenderer(final int flag, final VideoTrack videoTrack) {
        if(this.surfaceViewRenderer == null) {
            this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(flag, videoTrack);
        } else {
            Log.w(Client.class.getSimpleName(), "视频预览已经存在");
        }
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
