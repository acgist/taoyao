#include "../include/Player.hpp"

#include <mutex>

#include <hilog/log.h>

static std::recursive_mutex audioMutex;

// 播放回调
static int32_t OnError(OH_AudioRenderer* renderer, void* userData, OH_AudioStream_Result error);
static int32_t OnWriteData(OH_AudioRenderer* renderer, void* userData, void* buffer, int32_t length);
static int32_t OnStreamEvent(OH_AudioRenderer* renderer, void* userData, OH_AudioStream_Event event);
static int32_t OnInterruptEvent(OH_AudioRenderer* renderer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint);

acgist::AudioPlayer::AudioPlayer() {
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_Create(&this->builder, AUDIOSTREAM_TYPE_RENDERER);
    TAOYAO_AUDIO_RET_LOG("配置音频构造器：%{public}d", ret);
    // 配置音频播放参数
    OH_AudioStreamBuilder_SetSamplingRate(this->builder, acgist::samplingRate);
    OH_AudioStreamBuilder_SetChannelCount(this->builder, acgist::channelCount);
    OH_AudioStreamBuilder_SetSampleFormat(this->builder, OH_AudioStream_SampleFormat::AUDIOSTREAM_SAMPLE_S16LE);
    OH_AudioStreamBuilder_SetEncodingType(this->builder, OH_AudioStream_EncodingType::AUDIOSTREAM_ENCODING_TYPE_RAW);
    OH_AudioStreamBuilder_SetRendererInfo(this->builder, OH_AudioStream_Usage::AUDIOSTREAM_USAGE_MUSIC);
    OH_LOG_DEBUG(LOG_APP, "配置音频播放参数：%d %d", acgist::samplingRate, acgist::channelCount);
    // 配置音频播放回调
    OH_AudioRenderer_Callbacks callbacks;
    callbacks.OH_AudioRenderer_OnError          = OnError;
    callbacks.OH_AudioRenderer_OnWriteData      = OnWriteData;
    callbacks.OH_AudioRenderer_OnStreamEvent    = OnStreamEvent;
    callbacks.OH_AudioRenderer_OnInterruptEvent = OnInterruptEvent;
    ret = OH_AudioStreamBuilder_SetRendererCallback(this->builder, callbacks, this);
    TAOYAO_AUDIO_RET_LOG("设置音频播放回调：%{public}d", ret);
}

acgist::AudioPlayer::~AudioPlayer() {
    this->stop();
    if(this->builder != nullptr) {
        OH_AudioStream_Result ret = OH_AudioStreamBuilder_Destroy(this->builder);
        this->builder = nullptr;
        TAOYAO_AUDIO_RET_LOG("释放音频构造器：%{public}d", ret);
    }
}

bool acgist::AudioPlayer::start() {
    std::lock_guard<std::recursive_mutex> audioLock(audioMutex);
    if (this->running) {
        return true;
    }
    this->running = true;
    // 配置音频播放器
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_GenerateRenderer(this->builder, &this->audioRenderer);
    TAOYAO_AUDIO_RET_LOG("配置音频播放器：%{public}d", ret);
    // 开始音频播放
    ret = OH_AudioRenderer_Start(this->audioRenderer);
    TAOYAO_AUDIO_RET_LOG("开始音频播放：%{public}d", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

bool acgist::AudioPlayer::stop() {
    std::lock_guard<std::recursive_mutex> audioLock(audioMutex);
    if (!this->running) {
        return true;
    }
    this->running = false;
    // 停止音频播放
    OH_AudioStream_Result ret = OH_AudioRenderer_Stop(this->audioRenderer);
    TAOYAO_AUDIO_RET_LOG("停止音频播放：%{public}d", ret);
    // 释放音频播放器
    ret = OH_AudioRenderer_Release(this->audioRenderer);
    this->audioRenderer = nullptr;
    TAOYAO_AUDIO_RET_LOG("释放音频播放器：%{public}d", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

static int32_t OnError(OH_AudioRenderer* renderer, void* userData, OH_AudioStream_Result error) {
    OH_LOG_ERROR(LOG_APP, "音频播放异常：%o", error);
    return 0;
}

static int32_t OnWriteData(OH_AudioRenderer* renderer, void* userData, void* buffer, int32_t length) {
    // TODO: 多个需要混音写入buffer
    return 0;
}

static int32_t OnStreamEvent(OH_AudioRenderer* renderer, void* userData, OH_AudioStream_Event event) {
    OH_LOG_DEBUG(LOG_APP, "音频播放事件：%o", event);
    return 0;
}

static int32_t OnInterruptEvent(OH_AudioRenderer* renderer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint) {
    OH_LOG_DEBUG(LOG_APP, "打断音频播放：%o %o", type, hint);
    return 0;
}
