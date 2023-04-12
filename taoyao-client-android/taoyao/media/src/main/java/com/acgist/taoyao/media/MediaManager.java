package com.acgist.taoyao.media;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.acgist.taoyao.boot.utils.DateUtils;
import com.acgist.taoyao.media.client.PhotographClient;
import com.acgist.taoyao.media.client.RecordClient;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.config.MediaAudioProperties;
import com.acgist.taoyao.media.config.MediaProperties;
import com.acgist.taoyao.media.config.MediaVideoProperties;
import com.acgist.taoyao.media.config.WebrtcProperties;
import com.acgist.taoyao.media.signal.ITaoyao;

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
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * 媒体来源管理器
 *
 * @author acgist
 *
 * TODO：动态码率（BITRATE_MODE_VBR、BITRATE_MODE）
 */
public final class MediaManager {

    private static final MediaManager INSTANCE = new MediaManager();

    public static final MediaManager getInstance() {
        return INSTANCE;
    }

    /**
     * 当前终端数量
     */
    private volatile int clientCount;
    /**
     * 当前媒体共享数量
     */
    private volatile int shareClientCount;
    /**
     * 是否打开音频播放
     */
    private boolean playAudio;
    /**
     * 是否打开视频播放
     */
    private boolean playVideo;
    /**
     * 是否消费音频
     */
    private boolean audioConsume;
    /**
     * 是否消费视频
     */
    private boolean videoConsume;
    /**
     * 是否生产音频
     */
    private boolean audioProduce;
    /**
     * 是否生产视频
     */
    private boolean videoProduce;
    /**
     * 视频路径
     */
    private String imagePath;
    /**
     * 图片路径
     */
    private String videoPath;
    /**
     * 视频来源类型
     */
    private VideoSourceType videoSourceType;
    /**
     * 传输通道类型
     */
    private TransportType transportType;
    /**
     * 信令
     */
    private ITaoyao taoyao;
    /**
     * Handler
     */
    private Handler handler;
    /**
     * 上下文
     */
    private Context context;
    /**
     * 媒体配置
     */
    private MediaProperties mediaProperties;
    /**
     * 音频配置
     */
    private MediaAudioProperties mediaAudioProperties;
    /**
     * 视频配置
     */
    private MediaVideoProperties mediaVideoProperties;
    /**
     * WebRTC配置
     */
    private WebrtcProperties webrtcProperties;
    /**
     * EGL
     */
    private EglBase eglBase;
    /**
     * 媒体流：声音、视频
     */
    private MediaStream mediaStream;
    /**
     * 音频Track
     */
    private AudioTrack audioTrack;
    /**
     * 音频来源
     */
    private AudioSource audioSource;
    /**
     * 视频Track
     */
    private VideoTrack videoTrack;
    /**
     * 视频来源
     */
    private VideoSource videoSource;
    /**
     * 视频捕获
     */
    private VideoCapturer videoCapturer;
    /**
     * 录制终端
     */
    private RecordClient recordClient;
    /**
     * 录制视频Track
     */
    private VideoTrack recordVideoTrack;
    /**
     * 录制视频来源
     */
    private VideoSource recordVideoSource;
    /**
     * 录制视频捕获
     */
    private VideoCapturer recordVideoCapturer;
    /**
     * 拍照终端
     */
    private PhotographClient photographClient;
    /**
     * 视频来源
     */
    private SurfaceTextureHelper surfaceTextureHelper;
    /**
     * PeerConnectionFactory
     */
    private PeerConnectionFactory peerConnectionFactory;

    static {
        // 采样
//      WebRtcAudioUtils.setDefaultSampleRateHz();
        // 噪声消除
//      WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        // 回声消除
//      WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        // 自动增益
//      WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
        // OpenSL ES
//      WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
        final MediaCodecList mediaCodecList = new MediaCodecList(-1);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
//            if (mediaCodecInfo.isEncoder()) {
                final String[] supportedTypes = mediaCodecInfo.getSupportedTypes();
                Log.d(MediaManager.class.getSimpleName(), "编码器名称：" + mediaCodecInfo.getName());
                Log.d(MediaManager.class.getSimpleName(), "编码器类型：" + String.join(" , ", supportedTypes));
                for (String supportType : supportedTypes) {
                    final MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType(supportType);
                    Log.d(MediaManager.class.getSimpleName(), "编码器支持的文件格式：" + codecCapabilities.getMimeType());
//                  MediaCodecInfo.CodecCapabilities.COLOR_*
//                  final int[] colorFormats = codecCapabilities.colorFormats;
//                  Log.d(MediaManager.class.getSimpleName(), "编码器支持的色彩格式：" + IntStream.of(colorFormats).boxed().map(String::valueOf).collect(Collectors.joining(" , ")));
                }
//            }
        }
    }

    private MediaManager() {
        this.clientCount = 0;
        this.shareClientCount = 0;
    }

    /**
     * @return 是否可用
     */
    public boolean available() {
        return this.handler != null && this.context != null && this.taoyao != null;
    }

    /**
     * @param handler      Handler
     * @param context      上下文
     * @param playAudio    是否播放音频
     * @param playVideo    是否播放视频
     * @param audioConsume 是否消费音频
     * @param videoConsume 是否消费视频
     * @param audioProduce 是否生产音频
     * @param videoProduce 是否生产视频
     */
    public void initContext(
        Handler handler, Context context,
        boolean playAudio, boolean playVideo,
        boolean audioConsume, boolean videoConsume,
        boolean audioProduce, boolean videoProduce,
        String imagePath, String videoPath,
        TransportType transportType
    ) {
        this.handler = handler;
        this.context = context;
        this.playAudio = playAudio;
        this.playVideo = playVideo;
        this.audioConsume = audioConsume;
        this.videoConsume = videoConsume;
        this.audioProduce = audioProduce;
        this.videoProduce = videoProduce;
        this.imagePath = imagePath;
        this.videoPath = videoPath;
        this.transportType = transportType;
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this.context)
//             .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
//             .setEnableInternalTracer(true)
               .createInitializationOptions()
        );
    }

    /**
     * @param taoyao  信令
     */
    public void initTaoyao(ITaoyao taoyao) {
        this.taoyao = taoyao;
    }

    /**
     * 新建终端
     *
     * @param videoSourceType 视频类型
     *
     * @return PeerConnectionFactory PeerConnectionFactory
     */
    public PeerConnectionFactory newClient(VideoSourceType videoSourceType) {
        synchronized (this) {
            while(this.mediaProperties == null) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    Log.e(MediaManager.class.getSimpleName(), "等待配置异常", e);
                }
            }
            if (this.clientCount <= 0) {
                this.initMedia(videoSourceType);
                this.nativeInit();
            }
            this.clientCount++;
        }
        return this.peerConnectionFactory;
    }

    /**
     * 关闭一个终端
     * 最后一个终端关闭时，释放所有资源。
     * 注意：所有本地媒体关闭调用，不要直接关闭本地媒体流。
     *
     * @return 剩余终端数量
     */
    public int closeClient() {
        synchronized (this) {
            this.clientCount--;
            if (this.clientCount <= 0) {
                this.close();
                this.nativeStop();
            }
            return this.clientCount;
        }
    }

    public boolean isRecording() {
        return this.recordClient != null;
    }

    /**
     * 加载媒体
     *
     * @param videoSourceType 视频来源类型
     */
    private void initMedia(VideoSourceType videoSourceType) {
        Log.i(MediaManager.class.getSimpleName(), "加载媒体：" + videoSourceType);
        this.videoSourceType = videoSourceType;
        this.eglBase = EglBase.create();
        final VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(this.eglBase.getEglBaseContext());
        final VideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(this.eglBase.getEglBaseContext(), true, true);
        final JavaAudioDeviceModule javaAudioDeviceModule = JavaAudioDeviceModule.builder(this.context)
//          .setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            // 本地音频
            .setSamplesReadyCallback(audioSamples -> {
                if(this.recordClient != null) {
                    this.recordClient.putAudio(audioSamples);
                }
            })
            // 超低延迟
//          .setUseLowLatency()
            // 远程音频
//          .setAudioTrackStateCallback()
//          .setAudioFormat(AudioFormat.ENCODING_PCM_32BIT)
//          .setUseHardwareNoiseSuppressor(true)
//          .setUseHardwareAcousticEchoCanceler(true)
            .createAudioDeviceModule();
        this.peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(javaAudioDeviceModule)
            // 变声
//          .setAudioProcessingFactory()
//          .setAudioEncoderFactoryFactory(new BuiltinAudioEncoderFactoryFactory())
//          .setAudioDecoderFactoryFactory(new BuiltinAudioDecoderFactoryFactory())
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .createPeerConnectionFactory();
        this.mediaStream = this.peerConnectionFactory.createLocalMediaStream("Taoyao");
        Arrays.stream(videoEncoderFactory.getSupportedCodecs()).forEach(v -> {
            Log.d(MediaManager.class.getSimpleName(), "支持的视频解码器：" + v.name);
        });
        this.initAudio();
        this.initVideo();
    }

    /**
     * 加载音频
     */
    private void initAudio() {
        // 关闭音频
        this.closeAudio();
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
        this.audioSource = this.peerConnectionFactory.createAudioSource(mediaConstraints);
        this.audioTrack = this.peerConnectionFactory.createAudioTrack("TaoyaoA0", this.audioSource);
        this.audioTrack.setVolume(Config.DEFAULT_VOLUME);
        this.audioTrack.setEnabled(true);
        this.mediaStream.addTrack(this.audioTrack);
        Log.i(MediaManager.class.getSimpleName(), "加载音频：" + this.audioTrack.id());
    }

    /**
     * 加载视频
     */
    private void initVideo() {
        // 关闭视频
        this.closeVideo();
        // 加载视频
        Log.i(MediaManager.class.getSimpleName(), "加载视频：" + this.videoSourceType);
        if (this.videoSourceType.isCamera()) {
            this.initCamera();
        } else if (this.videoSourceType == VideoSourceType.FILE) {
        } else if (this.videoSourceType == VideoSourceType.SCREEN) {
            final Message message = new Message();
            message.what = Config.WHAT_SCREEN_CAPTURE;
            this.handler.sendMessage(message);
        } else {
            // 其他来源
        }
    }

    /**
     * 加载摄像头
     */
    private void initCamera() {
        final CameraEnumerator cameraEnumerator = new Camera2Enumerator(this.context);
        final String[] names = cameraEnumerator.getDeviceNames();
        for (String name : names) {
            if (this.videoSourceType == VideoSourceType.FRONT && cameraEnumerator.isFrontFacing(name)) {
                this.videoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
                this.recordVideoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
            } else if (this.videoSourceType == VideoSourceType.BACK && cameraEnumerator.isBackFacing(name)) {
                this.videoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
                this.recordVideoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
            } else {
                // 忽略其他摄像头
            }
        }
        this.initVideoTrack();
    }

    /**
     * 加载屏幕
     *
     * @param intent Intent
     */
    public void initScreen(Intent intent) {
        this.videoCapturer = new ScreenCapturerAndroid(intent, new ScreenCallback());
        this.recordVideoCapturer = new ScreenCapturerAndroid(intent, new ScreenCallback());
        this.initVideoTrack();
    }

    /**
     * 加载视频
     */
    private void initVideoTrack() {
        // 加载视频
        this.surfaceTextureHelper = SurfaceTextureHelper.create("MediaVideoThread", this.eglBase.getEglBaseContext());
//      this.surfaceTextureHelper.setTextureSize();
//      this.surfaceTextureHelper.setFrameRotation();
        // 次码流
        this.videoSource = this.peerConnectionFactory.createVideoSource(this.videoCapturer.isScreencast());
        this.videoCapturer.initialize(this.surfaceTextureHelper, this.context, this.videoSource.getCapturerObserver());
        this.videoTrack = this.peerConnectionFactory.createVideoTrack("TaoyaoV0", this.videoSource);
        this.videoTrack.setEnabled(true);
        this.mediaStream.addTrack(this.videoTrack);
        Log.i(MediaManager.class.getSimpleName(), "加载视频（次码流）：" + this.videoTrack.id());
        // 主码流
        this.recordVideoSource = this.peerConnectionFactory.createVideoSource(this.recordVideoCapturer.isScreencast());
        this.recordVideoCapturer.initialize(this.surfaceTextureHelper, this.context, this.recordVideoSource.getCapturerObserver());
        this.recordVideoTrack = this.peerConnectionFactory.createVideoTrack("TaoyaoV1", this.recordVideoSource);
        this.recordVideoTrack.addSink(videoFrame -> {
            if(this.recordClient != null) {
                this.recordClient.putVideo(videoFrame);
            }
            if(this.photographClient != null) {
                synchronized (this.photographClient) {
                    this.photographClient.photograph(videoFrame);
                    this.photographClient = null;
                }
            }
        });
        this.recordVideoTrack.setEnabled(true);
        Log.i(MediaManager.class.getSimpleName(), "加载视频（主码流）：" + this.recordVideoTrack.id());
        // 视频处理
//      this.videoSource.setVideoProcessor();
    }

    /**
     * 更新配置
     *
     * @param mediaProperties      媒体配置
     * @param mediaAudioProperties 音频配置
     * @param mediaVideoProperties 视频配置
     */
    public void updateMediaConfig(MediaProperties mediaProperties, MediaAudioProperties mediaAudioProperties, MediaVideoProperties mediaVideoProperties) {
        this.mediaProperties = mediaProperties;
        this.updateAudioConfig(mediaAudioProperties);
        this.updateVideoConfig(mediaVideoProperties);
        synchronized (this) {
            this.notifyAll();
        }
    }

    public void updateAudioConfig(MediaAudioProperties mediaAudioProperties) {
        this.mediaAudioProperties = mediaAudioProperties;
    }

    public void updateVideoConfig(MediaVideoProperties mediaVideoProperties) {
        this.mediaVideoProperties = mediaVideoProperties;
        if(this.videoCapturer != null) {
            try {
                this.videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(MediaManager.class.getSimpleName(), "暂停视频捕获异常", e);
            }
            this.videoCapturer.startCapture(this.mediaVideoProperties.getHeight(), this.mediaVideoProperties.getWidth(), this.mediaVideoProperties.getFrameRate());
        }
    }

    public void updateWebrtcConfig(WebrtcProperties webrtcProperties) {
        this.webrtcProperties = webrtcProperties;
    }

    /**
     * 切换视频来源
     *
     * @param videoSourceType 来源类型
     */
    public void updateVideoSource(VideoSourceType videoSourceType) {
        if (this.videoSourceType == videoSourceType) {
            return;
        }
        this.videoSourceType = videoSourceType;
        Log.i(MediaManager.class.getSimpleName(), "设置视频来源：" + videoSourceType);
        if (this.videoSourceType.isCamera() && videoSourceType.isCamera()) {
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

    public MediaStream getMediaStream() {
        return this.mediaStream;
    }

    public void startVideoCapture() {
        synchronized (this) {
            if(this.videoCapturer == null) {
                return;
            }
            if(this.shareClientCount > 0) {
                this.shareClientCount++;
                return;
            } else {
                this.shareClientCount++;
                this.videoCapturer.startCapture(this.mediaVideoProperties.getHeight(), this.mediaVideoProperties.getWidth(), this.mediaVideoProperties.getFrameRate());
            }
        }
    }

    public void stopVideoCapture() {
        synchronized (this) {
            if(this.videoCapturer == null) {
                return;
            }
            if(this.shareClientCount <= 0) {
                return;
            }
            this.shareClientCount--;
            if(this.shareClientCount <= 0) {
                try {
                    this.videoCapturer.stopCapture();
                } catch (InterruptedException e) {
                    Log.e(MediaManager.class.getSimpleName(), "关闭视频捕获（次码流）异常", e);
                }
            }
        }
    }

    public String photograph() {
        synchronized (this) {
            if(this.recordVideoCapturer == null) {
                // 如果没有拉流不能拍照
                return null;
            }
            String filepath;
            if(this.recordClient == null) {
                final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideos().get("fd-video");
                this.recordVideoCapturer.startCapture(mediaVideoProperties.getHeight(), mediaVideoProperties.getWidth(), mediaVideoProperties.getFrameRate());
                this.photographClient = new PhotographClient(this.imagePath);
                filepath = this.photographClient.waitForPhotograph();
                try {
                    this.recordVideoCapturer.stopCapture();
                } catch (InterruptedException e) {
                    Log.e(MediaManager.class.getSimpleName(), "关闭视频捕获（主码流）异常", e);
                }
            } else {
                this.photographClient = new PhotographClient(this.imagePath);
                filepath = this.photographClient.waitForPhotograph();
            }
            return filepath;
        }
    }

    public RecordClient startRecordVideoCapture() {
        synchronized (this) {
            if(this.recordClient != null) {
                return this.recordClient;
            }
            this.recordClient = new RecordClient(
                this.videoPath,
                this.taoyao,
                this.handler
            );
            this.recordClient.start();
            final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideos().get("fd-video");
            this.recordVideoCapturer.startCapture(mediaVideoProperties.getHeight(), mediaVideoProperties.getWidth(), mediaVideoProperties.getFrameRate());
            return this.recordClient;
        }
    }

    public void stopRecordVideoCapture() {
        synchronized (this) {
            if(this.recordClient == null) {
                return;
            } else {
                this.recordClient.close();
                this.recordClient = null;
                try {
                    this.recordVideoCapturer.stopCapture();
                } catch (InterruptedException e) {
                    Log.e(MediaManager.class.getSimpleName(), "关闭视频捕获（主码流）异常", e);
                }
            }
        }
    }

    /**
     * @param flag         Config.WHAT_*
     * @param videoTrack   视频媒体流Track
     *
     * @return 播放控件
     */
    public SurfaceViewRenderer buildSurfaceViewRenderer(
        final int flag,
        final VideoTrack videoTrack
    ) {
        // 预览控件
        final SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(this.context);
        this.handler.post(() -> {
            // 视频反转
            surfaceViewRenderer.setMirror(false);
            // 视频拉伸
            surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            // 硬件拉伸
            surfaceViewRenderer.setEnableHardwareScaler(true);
            // 加载OpenSL ES
            surfaceViewRenderer.init(this.eglBase.getEglBaseContext(), null);
            // 强制播放
            if(!videoTrack.enabled()) {
                videoTrack.setEnabled(true);
            }
            videoTrack.addSink(surfaceViewRenderer);
        });
        // 页面加载
        final Message message = new Message();
        message.obj = surfaceViewRenderer;
        message.what = flag;
        this.handler.sendMessage(message);
        return surfaceViewRenderer;
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
    private void closeAudio() {
        if(this.audioTrack != null) {
            this.audioTrack.dispose();
            this.audioTrack = null;
        }
        if(this.audioSource != null) {
            this.audioSource.dispose();
            this.audioSource = null;
        }
    }

    /**
     * 关闭视频
     */
    private void closeVideo() {
        this.stopVideoCapture();
        this.stopRecordVideoCapture();
        if(this.videoTrack != null) {
            this.videoTrack.dispose();
            this.videoTrack = null;
        }
        if(this.videoSource != null) {
            this.videoSource.dispose();
            this.videoSource = null;
        }
        if (this.videoCapturer != null) {
            this.videoCapturer.dispose();
            this.videoCapturer = null;
        }
        if(this.recordVideoTrack != null) {
            this.recordVideoTrack.dispose();
            this.recordVideoTrack = null;
        }
        if(this.recordVideoSource != null) {
            this.recordVideoSource.dispose();
            this.recordVideoSource = null;
        }
        if(this.recordVideoCapturer != null) {
            this.recordVideoCapturer.dispose();
            this.recordVideoCapturer = null;
        }
        if(this.surfaceTextureHelper != null) {
            this.surfaceTextureHelper.dispose();
            this.surfaceTextureHelper = null;
        }
    }

    private void closeMedia() {
        if (this.eglBase != null) {
            this.eglBase.release();
            this.eglBase = null;
        }
        if (this.mediaStream != null) {
            this.mediaStream.dispose();
            this.mediaStream = null;
        }
        if (this.peerConnectionFactory != null) {
            this.peerConnectionFactory.dispose();
            this.peerConnectionFactory = null;
        }
    }

    /**
     * 释放资源
     */
    private void close() {
        this.closeAudio();
        this.closeVideo();
        this.closeMedia();
//      PeerConnectionFactory.shutdownInternalTracer();
//      PeerConnectionFactory.stopInternalTracingCapture();
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
            Log.i(MediaManager.class.getSimpleName(), "开始视频捕获");
        }

        @Override
        public void onFirstFrameAvailable() {
        }

        @Override
        public void onCameraClosed() {
            Log.i(MediaManager.class.getSimpleName(), "停止视频捕获");
        }

    }

    /**
     * 屏幕录制回调
     */
    private static class ScreenCallback extends MediaProjection.Callback {

        @Override
        public void onStop() {
            super.onStop();
            Log.i(MediaManager.class.getSimpleName(), "停止屏幕捕获");
        }
    }

    private native void nativeInit();
    private native void nativeStop();

}
