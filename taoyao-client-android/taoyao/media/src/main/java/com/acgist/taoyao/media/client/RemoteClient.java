package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.provider.MediaStore;

import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间远程终端
 *
 * @author acgist
 */
public class RemoteClient extends RoomClient {

    /**
     * 媒体流Track
     * 消费者ID = 媒体流Track
     */
    protected final Map<String, MediaStreamTrack> tracks;

    public RemoteClient(String name, String clientId, ITaoyao taoyao, Handler handler) {
        super(name, clientId, taoyao, handler);
        this.tracks = new ConcurrentHashMap<>();
    }

    @Override
    public void playVideo() {
        super.playVideo();
        final VideoTrack videoTrack = (VideoTrack) this.tracks.values().stream()
            .filter(v -> MediaStreamTrack.VIDEO_TRACK_KIND.equals(v.kind()))
            .findFirst()
            .orElse(null);
        if(videoTrack == null) {
            return;
        }
        if(this.surfaceViewRenderer == null) {
            this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_REMOTE_VIDEO, videoTrack);
        } else {
            videoTrack.setEnabled(true);
        }
    }

    @Override
    public void playAudio() {
        super.playAudio();
        this.tracks.values().stream()
            .filter(v -> MediaStreamTrack.AUDIO_TRACK_KIND.equals(v.kind()))
            .forEach(v -> v.setEnabled(true));
    }

    @Override
    public void close() {
        super.close();
        this.tracks.values().forEach(MediaStreamTrack::dispose);
        this.tracks.clear();
    }

}
