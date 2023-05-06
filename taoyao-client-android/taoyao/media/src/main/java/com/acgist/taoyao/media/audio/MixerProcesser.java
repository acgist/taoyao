package com.acgist.taoyao.media.audio;

import android.util.Log;

import com.acgist.taoyao.media.client.RecordClient;

import org.webrtc.AudioSource;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 混音处理器
 *
 * JavaAudioDeviceModule              : 音频
 * WebRtcAudioTrack#AudioTrackThread  ：远程音频
 * WebRtcAudioRecord#AudioRecordThread：本地音频
 *
 * 注意：只能远程终端拉取才能采集音频数据，如果需要离线采集自己使用AudioRecord实现。
 *
 * @author acgist
 */
public class MixerProcesser extends Thread implements JavaAudioDeviceModule.SamplesReadyCallback {

    private boolean close;
    private final RecordClient recordClient;
    private final BlockingQueue<JavaAudioDeviceModule.AudioSamples> local;
    private final BlockingQueue<JavaAudioDeviceModule.AudioSamples> remote;

    public MixerProcesser(RecordClient recordClient) {
        this.setDaemon(true);
        this.setName("AudioMixer");
        this.close = false;
        this.recordClient = recordClient;
        this.local  = new LinkedBlockingQueue<>(1024);
        this.remote = new LinkedBlockingQueue<>(1024);
    }

    @Override
    public void onWebRtcAudioTrackSamplesReady(JavaAudioDeviceModule.AudioSamples samples) {
//        Log.d(MixerProcesser.class.getSimpleName(), "远程音频信息：" + samples.getAudioFormat());
        if(!this.remote.offer(samples)) {
            Log.e(MixerProcesser.class.getSimpleName(), "远程音频队列阻塞");
        }
    }

    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples samples) {
//        Log.d(MixerProcesser.class.getSimpleName(), "本地音频信息：" + samples.getAudioFormat());
        if(!this.local.offer(samples)) {
            Log.e(MixerProcesser.class.getSimpleName(), "本地音频队列阻塞");
        }
    }

    @Override
    public void run() {
        long pts = System.nanoTime();
//        final byte[] target = new byte[length];
        // PCM时间计算：1000000 microseconds / 48000 hz / 2 bytes
        JavaAudioDeviceModule.AudioSamples local;
        JavaAudioDeviceModule.AudioSamples remote;
        int localValue;
        int remoteValue;
        byte[] localData;
        byte[] remoteData;
        byte[] data = null;
        // TODO：固定长度采样率等等
        while(!this.close) {
            try {
                local  = this.local.poll(100, TimeUnit.MILLISECONDS);
                remote = this.remote.poll();
                if(local != null && remote != null) {
                    localData  = local.getData();
                    remoteData = remote.getData();
                    Log.d(MixerProcesser.class.getSimpleName(), String.format("""
                        混音长度：%d - %d
                        混音采样：%d - %d
                        混音格式：%d - %d
                        """, localData.length, remoteData.length, local.getSampleRate(), remote.getSampleRate(), local.getAudioFormat(), remote.getAudioFormat()));
                    data = new byte[localData.length];
                    for (int index = 0; index < localData.length; index++) {
                        localValue  = localData[index];
                        remoteValue = remoteData[index];
                        data[index] = (byte) ((localValue +remoteValue) / 2);
                    }
                    pts += data.length * (1_000_000 / local.getSampleRate() / 2);
                } else if(local != null && remote == null) {
                    data = local.getData();
                    pts += data.length * (1_000_000 / local.getSampleRate() / 2);
                } else if(local == null && remote != null) {
                    data = remote.getData();
                    pts += data.length * (1_000_000 / remote.getSampleRate() / 2);
                } else {
                    continue;
                }
                this.recordClient.onPcm(pts, data);
            } catch (Exception e) {
                Log.e(MixerProcesser.class.getSimpleName(), "音频处理异常", e);
            }
        }
    }

    public void close() {
        this.close = true;
    }

}
