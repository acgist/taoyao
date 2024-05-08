#include "../include/Capturer.hpp"

#include <hilog/log.h>

#include "rtc_base/time_utils.h"

// 采集回调
static int32_t OnError(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Result error);
static int32_t OnReadData(OH_AudioCapturer* capturer, void* userData, void* buffer, int32_t length);
static int32_t OnStreamEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Event event);
static int32_t OnInterruptEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint);

acgist::AudioCapturer::AudioCapturer() {
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_Create(&this->builder, AUDIOSTREAM_TYPE_RENDERER);
    OH_LOG_INFO(LOG_APP, "构造音频构造器：%o", ret);
    // 配置音频参数
    OH_AudioStreamBuilder_SetSamplingRate(this->builder, acgist::samplingRate);
    OH_AudioStreamBuilder_SetChannelCount(this->builder, acgist::channelCount);
    OH_AudioStreamBuilder_SetLatencyMode(this->builder,  OH_AudioStream_LatencyMode::AUDIOSTREAM_LATENCY_MODE_NORMAL);
    OH_AudioStreamBuilder_SetSampleFormat(this->builder, OH_AudioStream_SampleFormat::AUDIOSTREAM_SAMPLE_S16LE);
    OH_LOG_DEBUG(LOG_APP, "配置音频参数：%d %d", acgist::samplingRate, acgist::channelCount);
    // 设置回调函数
    OH_AudioCapturer_Callbacks callbacks;
    callbacks.OH_AudioCapturer_OnError          = OnError;
    callbacks.OH_AudioCapturer_OnReadData       = OnReadData;
    callbacks.OH_AudioCapturer_OnStreamEvent    = OnStreamEvent;
    callbacks.OH_AudioCapturer_OnInterruptEvent = OnInterruptEvent;
    ret = OH_AudioStreamBuilder_SetCapturerCallback(this->builder, callbacks, this);
    OH_LOG_DEBUG(LOG_APP, "设置回调函数：%o", ret);
}

acgist::AudioCapturer::~AudioCapturer() {
    this->stop();
    if(this->builder != nullptr) {
        OH_AudioStream_Result ret = OH_AudioStreamBuilder_Destroy(this->builder);
        this->builder = nullptr;
        OH_LOG_INFO(LOG_APP, "释放音频采集：%o", ret);
    }
}

bool acgist::AudioCapturer::start() {
    if(this->running) {
        return true;
    }
    this->running = true;
    // 构造音频采集器
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_GenerateCapturer(this->builder, &this->audioCapturer);
    OH_LOG_DEBUG(LOG_APP, "构造音频采集器：%o", ret);
    // 开始音频采集
    ret = OH_AudioCapturer_Start(this->audioCapturer);
    OH_LOG_DEBUG(LOG_APP, "开始音频采集：%o", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

bool acgist::AudioCapturer::stop() {
    if(!this->running) {
        return true;
    }
    this->running = false;
    if(this->audioCapturer == nullptr) {
        return true;
    }
    // 停止音频采集
    OH_AudioStream_Result ret = OH_AudioCapturer_Stop(this->audioCapturer);
    OH_LOG_DEBUG(LOG_APP, "停止音频采集：%o", ret);
    // 释放音频采集器
    ret = OH_AudioCapturer_Release(this->audioCapturer);
    this->audioCapturer = nullptr;
    OH_LOG_DEBUG(LOG_APP, "释放音频采集器：%o", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

static int32_t OnError(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Result error) {
    OH_LOG_ERROR(LOG_APP, "音频采集异常：%o", error);
    return 0;
}

static int32_t OnReadData(OH_AudioCapturer* capturer, void* userData, void* buffer, int32_t length) {
    acgist::AudioCapturer* audioCapturer = (acgist::AudioCapturer*) userData;
    int64_t timeMillis = rtc::TimeMillis();
    for (auto iterator = audioCapturer->map.begin(); iterator != audioCapturer->map.end(); ++iterator) {
        iterator->second->OnData(buffer, acgist::bitsPerSample, acgist::samplingRate, acgist::channelCount, sizeof(buffer) / 2, timeMillis);
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
