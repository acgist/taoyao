package com.acgist.taoyao.media.client;

import android.graphics.YuvImage;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.acgist.taoyao.boot.utils.DateUtils;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.VideoSourceType;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrameDrawer;
import org.webrtc.VideoSink;
import org.webrtc.YuvHelper;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 录像机
 *
 * https://www.freesion.com/article/448330501/
 * https://blog.csdn.net/nanoage/article/details/127406494
 * https://webrtc.org.cn/20190419_tutorial3_webrtc_android
 * https://blog.csdn.net/CSDN_Mew/article/details/103406781
 * https://blog.csdn.net/Tong_Hou/article/details/112116349
 * https://blog.csdn.net/u011418943/article/details/127108642
 * https://blog.csdn.net/m0_60259116/article/details/126875532
 * https://blog.csdn.net/csdn_shen0221/article/details/119982257
 * https://blog.csdn.net/csdn_shen0221/article/details/120331004
 * https://github.com/flutter-webrtc/flutter-webrtc/blob/main/android/src/main/java/com/cloudwebrtc/webrtc/record/VideoFileRenderer.java
 *
 * @author acgist
 */
public class RecordClient extends Client implements VideoSink, JavaAudioDeviceModule.SamplesReadyCallback {

    /**
     * 音频准备录制
     */
    private volatile boolean audioActive;
    /**
     * 视频准备录制
     */
    private volatile boolean videoActive;
    /**
     * 录制文件名称
     */
    private final String filename;
    /**
     * 录制文件路径
     */
    private final String filepath;
    /**
     * 音频编码
     */
    private MediaCodec audioCodec;
    /**
     * 音频处理线程
     */
    private HandlerThread audioThread;
    /**
     * 音频Handler
     */
    private Handler audioHandler;
    /**
     * 视频编码
     */
    private MediaCodec videoCodec;
    /**
     * 视频处理线程
     */
    private HandlerThread videoThread;
    /**
     * 视频Handler
     */
    private Handler videoHandler;
    /**
     * 媒体合成器
     */
    private MediaMuxer mediaMuxer;
    private final BlockingQueue<JavaAudioDeviceModule.AudioSamples> audioSamplesQueue;
    private final BlockingQueue<VideoFrame> videoFrameQueue;

    public RecordClient(String path, ITaoyao taoyao, Handler mainHandler) {
        super("本地录像", "LocalRecordClient", taoyao, mainHandler);
        this.filename = DateUtils.format(LocalDateTime.now(), DateUtils.DateTimeStyle.YYYYMMDDHH24MMSS) + ".mp4";
        this.filepath = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), path, this.filename).toString();
        this.audioSamplesQueue = new LinkedBlockingQueue<>();
        this.videoFrameQueue = new LinkedBlockingQueue<>();
    }

    public void start() {
        synchronized (this) {
            if(this.init) {
                return;
            }
            Log.i(RecordClient.class.getSimpleName(), "录制视频文件：" + this.filepath);
            super.init();
            this.mediaManager.newClient(VideoSourceType.BACK);
            this.record(null, null, 1, 1);
        }
    }

    private void record(String audioFormat, String videoFormat, int width, int height) {
        if (
            this.audioThread == null || !this.audioThread.isAlive() ||
            this.videoThread == null || !this.videoThread.isAlive()
        ) {
            this.initMediaMuxer();
            this.initAudioThread(MediaFormat.MIMETYPE_AUDIO_AAC, 96000, 44100, 1);
            this.initVideoThread(MediaFormat.MIMETYPE_VIDEO_AVC, 2500 * 1000, 30, 1, 1920, 1080);
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
            final MediaFormat audioFormat = MediaFormat.createAudioFormat(audioType, sampleRate, channelCount);
//          audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AudioFormat.ENCODING_PCM_16BIT);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//            audioFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8 * 1024);
            this.audioCodec = MediaCodec.createEncoderByType(audioType);
            this.audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.e(RecordClient.class.getSimpleName(), "加载音频录制线程异常", e);
        }
        this.audioThread = new HandlerThread("AudioRecoderThread");
        this.audioThread.start();
        this.audioHandler = new Handler(this.audioThread.getLooper());
        this.audioHandler.post(this::audioCodec);
    }

    private void audioCodec() {
        int trackIndex = -1;
        int outputIndex;
        long pts = 0L;
        this.audioCodec.start();
        this.audioActive = true;
        JavaAudioDeviceModule.AudioSamples audioSamples = null;
        final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!this.close) {


            try {
                audioSamples = this.audioSamplesQueue.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.e(RecordClient.class.getSimpleName(), "录制线程等待异常", e);
            }
            if(audioSamples == null) {
                continue;
            }
            int index = this.audioCodec.dequeueInputBuffer(1000L * 1000);
            if (index >= 0) {
                final byte[] data = audioSamples.getData();
                final ByteBuffer buffer = this.audioCodec.getInputBuffer(index);
                buffer.put(data);
                this.audioCodec.queueInputBuffer(index, 0, data.length, this.audioPts, 0);
                this.audioPts += data.length * 125 / 12; // 1000000 microseconds / 48000hz / 2 bytes
//                presTime += data.length * 125 / 12; // 1000000 microseconds / 48000hz / 2 bytes
//                presTime += data.length * (1_000_000 / audioSamples.getSampleRate() / 2); //16位最后那个数字是2，8位是1
            } else {
                // WARN
            }
            audioSamples = null;


            outputIndex = this.audioCodec.dequeueOutputBuffer(bufferInfo, 1000L * 1000);
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//          } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                synchronized (this) {
                    trackIndex = this.mediaMuxer.addTrack(this.audioCodec.getOutputFormat());
                    Log.i(RecordClient.class.getSimpleName(), "开始录制音频：" + trackIndex);
                    if (this.videoActive) {
                        Log.i(RecordClient.class.getSimpleName(), "开始录制文件：" + this.filename);
                        this.mediaMuxer.start();
                        this.notifyAll();
                    } else if (!this.close) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            Log.e(RecordClient.class.getSimpleName(), "录制线程等待异常", e);
                        }
                    }
                }
            } else if (outputIndex >= 0) {
                if(pts == 0L) {
                    pts = bufferInfo.presentationTimeUs;
                }
                final ByteBuffer outputBuffer = this.audioCodec.getOutputBuffer(outputIndex);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                bufferInfo.presentationTimeUs -= pts;
//              this.mediaMuxer.writeSampleData(trackIndex, outputBuffer, info);
                this.audioCodec.releaseOutputBuffer(outputIndex, false);
                Log.d(RecordClient.class.getSimpleName(), "录制音频帧（时间戳）：" + bufferInfo.flags + " - " + (bufferInfo.presentationTimeUs / 1_000_000F));
//              if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME == MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) {
//              }
                if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//                  this.close();
                    break;
                }
            } else {

            }
        }
        synchronized (this) {
            if (this.audioCodec != null) {
                Log.i(RecordClient.class.getSimpleName(), "结束录制音频");
                this.audioCodec.stop();
                this.audioCodec.release();
                this.audioCodec = null;
            }
            this.audioActive = false;
            if (this.mediaMuxer != null && !this.videoActive) {
                Log.i(RecordClient.class.getSimpleName(), "结束录制文件：" + this.filename);
                this.mediaMuxer.stop();
                this.mediaMuxer.release();
                this.mediaMuxer = null;
            }
        }
    }

    private volatile long audioPts = 0;

    /**
     * @param audioSamples PCM数据
     */
    public void putAudio(JavaAudioDeviceModule.AudioSamples audioSamples) {
        if(this.close || !this.audioActive) {
            return;
        }
        Log.i(RecordClient.class.getSimpleName(), "音频信息：" + audioSamples.getAudioFormat());
        try {
            this.audioSamplesQueue.put(audioSamples);
        } catch (InterruptedException e) {
            Log.e(RecordClient.class.getSimpleName(), "录制线程等待异常", e);
        }
    }

    /**
     * @param videoType      视频格式
     * @param bitRate        比特率：800 * 1000 | 1600 * 1000 | 2500 * 1000
     * @param frameRate      帧率：30
     * @param iFrameInterval 关键帧频率：1 ~ 5
     * @param width          宽度：1920
     * @param height         高度：1080
     */
    private void initVideoThread(String videoType, int bitRate, int frameRate, int iFrameInterval, int width, int height) {
        try {
            final MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080);
//          videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
//          videoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
//            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1920 * 1080 * 5);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000);
//          videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800 * 1000);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
          videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            this.videoCodec = MediaCodec.createEncoderByType(videoType);
            this.videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.e(RecordClient.class.getSimpleName(), "加载视频录制线程异常", e);
        }
        this.videoThread = new HandlerThread("VideoRecoderThread");
        this.videoThread.start();
        this.videoHandler = new Handler(this.videoThread.getLooper());
        this.videoHandler.post(this::videoCodec);
    }

    private void videoCodec() {
        int trackIndex = -1;
        int outputIndex;
        long pts = 0L;

        this.videoCodec.start();
        this.videoActive = true;
        final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        VideoFrame videoFrame = null;
        while (!this.close) {
            try {
                videoFrame = this.videoFrameQueue.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.e(RecordClient.class.getSimpleName(), "录制线程等待异常", e);
            }
            if(videoFrame == null) {
                continue;
            }


            final TextureBufferImpl buffer = (TextureBufferImpl) videoFrame.getBuffer();
            final int outputFrameSize = videoFrame.getRotatedWidth() * videoFrame.getRotatedHeight() * 3 / 2;
//            final ByteBuffer outputFrameBuffer = ByteBuffer.allocateDirect(outputFrameSize);
            final int index = this.videoCodec.dequeueInputBuffer(1000L * 1000);
            VideoFrame.I420Buffer i420 = buffer.toI420();
            final ByteBuffer bufferx = this.videoCodec.getInputBuffer(index);
//            YuvHelper.I420Copy(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), outputFrameBuffer, i420.getWidth(), i420.getHeight());
//            YuvHelper.I420Rotate(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), outputFrameBuffer, i420.getWidth(), i420.getHeight(), videoFrame.getRotation());
            YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), bufferx, i420.getWidth(), i420.getHeight());
            i420.release();
            videoFrame.release();
//            bufferx.put(outputFrameBuffer.array());
            this.videoCodec.queueInputBuffer(index, 0, outputFrameSize, videoFrame.getTimestampNs(), 0);


//            this.videoFrameDrawer.drawFrame(videoFrame, this.glRectDrawer, null, 0, 0, videoFrame.getRotatedWidth(), videoFrame.getRotatedHeight());
//            videoFrame.release();
//            videoFrame = null;
//            this.eglBase.swapBuffers();
            outputIndex = this.videoCodec.dequeueOutputBuffer(bufferInfo, 1000L * 1000);
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//          } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                synchronized (this) {
                    trackIndex = this.mediaMuxer.addTrack(this.videoCodec.getOutputFormat());
                    Log.i(RecordClient.class.getSimpleName(), "开始录制视频：" + trackIndex);
                    if (this.audioActive) {
                        Log.i(RecordClient.class.getSimpleName(), "开始录制文件：" + this.filename);
                        this.mediaMuxer.start();
                        this.notifyAll();
                    } else if (!this.close) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            Log.e(RecordClient.class.getSimpleName(), "录制线程等待异常", e);
                        }
                    }
                }
            } else if (outputIndex >= 0) {
                if(pts == 0L) {
                    pts = bufferInfo.presentationTimeUs / 1000;
                }
                final ByteBuffer outputBuffer = this.videoCodec.getOutputBuffer(outputIndex);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                bufferInfo.presentationTimeUs /= 1000;
                bufferInfo.presentationTimeUs -= pts;
                this.mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                this.videoCodec.releaseOutputBuffer(outputIndex, false);
                Log.d(RecordClient.class.getSimpleName(), "录制视频帧（时间戳）：" + bufferInfo.flags + " - " + (bufferInfo.presentationTimeUs / 1_000_000F));
//              if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME == MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) {
//              }
                if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//                  this.close();
                    break;
                }
            } else {
            }
        }
        synchronized (this) {
            if (this.videoCodec != null) {
                Log.i(RecordClient.class.getSimpleName(), "结束录制视频");
                this.videoCodec.stop();
                this.videoCodec.release();
                this.videoCodec = null;
            }
            this.videoActive = false;
            if (this.mediaMuxer != null && !this.audioActive) {
                Log.i(RecordClient.class.getSimpleName(), "结束录制文件：" + this.filename);
                this.mediaMuxer.stop();
                this.mediaMuxer.release();
                this.mediaMuxer = null;
            }
        }
    }

    public void putVideo(VideoFrame videoFrame) {
        if (this.close || !this.videoActive) {
            return;
        }
        Log.i(RecordClient.class.getSimpleName(), "视频信息：" + videoFrame.getRotatedWidth() + " - " + videoFrame.getRotatedHeight());
        try {
            this.videoFrameQueue.put(videoFrame);
        } catch (InterruptedException e) {
            Log.e(RecordClient.class.getSimpleName(), "录制线程等待异常", e);
        }
    }

    private void initMediaMuxer() {
        try {
            this.mediaMuxer = new MediaMuxer(
                this.filepath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            );
            // 设置方向
//          this.mediaMuxer.setOrientationHint();
        } catch (IOException e) {
            Log.e(MediaManager.class.getSimpleName(), "加载媒体合成器异常", e);
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if(this.close) {
                return;
            }
            super.close();
            Log.i(RecordClient.class.getSimpleName(), "结束录制：" + this.filepath);
            if (audioThread != null) {
                this.audioThread.quitSafely();
            }
            if (this.videoThread != null) {
                this.videoThread.quitSafely();
            }
            this.notifyAll();
            this.mediaManager.closeClient();
        }
    }

    public String getFilename() {
        return this.filename;
    }

    public String getFilepath() {
        return this.filepath;
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
    }

    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples audioSamples) {
    }

}
