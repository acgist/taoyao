#include "../include/WebRTC.hpp"

#include <mutex>

#include <hilog/log.h>

#include <native_window/external_window.h>
#include <multimedia/player_framework/native_avbuffer.h>
#include <multimedia/player_framework/native_averrors.h>
#include <multimedia/player_framework/native_avformat.h>
#include <multimedia/player_framework/native_avcodec_base.h>
#include <multimedia/player_framework/native_avcapability.h>

#include "api/video_codecs/video_encoder.h"
#include "api/video_codecs/video_decoder.h"

static std::recursive_mutex videoMutex;

// 编码回调
static void OnError(OH_AVCodec* codec, int32_t errorCode, void* userData);
static void OnStreamChanged(OH_AVCodec* codec, OH_AVFormat* format, void* userData);
static void OnNeedInputBuffer(OH_AVCodec* codec, uint32_t index, OH_AVBuffer* buffer, void* userData);
static void OnNewOutputBuffer(OH_AVCodec* codec, uint32_t index, OH_AVBuffer* buffer, void* userData);
    
acgist::TaoyaoVideoEncoder::TaoyaoVideoEncoder() {
    OH_AVCapability* capability = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_AVC, true);
    const char* codecName = OH_AVCapability_GetName(capability);
    this->avCodec = OH_VideoEncoder_CreateByName(codecName);
    OH_LOG_INFO(LOG_APP, "配置视频编码格式：%s", codecName);
    // 注册视频编码回调
    OH_AVCodecCallback callback = { &OnError, &OnStreamChanged, &OnNeedInputBuffer, &OnNewOutputBuffer };
    OH_AVErrCode ret = OH_VideoEncoder_RegisterCallback(this->avCodec, callback, this);
    OH_LOG_INFO(LOG_APP, "注册视频编码回调：%o", ret);
    // 配置视频编码参数
    OH_AVFormat* format = OH_AVFormat_Create();
    this->initFormatConfig(format);
    ret = OH_VideoEncoder_Configure(this->avCodec, format);
    OH_AVFormat_Destroy(format);
    OH_LOG_INFO(LOG_APP, "配置视频编码参数：%o %d %d %f %d %lld", ret, acgist::width, acgist::height, acgist::frameRate, acgist::iFrameInterval, acgist::bitrate);
    // 视频编码准备就绪
    ret = OH_VideoEncoder_Prepare(this->avCodec);
    OH_LOG_INFO(LOG_APP, "视频编码准备就绪：%o", ret);
}

acgist::TaoyaoVideoEncoder::~TaoyaoVideoEncoder() {
    if(this->avCodec != nullptr) {
        OH_AVErrCode ret = OH_VideoEncoder_Destroy(this->avCodec);
        this->avCodec = nullptr;
        OH_LOG_INFO(LOG_APP, "释放视频编码器：%o", ret);
    }
}

void acgist::TaoyaoVideoEncoder::initFormatConfig(OH_AVFormat* format) {
    // https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/avcodec/video-encoding.md
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_WIDTH,                     acgist::width);
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_HEIGHT,                    acgist::height);
    OH_AVFormat_SetLongValue(format,   OH_MD_KEY_BITRATE,                   acgist::bitrate);
    OH_AVFormat_SetDoubleValue(format, OH_MD_KEY_FRAME_RATE,                acgist::frameRate);
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_I_FRAME_INTERVAL,          acgist::iFrameInterval);
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_QUALITY,                   0);
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_RANGE_FLAG,                false);
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_PIXEL_FORMAT,              AV_PIXEL_FORMAT_YUVI420);
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_PROFILE,                   static_cast<int32_t>(OH_AVCProfile::AVC_PROFILE_BASELINE));
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_COLOR_PRIMARIES,           static_cast<int32_t>(OH_ColorPrimary::COLOR_PRIMARY_BT709));
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_MATRIX_COEFFICIENTS,       static_cast<int32_t>(OH_MatrixCoefficient::MATRIX_COEFFICIENT_IDENTITY));
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_TRANSFER_CHARACTERISTICS,  static_cast<int32_t>(OH_TransferCharacteristic::TRANSFER_CHARACTERISTIC_BT709));
    OH_AVFormat_SetIntValue(format,    OH_MD_KEY_VIDEO_ENCODE_BITRATE_MODE, static_cast<int32_t>(OH_VideoEncodeBitrateMode::CBR));
}

void acgist::TaoyaoVideoEncoder::restart() {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    OH_AVErrCode ret = OH_VideoEncoder_Flush(this->avCodec);
    OH_LOG_INFO(LOG_APP, "清空视频编码队列：%o", ret);
    ret = OH_VideoEncoder_Start(this->avCodec);
    OH_LOG_INFO(LOG_APP, "开始视频编码：%o", ret);
}

void acgist::TaoyaoVideoEncoder::reset(OH_AVFormat* format) {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    OH_AVErrCode ret = OH_VideoEncoder_Reset(this->avCodec);
    OH_LOG_INFO(LOG_APP, "重置视频编码：%o", ret);
    ret = OH_VideoEncoder_Configure(this->avCodec, format);
    OH_LOG_INFO(LOG_APP, "配置视频编码：%o", ret);
}

void acgist::TaoyaoVideoEncoder::resetIntConfig(const char* key, int32_t value) {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    OH_AVFormat* format = OH_AVFormat_Create();
    OH_AVFormat_SetIntValue(format, key, value);
    OH_AVErrCode ret = OH_VideoEncoder_SetParameter(this->avCodec, format);
    OH_LOG_INFO(LOG_APP, "动态配置视频编码：%o %s %d", ret, key, value);
    OH_AVFormat_Destroy(format);
}

void acgist::TaoyaoVideoEncoder::resetLongConfig(const char* key, int64_t value) {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    OH_AVFormat* format = OH_AVFormat_Create();
    OH_AVFormat_SetLongValue(format, key, value);
    OH_AVErrCode ret = OH_VideoEncoder_SetParameter(this->avCodec, format);
    OH_LOG_INFO(LOG_APP, "动态配置视频编码：%o %s %lld", ret, key, value);
    OH_AVFormat_Destroy(format);
}

void acgist::TaoyaoVideoEncoder::resetDoubleConfig(const char* key, double value) {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    OH_AVFormat* format = OH_AVFormat_Create();
    OH_AVFormat_SetDoubleValue(format, key, value);
    OH_AVErrCode ret = OH_VideoEncoder_SetParameter(this->avCodec, format);
    OH_LOG_INFO(LOG_APP, "动态配置视频编码：%o %s %f", ret, key, value);
    OH_AVFormat_Destroy(format);
}

bool acgist::TaoyaoVideoEncoder::start() {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    if(this->running) {
        return true;
    }
    this->running = true;
    OH_AVErrCode ret = OH_VideoEncoder_Start(this->avCodec);
    OH_LOG_INFO(LOG_APP, "开始视频编码：%o", ret);
    return ret == OH_AVErrCode::AV_ERR_OK;
}

bool acgist::TaoyaoVideoEncoder::stop() {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    if(!this->running) {
        return true;
    }
    this->running = false;
    // Buffer模式
    OH_AVErrCode ret = OH_VideoEncoder_Stop(this->avCodec);
    OH_LOG_INFO(LOG_APP, "结束视频编码：%o", ret);
    // Surface模式
    // OH_AVErrCode ret = OH_VideoEncoder_NotifyEndOfStream(this->avCodec);
    // OH_LOG_INFO(LOG_APP, "通知视频编码结束：%o", ret);
    return ret == OH_AVErrCode::AV_ERR_OK;
}

int32_t acgist::TaoyaoVideoEncoder::Release() {
    // TODO: 释放
    delete this;
    return 0;
}

int32_t acgist::TaoyaoVideoEncoder::RegisterEncodeCompleteCallback(webrtc::EncodedImageCallback* callback) {
    this->encodedImageCallback = callback;
    return 0;
}

void acgist::TaoyaoVideoEncoder::SetRates(const webrtc::VideoEncoder::RateControlParameters& parameters) {
    // TODO: 动态调整编码
}

webrtc::VideoEncoder::EncoderInfo acgist::TaoyaoVideoEncoder::GetEncoderInfo() const {
    webrtc::VideoEncoder::EncoderInfo info;
    // TODO
    return info;
}

int32_t acgist::TaoyaoVideoEncoder::Encode(const webrtc::VideoFrame& videoFrame, const std::vector<webrtc::VideoFrameType>* frame_types) {
    OH_AVCodecBufferAttr info;
    info.size   = videoFrame.width() * videoFrame.height() * 3 / 2;
    info.offset = 0;
    info.pts    = 0;
    info.flags  = 0;
    // TODO: videoFrame.video_frame_buffer->
    OH_AVErrCode ret = OH_AVBuffer_SetBufferAttr(this->buffer, &info);
    ret = OH_VideoEncoder_PushInputBuffer(this->avCodec, index);
    return 0;
}

static void OnError(OH_AVCodec* codec, int32_t errorCode, void* userData) {
    OH_LOG_ERROR(LOG_APP, "视频编码发送错误：%d", errorCode);
}

static void OnStreamChanged(OH_AVCodec* codec, OH_AVFormat* format, void* userData) {
    OH_LOG_DEBUG(LOG_APP, "视频编码配置变化");
}

static void OnNeedInputBuffer(OH_AVCodec* codec, uint32_t index, OH_AVBuffer* buffer, void* userData) {
    if(userData == nullptr) {
        OH_VideoEncoder_PushInputBuffer(codec, index);
        return;
    }
    acgist::TaoyaoVideoEncoder* videoEncoder = (acgist::TaoyaoVideoEncoder*) userData;
    if(videoEncoder->running) {
        videoEncoder->index  = index;
        videoEncoder->buffer = buffer;
    } else {
        // 写入结束
        OH_AVCodecBufferAttr info;
        info.size   = 0;
        info.offset = 0;
        info.pts    = 0;
        info.flags  = AVCODEC_BUFFER_FLAGS_EOS;
        OH_AVErrCode ret = OH_AVBuffer_SetBufferAttr(buffer, &info);
        ret = OH_VideoEncoder_PushInputBuffer(codec, index);
        OH_LOG_INFO(LOG_APP, "通知视频编码结束：%o", ret);
    }
}

static void OnNewOutputBuffer(OH_AVCodec* codec, uint32_t index, OH_AVBuffer* buffer, void* userData) {
    if(userData == nullptr) {
        OH_VideoEncoder_FreeOutputBuffer(codec, index);
        return;
    }
    acgist::TaoyaoVideoEncoder* videoEncoder = (acgist::TaoyaoVideoEncoder*) userData;
    // TODO: 全局是否性能更好
    OH_AVCodecBufferAttr info;
    OH_AVErrCode ret = OH_AVBuffer_GetBufferAttr(buffer, &info);
    char* data = reinterpret_cast<char*>(OH_AVBuffer_GetAddr(buffer));
    //     webrtc::VideoFrameType flags =
    //     info.flags == AVCODEC_BUFFER_FLAGS_SYNC_FRAME       ? webrtc::VideoFrameType::kVideoFrameKey   :
    //     info.flags == AVCODEC_BUFFER_FLAGS_INCOMPLETE_FRAME ? webrtc::VideoFrameType::kVideoFrameDelta :
    //     webrtc::VideoFrameType::kEmptyFrame;
    // frame_types->push_back(std::move(flags));
//     videoEncoder->encodedImageCallback
    // TODO: 继续处理
    ret = OH_VideoEncoder_FreeOutputBuffer(codec, index);
}
