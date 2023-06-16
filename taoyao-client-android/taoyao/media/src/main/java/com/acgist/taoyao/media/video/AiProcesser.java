package com.acgist.taoyao.media.video;

import org.webrtc.VideoFrame;

/**
 * AI识别处理器
 *
 * 建议不要每帧识别，如果没有识别出来结果可以复用识别结果。
 *
 * @author acgist
 */
public class AiProcesser extends VideoProcesser {

    public AiProcesser() {
        super("AI识别处理器");
    }

    @Override
    protected void doProcess(VideoFrame.I420Buffer i420Buffer) {
    }

}
