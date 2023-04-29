package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.utils.ListUtils;
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
    protected long audioProducerPointer;
    protected long videoProducerPointer;

    public LocalClient(String name, String clientId, ITaoyao taoyao, Handler mainHandler) {
        super(name, clientId, taoyao, mainHandler);
        this.tracks = new ConcurrentHashMap<>();
    }

    public MediaStream getMediaStream() {
        return this.mediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    @Override
    public void playAudio() {
        super.playAudio();
        if(this.mediaStream == null) {
            return;
        }
        ListUtils.getOnlyOne(this.mediaStream.audioTracks, audioTrack -> {
            audioTrack.setEnabled(true);
            return audioTrack;
        });
    }

    @Override
    public void playVideo() {
        super.playVideo();
        if(this.mediaStream == null) {
            return;
        }
        ListUtils.getOnlyOne(this.mediaStream.videoTracks, videoTrack -> {
            videoTrack.setEnabled(true);
            if(this.surfaceViewRenderer == null) {
                this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_LOCAL_VIDEO, videoTrack);
            }
            return videoTrack;
        });
    }

    @Override
    public void close() {
        Log.i(RemoteClient.class.getSimpleName(), "关闭本地终端：" + this.clientId);
        super.close();
        if(this.mediaStream == null) {
            return;
        }
        synchronized (this.mediaStream) {
            this.mediaStream.dispose();
        }
    }

    /**
     * 关闭生产者
     *
     * @param producerId 生产者ID
     */
    public void close(String producerId) {
        Log.i(RemoteClient.class.getSimpleName(), "关闭本地终端生产者者：" + this.clientId + " - " + producerId);
        final Long pointer = this.tracks.get(producerId);
        if(pointer == null || this.mediaStream == null) {
            return;
        }
        synchronized (this.mediaStream) {
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
