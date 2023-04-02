package com.acgist.taoyao.media;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.acgist.taoyao.config.Config;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 媒体来源管理器
 *
 * @author acgist
 *
 * https://zhuanlan.zhihu.com/p/82446482
 * https://www.jianshu.com/p/97acd9a51909
 * https://juejin.cn/post/7036308428305727519
 * https://blog.csdn.net/nanoage/article/details/127406494
 * https://webrtc.org.cn/20190419_tutorial3_webrtc_android
 * https://blog.csdn.net/CSDN_Mew/article/details/103406781
 * https://blog.csdn.net/Tong_Hou/article/details/112116349
 * https://blog.csdn.net/u011418943/article/details/127108642
 * https://blog.csdn.net/csdn_shen0221/article/details/120331004
 * https://blog.csdn.net/csdn_shen0221/article/details/119982257
 *
 * TODO：动态码率（BITRATE_MODE_VBR、BITRATE_MODE）
 */
public class MediaManager {

    /**
     * 来源类型
     *
     * @author acgist
     */
    public enum Type {

        // 文件共享
        FILE,
        // 后置摄像头
        BACK,
        // 前置摄像头
        FRONT,
        // 屏幕共享
        SCREEN;

        /**
         * @return 是否摄像头
         */
        public boolean isCamera() {
            return this == BACK || this == FRONT;
        }

    }

    private static final MediaManager INSTANCE = new MediaManager();

    public static final MediaManager getInstance() {
        return INSTANCE;
    }

    private volatile int clientCount;
    /**
     * 视频类型
     */
    private Type type;
    /**
     * Handler
     */
    private Handler handler;
    /**
     * 上下文
     */
    private Context context;
    /**
     * 本地终端
     */
    private LocalClient localClient;
    /**
     *
     */
    private EglBase eglBase;
    /**
     * 媒体流：声音、主码流（预览流）、次码流
     */
    private MediaStream mediaStream;
    /**
     * 视频捕获
     * FileVideoCapturer
     * CameraVideoCapturer
     * ScreenCapturerAndroid
     */
    private VideoCapturer videoCapturer;
    /**
     * Peer连接工厂
     */
    private PeerConnectionFactory peerConnectionFactory;

    static {
        // 设置采样
//      WebRtcAudioUtils.setDefaultSampleRateHz();
        // 噪声消除
//      WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        // 回声消除
//      WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        // 自动增益
//      WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
        // 使用OpenSL ES
//      WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
    }

    private MediaManager() {
        this.clientCount = 0;
    }

    /**
     * 新建终端
     *
     * @param type 视频类型：第一个终端进入有效
     *
     * @return PeerConnectionFactory
     */
    public PeerConnectionFactory newClient(Type type) {
        synchronized (this) {
            if(this.clientCount <= 0) {
                this.initMedia(type);
            }
            this.clientCount++;
        }
        return this.peerConnectionFactory;
    }

    /**
     * 关闭一个终端
     *
     * @return 剩余终端数量
     */
    public int closeClient() {
        synchronized (this) {
            this.clientCount--;
            if(this.clientCount <= 0) {
                this.close();
            }
            return this.clientCount;
        }
    }

    public void initMedia(Handler handler, Context context) {
        this.handler = handler;
        this.context = context;
    }

    /**
     * 加载媒体流
     */
    public void initMedia(Type type) {
        Log.i(MediaManager.class.getSimpleName(), "加载媒体：" + type);
        this.type = type;
        this.eglBase = EglBase.create();
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this.context)
//          .setEnableInternalTracer(true)
            .createInitializationOptions()
        );
        final VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(this.eglBase.getEglBaseContext());
        final VideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(this.eglBase.getEglBaseContext(), true, true);
        final JavaAudioDeviceModule javaAudioDeviceModule = JavaAudioDeviceModule.builder(this.context)
//          .setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            // 本地音频
            .setSamplesReadyCallback(MediaRecorder.getInstance().audioRecoder)
            // 远程音频
//          .setAudioTrackStateCallback()
//          .setUseHardwareNoiseSuppressor(true)
//          .setUseHardwareAcousticEchoCanceler(true)
            .createAudioDeviceModule();
        this.peerConnectionFactory = PeerConnectionFactory.builder()
//          .setAudioProcessingFactory()
//          .setAudioEncoderFactoryFactory(new BuiltinAudioEncoderFactoryFactory())
//          .setAudioDecoderFactoryFactory(new BuiltinAudioDecoderFactoryFactory())
            .setAudioDeviceModule(javaAudioDeviceModule)
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .createPeerConnectionFactory();
        this.mediaStream = this.peerConnectionFactory.createLocalMediaStream("ARDAMS");
        Arrays.stream(videoEncoderFactory.getSupportedCodecs()).forEach(v -> {
            Log.d(MediaManager.class.getSimpleName(), "支持的视频解码器：" + v.name);
        });
        this.initAudio();
        this.initVideo();
    }

    /**
     * 切换视频来源
     *
     * @param type 来源类型
     *
     * TODO：设置分享
     */
    public void exchange(Type type) {
        if(this.type == type) {
            return;
        }
        this.type = type;
        Log.i(MediaManager.class.getSimpleName(), "设置视频来源：" + type);
        if(this.type.isCamera() && type.isCamera()) {
            // TODO：测试是否需要完全重置
            final CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) this.videoCapturer;
            cameraVideoCapturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                @Override
                public void onCameraSwitchDone(boolean success) {
                }
                @Override
                public void onCameraSwitchError(String message) {
                }
            });
        } else {
            this.initVideo();
        }
    }

    /**
     * 加载音频
     */
    private void initAudio() {
        // 关闭音频
        this.closeAudioTrack();
        // 加载音频
        final MediaConstraints mediaConstraints = new MediaConstraints();
        // 高音过滤
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAudioMirroring", "false"));
        // 自动增益
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl2", "true"));
        // 回声消除
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation2", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googDAEchoCancellation", "true"));
        // 噪音处理
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression2", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"));
        final AudioSource audioSource = this.peerConnectionFactory.createAudioSource(mediaConstraints);
        final AudioTrack audioTrack = this.peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource);
//      audioTrack.setVolume(100);
        audioTrack.setEnabled(true);
        this.mediaStream.addTrack(audioTrack);
        Log.i(MediaManager.class.getSimpleName(), "加载音频：" + audioTrack.id());
    }

    /**
     * 加载视频
     */
    private void initVideo() {
        this.closeVideoTrack();
        if(this.videoCapturer != null) {
            this.videoCapturer.dispose();
        }
        if(this.type.isCamera()) {
            final CameraEnumerator cameraEnumerator = new Camera2Enumerator(this.context);
            final String[] names = cameraEnumerator.getDeviceNames();
            for(String name : names) {
                if(this.type == Type.FRONT && cameraEnumerator.isFrontFacing(name)) {
                    Log.i(MediaManager.class.getSimpleName(), "加载视频（前置摄像头）：" + name);
                    this.videoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
                } else if(this.type == Type.BACK && cameraEnumerator.isBackFacing(name)) {
                    Log.i(MediaManager.class.getSimpleName(), "加载视频（后置摄像头）：" + name);
                    this.videoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
                } else {
                    Log.d(MediaManager.class.getSimpleName(), "忽略摄像头：" + name);
                }
            }
            this.initVideoTrack();
        } else if(this.type == Type.FILE) {
            Log.i(MediaManager.class.getSimpleName(), "加载视频（文件）");
        } else if(this.type == Type.SCREEN) {
            Log.i(MediaManager.class.getSimpleName(), "加载视频（录屏）");
            final Message message = new Message();
            message.what = Config.WHAT_SCREEN_CAPTURE;
            this.handler.sendMessage(message);
        }
    }

    public void screenRecord(Intent intent) {
        this.videoCapturer = new ScreenCapturerAndroid(intent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
            super.onStop();
            Log.i(MediaManager.class.getSimpleName(), "停止屏幕捕获");
            }
        });
        this.initVideoTrack();
    }

    /**
     * 加载视频
     */
    private void initVideoTrack() {
        final SurfaceViewRenderer surfaceViewRenderer = this.preview();
        // 加载视频
        final SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("MediaVideoThread", this.eglBase.getEglBaseContext());
//      surfaceTextureHelper.setTextureSize();
//      surfaceTextureHelper.setFrameRotation();
        final VideoSource videoSource = this.peerConnectionFactory.createVideoSource(this.videoCapturer.isScreencast());
        // 美颜水印
//      videoSource.setVideoProcessor();
        this.videoCapturer.initialize(surfaceTextureHelper, this.context, videoSource.getCapturerObserver());
        this.videoCapturer.startCapture(480, 640, 30);
        final VideoTrack videoTrack = this.peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource);
        videoTrack.addSink(surfaceViewRenderer);
        videoTrack.addSink(MediaRecorder.getInstance().videoRecoder);
        videoTrack.setEnabled(true);
        this.mediaStream.addTrack(videoTrack);
        Log.i(MediaManager.class.getSimpleName(), "加载视频：" + videoTrack.id());
    }

    public MediaStream getMediaStream() {
        return this.mediaStream;
    }

    private SurfaceViewRenderer preview() {
        // 设置预览
        final SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(this.context);
        this.handler.post(() -> {
            // 视频反转
            surfaceViewRenderer.setMirror(false);
            // 视频拉伸
            surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            // 硬件拉伸
            surfaceViewRenderer.setEnableHardwareScaler(true);
            // 加载
            surfaceViewRenderer.init(this.eglBase.getEglBaseContext(), null);
        });
        // 事件
//      surfaceViewRenderer.setOnClickListener();
        // TODO：迁移localvideo
//        surfaceViewRenderer.release();
        // 页面加载
        final Message message = new Message();
        message.obj = surfaceViewRenderer;
        message.what = Config.WHAT_NEW_LOCAL_VIDEO;
        // TODO：恢复
//        this.handler.sendMessage(message);
        // 暂停
//        surfaceViewRenderer.pauseVideo();
        // 恢复
//        surfaceViewRenderer.disableFpsReduction();
        return surfaceViewRenderer;
    }

    public void remotePreview(final MediaStream mediaStream) {
        // 设置预览
        final SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(this.context);
        this.handler.post(() -> {
            // 视频反转
            surfaceViewRenderer.setMirror(false);
            // 视频拉伸
            surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            // 硬件拉伸
            surfaceViewRenderer.setEnableHardwareScaler(true);
            // 加载
            surfaceViewRenderer.init(this.eglBase.getEglBaseContext(), null);
            // 开始播放
            final VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.setEnabled(true);
            videoTrack.addSink(surfaceViewRenderer);
        });
        // 页面加载
        final Message message = new Message();
        message.obj = surfaceViewRenderer;
        message.what = Config.WHAT_NEW_REMOTE_VIDEO;
        this.handler.sendMessage(message);
        // 暂停
//        surfaceViewRenderer.pauseVideo();
        // 恢复
//        surfaceViewRenderer.disableFpsReduction();
    }

    public void pauseAudio() {
        synchronized (this.mediaStream.audioTracks) {
            this.mediaStream.audioTracks.forEach(a -> a.setEnabled(false));
        }
    }

    public void resumeAudio() {
        synchronized (this.mediaStream.audioTracks) {
            this.mediaStream.audioTracks.forEach(a -> a.setEnabled(true));
        }
    }

    public void pauseVideo() {
        synchronized (this.mediaStream.videoTracks) {
            this.mediaStream.videoTracks.forEach(v -> v.setEnabled(false));
        }
        synchronized (this.mediaStream.preservedVideoTracks) {
            this.mediaStream.preservedVideoTracks.forEach(v -> v.setEnabled(false));
        }
    }

    public void resumeVideo() {
        synchronized (this.mediaStream.videoTracks) {
            this.mediaStream.videoTracks.forEach(v -> v.setEnabled(true));
        }
        synchronized (this.mediaStream.preservedVideoTracks) {
            this.mediaStream.preservedVideoTracks.forEach(v -> v.setEnabled(true));
        }
    }

    /**
     * 关闭声音
     */
    private void closeAudioTrack() {
        synchronized (this.mediaStream.audioTracks) {
            AudioTrack track;
            final Iterator<AudioTrack> iterator = this.mediaStream.audioTracks.iterator();
            while(iterator.hasNext()) {
                track = iterator.next();
                iterator.remove();
                track.dispose();
            }
        }
    }

    /**
     * 关闭视频
     */
    private void closeVideoTrack() {
        // 次码流
        this.closeVideoTrack(this.mediaStream.videoTracks);
        // 主码流
        this.closeVideoTrack(this.mediaStream.preservedVideoTracks);
    }

    /**
     * 关闭视频
     *
     * @param list 视频列表
     */
    private void closeVideoTrack(List<VideoTrack> list) {
        synchronized (list) {
            VideoTrack track;
            final Iterator<VideoTrack> iterator = list.iterator();
            while(iterator.hasNext()) {
                track = iterator.next();
                iterator.remove();
                track.dispose();
            }
        }
    }

    /**
     * 释放资源
     */
    public void close() {
        this.closeAudioTrack();
        this.closeVideoTrack();
        if(this.eglBase != null) {
            this.eglBase.release();
            this.eglBase = null;
        }
        if(this.videoCapturer != null) {
            this.videoCapturer.dispose();
            this.videoCapturer = null;
        }
        if(this.mediaStream != null) {
            this.mediaStream.dispose();
            this.mediaStream = null;
        }
        if(this.peerConnectionFactory != null) {
            this.peerConnectionFactory.dispose();
            this.peerConnectionFactory = null;
        }
    }

    /**
     * 摄像头事件
     *
     * @author acgist
     */
    private static class MediaCameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

        @Override
        public void onCameraError(String message) {
        }

        @Override
        public void onCameraDisconnected() {
        }

        @Override
        public void onCameraFreezed(String message) {
        }

        @Override
        public void onCameraOpening(String message) {
        }

        @Override
        public void onFirstFrameAvailable() {
        }

        @Override
        public void onCameraClosed() {
        }
    }

}
