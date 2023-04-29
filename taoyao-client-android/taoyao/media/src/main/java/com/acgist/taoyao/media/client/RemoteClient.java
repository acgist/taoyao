package com.acgist.taoyao.media.client;

import android.os.Handler;
import android.util.Log;

import com.acgist.taoyao.boot.utils.ListUtils;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 房间远程终端
 *
 * @author acgist
 */
public class RemoteClient extends RoomClient {

    /**
     * 媒体流Track
     * 消费者ID = 媒体流Track
     * 注意：track由mediasoup的consumer释放
     */
    protected final Map<String, MediaStreamTrack> tracks;
    protected long audioConsumerPointer;
    protected long videoConsumerPointer;

    public RemoteClient(String name, String clientId, ITaoyao taoyao, Handler mainHandler) {
        super(name, clientId, taoyao, mainHandler);
        this.tracks = new ConcurrentHashMap<>();
    }

    @Override
    public void playAudio() {
        super.playAudio();
        ListUtils.getOnlyOne(
            this.tracks.values().stream().filter(v -> MediaStreamTrack.AUDIO_TRACK_KIND.equals(v.kind())).collect(Collectors.toList()),
            audioTrack -> {
                audioTrack.setEnabled(true);
                return audioTrack;
            }
        );
    }

    @Override
    public void playVideo() {
        super.playVideo();
        ListUtils.getOnlyOne(
            this.tracks.values().stream()
                .filter(v -> MediaStreamTrack.VIDEO_TRACK_KIND.equals(v.kind()))
                .map(v -> (VideoTrack) v)
                .collect(Collectors.toList()),
            videoTrack -> {
                videoTrack.setEnabled(true);
                if(this.surfaceViewRenderer == null) {
                    this.surfaceViewRenderer = this.mediaManager.buildSurfaceViewRenderer(Config.WHAT_NEW_REMOTE_VIDEO, videoTrack);
                }
                return videoTrack;
            }
        );
    }

    @Override
    public void close() {
        Log.i(RemoteClient.class.getSimpleName(), "关闭远程终端：" + this.clientId);
        super.close();
        synchronized (this.tracks) {
            // 注意：使用nativeMediaConsumerClose释放
            this.tracks.clear();
        }
    }

    /**
     * 关闭消费者
     *
     * @param consumerId 消费者ID
     */
    public void close(String consumerId) {
        Log.i(RemoteClient.class.getSimpleName(), "关闭远程终端消费者：" + this.clientId + " - " + consumerId);
        synchronized (this.tracks) {
            // 注意：使用nativeMediaConsumerClose释放
            this.tracks.remove(consumerId);
        }
    }

}
