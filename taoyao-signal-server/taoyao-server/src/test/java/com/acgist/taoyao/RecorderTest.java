package com.acgist.taoyao;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.signal.party.room.Recorder;

public class RecorderTest {

    @Test
    public void testStart() throws InterruptedException {
        final FfmpegProperties ffmpegProperties = new FfmpegProperties();
        ffmpegProperties.setHost("127.0.0.1");
        ffmpegProperties.setMinPort(50000);
        ffmpegProperties.setMaxPort(59999);
        ffmpegProperties.setRecordSdp("""
        v=0
        o=- 0 0 IN IP4 127.0.0.1
        s=TaoyaoRecord
        t=0 0
        m=audio %d RTP/AVP 97
        c=IN IP4 0.0.0.0
        a=rtcp:%d
        a=rtpmap:97 OPUS/48000/2
        a=recvonly
        m=video %d RTP/AVP 96
        c=IN IP4 0.0.0.0
        a=rtcp:%d
        a=rtpmap:96 VP8/90000
        a=recvonly
        """);
//      ffmpeg -re -i video.mp4 -c:a libopus -vn -f rtp rtp://192.168.1.100:50000 -c:v vp8 -an -f rtp rtp://192.168.1.100:50002 -sdp_file taoyao.sdp
        ffmpegProperties.setRecord("ffmpeg -y -protocol_whitelist \"file,rtp,udp\" -thread_queue_size 1024 -c:a libopus -c:v libvpx -r:v %d -i %s -c:a aac -c:v h264 %s");
        ffmpegProperties.setPreview("ffmpeg -y -i %s -ss %d -vframes 1 -f image2 %s");
        ffmpegProperties.setDuration("ffprobe -i %s -show_entries format=duration");
        ffmpegProperties.setStorageVideoPath("D:\\tmp\\video");
        final Recorder recorder = new Recorder("taoyao", null, null, ffmpegProperties);
        recorder.start();
        Thread.sleep(20 * 1000);
        recorder.stop();
        Thread.sleep(20 * 1000);
    }
    
}
