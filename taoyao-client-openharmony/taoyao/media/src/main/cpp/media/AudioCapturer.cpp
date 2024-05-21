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
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_Create(&this->builder, AUDIOSTREAM_TYPE_CAPTURER);
    TAOYAO_AUDIO_RET_LOG("配置音频构造器：%{public}d", ret);
    // 配置音频采集参数
    OH_AudioStreamBuilder_SetSamplingRate(this->builder, acgist::samplingRate);
    OH_AudioStreamBuilder_SetChannelCount(this->builder, acgist::channelCount);
    OH_AudioStreamBuilder_SetLatencyMode(this->builder,  OH_AudioStream_LatencyMode::AUDIOSTREAM_LATENCY_MODE_NORMAL);
    OH_AudioStreamBuilder_SetSampleFormat(this->builder, OH_AudioStream_SampleFormat::AUDIOSTREAM_SAMPLE_S16LE);
    OH_LOG_DEBUG(LOG_APP, "配置音频采集参数：%{public}d %{public}d", acgist::samplingRate, acgist::channelCount);
    // 设置音频采集回调
    OH_AudioCapturer_Callbacks callbacks;
    callbacks.OH_AudioCapturer_OnError          = OnError;
    callbacks.OH_AudioCapturer_OnReadData       = OnReadData;
    callbacks.OH_AudioCapturer_OnStreamEvent    = OnStreamEvent;
    callbacks.OH_AudioCapturer_OnInterruptEvent = OnInterruptEvent;
    ret = OH_AudioStreamBuilder_SetCapturerCallback(this->builder, callbacks, this);
    TAOYAO_AUDIO_RET_LOG("设置音频采集回调：%{public}d", ret);
}

acgist::AudioCapturer::~AudioCapturer() {
    this->stop();
    if(this->builder != nullptr) {
        OH_AudioStream_Result ret = OH_AudioStreamBuilder_Destroy(this->builder);
        this->builder = nullptr;
        TAOYAO_AUDIO_RET_LOG("释放音频构造器：%{public}d", ret);
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
    TAOYAO_AUDIO_RET_LOG("配置音频采集器：%{public}d", ret);
    // 开始音频采集
    ret = OH_AudioCapturer_Start(this->audioCapturer);
    TAOYAO_AUDIO_RET_LOG("开始音频采集：%{public}d", ret);
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
    TAOYAO_AUDIO_RET_LOG("停止音频采集：%{public}d", ret);
    // 释放音频采集器
    ret = OH_AudioCapturer_Release(this->audioCapturer);
    this->audioCapturer = nullptr;
    TAOYAO_AUDIO_RET_LOG("释放音频采集器：%{public}d", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

static int32_t OnError(OH_AudioCapturer* capturer, void* userData, OH_AudioStream_Result error) {
    OH_LOG_ERROR(LOG_APP, "音频采集异常：%o", error);
    return 0;
}

static int32_t OnReadData(OH_AudioCapturer* capturer, void* userData, void* buffer, int32_t length) {
    OH_LOG_DEBUG(LOG_APP, "音频帧数据采集回调：%{public}d", length);
    if(userData == nullptr) {
        return -1;
    }
    acgist::AudioCapturer* audioCapturer = (acgist::AudioCapturer*) userData;
    if(audioCapturer->source == nullptr) {
        return -2;
    }
    // 单声道 48000 / 1000 * 10 * 2(16bit)
    // 双声道 48000 / 1000 * 10 * 2(16bit) * 2
    // 字节数量 * 8 / 位深 / 通道数量
    size_t number_of_frames = length / 2;
    // size_t number_of_frames = length * 8 / 16 / 2;
    audioCapturer->source->OnData((uint16_t*) buffer, acgist::bitsPerSample, acgist::samplingRate, acgist::channelCount, number_of_frames);
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
