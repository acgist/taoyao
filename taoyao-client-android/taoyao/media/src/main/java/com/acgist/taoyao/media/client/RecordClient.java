package com.acgist.taoyao.media.client;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.acgist.taoyao.boot.utils.DateUtils;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.audio.MixerProcesser;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.YuvHelper;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 录像机
 *
 * @author acgist
 */
public class RecordClient extends Client implements VideoSink {

    /**
     * 等待时间（毫秒）
     */
    private static final long WAIT_TIME_MS = 50;
    /**
     * 等待时间（纳秒）
     */
    private static final long WAIT_TIME_US = WAIT_TIME_MS * 1000;

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
     * 比特率：96 * 1000 | 128 * 1000 | 256 * 1000
     * 比特率：96 * 1024 | 128 * 1024 | 256 * 1024
     */
    private final int audioBitRate;
    /**
     * 采样率：32000 | 44100 | 48000
     */
    private final int sampleRate;
    /**
     * 通道数量：1 | 2
     */
    private final int channelCount;
    /**
     * 比特率：800 * 1000 | 1600 * 1000 | 2500 * 1000
     * 比特率：800 * 1024 | 1600 * 1024 | 2500 * 1024
     */
    private final int videoBitRate;
    /**
     * 帧率：15 | 20 | 25 | 30
     */
    private final int frameRate;
    /**
     * 关键帧频率：1 ~ 5
     */
    private final int iFrameInterval;
    /**
     * 宽度：1920
     */
    private final int width;
    /**
     * 高度：1080
     */
    private final int height;
    /**
     * YUV数据大小
     */
    private final int yuvSize;
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
    private VideoTrack videoTrack;
    private MixerProcesser mixerProcesser;
    private JavaAudioDeviceModule javaAudioDeviceModule;

    public RecordClient(
        int audioBitRate, int sampleRate, int channelCount,
        int videoBitRate, int frameRate,  int iFrameInterval,
        int width, int height,
        String path, ITaoyao taoyao, Handler mainHandler
    ) {
        super("本地录像", "LocalRecordClient", taoyao, mainHandler);
        this.audioBitRate   = audioBitRate * 1024;
        this.sampleRate     = sampleRate;
        this.channelCount   = channelCount;
        this.videoBitRate   = videoBitRate * 1024;
        this.frameRate      = frameRate;
        this.iFrameInterval = iFrameInterval;
        this.width          = width;
        this.height         = height;
        this.yuvSize        = width * height * 3 / 2;
        this.filename       = DateUtils.format(LocalDateTime.now(), DateUtils.DateTimeStyle.YYYYMMDDHH24MMSS) + ".mp4";
        this.filepath       = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath(), path, this.filename).toString();
        this.audioActive    = false;
        this.videoActive    = false;
    }

    public void start() {
        synchronized (this) {
            if(this.init) {
                return;
            }
            Log.i(RecordClient.class.getSimpleName(), "录制视频文件：" + this.filepath);
            super.init();
            this.mediaManager.newClient();
            if (
                this.audioThread == null || !this.audioThread.isAlive() ||
                this.videoThread == null || !this.videoThread.isAlive()
            ) {
                this.initMediaMuxer();
                this.initAudioThread(MediaFormat.MIMETYPE_AUDIO_AAC);
                this.initVideoThread(MediaFormat.MIMETYPE_VIDEO_AVC);
            }
        }
    }

    /**
     * @param audioType 类型
     */
    private void initAudioThread(String audioType) {
        try {
            final MediaFormat audioFormat = MediaFormat.createAudioFormat(audioType, this.sampleRate, this.channelCount);
//          audioFormat.setString(MediaFormat.KEY_MIME, audioType);
//          audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, this.sampleRate);
//          audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, this.channelCount);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, this.audioBitRate);
//          audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AudioFormat.ENCODING_PCM_16BIT);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//          audioFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
//          audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 8 * 8); // 设置缓冲大小
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
        long pts       = 0L;
        int trackIndex = -1;
        int outputIndex;
        this.audioCodec.start();
        this.audioActive = true;
        final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!this.close) {
            outputIndex = this.audioCodec.dequeueOutputBuffer(bufferInfo, WAIT_TIME_US);
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//          } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                synchronized (this) {
                    trackIndex = this.mediaMuxer.addTrack(this.audioCodec.getOutputFormat());
                    Log.i(RecordClient.class.getSimpleName(), "开始录制音频：" + trackIndex);
                    if (!this.close && this.videoActive) {
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
                this.mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                this.audioCodec.releaseOutputBuffer(outputIndex, false);
                Log.d(RecordClient.class.getSimpleName(), "录制音频帧（时间戳）：" + bufferInfo.flags + " - " + (bufferInfo.presentationTimeUs / 1_000_000F));
//              if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME == MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) {
//              }
                if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    this.close();
                    break;
                }
            } else {
                // WARN
            }
        }
        synchronized (this) {
            if (this.audioCodec != null && this.audioActive) {
                Log.i(RecordClient.class.getSimpleName(), "结束录制音频");
                this.audioCodec.stop();
                this.audioCodec.release();
                this.audioCodec = null;
            }
            this.audioActive = false;
            if (this.mediaMuxer != null && !this.videoActive) {
                Log.i(RecordClient.class.getSimpleName(), "结束录制文件：" + this.filename);
//              this.mediaMuxer.stop();
                this.mediaMuxer.release();
                this.mediaMuxer = null;
            }
        }
    }

    /**
     * @param videoType 视频格式
     */
    private void initVideoThread(String videoType) {
        try {
            final MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, this.width, this.height);
//          videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
//          videoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, this.videoBitRate);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, this.frameRate);
//          videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, this.iFrameInterval);
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
        long pts        = 0L;
        int trackIndex  = -1;
        int outputIndex;
        this.videoCodec.start();
        this.videoActive = true;
        final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!this.close) {
            outputIndex = this.videoCodec.dequeueOutputBuffer(bufferInfo, WAIT_TIME_US);
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//          } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                synchronized (this) {
                    trackIndex = this.mediaMuxer.addTrack(this.videoCodec.getOutputFormat());
                    Log.i(RecordClient.class.getSimpleName(), "开始录制视频：" + trackIndex);
                    if (!this.close && this.audioActive) {
                        Log.i(RecordClient.class.getSimpleName(), "开始录制文件：" + this.filename);
                        this.mediaMuxer.start();
                        this.notifyAll();
                    } else if (!this.close) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            Log.e(RecordClient.class.getSimpleName(), "录制线程等待异常", e);
                        }
                    } else {
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
                Log.d(RecordClient.class.getSimpleName(), "录制视频帧（时间戳）：" + (bufferInfo.presentationTimeUs / 1_000_000F));
//              if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//              } else if (bufferInfo.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME == MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) {
//              }
                if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    this.close();
                    break;
                }
            } else {
                // WARN
            }
        }
        synchronized (this) {
            if (this.videoCodec != null && this.videoActive) {
                Log.i(RecordClient.class.getSimpleName(), "结束录制视频");
                this.videoCodec.stop();
                this.videoCodec.release();
                this.videoCodec = null;
            }
            this.videoActive = false;
            if (this.mediaMuxer != null && !this.audioActive) {
                Log.i(RecordClient.class.getSimpleName(), "结束录制文件：" + this.filename);
//              this.mediaMuxer.stop();
                this.mediaMuxer.release();
                this.mediaMuxer = null;
            }
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

    public void record(VideoSource videoSource, JavaAudioDeviceModule javaAudioDeviceModule, PeerConnectionFactory peerConnectionFactory) {
        // 音频
        if(javaAudioDeviceModule != null) {
            this.javaAudioDeviceModule = javaAudioDeviceModule;
            this.mixerProcesser = new MixerProcesser(this.sampleRate, this.channelCount, this);
            this.mixerProcesser.start();
            this.javaAudioDeviceModule.setMixerProcesser(this.mixerProcesser);
        }
        // 视频
        if(videoSource != null && peerConnectionFactory != null) {
            this.videoTrack = peerConnectionFactory.createVideoTrack("TaoyaoVR", videoSource);
            this.videoTrack.setEnabled(true);
            this.videoTrack.addSink(this);
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
            if(this.javaAudioDeviceModule != null) {
                this.javaAudioDeviceModule.removeMixerProcesser();
            }
            if(this.mixerProcesser != null) {
                this.mixerProcesser.close();
                this.mixerProcesser = null;
            }
            if(this.videoTrack != null) {
                this.videoTrack.removeSink(this);
                this.videoTrack.dispose();
                this.videoTrack = null;
            }
            if (this.audioThread != null) {
                this.audioThread.quitSafely();
                this.audioThread = null;
            }
            if (this.videoThread != null) {
                this.videoThread.quitSafely();
                this.videoThread = null;
            }
            final File file = new File(this.filepath);
            if(file.length() <= 0) {
                Log.i(RecordClient.class.getSimpleName(), "删除没有录制数据文件：" + this.filepath);
                file.delete();
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

    /**
     * @param pts  PTS时间偏移
     * @param data PCM数据
     */
    public void onPcm(long pts, byte[] data) {
        if(this.close || !this.audioActive) {
            return;
        }
        final int index = this.audioCodec.dequeueInputBuffer(WAIT_TIME_US);
        if (index < 0) {
            return;
        }
        final ByteBuffer buffer = this.audioCodec.getInputBuffer(index);
        buffer.put(data);
        this.audioCodec.queueInputBuffer(index, 0, data.length, pts, 0);
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
        if (this.close || !this.videoActive) {
            return;
        }
//      Log.d(RecordClient.class.getSimpleName(), "视频信息：" + videoFrame.getRotatedWidth() + " - " + videoFrame.getRotatedHeight());
        final int index = this.videoCodec.dequeueInputBuffer(WAIT_TIME_US);
        if(index < 0) {
            return;
        }
        // 内存抖动未提
        final ByteBuffer inputBuffer = this.videoCodec.getInputBuffer(index);
        final VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
        YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), inputBuffer, i420.getWidth(), i420.getHeight());
        i420.release();
        this.videoCodec.queueInputBuffer(index, 0, this.yuvSize, videoFrame.getTimestampNs(), 0);
//      final int videoFrameSize = videoFrame.getRotatedWidth() * videoFrame.getRotatedHeight() * 3 / 2;
//      this.videoCodec.queueInputBuffer(index, 0, this.videoFrameSize, videoFrame.getTimestampNs(), 0);
    }

}
