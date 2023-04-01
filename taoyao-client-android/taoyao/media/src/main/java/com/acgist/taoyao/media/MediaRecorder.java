package com.acgist.taoyao.media;

import android.graphics.YuvImage;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrameDrawer;
import org.webrtc.VideoSink;
import org.webrtc.YuvConverter;
import org.webrtc.YuvHelper;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 录像机
 * <p>
 * https://blog.csdn.net/m0_60259116/article/details/126875532
 *
 * @author acgist
 */
public final class MediaRecorder {

    private static final MediaRecorder INSTANCE = new MediaRecorder();

    public static final MediaRecorder getInstance() {
        return INSTANCE;
    }

    /**
     * 是否正在录像
     */
    private volatile boolean active;
    private volatile boolean audioActive;
    private volatile boolean videoActive;
    private volatile long pts;
    private String file;
    /**
     * 音频编码
     */
    private MediaCodec audioCodec;
    private HandlerThread audioThread;
    /**
     * 视频编码
     */
    private MediaCodec videoCodec;
    private HandlerThread videoThread;
    private Handler videoHandler;
    private ExecutorService executorService;
    /**
     * 媒体合成器
     */
    private MediaMuxer mediaMuxer;
    /**
     * 音频录制
     */
    public final JavaAudioDeviceModule.SamplesReadyCallback audioRecoder;
    /**
     * 视频录制
     */
    public final VideoSink videoRecoder;

    private MediaRecorder() {
        final MediaCodecList mediaCodecList = new MediaCodecList(-1);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
            if (mediaCodecInfo.isEncoder()) {
                final String[] supportedTypes = mediaCodecInfo.getSupportedTypes();
                Log.d(MediaRecorder.class.getSimpleName(), "编码器名称：" + mediaCodecInfo.getName());
                Log.d(MediaRecorder.class.getSimpleName(), "编码器类型：" + String.join(" , ", supportedTypes));
                for (String supportType : supportedTypes) {
                    final MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType(supportType);
                    final int[] colorFormats = codecCapabilities.colorFormats;
                    Log.d(MediaRecorder.class.getSimpleName(), "编码器格式：" + codecCapabilities.getMimeType());
//                  MediaCodecInfo.CodecCapabilities.COLOR_*
                    Log.d(MediaRecorder.class.getSimpleName(), "编码器支持格式：" + IntStream.of(colorFormats).boxed().map(String::valueOf).collect(Collectors.joining(" , ")));
                }
            }
        }
        this.executorService = Executors.newFixedThreadPool(2);
        this.audioRecoder = audioSamples -> {
        };
        this.videoRecoder = videoFrame -> {
            if (this.active && this.videoActive) {
                this.executorService.submit(() -> {
//                    videoFrame.retain();
                    final int outputFrameSize = videoFrame.getRotatedWidth() * videoFrame.getRotatedHeight() * 3 / 2;
                    final ByteBuffer outputFrameBuffer = ByteBuffer.allocateDirect(outputFrameSize);
                        final int index = this.videoCodec.dequeueInputBuffer(1000L * 1000);
//                      YuvImage：截图
                        // YV12
                        VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
//                      i420.retain();
                    Log.i(MediaRecorder.class.getSimpleName(), "视频信息：" + videoFrame.getRotatedWidth() + " - " + videoFrame.getRotatedHeight());
//                      YuvHelper.I420Copy(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), outputFrameBuffer, i420.getWidth(), i420.getHeight());
                    // NV12
                    YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), outputFrameBuffer, i420.getWidth(), i420.getHeight());
//                    YuvHelper.I420Rotate(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), outputFrameBuffer, i420.getWidth(), i420.getHeight(), videoFrame.getRotation());
                    final ByteBuffer x = this.videoCodec.getInputBuffer(index);
//                      i420.release();
                        x.put(outputFrameBuffer.array());
                        this.videoCodec.queueInputBuffer(index, 0, outputFrameSize, System.currentTimeMillis(), 0);
//                      this.putVideo(outputFrameBuffer, System.currentTimeMillis());
//                      videoFrame.release();
                });
            }
        };
    }

    /**
     * @return 是否正在录像
     */
    public boolean isActive() {
        return this.active;
    }

    public void init(String file, String audioFormat, String videoFormat, int width, int height) {
        synchronized (MediaRecorder.INSTANCE) {
            this.file = file;
            this.active = true;
            if (
                this.audioThread == null || !this.audioThread.isAlive() ||
                    this.videoThread == null || !this.videoThread.isAlive()
            ) {
                this.initMediaMuxer(file);
                this.initAudioThread(MediaFormat.MIMETYPE_AUDIO_AAC, 96000, 44100, 1);
                this.initVideoThread(MediaFormat.MIMETYPE_VIDEO_AVC, 2500 * 1000, 30, 1, 1920, 1080);
            }
        }
    }

    /**
     * @param audioType    类型
     * @param bitRate      比特率：96 * 1000 | 128 * 1000 | 256 * 1000
     * @param sampleRate   采样率：32000 | 44100 | 48000
     * @param channelCount 通道数量
     */
    private void initAudioThread(String audioType, int bitRate, int sampleRate, int channelCount) {
        try {
            this.audioCodec = MediaCodec.createEncoderByType(audioType);
            final MediaFormat audioFormat = MediaFormat.createAudioFormat(audioType, sampleRate, channelCount);
//          audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AudioFormat.ENCODING_PCM_16BIT);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8 * 1024);
            this.audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.e(MediaRecorder.class.getSimpleName(), "加载音频录制线程异常", e);
        }
        final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        this.audioThread = new HandlerThread("AudioRecoderThread");
        this.audioThread.start();
        final Handler audioHandler = new Handler(this.audioThread.getLooper());
        audioHandler.post(() -> {
            int trackIndex = -1;
            int outputIndex;
            this.audioCodec.start();
            this.audioActive = true;
            while (this.active) {
                outputIndex = this.audioCodec.dequeueOutputBuffer(info, 1000L * 1000);
                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized (MediaRecorder.INSTANCE) {
                        trackIndex = this.mediaMuxer.addTrack(this.audioCodec.getOutputFormat());
                        Log.i(MediaRecorder.class.getSimpleName(), "开始录制音频：" + trackIndex);
                        if (this.videoActive) {
                            Log.i(MediaRecorder.class.getSimpleName(), "开始录制文件：" + this.file);
                            this.pts = System.currentTimeMillis();
                            this.mediaMuxer.start();
                            MediaRecorder.INSTANCE.notifyAll();
                        } else if (this.active) {
                            try {
                                MediaRecorder.INSTANCE.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                } else if (outputIndex >= 0) {
                    final ByteBuffer outputBuffer = this.audioCodec.getOutputBuffer(outputIndex);
                    outputBuffer.position(info.offset);
                    outputBuffer.limit(info.offset + info.size);
                    info.presentationTimeUs = (info.presentationTimeUs - this.pts) * 1000;
//                    this.mediaMuxer.writeSampleData(trackIndex, outputBuffer, info);
                    this.audioCodec.releaseOutputBuffer(outputIndex, false);
                }
            }
            synchronized (MediaRecorder.INSTANCE) {
                if (this.audioCodec != null) {
                    Log.i(MediaRecorder.class.getSimpleName(), "结束录制音频");
                    this.audioCodec.stop();
                    this.audioCodec.release();
                    this.audioCodec = null;
                }
                this.audioActive = false;
                if (this.mediaMuxer != null && !this.videoActive) {
                    Log.i(MediaRecorder.class.getSimpleName(), "结束录制文件：" + this.file);
                    this.mediaMuxer.stop();
                    this.mediaMuxer.release();
                    this.mediaMuxer = null;
                }
            }
        });
    }

    public void putAudio(byte[] bytes) {

    }

    /**
     * @param videoType      视频格式
     * @param bitRate        比特率：800 * 1000 | 1600 * 1000 | 2500 * 1000
     * @param frameRate      帧率：30
     * @param iFrameInterval 关键帧频率：1
     * @param width          宽度：1920
     * @param height         高度：1080
     */
    private void initVideoThread(String videoType, int bitRate, int frameRate, int iFrameInterval, int width, int height) {
        try {
            this.videoCodec = MediaCodec.createEncoderByType(videoType);
            final MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 360, 480);
//            videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
//            videoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800 * 1000);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            this.videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.e(MediaRecorder.class.getSimpleName(), "加载视频录制线程异常", e);
        }
        final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        this.videoThread = new HandlerThread("VideoRecoderThread");
        this.videoThread.start();
        this.videoHandler = new Handler(this.videoThread.getLooper());
        this.videoHandler.post(() -> {
            int trackIndex = -1;
            int outputIndex;
            this.videoCodec.start();
            this.videoActive = true;
            while (this.active) {
                outputIndex = this.videoCodec.dequeueOutputBuffer(info, 1000L * 1000);
                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized (MediaRecorder.INSTANCE) {
                        trackIndex = this.mediaMuxer.addTrack(this.videoCodec.getOutputFormat());
                        Log.i(MediaRecorder.class.getSimpleName(), "开始录制视频：" + trackIndex);
                        if (this.audioActive) {
                            Log.i(MediaRecorder.class.getSimpleName(), "开始录制文件：" + this.file);
                            this.pts = System.currentTimeMillis();
                            this.mediaMuxer.start();
                            MediaRecorder.INSTANCE.notifyAll();
                        } else if (this.active) {
                            try {
                                MediaRecorder.INSTANCE.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                } else if (outputIndex >= 0) {
                    final ByteBuffer outputBuffer = this.videoCodec.getOutputBuffer(outputIndex);
                    outputBuffer.position(info.offset);
                    outputBuffer.limit(info.offset + info.size);
                    info.presentationTimeUs = (info.presentationTimeUs - this.pts) * 1000;
                    this.mediaMuxer.writeSampleData(trackIndex, outputBuffer, info);
                    this.videoCodec.releaseOutputBuffer(outputIndex, false);
                    Log.d(MediaRecorder.class.getSimpleName(), "录制视频帧（时间戳）：" + (info.presentationTimeUs / 1000000F));
//                  if(info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//                  } else if(info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//                  } else if(info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//                  } else if(info.flags == MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) {
//                  }
                }
            }
            synchronized (MediaRecorder.INSTANCE) {
                if (this.videoCodec != null) {
                    Log.i(MediaRecorder.class.getSimpleName(), "结束录制视频");
                    this.videoCodec.stop();
                    this.videoCodec.release();
                    this.videoCodec = null;
                }
                this.videoActive = false;
                if (this.mediaMuxer != null && !this.audioActive) {
                    Log.i(MediaRecorder.class.getSimpleName(), "结束录制文件：" + this.file);
                    this.mediaMuxer.stop();
                    this.mediaMuxer.release();
                    this.mediaMuxer = null;
                }
            }
        });
    }

    public void putVideo(ByteBuffer buffer, long pts) {
        while (this.active && this.videoActive) {
            final int index = this.videoCodec.dequeueInputBuffer(1000L * 1000);
            if (index < 0) {
                continue;
            }
            final ByteBuffer byteBuffer = this.videoCodec.getInputBuffer(index);
            byteBuffer.put(buffer);
            this.videoCodec.queueInputBuffer(index, 0, buffer.capacity(), pts, 0);
        }
    }

    private void initMediaMuxer(String file) {
        try {
            this.mediaMuxer = new MediaMuxer(
                Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath(), file).toAbsolutePath().toString(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            );
            // 设置方向
//          this.mediaMuxer.setOrientationHint();
        } catch (IOException e) {
            Log.e(MediaManager.class.getSimpleName(), "加载媒体合成器异常", e);
        }
    }

    public void stop() {
        synchronized (MediaRecorder.INSTANCE) {
            Log.i(MediaRecorder.class.getSimpleName(), "结束录制：" + this.file);
            this.active = false;
            if (audioThread != null) {
                this.audioThread.quitSafely();
                this.audioThread = null;
            }
            if (this.videoThread != null) {
                this.videoThread.quitSafely();
                this.videoThread = null;
            }
            if(this.executorService != null) {
                this.executorService.shutdown();
                this.executorService = null;
            }
            MediaRecorder.INSTANCE.notifyAll();
        }
    }

}
