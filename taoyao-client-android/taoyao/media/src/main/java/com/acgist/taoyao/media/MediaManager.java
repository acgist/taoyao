package com.acgist.taoyao.media;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
import org.webrtc.CapturerObserver;
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

import java.util.Arrays;

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
     * 视频路径
     */
    private String imagePath;
    /**
     * 图片路径
     */
    private String videoPath;
    /**
     * 图片质量
     */
    private int imageQuantity;
    /**
     * 音频质量
     */
    private String audioQuantity;
    /**
     * 视频质量
     */
    private String videoQuantity;
    /**
     * 通道数量
     */
    private int channelCount;
    /**
     * 关键帧频率
     */
    private int iFrameInterval;
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
    private Handler mainHandler;
    /**
     * 上下文
     */
    private Context context;
    /**
     * 媒体配置
     */
    private MediaProperties mediaProperties;
    /**
     * 当前共享视频配置
     */
    private MediaVideoProperties mediaVideoProperties;
    /**
     * 当前共享音频配置
     */
    private MediaAudioProperties mediaAudioProperties;
    /**
     * WebRTC配置
     */
    private WebrtcProperties webrtcProperties;
    /**
     * EGL
     */
    private EglBase eglBase;
    /**
     * EGL共享上下文
     */
    private EglBase.Context shareEglContext;
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
     * 视频捕获
     */
    private VideoCapturer videoCapturer;
    /**
     * 主码流视频Track
     */
    private VideoTrack mainVideoTrack;
    /**
     * 主码流视频来源
     */
    private VideoSource mainVideoSource;
    /**
     * 次码流视频Track
     */
    private VideoTrack shareVideoTrack;
    /**
     * 次码流视频来源
     */
    private VideoSource shareVideoSource;
    /**
     * 录制终端
     */
    private RecordClient recordClient;
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
    }

    /**
     * @return 是否可用：所有配置加载完成
     */
    public boolean available() {
        return
            this.taoyao          != null &&
            this.context         != null &&
            this.mediaProperties != null;
    }

    /**
     * @param mainHandler   Handler
     * @param context      上下文
     */
    public void initContext(
        Handler mainHandler, Context context,
        int imageQuantity, String audioQuantity, String videoQuantity,
        int channelCount, int iFrameInterval,
        String imagePath, String videoPath,
        TransportType transportType
    ) {
        this.mainHandler = mainHandler;
        this.context   = context;
        this.imageQuantity = imageQuantity;
        this.audioQuantity = audioQuantity;
        this.videoQuantity = videoQuantity;
        this.channelCount = channelCount;
        this.iFrameInterval = iFrameInterval;
        this.imagePath = imagePath;
        this.videoPath = videoPath;
        this.transportType = transportType;
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
            while(!this.available()) {
                Log.i(MediaManager.class.getSimpleName(), "等待配置");
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    Log.e(MediaManager.class.getSimpleName(), "等待配置异常", e);
                }
            }
            if (this.clientCount <= 0) {
                Log.i(MediaManager.class.getSimpleName(), "加载PeerConnectionFactory");
                this.initPeerConnectionFactory();
                this.nativeInit();
                this.initMedia(videoSourceType);
                this.startVideoCapture();
            }
            this.clientCount++;
        }
        return this.peerConnectionFactory;
    }

    /**
     * 关闭一个终端
     * 最后一个终端关闭时，释放所有资源。
     *
     * @return 剩余终端数量
     */
    public int closeClient() {
        synchronized (this) {
            this.clientCount--;
            if (this.clientCount <= 0) {
                Log.i(MediaManager.class.getSimpleName(), "释放PeerConnectionFactory");
                this.closeAudio();
                this.closeMainVideo();
                this.closeShareVideo();
                this.closeMedia();
                this.stopVideoCapture();
                this.nativeStop();
                this.stopPeerConnectionFactory();
            }
            return this.clientCount;
        }
    }

    public boolean isRecording() {
        return this.recordClient != null;
    }

    public MediaProperties getMediaProperties() {
        return this.mediaProperties;
    }

    private void initPeerConnectionFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this.context)
//              .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
//              .setNativeLibraryName("jingle_peerconnection_so")
//              .setEnableInternalTracer(true)
                .createInitializationOptions()
        );
    }

    private void stopPeerConnectionFactory() {
        PeerConnectionFactory.shutdownInternalTracer();
    }

    /**
     * 加载媒体
     *
     * @param videoSourceType 视频来源类型
     */
    private void initMedia(VideoSourceType videoSourceType) {
        Log.i(MediaManager.class.getSimpleName(), "加载媒体：" + videoSourceType);
        this.eglBase = EglBase.create();
        this.shareEglContext = this.eglBase.getEglBaseContext();
        this.videoSourceType = videoSourceType;
        final VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(this.shareEglContext);
        final VideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(this.shareEglContext, true, true);
        final JavaAudioDeviceModule javaAudioDeviceModule = this.javaAudioDeviceModule();
        this.peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .setAudioDeviceModule(javaAudioDeviceModule)
//          .setAudioProcessingFactory()
//          .setAudioEncoderFactoryFactory(new BuiltinAudioEncoderFactoryFactory())
//          .setAudioDecoderFactoryFactory(new BuiltinAudioDecoderFactoryFactory())
            .createPeerConnectionFactory();
        this.mediaStream = this.peerConnectionFactory.createLocalMediaStream("Taoyao");
        Arrays.stream(videoEncoderFactory.getSupportedCodecs()).forEach(v -> {
            Log.d(MediaManager.class.getSimpleName(), "支持的视频解码器：" + v.name);
        });
        this.initAudio();
        this.initVideo();
    }

    private JavaAudioDeviceModule javaAudioDeviceModule() {
//      final AudioAttributes audioAttributes = new AudioAttributes.Builder()
//          .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
//          .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
//          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//          .build();
        final JavaAudioDeviceModule javaAudioDeviceModule = JavaAudioDeviceModule.builder(this.context)
//          .setSampleRate()
//          .setAudioSource(MediaRecorder.AudioSource.MIC)
//          .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT)
//          .setAudioAttributes(audioAttributes)
//          .setUseStereoInput()
//          .setUseStereoOutput()
            // 超低延迟
//          .setUseLowLatency()
            .setSamplesReadyCallback(audioSamples -> {
                if(this.recordClient != null) {
                    this.recordClient.onWebRtcAudioRecordSamplesReady(audioSamples);
                }
            })
            .setAudioTrackStateCallback(new JavaAudioDeviceModule.AudioTrackStateCallback() {
                @Override
                public void onWebRtcAudioTrackStart() {
                    Log.i(MediaManager.class.getSimpleName(), "WebRTC声音Track开始");
                }
                @Override
                public void onWebRtcAudioTrackStop() {
                    Log.i(MediaManager.class.getSimpleName(), "WebRTC声音Track结束");
                }
            })
            .setAudioRecordStateCallback(new JavaAudioDeviceModule.AudioRecordStateCallback() {
                @Override
                public void onWebRtcAudioRecordStart() {
                    Log.i(MediaManager.class.getSimpleName(), "WebRTC声音录制开始");
                }
                @Override
                public void onWebRtcAudioRecordStop() {
                    Log.i(MediaManager.class.getSimpleName(), "WebRTC声音录制结束");
                }
            })
//          .setUseHardwareNoiseSuppressor(true)
//          .setUseHardwareAcousticEchoCanceler(true)
            .createAudioDeviceModule();
//          javaAudioDeviceModule.setSpeakerMute(false);
//          javaAudioDeviceModule.setMicrophoneMute(false);
        return javaAudioDeviceModule;
    }

    /**
     * 加载音频
     */
    private void initAudio() {
        // 加载音频
        final MediaConstraints mediaConstraints = new MediaConstraints();
        // 高音过滤
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAudioMirroring", "false"));
        // 自动增益：AGC
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl2", "true"));
        // 回声消除：AEC
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation2", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googDAEchoCancellation", "true"));
        // 噪音处理：NS
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
        // 加载视频
        Log.i(MediaManager.class.getSimpleName(), "加载视频：" + this.videoSourceType);
        if (this.videoSourceType == VideoSourceType.FILE) {
            this.initFile();
        } else if (this.videoSourceType.isCamera()) {
            this.initCamera();
        } else if (this.videoSourceType == VideoSourceType.SCREEN) {
            this.initSharePromise();
        } else {
            // 其他来源
        }
    }

    private void initFile() {
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
            } else if (this.videoSourceType == VideoSourceType.BACK && cameraEnumerator.isBackFacing(name)) {
                this.videoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
            } else {
                // 忽略其他摄像头
            }
        }
        this.initVideoTrack();
    }

    private void initSharePromise() {
        final Message message = new Message();
        message.what = Config.WHAT_SCREEN_CAPTURE;
        this.mainHandler.sendMessage(message);
    }

    /**
     * 加载屏幕
     *
     * @param intent Intent
     */
    public void initScreen(Intent intent) {
        this.videoCapturer = new ScreenCapturerAndroid(intent, new ScreenCallback());
        this.initVideoTrack();
    }

    /**
     * 加载视频
     */
    private void initVideoTrack() {
        // 加载视频
        this.surfaceTextureHelper = SurfaceTextureHelper.create("MediaVideoThread", this.shareEglContext);
//      this.surfaceTextureHelper.setTextureSize();
//      this.surfaceTextureHelper.setFrameRotation();
        // 主码流
        this.mainVideoSource = this.peerConnectionFactory.createVideoSource(this.videoCapturer.isScreencast());
        this.mainVideoTrack  = this.peerConnectionFactory.createVideoTrack("TaoyaoV0", this.mainVideoSource);
        this.mainVideoTrack.setEnabled(true);
        Log.i(MediaManager.class.getSimpleName(), "加载视频（主码流）：" + this.mainVideoTrack.id());
        // 次码流
        this.shareVideoSource = this.peerConnectionFactory.createVideoSource(this.videoCapturer.isScreencast());
        this.shareVideoSource.adaptOutputFormat(this.mediaVideoProperties.getWidth(), this.mediaVideoProperties.getHeight(), this.mediaVideoProperties.getFrameRate());
        this.shareVideoTrack  = this.peerConnectionFactory.createVideoTrack("TaoyaoV1", this.shareVideoSource);
        this.shareVideoTrack.setEnabled(true);
        Log.i(MediaManager.class.getSimpleName(), "加载视频（次码流）：" + this.shareVideoTrack.id());
        // 共享次码流
        this.mediaStream.addTrack(this.shareVideoTrack);
        // 视频捕获
        this.videoCapturer.initialize(this.surfaceTextureHelper, this.context, new VideoCapturerObserver());
        // 次码流视频处理
//      this.shareVideoSource.setVideoProcessor();
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
        this.updateAudioConfig(this.mediaProperties.getAudio());
        this.updateVideoConfig(this.mediaProperties.getVideo());
        synchronized (this) {
            this.notifyAll();
        }
    }

    public void updateAudioConfig(MediaAudioProperties mediaAudioProperties) {
        this.mediaAudioProperties = mediaAudioProperties;
    }

    public void updateVideoConfig(MediaVideoProperties mediaVideoProperties) {
        this.mediaVideoProperties = mediaVideoProperties;
        if(this.shareVideoSource == null) {
            return;
        }
        this.shareVideoSource.adaptOutputFormat(this.mediaVideoProperties.getWidth(), this.mediaVideoProperties.getHeight(), this.mediaVideoProperties.getFrameRate());
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
        Log.i(MediaManager.class.getSimpleName(), "设置视频来源：" + videoSourceType);
        final VideoSourceType old = this.videoSourceType;
        this.videoSourceType = videoSourceType;
        if (old.isCamera() && videoSourceType.isCamera()) {
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

    private void startVideoCapture() {
        if(this.videoCapturer == null) {
            return;
        }
        final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideos().get(this.videoQuantity);
        this.videoCapturer.startCapture(mediaVideoProperties.getWidth(), mediaVideoProperties.getHeight(), mediaVideoProperties.getFrameRate());
    }

    private void stopVideoCapture() {
        if(this.videoCapturer == null) {
            return;
        }
        try {
            videoCapturer.stopCapture();
        } catch (InterruptedException e) {
            Log.e(MediaManager.class.getSimpleName(), "关闭视频捕获异常", e);
        }
    }

    public String photograph() {
        synchronized (this) {
            String filepath;
            final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideos().get(this.videoQuantity);
            final PhotographClient photographClient = new PhotographClient(this.imageQuantity, this.imagePath);
            if(this.clientCount <= 0) {
                filepath = photographClient.photograph(mediaVideoProperties.getWidth(), mediaVideoProperties.getHeight(), VideoSourceType.BACK, this.context);
            } else {
                this.photographClient = photographClient;
                this.mainVideoTrack.addSink(this.photographClient);
                filepath = this.photographClient.waitForPhotograph();
                this.mainVideoTrack.removeSink(this.photographClient);
            }
            this.photographClient = null;
            return filepath;
        }
    }

    public RecordClient startRecord() {
        synchronized (this) {
            if(this.recordClient != null) {
                return this.recordClient;
            }
            final MediaAudioProperties mediaAudioProperties = this.mediaProperties.getAudios().get(this.audioQuantity);
            final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideos().get(this.videoQuantity);
            this.recordClient = new RecordClient(
                mediaAudioProperties.getBitrate(), mediaAudioProperties.getSampleRate(), this.channelCount,
                mediaVideoProperties.getBitrate(), mediaVideoProperties.getFrameRate(),  this.iFrameInterval,
                mediaVideoProperties.getWidth(),   mediaVideoProperties.getHeight(),
                this.videoPath, this.taoyao, this.mainHandler
            );
            this.recordClient.start();
            this.mainVideoTrack.addSink(this.recordClient);
            return this.recordClient;
        }
    }

    public void stopRecord() {
        synchronized (this) {
            if(this.recordClient == null) {
                return;
            } else {
                this.mainVideoTrack.removeSink(this.recordClient);
                this.recordClient.close();
                this.recordClient = null;
            }
        }
    }

    /**
     * @param flag       Config.WHAT_*
     * @param videoTrack 视频媒体流Track
     *
     * @return 播放控件
     */
    public SurfaceViewRenderer buildSurfaceViewRenderer(final int flag, final VideoTrack videoTrack) {
        // 预览控件
        final SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(this.context);
        this.mainHandler.post(() -> {
            // 视频反转
            surfaceViewRenderer.setMirror(false);
            // 视频拉伸
            surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            // 硬件拉伸
            surfaceViewRenderer.setEnableHardwareScaler(true);
//          surfaceViewRenderer.setFpsReduction();
            // 加载OpenSL ES
            surfaceViewRenderer.init(this.shareEglContext, null);
            // 添加播放
            videoTrack.addSink(surfaceViewRenderer);
        });
        // 页面加载
        final Message message = new Message();
        message.obj = surfaceViewRenderer;
        message.what = flag;
        this.mainHandler.sendMessage(message);
        return surfaceViewRenderer;
    }

    /**
     * 关闭声音
     */
    private void closeAudio() {
//      if(this.audioTrack != null) {
//          this.audioTrack.dispose();
//          this.audioTrack = null;
//      }
        if(this.audioSource != null) {
            this.audioSource.dispose();
            this.audioSource = null;
        }
    }

    /**
     * 关闭视频
     */
    private void closeShareVideo() {
//      if(this.videoTrack != null) {
//          this.videoTrack.dispose();
//          this.videoTrack = null;
//      }
        if(this.shareVideoSource != null) {
            this.shareVideoSource.dispose();
            this.shareVideoSource = null;
        }
    }

    private void closeMainVideo() {
        if(this.mainVideoTrack != null) {
            this.mainVideoTrack.dispose();
            this.mainVideoTrack = null;
        }
        if(this.mainVideoSource != null) {
            this.mainVideoSource.dispose();
            this.mainVideoSource = null;
        }
    }

    private void closeMedia() {
        if (this.eglBase != null) {
            this.eglBase.release();
            this.eglBase = null;
            this.shareEglContext = null;
        }
        if (this.mediaStream != null) {
            this.mediaStream.dispose();
            this.mediaStream = null;
        }
        if (this.videoCapturer != null) {
            this.videoCapturer.dispose();
            this.videoCapturer = null;
        }
        if(this.surfaceTextureHelper != null) {
            this.surfaceTextureHelper.dispose();
            this.surfaceTextureHelper = null;
        }
        if (this.peerConnectionFactory != null) {
            this.peerConnectionFactory.dispose();
            this.peerConnectionFactory = null;
        }
    }

    private class VideoCapturerObserver implements CapturerObserver {

        private CapturerObserver mainObserver;
        private CapturerObserver shareObserver;

        public VideoCapturerObserver() {
            this.mainObserver  = MediaManager.this.mainVideoSource.getCapturerObserver();
            this.shareObserver = MediaManager.this.shareVideoSource.getCapturerObserver();
        }

        @Override
        public void onCapturerStarted(boolean status) {
            this.mainObserver.onCapturerStarted(status);
            this.shareObserver.onCapturerStarted(status);
        }

        @Override
        public void onCapturerStopped() {
            this.mainObserver.onCapturerStopped();
            this.shareObserver.onCapturerStopped();
        }

        @Override
        public void onFrameCaptured(VideoFrame videoFrame) {
            this.mainObserver.onFrameCaptured(videoFrame);
            this.shareObserver.onFrameCaptured(videoFrame);
        }

    }

    /**
     * 摄像头事件
     *
     * @author acgist
     */
    private class MediaCameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

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
    private class ScreenCallback extends MediaProjection.Callback {

        @Override
        public void onStop() {
            super.onStop();
            Log.i(MediaManager.class.getSimpleName(), "停止屏幕捕获");
        }

    }

    private native void nativeInit();
    private native void nativeStop();

}
