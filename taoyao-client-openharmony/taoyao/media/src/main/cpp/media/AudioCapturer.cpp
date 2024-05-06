#include "../include/Capturer.hpp"

#include <hilog/log.h>

#include "rtc_base/time_utils.h"

// 采样率
static int32_t samplingRate = 48000;
// 声道数
static int32_t channelCount = 2;
// 音频场景
static OH_AudioStream_LatencyMode  latencyMode  = OH_AudioStream_LatencyMode::AUDIOSTREAM_LATENCY_MODE_NORMAL;
// 音频格式
static OH_AudioStream_SampleFormat sampleFormat = OH_AudioStream_SampleFormat::AUDIOSTREAM_SAMPLE_S16LE;

static int32_t OnError(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Result error);
static int32_t OnReadData(OH_AudioCapturer* capturer, void* userData, void* buffer, int32_t length);
static int32_t OnStreamEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Event event);
static int32_t OnInterruptEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint);

acgist::AudioCapturer::AudioCapturer() {
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_Create(&this->builder, AUDIOSTREAM_TYPE_RENDERER);
    OH_LOG_INFO(LOG_APP, "构造音频采集：%o", ret);
}

acgist::AudioCapturer::~AudioCapturer() {
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_Destroy(this->builder);
    this->builder = nullptr;
    OH_LOG_INFO(LOG_APP, "释放音频采集：%o", ret);
}

bool acgist::AudioCapturer::start() {
    // 配置采集参数
    OH_AudioStreamBuilder_SetSamplingRate(this->builder, samplingRate);
    OH_AudioStreamBuilder_SetChannelCount(this->builder, channelCount);
    OH_AudioStreamBuilder_SetLatencyMode(this->builder, latencyMode);
    OH_AudioStreamBuilder_SetSampleFormat(this->builder, sampleFormat);
    OH_LOG_DEBUG(LOG_APP, "配置音频格式：%d %d %o %o", samplingRate, channelCount, latencyMode, sampleFormat);
    // 设置回调函数
    OH_AudioCapturer_Callbacks callbacks;
    callbacks.OH_AudioCapturer_OnError          = OnError;
    callbacks.OH_AudioCapturer_OnReadData       = OnReadData;
    callbacks.OH_AudioCapturer_OnStreamEvent    = OnStreamEvent;
    callbacks.OH_AudioCapturer_OnInterruptEvent = OnInterruptEvent;
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_SetCapturerCallback(this->builder, callbacks, this);
    OH_LOG_DEBUG(LOG_APP, "设置回调函数：%o", ret);
    // 构造音频采集器
    ret = OH_AudioStreamBuilder_GenerateCapturer(this->builder, &this->audioCapturer);
    OH_LOG_DEBUG(LOG_APP, "构造音频采集器：%o", ret);
    // 开始录制
    ret = OH_AudioCapturer_Start(this->audioCapturer);
    OH_LOG_DEBUG(LOG_APP, "开始录制：%o", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

bool acgist::AudioCapturer::stop() {
    // 停止录制
    OH_AudioStream_Result ret = OH_AudioCapturer_Stop(this->audioCapturer);
    OH_LOG_DEBUG(LOG_APP, "停止录制：%o", ret);
    // 释放音频采集器
    ret = OH_AudioCapturer_Release(this->audioCapturer);
    this->audioCapturer = nullptr;
    OH_LOG_DEBUG(LOG_APP, "释放音频采集器：%o", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

static int32_t OnError(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Result error) {
    OH_LOG_ERROR(LOG_APP, "音频采集发生异常：%o", error);
    return 0;
}

static int32_t OnReadData(OH_AudioCapturer* capturer, void* userData, void* buffer, int32_t length) {
    acgist::AudioCapturer* audioCapturer = (acgist::AudioCapturer*) userData;
    int64_t timeMillis = rtc::TimeMillis();
    for (auto iterator = audioCapturer->map.begin(); iterator != audioCapturer->map.end(); ++iterator) {
        iterator->second->OnData(buffer, 16, samplingRate, channelCount, sizeof(buffer) / 2, timeMillis);
    }
    return 0;
}

static int32_t OnStreamEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Event event) {
    OH_LOG_DEBUG(LOG_APP, "音频采集事件：%o", event);
    return 0;
}

static int32_t OnInterruptEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint) {
    OH_LOG_DEBUG(LOG_APP, "音频采集打断：%o %o", type, hint);
    return 0;
}
