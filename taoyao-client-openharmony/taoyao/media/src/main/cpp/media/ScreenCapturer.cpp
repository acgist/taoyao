#include "../include/Capturer.hpp"

#include <hilog/log.h>

#include "api/video/nv12_buffer.h"
#include "api/video/i420_buffer.h"

// 没法
static acgist::ScreenCapturer* screenCapturer = nullptr;

static void OnError(OH_AVScreenCapture* capture, int32_t errorCode) {
    OH_LOG_ERROR(LOG_APP, "屏幕采集异常：%{public}d", errorCode);
}

static void OnAudioBufferAvailable(OH_AVScreenCapture* capture, bool isReady, OH_AudioCaptureSourceType type) {
    // OH_LOG_DEBUG(LOG_APP, "屏幕采集音频数据帧：%{public}d", isReady);
    if (isReady) {
        OH_AudioBuffer* buffer = new OH_AudioBuffer;
        int32_t ret = OH_AVScreenCapture_AcquireAudioBuffer(capture, &buffer, type);
        (void) buffer->buf;
        (void) buffer->size;
        (void) buffer->timestamp;
        delete buffer;
        buffer = nullptr;
    }
    OH_AVScreenCapture_ReleaseAudioBuffer(capture, type);
}

static void OnVideoBufferAvailable(OH_AVScreenCapture* capture, bool isReady) {
    OH_LOG_DEBUG(LOG_APP, "屏幕采集视频数据帧：%{public}d", isReady);
    if (isReady) {
        int32_t fence     = 0;
        int64_t timestamp = 0;
        OH_Rect damage;
        OH_NativeBuffer* buffer = OH_AVScreenCapture_AcquireVideoBuffer(capture, &fence, &timestamp, &damage);
        void* virAddr = nullptr;
        OH_NativeBuffer_Map(buffer, &virAddr);
        OH_NativeBuffer_Config config;
        OH_NativeBuffer_GetConfig(buffer, &config);
        // rtc::scoped_refptr<webrtc::I420Buffer> videoFrameBuffer = webrtc::I420Buffer::Copy(config.width, config.height, (uint8_t*) virAddr, 0, (uint8_t*) virAddr, 0, (uint8_t*) virAddr, 0);
        // webrtc::VideoFrame::Builder builder;
        // webrtc::VideoFrame videoFrame = builder.set_timestamp_ms(timestamp).set_video_frame_buffer(videoFrameBuffer).build();
        // screenCapturer->source->OnData(videoFrame);
        
//          height = (height > 0) ? height : -height; // abs
//  width = stride;
//  size_t size = stride * height + stride * height / 2;
//  if (bufferSize < size) {
//    return false;
//  }
//
//  rtc::scoped_refptr<webrtc::I420Buffer> i420_buffer = webrtc::I420Buffer::Create(width, height);
//  libyuv::NV21ToI420(buffer, width, buffer + width * height, width, i420_buffer.get()->MutableDataY(),
//                     i420_buffer.get()->StrideY(), i420_buffer.get()->MutableDataU(), i420_buffer.get()->StrideU(),
//                     i420_buffer.get()->MutableDataV(), i420_buffer.get()->StrideV(), width, height);
//
//  webrtc::VideoFrame video_frame = webrtc::VideoFrame::Builder()
//                                     .set_video_frame_buffer(i420_buffer)
//                                     .set_timestamp_rtp(0)
//                                     .set_timestamp_ms(rtc::TimeMillis())
//                                     .set_rotation(webrtc::kVideoRotation_90)
//                                     .build();
        OH_NativeBuffer_Unmap(buffer);
    }
    OH_AVScreenCapture_ReleaseVideoBuffer(capture);
}

acgist::ScreenCapturer::ScreenCapturer() {
    this->avScreenCapture = OH_AVScreenCapture_Create();
    OH_AVScreenCaptureCallback callback;
    callback.onError = OnError;
    callback.onAudioBufferAvailable = OnAudioBufferAvailable;
    callback.onVideoBufferAvailable = OnVideoBufferAvailable;
    OH_AVScreenCapture_SetCallback(this->avScreenCapture, callback);
    OH_AudioCaptureInfo audioCaptureInfo = {
        .audioSampleRate = acgist::samplingRate,
        .audioChannels   = acgist::channelCount,
        .audioSource     = OH_MIC
    };
    OH_AudioInfo audioInfo = {
        .micCapInfo = audioCaptureInfo,
    };
    OH_VideoCaptureInfo videoCaptureInfo = {
        // .videoFrameWidth  = static_cast<int32_t>(acgist::width),
        // .videoFrameHeight = static_cast<int32_t>(acgist::height),
        .videoFrameWidth  = static_cast<int32_t>(800),
        .videoFrameHeight = static_cast<int32_t>(1280),
        .videoSource      = OH_VIDEO_SOURCE_SURFACE_RGBA
        // .videoSource      = OH_VIDEO_SOURCE_SURFACE_YUV
    };
    // OH_VideoEncInfo videoEncInfo = {
    //     .videoCodec     = OH_VideoCodecFormat::OH_H264, 
    //     .videoBitrate   = acgist::bitrate, 
    //     .videoFrameRate = acgist::frameRate
    // };
    OH_VideoInfo videoInfo = {
        .videoCapInfo = videoCaptureInfo, 
        // .videoEncInfo = videoEncInfo
    };
    OH_AVScreenCaptureConfig config = {
        .captureMode = OH_CAPTURE_HOME_SCREEN,
        .dataType    = OH_ORIGINAL_STREAM,
        .audioInfo   = audioInfo,
        .videoInfo   = videoInfo,
    };
    OH_AVScreenCapture_Init(this->avScreenCapture, config);
    OH_AVScreenCapture_SetMicrophoneEnabled(this->avScreenCapture, false);
    screenCapturer = this;
}

bool acgist::ScreenCapturer::start() {
    OH_LOG_INFO(LOG_APP, "打开屏幕采集");
    OH_AVScreenCapture_StartScreenCapture(this->avScreenCapture);
    return true;
}

bool acgist::ScreenCapturer::stop() {
    OH_LOG_INFO(LOG_APP, "关闭屏幕采集");
    OH_AVScreenCapture_StopScreenCapture(this->avScreenCapture);
    return true;
}

acgist::ScreenCapturer::~ScreenCapturer() {
    OH_AVScreenCapture_Release(this->avScreenCapture);
    this->avScreenCapture = nullptr;
}
