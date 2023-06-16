package com.acgist.taoyao.media.video;

import android.util.Log;

import org.webrtc.VideoFrame;

import java.io.Closeable;

/**
 * 视频处理器
 *
 * @author acgist
 */
public abstract class VideoProcesser implements Closeable {

    /**
     * 处理器名称
     */
    protected final String name;
    /**
     * 下一个视频处理器
     */
    protected VideoProcesser next;

    public VideoProcesser(String name) {
        this.name = name;
        Log.i(WatermarkProcesser.class.getSimpleName(), "加载视频处理器" + name);
    }

    /**
     * 处理视频帧
     *
     * @param i420Buffer 视频帧
     */
    public void process(VideoFrame.I420Buffer i420Buffer) {
        this.doProcess(i420Buffer);
        if(this.next == null) {
            // 忽略
        } else {
            this.next.process(i420Buffer);
        }
    }

    /**
     * 处理视频帧
     *
     * @param i420Buffer 视频帧
     */
    protected abstract void doProcess(VideoFrame.I420Buffer i420Buffer);

    /**
     * 关闭处理器
     */
    public void close() {
        Log.i(VideoProcesser.class.getSimpleName(), "关闭视频处理器：" + this.name);
        if(this.next == null) {
            // 忽略
        } else {
            this.next.close();
        }
    }

}
