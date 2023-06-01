package com.acgist.taoyao;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.signal.party.media.Recorder;

public class RecorderTest {

    @Test
    public void testStart() throws InterruptedException {
        final FfmpegProperties ffmpegProperties = new FfmpegProperties();
        ffmpegProperties.setStorageVideoPath("D:\\tmp\\video");
        ffmpegProperties.setMinPort(50000);
        ffmpegProperties.setMaxPort(59999);
        ffmpegProperties.setHost("127.0.0.1");
        ffmpegProperties.setSdp("""
        v=0
        o=- 0 0 IN IP4 %s
        s=TaoyaoRecord
        t=0 0
        m=audio %d RTP/AVP 97
        c=IN IP4 %s
        a=rtpmap:97 OPUS/48000/2
        a=fmtp:97 sprop-stereo=1
        m=video %d RTP/AVP 96
        c=IN IP4 %s
        a=rtpmap:96 VP8/90000
        a=fmtp:96 packetization-mode=1
        """);
//        ffmpegProperties.setSdp("""
//        v=0
//        o=- 0 0 IN IP4 %s
//        s=TaoyaoRecord
//        t=0 0
//        m=audio %d RTP/AVP 97
//        c=IN IP4 %s
//        a=rtpmap:97 OPUS/48000/2
//        a=fmtp:97 sprop-stereo=1
//        m=video %d RTP/AVP 96
//        c=IN IP4 %s
//        a=rtpmap:96 H264/90000
//        a=fmtp:96 packetization-mode=1
//        """);
        ffmpegProperties.setRecord("ffmpeg -protocol_whitelist \"file,rtp,udp\" -y -i %s %s");
        ffmpegProperties.setPreview("ffmpeg -y -i %s -ss %d -vframes 1 -f image2 %s");
        ffmpegProperties.setDuration("ffprobe -i %s -show_entries format=duration");
        final Recorder recorder = new Recorder("taoyao", null, null, ffmpegProperties);
        recorder.start();
        Thread.sleep(20 * 1000);
        recorder.stop();
        Thread.sleep(20 * 1000);
    }
    
}
