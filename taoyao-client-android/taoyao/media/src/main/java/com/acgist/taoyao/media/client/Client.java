package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.MediaStreamTrack;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

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
    /**
     * 视频预览
     */
    protected SurfaceViewRenderer surfaceViewRenderer;

    public Client(String name, String clientId, Handler handler, ITaoyao taoyao) {
        this.name = name;
        this.clientId = clientId;
        this.taoyao = taoyao;
        this.handler = handler;
        this.mediaManager = MediaManager.getInstance();
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

    @Override
    public void close() {
        Log.i(this.getClass().getSimpleName(), "关闭终端：" + this.clientId);
        if(this.surfaceViewRenderer != null) {
            this.surfaceViewRenderer.release();
            this.surfaceViewRenderer = null;
        }
    }

}
