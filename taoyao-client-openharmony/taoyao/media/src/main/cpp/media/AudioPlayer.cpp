#include "../include/Player.hpp"

#include <hilog/log.h>

#include <ohaudio/native_audiorenderer.h>
#include <ohaudio/native_audiostreambuilder.h>

// 播放回调
static int32_t OnError(OH_AudioRenderer* renderer, void* userData, OH_AudioStream_Result error);
static int32_t OnWriteData(OH_AudioRenderer* renderer, void* userData, void* buffer, int32_t length);
static int32_t OnStreamEvent(OH_AudioRenderer* renderer, void* userData, OH_AudioStream_Event event);
static int32_t OnInterruptEvent(OH_AudioRenderer* renderer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint);

acgist::AudioPlayer::AudioPlayer() {
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_Create(&this->builder, AUDIOSTREAM_TYPE_RENDERER);
    OH_LOG_INFO(LOG_APP, "构造音频构造器：%o", ret);
    // 配置音频播放参数
    OH_AudioStreamBuilder_SetSamplingRate(this->builder, acgist::samplingRate);
    OH_AudioStreamBuilder_SetChannelCount(this->builder, acgist::channelCount);
    OH_AudioStreamBuilder_SetSampleFormat(this->builder, OH_AudioStream_SampleFormat::AUDIOSTREAM_SAMPLE_S16LE);
    OH_AudioStreamBuilder_SetEncodingType(this->builder, OH_AudioStream_EncodingType::AUDIOSTREAM_ENCODING_TYPE_RAW);
    OH_AudioStreamBuilder_SetRendererInfo(this->builder, OH_AudioStream_Usage::AUDIOSTREAM_USAGE_MUSIC);
    OH_LOG_DEBUG(LOG_APP, "配置音频播放参数：%d %d", acgist::samplingRate, acgist::channelCount);
    // 配置回调函数
    OH_AudioRenderer_Callbacks callbacks;
    callbacks.OH_AudioRenderer_OnError          = OnError;
    callbacks.OH_AudioRenderer_OnWriteData      = OnWriteData;
    callbacks.OH_AudioRenderer_OnStreamEvent    = OnStreamEvent;
    callbacks.OH_AudioRenderer_OnInterruptEvent = OnInterruptEvent;
    ret = OH_AudioStreamBuilder_SetRendererCallback(this->builder, callbacks, this);
    OH_LOG_DEBUG(LOG_APP, "设置播放回调函数：%o", ret);
}

acgist::AudioPlayer::~AudioPlayer() {
    this->stop();
    if(this->builder != nullptr) {
        OH_AudioStream_Result ret = OH_AudioStreamBuilder_Destroy(this->builder);
        this->builder = nullptr;
        OH_LOG_INFO(LOG_APP, "释放音频播放：%o", ret);
    }
}

bool acgist::AudioPlayer::start() {
    if (this->running) {
        return true;
    }
    this->running = true;
    // 构造音频播放器
    OH_AudioStream_Result ret = OH_AudioStreamBuilder_GenerateRenderer(this->builder, &this->audioRenderer);
    OH_LOG_DEBUG(LOG_APP, "构造音频播放器：%o", ret);
    // 开始音频播放
    ret = OH_AudioRenderer_Start(this->audioRenderer);
    OH_LOG_DEBUG(LOG_APP, "开始音频播放：%o", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

bool acgist::AudioPlayer::stop() {
    if (!this->running) {
        return true;
    }
    this->running = false;
    if (this->audioRenderer == nullptr) {
        return true;
    }
    // 停止音频播放
    OH_AudioStream_Result ret = OH_AudioRenderer_Stop(this->audioRenderer);
    OH_LOG_DEBUG(LOG_APP, "停止音频播放：%o", ret);
    // 释放音频播放器
    ret = OH_AudioRenderer_Release(this->audioRenderer);
    this->audioRenderer = nullptr;
    OH_LOG_DEBUG(LOG_APP, "释放音频播放器：%o", ret);
    return ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS;
}

static int32_t OnError(OH_AudioRenderer* renderer, void* userData, OH_AudioStream_Result error) {
    OH_LOG_ERROR(LOG_APP, "音频播放异常：%o", error);
    return 0;
}

static int32_t OnWriteData(OH_AudioRenderer* renderer, void* userData, void* buffer, int32_t length) {
    // TODO: 混音写入buffer
    return 0;
}

static int32_t OnStreamEvent(OH_AudioRenderer* renderer, void* userData, OH_AudioStream_Event event) {
    OH_LOG_DEBUG(LOG_APP, "音频播放事件：%o", event);
    return 0;
}

static int32_t OnInterruptEvent(OH_AudioRenderer* renderer, void* userData, OH_AudioInterrupt_ForceType type, OH_AudioInterrupt_Hint hint) {
    OH_LOG_DEBUG(LOG_APP, "音频播放打断：%o %o", type, hint);
    return 0;
}
