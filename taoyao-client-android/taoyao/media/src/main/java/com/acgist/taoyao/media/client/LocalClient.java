package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间本地终端
 *
 * @author acgist
 */
public class LocalClient extends RoomClient {

    /**
     * 媒体流
     */
    protected MediaStream mediaStream;
    /**
     * 媒体流Track
     * 生产者ID = 媒体流Track指针
     */
    protected final Map<String, Long> tracks;
    /**
     * 音频媒体生产者指针
     */
    protected long audioProducerPointer;
    /**
     * 视频媒体生产者指针
     */
    protected long videoProducerPointer;

    /**
     * @param name        终端名称
     * @param clientId    终端ID
     * @param taoyao      信令
     * @param mainHandler MainHandler
     */
    public LocalClient(String name, String clientId, ITaoyao taoyao, Handler mainHandler) {
        super(name, clientId, taoyao, mainHandler);
        this.tracks = new ConcurrentHashMap<>();
    }

    /**
     * @return 媒体流
     */
    public MediaStream getMediaStream() {
        return this.mediaStream;
    }

    /**
     * @param mediaStream 媒体流
     */
    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    @Override
    public void playAudio() {
        super.playAudio();
        if(this.mediaStream == null) {
            return;
        }
        this.mediaStream.audioTracks.forEach(audioTrack -> {
            audioTrack.setVolume(Config.DEFAULT_VOLUME);
            audioTrack.setEnabled(true);
        });
    }

    @Override
    public void pauseAudio() {
        super.pauseAudio();
        if(this.mediaStream == null) {
            return;
        }
        this.mediaStream.audioTracks.forEach(audioTrack -> {
            audioTrack.setEnabled(false);
        });
    }

    @Override
    public void resumeAudio() {
        super.resumeAudio();
        if(this.mediaStream == null) {
            return;
        }
        this.mediaStream.audioTracks.forEach(audioTrack -> {
            audioTrack.setEnabled(true);
        });
    }

    @Override
    public void playVideo() {
        super.playVideo();
        if(this.mediaStream == null) {
            return;
        }
        this.mediaStream.videoTracks.forEach(videoTrack -> {
            videoTrack.setEnabled(true);
            if(this.surfaceViewRenderer == null) {
                this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_LOCAL_VIDEO, videoTrack);
            }
        });
    }

    @Override
    public void pauseVideo() {
        super.pauseVideo();
        if(this.mediaStream == null) {
            return;
        }
        this.mediaStream.videoTracks.forEach(videoTrack -> {
            videoTrack.setEnabled(false);
        });
    }

    @Override
    public void resumeVideo() {
        super.resumeVideo();
        if(this.mediaStream == null) {
            return;
        }
        this.mediaStream.videoTracks.forEach(videoTrack -> {
            videoTrack.setEnabled(true);
        });
    }

    @Override
    public void close() {
        synchronized (this) {
            if(this.close) {
                return;
            }
            super.close();
            Log.i(RemoteClient.class.getSimpleName(), "关闭本地终端：" + this.clientId);
            if(this.mediaStream == null) {
                return;
            }
            synchronized (this.mediaStream) {
                this.mediaStream.dispose();
            }
        }
    }

    /**
     * 关闭生产者
     *
     * @param producerId 生产者ID
     */
    public void close(String producerId) {
        if(this.mediaStream == null) {
            return;
        }
        Log.i(RemoteClient.class.getSimpleName(), "关闭本地终端生产者：" + this.clientId + " - " + producerId);
        synchronized (this.mediaStream) {
            final Long pointer = this.tracks.get(producerId);
            // TODO：测试remove方法
//          final Long pointer = this.tracks.remove(producerId);
            if(pointer == null) {
                return;
            }
            if(pointer.equals(this.audioProducerPointer)) {
                this.mediaStream.audioTracks.forEach(MediaStreamTrack::dispose);
                this.mediaStream.audioTracks.clear();
            } else if(pointer.equals(this.videoProducerPointer)) {
                this.mediaStream.videoTracks.forEach(MediaStreamTrack::dispose);
                this.mediaStream.videoTracks.clear();
            } else {
            }
        }
    }

}
