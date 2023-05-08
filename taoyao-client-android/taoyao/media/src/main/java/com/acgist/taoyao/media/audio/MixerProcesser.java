package com.acgist.taoyao.media.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.acgist.taoyao.media.client.RecordClient;

import org.webrtc.audio.JavaAudioDeviceModule;

import java.nio.ByteBuffer;
import java.util.Arrays;
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
 * AudioFormat.ENCODING_PCM_16BIT = 2KB
 *
 * PCM时间计算：1_000_000 microseconds / 48000 hz / 2 bytes
 *
 * @author acgist
 */
public class MixerProcesser extends Thread implements JavaAudioDeviceModule.SamplesReadyCallback {

    /**
     * 音频数据来源
     * 其实不用切换可以两个同时录制，但是有点浪费资源。
     *
     * @author acgist
     */
    public enum Source {
        // 本地
        NATIVE,
        // WebRTC
        WEBRTC;
    }

    private boolean close;
    private Source source;
    private final int sampleRate;
    private final int audioFormat;
    private final int audioSource;
    private final int channelCount;
    private final int channelConfig;
    private final AudioRecord audioRecord;
    private final RecordClient recordClient;
    private final BlockingQueue<JavaAudioDeviceModule.AudioSamples> local;
    private final BlockingQueue<JavaAudioDeviceModule.AudioSamples> remote;

    @SuppressLint("MissingPermission")
    public MixerProcesser(int sampleRate, int channelCount, RecordClient recordClient) {
        this.setDaemon(true);
        this.setName("AudioMixer");
        this.close         = false;
        this.source        = Source.WEBRTC;
        this.sampleRate    = sampleRate;
        this.audioFormat   = AudioFormat.ENCODING_PCM_16BIT;
        this.audioSource   = MediaRecorder.AudioSource.MIC;
        this.channelCount  = channelCount;
        this.channelConfig = AudioFormat.CHANNEL_IN_MONO;
        this.audioRecord   = new AudioRecord.Builder()
            .setAudioFormat(
                new AudioFormat.Builder()
                    .setEncoding(this.audioFormat)
                    .setSampleRate(this.sampleRate)
                    .setChannelMask(this.channelConfig)
                    .build()
            )
            .setAudioSource(this.audioSource)
            .setBufferSizeInBytes(AudioRecord.getMinBufferSize(this.sampleRate, this.channelConfig, this.audioFormat))
            .build();
        this.recordClient = recordClient;
        this.local  = new LinkedBlockingQueue<>(1024);
        this.remote = new LinkedBlockingQueue<>(1024);
    }

    @Override
    public void onWebRtcAudioTrackSamplesReady(JavaAudioDeviceModule.AudioSamples samples) {
//      Log.d(MixerProcesser.class.getSimpleName(), "远程音频信息：" + samples.getAudioFormat());
        if(!this.remote.offer(samples)) {
            Log.e(MixerProcesser.class.getSimpleName(), "远程音频队列阻塞");
        }
    }

    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples samples) {
//      Log.d(MixerProcesser.class.getSimpleName(), "本地音频信息：" + samples.getAudioFormat());
        if(!this.local.offer(samples)) {
            Log.e(MixerProcesser.class.getSimpleName(), "本地音频队列阻塞");
        }
    }

    @Override
    public void run() {
        long pts = System.nanoTime();
        byte[] mixData    = null;
        byte[] localData  = null;
        byte[] remoteData = null;
        byte[] recordData = null;
        int mixDataLength = 0;
        JavaAudioDeviceModule.AudioSamples local  = null;
        JavaAudioDeviceModule.AudioSamples remote = null;
        int recordSize    = 0;
        // 采集数据大小：采样频率 / (一秒 / 回调频率) * 通道数量 * 采样数据大小
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(this.sampleRate / (1000 / 10) * this.channelCount * 2);
        while(!this.close) {
            try {
                if(this.source == Source.NATIVE) {
                    recordSize = this.audioRecord.read(byteBuffer, byteBuffer.capacity());
                    if(recordSize != byteBuffer.capacity()) {
                        Thread.yield();
                        continue;
                    }
                    recordData = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.capacity() + byteBuffer.arrayOffset());
                    pts += recordData.length * (1_000_000 / this.sampleRate / 2);
                    this.recordClient.onPcm(pts, recordData);
                } else if(this.source == Source.WEBRTC) {
                    // 平均10毫秒
                    local  = this.local.poll(64, TimeUnit.MILLISECONDS);
                    remote = this.remote.poll();
                    if(local != null && remote != null) {
//                      Log.d(MixerProcesser.class.getSimpleName(), String.format("""
//                          混音长度：%d - %d
//                          混音采样：%d - %d
//                          混音格式：%d - %d
//                          混音数量：%d - %d""",
//                          local.getData().length,  remote.getData().length,
//                          local.getSampleRate(),   remote.getSampleRate(),
//                          local.getAudioFormat(),  remote.getAudioFormat(),
//                          local.getChannelCount(), remote.getChannelCount()
//                      ));
                        localData  = local.getData();
                        remoteData = remote.getData();
                        if(mixDataLength != localData.length) {
//                      if(mixDataLength != localData.length && mixDataLength != remoteData.length) {
                            mixDataLength = localData.length;
                            mixData = new byte[mixDataLength];
                        }
                        // 如果多路远程声音变小：(remote * 远程路数 + local) / (远程路数 + 1)
                        for (int index = 0; index < mixDataLength; index++) {
//                          -0x8000 ~ 0x7FFF;
                            mixData[index] = (byte) (((localData[index] + remoteData[index]) & 0x7FFF) / 2);
//                          mixData[index] = (byte) (((localData[index] + remoteData[index]) & 0xFFFF) / 2);
//                          mixData[index] = (byte) (((localData[index] + remoteData[index] * remoteCount) & 0xFFFF) / (1 + remoteCount));
                        }
                        pts += mixData.length * (1_000_000 / local.getSampleRate() / 2);
                        this.recordClient.onPcm(pts, mixData);
                    } else if(local != null && remote == null) {
                        localData = local.getData();
                        pts += localData.length * (1_000_000 / local.getSampleRate() / 2);
                        this.recordClient.onPcm(pts, localData);
                    } else if(local == null && remote != null) {
                        remoteData = remote.getData();
                        pts += remoteData.length * (1_000_000 / remote.getSampleRate() / 2);
                        this.recordClient.onPcm(pts, remoteData);
                    } else {
                        Thread.yield();
                        continue;
                    }
                } else {
                    Thread.yield();
                }
            } catch (Exception e) {
                Log.e(MixerProcesser.class.getSimpleName(), "音频处理异常", e);
            }
        }
        if(this.audioRecord != null) {
            this.audioRecord.stop();
            this.audioRecord.release();
        }
    }

    @Override
    public void startNative() {
        synchronized (this) {
            if(this.source == Source.NATIVE) {
                return;
            }
            this.audioRecord.startRecording();
            this.source = Source.NATIVE;
            Log.i(MixerProcesser.class.getSimpleName(), "混音切换来源：" + this.source);
        }
    }

    @Override
    public void startWebRTC() {
        synchronized (this) {
            if(this.source == Source.WEBRTC) {
                return;
            }
            this.audioRecord.stop();
            this.source = Source.WEBRTC;
            Log.i(MixerProcesser.class.getSimpleName(), "混音切换来源：" + this.source);
        }
    }

    public void close() {
        synchronized (this) {
            this.close = true;
        }
    }

}