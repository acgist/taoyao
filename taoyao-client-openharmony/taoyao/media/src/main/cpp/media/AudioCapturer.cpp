/**
 * 音频采集不用实现（系统已经实现）
 * 这里只是用来学习使用
 */
#include "../include/Capturer.hpp"

#include <mutex>

#include <hilog/log.h>

static std::recursive_mutex audioMutex;

// 采集回调
static int32_t OnError(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Result error);
static int32_t OnReadData(OH_AudioCapturer* capturer, void* userData, void* buffer, int32_t length);
static int32_t OnStreamEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Event event);
static int32_t OnInterruptEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint);

acgist::AudioCapturer::AudioCapturer() {
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_Create(&this->builder, AUDIOSTREAM_TYPE_RENDERER);
    OH_LOG_INFO(LOG_APP, "配置音频构造器：%o", ret);
    // 配置音频采集参数
    OH_AudioStreamBuilder_SetSamplingRate(this->builder, acgist::samplingRate);
    OH_AudioStreamBuilder_SetChannelCount(this->builder, acgist::channelCount);
    OH_AudioStreamBuilder_SetLatencyMode(this->builder,  OH_AudioStream_LatencyMode::AUDIOSTREAM_LATENCY_MODE_NORMAL);
    OH_AudioStreamBuilder_SetSampleFormat(this->builder, OH_AudioStream_SampleFormat::AUDIOSTREAM_SAMPLE_S16LE);
    OH_LOG_DEBUG(LOG_APP, "配置音频采集参数：%d %d", acgist::samplingRate, acgist::channelCount);
    // 设置采集回调
    OH_AudioCapturer_Callbacks callbacks;
    callbacks.OH_AudioCapturer_OnError          = OnError;
    callbacks.OH_AudioCapturer_OnReadData       = OnReadData;
    callbacks.OH_AudioCapturer_OnStreamEvent    = OnStreamEvent;
    callbacks.OH_AudioCapturer_OnInterruptEvent = OnInterruptEvent;
    ret = OH_AudioStreamBuilder_SetCapturerCallback(this->builder, callbacks, this);
    OH_LOG_DEBUG(LOG_APP, "设置音频采集回调：%o", ret);
}

acgist::AudioCapturer::~AudioCapturer() {
    this->stop();
    if(this->builder != nullptr) {
        OH_AudioStream_Result ret = OH_AudioStreamBuilder_Destroy(this->builder);
        this->builder = nullptr;
        OH_LOG_INFO(LOG_APP, "释放音频构造器：%o", ret);
    }
}

bool acgist::AudioCapturer::start() {
    std::lock_guard<std::recursive_mutex> audioLock(audioMutex);
    if(this->running) {
        return true;
    }
    this->running = true;
    // 配置音频采集器
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_GenerateCapturer(this->builder, &this->audioCapturer);
    OH_LOG_DEBUG(LOG_APP, "配置音频采集器：%o", ret);
    // 开始音频采集
    ret = OH_AudioCapturer_Start(this->audioCapturer);
    OH_LOG_DEBUG(LOG_APP, "开始音频采集：%o", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

bool acgist::AudioCapturer::stop() {
    std::lock_guard<std::recursive_mutex> audioLock(audioMutex);
    if(!this->running) {
        return true;
    }
    this->running = false;
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
    if(userData == nullptr) {
        return -1;
    }
    acgist::AudioCapturer* audioCapturer = (acgist::AudioCapturer*) userData;
    if(audioCapturer->source == nullptr) {
        return -2;
    }
    // 单声道 48000 / 1000 * 10 * 2(16bit)
    // 双声道 48000 / 1000 * 10 * 2(16bit) * 2
    audioCapturer->source->OnData(buffer, acgist::bitsPerSample, acgist::samplingRate, acgist::channelCount, length / 2);
    return 0;
}

static int32_t OnStreamEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Event event) {
    OH_LOG_DEBUG(LOG_APP, "音频采集事件：%o", event);
    return 0;
}

static int32_t OnInterruptEvent(OH_AudioCapturer* capturer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint) {
    OH_LOG_DEBUG(LOG_APP, "打断音频采集：%o %o", type, hint);
    return 0;
}
