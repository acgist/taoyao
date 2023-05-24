package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间远程终端
 *
 * 注意：这里媒体MediaStreamTrack使用Mediasoup方法释放不要直接调用MediaStreamTrack.dispose()释放
 *
 * @author acgist
 */
public class RemoteClient extends RoomClient {

    /**
     * 媒体流Track
     * 消费者ID = 媒体流Track
     *
     * 注意：track由mediasoup的consumer释放
     */
    protected final Map<String, MediaStreamTrack> tracks;
    /**
     * 音频消费者指针
     */
    protected long audioConsumerPointer;
    /**
     * 视频消费指针
     */
    protected long videoConsumerPointer;

    /**
     * @param name        终端名称
     * @param clientId    终端ID
     * @param taoyao      信令
     * @param mainHandler MainHandler
     */
    public RemoteClient(String name, String clientId, ITaoyao taoyao, Handler mainHandler) {
        super(name, clientId, taoyao, mainHandler);
        this.tracks = new ConcurrentHashMap<>();
    }

    @Override
    public void playAudio() {
        super.playAudio();
        this.tracks.values().stream()
        .filter(v -> MediaStreamTrack.AUDIO_TRACK_KIND.equals(v.kind()))
        .map(v -> (AudioTrack) v)
        .forEach(audioTrack -> {
            audioTrack.setVolume(Config.DEFAULT_VOLUME);
            audioTrack.setEnabled(true);
        });
    }

    @Override
    public void pauseAudio() {
        super.pauseAudio();
        this.tracks.values().stream()
        .filter(v -> MediaStreamTrack.AUDIO_TRACK_KIND.equals(v.kind()))
        .forEach(audioTrack -> {
            audioTrack.setEnabled(false);
        });
    }

    @Override
    public void resumeAudio() {
        super.resumeAudio();
        this.tracks.values().stream()
        .filter(v -> MediaStreamTrack.AUDIO_TRACK_KIND.equals(v.kind()))
        .forEach(audioTrack -> {
            audioTrack.setEnabled(true);
        });
    }

    @Override
    public void playVideo() {
        super.playVideo();
        this.tracks.values().stream()
        .filter(v -> MediaStreamTrack.VIDEO_TRACK_KIND.equals(v.kind()))
        .map(v -> (VideoTrack) v)
        .forEach(videoTrack -> {
            videoTrack.setEnabled(true);
            if(this.surfaceViewRenderer == null) {
                this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_REMOTE_VIDEO, videoTrack);
            }
        });
    }

    @Override
    public void pauseVideo() {
        super.pauseVideo();
        this.tracks.values().stream()
        .filter(v -> MediaStreamTrack.VIDEO_TRACK_KIND.equals(v.kind()))
        .forEach(audioTrack -> {
            audioTrack.setEnabled(false);
        });
    }

    @Override
    public void resumeVideo() {
        super.resumeVideo();
        this.tracks.values().stream()
        .filter(v -> MediaStreamTrack.VIDEO_TRACK_KIND.equals(v.kind()))
        .forEach(audioTrack -> {
            audioTrack.setEnabled(true);
        });
    }

    @Override
    public void close() {
        synchronized (this) {
            if(this.close) {
                return;
            }
            super.close();
            Log.i(RemoteClient.class.getSimpleName(), "关闭远程终端：" + this.clientId);
            synchronized (this.tracks) {
                // 注意：使用nativeMediaConsumerClose释放资源
                this.tracks.clear();
            }
        }
    }

    /**
     * 关闭消费者
     *
     * @param consumerId 消费者ID
     *
     * @return MediaStreamTrack
     */
    public MediaStreamTrack close(String consumerId) {
        Log.i(RemoteClient.class.getSimpleName(), "关闭远程终端消费者：" + this.clientId + " - " + consumerId);
        synchronized (this.tracks) {
            // 注意：使用nativeMediaConsumerClose释放资源
            return this.tracks.remove(consumerId);
        }
    }

}
