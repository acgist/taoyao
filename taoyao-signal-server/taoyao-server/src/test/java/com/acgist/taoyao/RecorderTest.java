package com.acgist.taoyao;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.signal.party.media.Recorder;

public class RecorderTest {

    @Test
    public void testStart() throws InterruptedException {
        final FfmpegProperties ffmpegProperties = new FfmpegProperties();
        ffmpegProperties.setStorageVideoPath("D:\\tmp\\video");
        ffmpegProperties.setSdp("""
        v=0
        o=- 0 0 IN IP4 127.0.0.1
        s=TaoyaoRecord
        t=0 0
        m=audio %d RTP/AVP 97
        c=IN IP4 127.0.0.1
        a=rtpmap:97 opus/48000/2
        a=fmtp:97 sprop-stereo=1
        m=video %d RTP/AVP 96
        c=IN IP4 127.0.0.1
        a=rtpmap:96 H264/90000
        a=fmtp:96 packetization-mode=1
        """);
        ffmpegProperties.setRecord("ffmpeg -y -protocol_whitelist \"file,rtp,udp\" -i %s %s");
        final Recorder recorder = new Recorder(ffmpegProperties);
        recorder.start();
        Thread.sleep(20 * 1000);
        recorder.stop();
        Thread.sleep(20 * 1000);
    }
    
}
