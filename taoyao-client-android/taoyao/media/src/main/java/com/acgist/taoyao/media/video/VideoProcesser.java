package com.acgist.taoyao.media.video;

import org.webrtc.VideoFrame;

import java.io.Closeable;

/**
 * 视频处理器
 */
public abstract class VideoProcesser implements Closeable {

    protected VideoProcesser next;

    public void process(VideoFrame.I420Buffer i420Buffer) {
        this.doProcess(i420Buffer);
        if(this.next == null) {
            // 忽略
        } else {
            this.next.process(i420Buffer);
        }
    }

    protected abstract void doProcess(VideoFrame.I420Buffer i420Buffer);

    public void close() {
        if(this.next == null) {
            // 忽略
        } else {
            this.next.close();
        }
    }

}
