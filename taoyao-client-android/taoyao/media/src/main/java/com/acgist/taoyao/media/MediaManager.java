package com.acgist.taoyao.media;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.acgist.taoyao.media.client.PhotographClient;
import com.acgist.taoyao.media.client.RecordClient;
import com.acgist.taoyao.media.config.Config;
import com.acgist.taoyao.media.config.MediaAudioProperties;
import com.acgist.taoyao.media.config.MediaProperties;
import com.acgist.taoyao.media.config.MediaVideoProperties;
import com.acgist.taoyao.media.config.WebrtcProperties;
import com.acgist.taoyao.media.signal.ITaoyao;
import com.acgist.taoyao.media.video.VideoProcesser;
import com.acgist.taoyao.media.video.WatermarkProcesser;

import org.apache.commons.lang3.StringUtils;
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
import java.util.Locale;
import java.util.UUID;

/**
 * 媒体管理器
 *
 * @author acgist
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
     * 语音播报
     */
    private boolean broadcaster;
    /**
     * 通道数量
     */
    private int channelCount;
    /**
     * 关键帧频率
     */
    private int iFrameInterval;
    /**
     * 水印
     */
    private String watermark;
    /**
     * 视频来源类型
     */
    private VideoSourceType videoSourceType;
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
     * WebRTC配置
     */
    private WebrtcProperties webrtcProperties;
    /**
     * EGL
     */
    private EglBase eglBase;
    /**
     * EGL上下文
     */
    private EglBase.Context eglContext;
    /**
     * 音频来源
     */
    private AudioSource audioSource;
    /**
     * 视频捕获
     */
    private VideoCapturer videoCapturer;
    /**
     * 主码流视频来源
     */
    private VideoSource mainVideoSource;
    /**
     * 次码流视频来源
     */
    private VideoSource shareVideoSource;
    /**
     * 录像终端
     */
    private RecordClient recordClient;
    /**
     * 视频来源
     */
    private SurfaceTextureHelper surfaceTextureHelper;
    /**
     * PeerConnectionFactory
     */
    private PeerConnectionFactory peerConnectionFactory;
    /**
     * JavaAudioDeviceModule
     */
    private JavaAudioDeviceModule javaAudioDeviceModule;
    /**
     * 语音播报
     */
    private TextToSpeech textToSpeech;
    /**
     * 视频处理
     */
    private VideoProcesser videoProcesser;
    /**
     * 录屏等待锁
     */
    private final Object screenLock = new Object();

    static {
//      // 设置采样
//      WebRtcAudioUtils.setDefaultSampleRateHz(48000);
//      // 噪声消除
//      WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
//      // 回声消除
//      WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
//      // 自动增益
//      WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
//      // 支持的编码器
//      final MediaCodecList mediaCodecList = new MediaCodecList(-1);
//      for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
//          final String[] supportedTypes = mediaCodecInfo.getSupportedTypes();
//          Log.d(MediaManager.class.getSimpleName(), "编码器名称：" + mediaCodecInfo.getName());
//          Log.d(MediaManager.class.getSimpleName(), "编码器类型：" + String.join(", ", supportedTypes));
//          for (String supportType : supportedTypes) {
//              final MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType(supportType);
//              Log.d(MediaManager.class.getSimpleName(), "编码器支持的文件格式：" + codecCapabilities.getMimeType());
//              // MediaCodecInfo.CodecCapabilities.COLOR_*
//              final int[] colorFormats = codecCapabilities.colorFormats;
//              Log.d(MediaManager.class.getSimpleName(), "编码器支持的色彩格式：" + IntStream.of(colorFormats).boxed().map(String::valueOf).collect(Collectors.joining(", ")));
//          }
//      }
    }

    private MediaManager() {
        this.clientCount = 0;
    }

    /**
     * @return 是否可用（所有配置加载完成）
     */
    public boolean available() {
        return
            this.taoyao          != null &&
            this.context         != null &&
            this.mainHandler     != null &&
            this.mediaProperties != null;
    }

    /**
     * @return 是否正在录像
     */
    public boolean isRecording() {
        return this.recordClient != null;
    }

    /**
     * @return MediaProperties
     */
    public MediaProperties getMediaProperties() {
        return this.mediaProperties;
    }

    /**
     * @return WebrtcProperties
     */
    public WebrtcProperties getWebrtcProperties() {
        return this.webrtcProperties;
    }

    /**
     * @param mainHandler     MainHandler
     * @param context         上下文
     * @param imageQuantity   图片质量
     * @param audioQuantity   音频质量
     * @param videoQuantity   视频质量
     * @param broadcaster     是否语音播报
     * @param channelCount    音频通道数量
     * @param iFrameInterval  关键帧频率
     * @param imagePath       图片保存路径
     * @param videoPath       视频保存路径
     * @param watermark       水印信息
     * @param videoSourceType 视频来源类型
     */
    public void initContext(
        Handler mainHandler, Context context,
        int imageQuantity, String audioQuantity, String videoQuantity,
        boolean broadcaster, int channelCount, int iFrameInterval,
        String imagePath, String videoPath,
        String watermark, VideoSourceType videoSourceType
    ) {
        this.mainHandler     = mainHandler;
        this.context         = context;
        this.imageQuantity   = imageQuantity;
        this.audioQuantity   = audioQuantity;
        this.videoQuantity   = videoQuantity;
        this.broadcaster     = broadcaster;
        this.channelCount    = channelCount;
        this.iFrameInterval  = iFrameInterval;
        this.imagePath       = imagePath;
        this.videoPath       = videoPath;
        this.watermark       = watermark;
        this.videoSourceType = videoSourceType;
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
     * @param taoyao 信令
     */
    public void initTaoyao(ITaoyao taoyao) {
        this.taoyao = taoyao;
        synchronized (this) {
            this.notifyAll();
        }
    }

    public synchronized void broadcast(String text) {
        if(!this.broadcaster) {
            return;
        }
        if(this.textToSpeech == null) {
            this.textToSpeech = new TextToSpeech(this.context, new MediaManager.TextToSpeechInitListener());
        }
        this.textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
//      this.textToSpeech.stop();
//      this.textToSpeech.shutdown();
    }

    /**
     * 新建终端
     *
     * @return PeerConnectionFactory PeerConnectionFactory
     */
    public PeerConnectionFactory newClient() {
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
                this.initMedia();
                this.startVideoCapture();
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
            if (this.clientCount <= 0) {
                Log.i(MediaManager.class.getSimpleName(), "释放PeerConnectionFactory");
                // 注意顺序
                this.stopVideoCapture();
                this.closeMedia();
                this.nativeStop();
                this.stopPeerConnectionFactory();
            }
            return this.clientCount;
        }
    }

    private void initPeerConnectionFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this.context)
//              .setFieldTrials("WebRTC-IntelVP8/Enabled/")
//              .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
//              .setNativeLibraryName("jingle_peerconnection_so")
//              .setEnableInternalTracer(true)
                .createInitializationOptions()
        );
        this.eglBase = EglBase.create();
        this.eglContext = this.eglBase.getEglBaseContext();
    }

    private void stopPeerConnectionFactory() {
        if (this.eglBase != null) {
            this.eglBase.release();
            this.eglBase = null;
            this.eglContext = null;
        }
        PeerConnectionFactory.shutdownInternalTracer();
    }

    /**
     * 加载媒体
     */
    private void initMedia() {
        Log.i(MediaManager.class.getSimpleName(), "加载媒体：" + this.videoSourceType);
        final VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(this.eglContext);
        final VideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(this.eglContext, true, true);
        this.javaAudioDeviceModule = this.javaAudioDeviceModule();
        this.peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .setAudioDeviceModule(this.javaAudioDeviceModule)
//          .setAudioProcessingFactory()
//          .setAudioEncoderFactoryFactory(new BuiltinAudioEncoderFactoryFactory())
//          .setAudioDecoderFactoryFactory(new BuiltinAudioDecoderFactoryFactory())
            .createPeerConnectionFactory();
        Arrays.stream(videoEncoderFactory.getSupportedCodecs()).forEach(v -> {
            Log.d(MediaManager.class.getSimpleName(), "支持的视频解码器：" + v.name);
        });
        this.initAudio();
        this.initVideo();
        this.initWatermark();
    }

    private JavaAudioDeviceModule javaAudioDeviceModule() {
//      final AudioAttributes audioAttributes = new AudioAttributes.Builder()
//          .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
//          .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
//          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//          .build();
//        WebRtcAudioRecord.setOnAudioSamplesReady(audioSamples -> {
//            if(this.recordClient != null) {
//                this.recordClient.onWebRtcAudioRecordSamplesReady(audioSamples);
//            }
//        });
        final JavaAudioDeviceModule javaAudioDeviceModule = JavaAudioDeviceModule.builder(this.context)
//          .setSampleRate(48000)
//          .setSampleRate(mediaAudioProperties.getSampleRate())
//          .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT)
//          .setAudioSource(MediaRecorder.AudioSource.MIC)
//          .setAudioAttributes(audioAttributes)
            // 超低延迟
//          .setUseLowLatency()
//          .setUseStereoInput()
//          .setUseStereoOutput()
            // 本地声音
//          .setSamplesReadyCallback()
            .setAudioTrackErrorCallback(new JavaAudioDeviceModule.AudioTrackErrorCallback() {
                @Override
                public void onWebRtcAudioTrackInitError(String errorMessage) {
                    Log.e(MediaManager.class.getSimpleName(), "WebRTC远程音频Track加载异常：" + errorMessage);
                }
                @Override
                public void onWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                    Log.e(MediaManager.class.getSimpleName(), "WebRTC远程音频Track开始异常：" + errorMessage);
                }
                @Override
                public void onWebRtcAudioTrackError(String errorMessage) {
                    Log.e(MediaManager.class.getSimpleName(), "WebRTC远程音频Track异常：" + errorMessage);
                }
            })
            .setAudioTrackStateCallback(new JavaAudioDeviceModule.AudioTrackStateCallback() {
                @Override
                public void onWebRtcAudioTrackStart() {
                    Log.i(MediaManager.class.getSimpleName(), "WebRTC远程音频Track开始");
                }
                @Override
                public void onWebRtcAudioTrackStop() {
                    Log.i(MediaManager.class.getSimpleName(), "WebRTC远程音频Track结束");
                }
            })
            .setAudioRecordErrorCallback(new JavaAudioDeviceModule.AudioRecordErrorCallback() {
                @Override
                public void onWebRtcAudioRecordInitError(String errorMessage) {
                    Log.e(MediaManager.class.getSimpleName(), "WebRTC本地音频录像加载异常：" + errorMessage);
                }
                @Override
                public void onWebRtcAudioRecordStartError(JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                    Log.e(MediaManager.class.getSimpleName(), "WebRTC本地音频录像开始异常：" + errorMessage);
                }
                @Override
                public void onWebRtcAudioRecordError(String errorMessage) {
                    Log.e(MediaManager.class.getSimpleName(), "WebRTC本地音频录像异常：" + errorMessage);
                }
            })
            .setAudioRecordStateCallback(new JavaAudioDeviceModule.AudioRecordStateCallback() {
                @Override
                public void onWebRtcAudioRecordStart() {
                    Log.i(MediaManager.class.getSimpleName(), "WebRTC本地音频录像开始");
                }
                @Override
                public void onWebRtcAudioRecordStop() {
                    Log.i(MediaManager.class.getSimpleName(), "WebRTC本地音频录像结束");
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
        final MediaConstraints mediaConstraints = this.buildMediaConstraints();
        this.audioSource = this.peerConnectionFactory.createAudioSource(mediaConstraints);
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
            this.initScreenPromise();
        } else {
            // 其他来源
        }
    }

    /**
     * 加载文件采集
     */
    private void initFile() {
        // 自己实现
//      new FileVideoCapturer();
    }

    /**
     * 加载摄像头采集
     */
    private void initCamera() {
        final CameraEnumerator cameraEnumerator = new Camera2Enumerator(this.context);
        final String[] names = cameraEnumerator.getDeviceNames();
        for (String name : names) {
            if (this.videoSourceType == VideoSourceType.BACK && cameraEnumerator.isBackFacing(name)) {
                this.videoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
            } else if (this.videoSourceType == VideoSourceType.FRONT && cameraEnumerator.isFrontFacing(name)) {
                this.videoCapturer = cameraEnumerator.createCapturer(name, new MediaCameraEventsHandler());
            } else {
                // 忽略其他摄像头
            }
        }
        this.initVideoSource();
    }

    /**
     * 加载屏幕采集
     */
    private void initScreenPromise() {
        this.mainHandler.obtainMessage(Config.WHAT_SCREEN_CAPTURE).sendToTarget();
        synchronized (this.screenLock) {
            try {
                this.screenLock.wait();
            } catch (InterruptedException e) {
                Log.e(MediaManager.class.getSimpleName(), "等待录屏授权异常", e);
            }
        }
    }

    /**
     * 加载屏幕采集
     *
     * @param intent Intent
     */
    public void initScreen(Intent intent) {
        this.videoCapturer = new ScreenCapturerAndroid(intent, new ScreenCallback());
        this.initVideoSource();
        synchronized (this.screenLock) {
            this.screenLock.notifyAll();
        }
    }

    /**
     * 加载视频来源
     */
    private void initVideoSource() {
        // 加载视频
        this.surfaceTextureHelper = SurfaceTextureHelper.create("MediaVideoThread", this.eglContext);
        // 主码流
        this.mainVideoSource = this.peerConnectionFactory.createVideoSource(this.videoCapturer.isScreencast());
        // 次码流
        this.shareVideoSource = this.peerConnectionFactory.createVideoSource(this.videoCapturer.isScreencast());
        final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideo();
        this.shareVideoSource.adaptOutputFormat(mediaVideoProperties.getWidth(), mediaVideoProperties.getHeight(), mediaVideoProperties.getFrameRate());
        // 视频捕获
        this.videoCapturer.initialize(this.surfaceTextureHelper, this.context, new VideoCapturerObserver());
    }

    private void initWatermark() {
        if(StringUtils.isNotEmpty(this.watermark)) {
            final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideos().get(this.videoQuantity);
            if(this.videoProcesser == null) {
                this.videoProcesser = new WatermarkProcesser(this.watermark, mediaVideoProperties.getWidth(), mediaVideoProperties.getHeight());
            } else {
                this.videoProcesser = new WatermarkProcesser(this.watermark, mediaVideoProperties.getWidth(), mediaVideoProperties.getHeight(), this.videoProcesser);
            }
        }
    }

    public void muteAllRemote() {
        this.javaAudioDeviceModule.setSpeakerMute(true);
    }

    public void unmuteAllRemote() {
        this.javaAudioDeviceModule.setSpeakerMute(false);
    }

    public void muteAllLocal() {
        this.javaAudioDeviceModule.setMicrophoneMute(true);
    }

    public void unmuteAllLocal() {
        this.javaAudioDeviceModule.setMicrophoneMute(false);
    }

    /**
     * 更新配置
     *
     * @param mediaProperties 媒体配置
     */
    public void updateMediaConfig(MediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
        this.updateAudioConfig();
        this.updateVideoConfig();
        synchronized (this) {
            this.notifyAll();
        }
    }

    public void updateAudioConfig(MediaAudioProperties mediaAudioProperties) {
        this.mediaProperties.setAudio(mediaAudioProperties);
        this.updateAudioConfig();
    }

    private void updateAudioConfig() {
        MediaAudioProperties mediaAudioProperties = this.mediaProperties.getAudio();
        // TODO：调整音频
    }

    public void updateVideoConfig(MediaVideoProperties mediaVideoProperties) {
        this.mediaProperties.setVideo(mediaVideoProperties);
        this.updateVideoConfig();
    }

    private void updateVideoConfig() {
        if(this.videoCapturer != null) {
            final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideos().get(this.videoQuantity);
            this.videoCapturer.changeCaptureFormat(mediaVideoProperties.getWidth(), mediaVideoProperties.getHeight(), mediaVideoProperties.getFrameRate());
        }
        if(this.shareVideoSource != null) {
            final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideo();
            this.shareVideoSource.adaptOutputFormat(mediaVideoProperties.getWidth(), mediaVideoProperties.getHeight(), mediaVideoProperties.getFrameRate());
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
        final VideoSourceType oldVideoSourceType = this.videoSourceType;
        this.videoSourceType = videoSourceType;
        Log.i(MediaManager.class.getSimpleName(), "设置视频来源：" + this.videoSourceType);
        if (videoSourceType.isCamera() && oldVideoSourceType.isCamera()) {
            // TODO：测试是否需要完全重置
            final CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) this.videoCapturer;
            cameraVideoCapturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                @Override
                public void onCameraSwitchDone(boolean success) {
                    Log.d(MediaManager.class.getSimpleName(), "切换镜头成功");
                }
                @Override
                public void onCameraSwitchError(String message) {
                    Log.e(MediaManager.class.getSimpleName(), "切换镜头失败：" + message);
                }
            });
        } else {
//          this.initVideo();
//          切换所有VideoTrack视频来源
        }
    }

    public MediaStream buildLocalMediaStream(boolean audioProduce, boolean videoProduce) {
        final long id = Thread.currentThread().getId();
        final MediaStream mediaStream = this.peerConnectionFactory.createLocalMediaStream("TaoyaoM" + id);
        Log.i(MediaManager.class.getSimpleName(), "加载媒体：" + mediaStream.getId());
        if(audioProduce) {
            final AudioTrack audioTrack = this.peerConnectionFactory.createAudioTrack("TaoyaoA" + id, this.audioSource);
            audioTrack.setVolume(Config.DEFAULT_VOLUME);
            audioTrack.setEnabled(true);
            mediaStream.addTrack(audioTrack);
            Log.i(MediaManager.class.getSimpleName(), "加载音频：" + audioTrack.id());
        }
        if(videoProduce) {
            final VideoTrack videoTrack  = this.peerConnectionFactory.createVideoTrack("TaoyaoV" + id, this.shareVideoSource);
            videoTrack.setEnabled(true);
            Log.i(MediaManager.class.getSimpleName(), "加载视频（次码流）：" + videoTrack.id());
            mediaStream.addTrack(videoTrack);
        }
        return mediaStream;
    }

    public MediaConstraints buildMediaConstraints() {
        final MediaConstraints mediaConstraints = new MediaConstraints();
        // ================ PC ================ //
//      mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
//      mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        // ================ Audio ================ //
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
        // ================ Video ================ //
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", this.mediaProperties.getMinWidth().toString()));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", this.mediaProperties.getMaxWidth().toString()));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", this.mediaProperties.getMinHeight().toString()));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", this.mediaProperties.getMaxHeight().toString()));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", this.mediaProperties.getMinFrameRate().toString()));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", this.mediaProperties.getMaxFrameRate().toString()));
        // ================ SCTP ================ //
//      mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"));
        return mediaConstraints;
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
            this.videoCapturer.stopCapture();
        } catch (InterruptedException e) {
            Log.e(MediaManager.class.getSimpleName(), "关闭视频捕获异常", e);
        }
    }

    public String photograph() {
        synchronized (this) {
            final PhotographClient photographClient = new PhotographClient(this.imageQuantity, this.imagePath);
            if(this.clientCount <= 0) {
                final MediaVideoProperties mediaVideoProperties = this.mediaProperties.getVideos().get(this.videoQuantity);
                photographClient.photograph(mediaVideoProperties.getWidth(), mediaVideoProperties.getHeight(), mediaVideoProperties.getFrameRate(), this.videoSourceType, this.context);
            } else {
                photographClient.photograph(this.mainVideoSource, this.peerConnectionFactory);
            }
            return photographClient.waitForPhotograph();
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
            this.recordClient.record(this.mainVideoSource, this.javaAudioDeviceModule, this.peerConnectionFactory);
            this.mainHandler.obtainMessage(Config.WHAT_RECORD, Boolean.TRUE).sendToTarget();
            return this.recordClient;
        }
    }

    public String stopRecord() {
        synchronized (this) {
            if(this.recordClient == null) {
                return null;
            } else {
                final String filepath = this.recordClient.getFilepath();
                this.recordClient.close();
                this.recordClient = null;
                this.mainHandler.obtainMessage(Config.WHAT_RECORD, Boolean.FALSE).sendToTarget();
                return filepath;
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
        // 添加播放
        videoTrack.addSink(surfaceViewRenderer);
        // 页面加载
        this.mainHandler.obtainMessage(flag, surfaceViewRenderer).sendToTarget();
        this.mainHandler.post(() -> {
            // 视频反转
//          surfaceViewRenderer.setMirror(false);
            // 旋转画面
//          surfaceViewRenderer.setRotation(90);
            // 视频拉伸
//          surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            // 调整帧率
//          surfaceViewRenderer.setFpsReduction();
            // 硬件拉伸
//          surfaceViewRenderer.setEnableHardwareScaler(true);
            // 加载OpenSL ES
            surfaceViewRenderer.init(this.eglContext, null);
        });
        return surfaceViewRenderer;
    }

    private void closeMedia() {
        if(this.audioSource != null) {
            this.audioSource.dispose();
            this.audioSource = null;
        }
        if(this.mainVideoSource != null) {
            this.mainVideoSource.dispose();
            this.mainVideoSource = null;
        }
        if(this.shareVideoSource != null) {
            this.shareVideoSource.dispose();
            this.shareVideoSource = null;
        }
        if (this.videoCapturer != null) {
            this.videoCapturer.dispose();
            this.videoCapturer = null;
        }
        if(this.surfaceTextureHelper != null) {
            this.surfaceTextureHelper.dispose();
            this.surfaceTextureHelper = null;
        }
        if(this.javaAudioDeviceModule != null) {
            this.javaAudioDeviceModule.release();
            this.javaAudioDeviceModule = null;
        }
        if (this.peerConnectionFactory != null) {
            this.peerConnectionFactory.dispose();
            this.peerConnectionFactory = null;
        }
        if(this.videoProcesser != null) {
            this.videoProcesser.close();
            this.videoProcesser = null;
        }
    }

    /**
     * 视频捕获器观察者
     *
     * @author acgist
     */
    private class VideoCapturerObserver implements CapturerObserver {

        private CapturerObserver mainObserver;
        private CapturerObserver shareObserver;

        public VideoCapturerObserver() {
            this.mainObserver  = MediaManager.this.mainVideoSource.getCapturerObserver();
            this.shareObserver = MediaManager.this.shareVideoSource.getCapturerObserver();
        }

        @Override
        public void onCapturerStarted(boolean status) {
            Log.i(MediaManager.class.getSimpleName(), "开始视频捕获");
            this.mainObserver.onCapturerStarted(status);
            this.shareObserver.onCapturerStarted(status);
        }

        @Override
        public void onCapturerStopped() {
            Log.i(MediaManager.class.getSimpleName(), "结束视频捕获");
            this.mainObserver.onCapturerStopped();
            this.shareObserver.onCapturerStopped();
        }

        @Override
        public void onFrameCaptured(VideoFrame videoFrame) {
            // TODO：验证使用一个source，使用cropandscale缩放看看性能能否提升
            // 注意：VideoFrame必须释放，多线程环境需要调用retain和release方法。
            if(MediaManager.this.videoProcesser == null) {
                this.mainObserver.onFrameCaptured(videoFrame);
                this.shareObserver.onFrameCaptured(videoFrame);
            } else {
                final VideoFrame.I420Buffer i420Buffer = videoFrame.getBuffer().toI420();
                MediaManager.this.videoProcesser.process(i420Buffer);
                final VideoFrame processVideoFrame = new VideoFrame(
                    i420Buffer.cropAndScale(0, 0, i420Buffer.getWidth(), i420Buffer.getHeight(), i420Buffer.getWidth(), i420Buffer.getHeight()),
                    videoFrame.getRotation(),
                    videoFrame.getTimestampNs()
                );
                i420Buffer.release();
                this.mainObserver.onFrameCaptured(processVideoFrame);
                this.shareObserver.onFrameCaptured(processVideoFrame);
                processVideoFrame.release();
            }
        }

    }

    /**
     * 摄像头事件处理器
     *
     * @author acgist
     */
    private class MediaCameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

        @Override
        public void onFirstFrameAvailable() {
        }

        @Override
        public void onCameraOpening(String message) {
            Log.i(MediaManager.class.getSimpleName(), "打开摄像头");
        }

        @Override
        public void onCameraFreezed(String message) {
            Log.i(MediaManager.class.getSimpleName(), "释放摄像头：" + message);
        }

        @Override
        public void onCameraClosed() {
            Log.i(MediaManager.class.getSimpleName(), "关闭摄像头");
        }

        @Override
        public void onCameraDisconnected() {
            Log.i(MediaManager.class.getSimpleName(), "断开摄像头");
        }

        @Override
        public void onCameraError(String message) {
            Log.e(MediaManager.class.getSimpleName(), "摄像头异常：" + message);
        }

    }

    /**
     * 屏幕录像回调
     *
     * @author acgist
     */
    private class ScreenCallback extends MediaProjection.Callback {

        @Override
        public void onStop() {
            super.onStop();
            Log.i(MediaManager.class.getSimpleName(), "停止屏幕捕获");
        }

    }

    private class TextToSpeechInitListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
            Log.i(MediaManager.class.getSimpleName(), "加载语音播报：" + status);
            if(status == TextToSpeech.SUCCESS) {
                MediaManager.this.textToSpeech.setLanguage(Locale.CANADA);
                MediaManager.this.textToSpeech.setPitch(1.0F);
                MediaManager.this.textToSpeech.setSpeechRate(1.0F);
            }
        }
    }

    /**
     * 加载MediasoupClient
     */
    private native void nativeInit();

    /**
     * 关闭MediasoupClient
     */
    private native void nativeStop();

}
