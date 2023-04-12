package com.acgist.taoyao.media.client;

import android.os.Handler;

import com.acgist.taoyao.boot.utils.ListUtils;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.VideoTrack;

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

    public LocalClient(String name, String clientId, ITaoyao taoyao, Handler handler) {
        super(name, clientId, taoyao, handler);
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
            if(this.surfaceViewRenderer == null) {
                this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_LOCAL_VIDEO, videoTrack);
            } else {
                videoTrack.setEnabled(true);
            }
            return videoTrack;
        });
    }

    @Override
    public void close() {
        super.close();
        this.tracks.clear();
    }

}
