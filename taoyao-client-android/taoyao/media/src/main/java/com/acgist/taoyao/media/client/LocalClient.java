package com.acgist.taoyao.media.client;

import android.os.Handler;

import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.MediaStream;
import org.webrtc.VideoTrack;

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

    public LocalClient(String name, String clientId, Handler handler, ITaoyao taoyao) {
        super(name, clientId, handler, taoyao);
    }

    public MediaStream getMediaStream() {
        return this.mediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    @Override
    public void playVideo() {
        super.playVideo();
        if(this.mediaStream == null) {
            return;
        }
        final VideoTrack videoTrack = this.mediaStream.videoTracks.get(0);
        if(this.surfaceViewRenderer == null) {
            this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_LOCAL_VIDEO, videoTrack);
        } else {
            videoTrack.setEnabled(true);
        }
    }

    @Override
    public void playAudio() {
        super.playAudio();
        if(this.mediaStream == null) {
            return;
        }
        this.mediaStream.audioTracks.forEach(v -> v.setEnabled(true));
    }

    @Override
    public void close() {
        super.close();
        if(this.mediaStream != null) {
            this.mediaStream.dispose();
        }
    }

}
