#include "../include/Capturer.hpp"

#include "hilog/log.h"

#include "rtc_base/time_utils.h"

#include "api/video/nv12_buffer.h"
#include "api/video/i420_buffer.h"

#include <multimedia/player_framework/native_avbuffer.h>
#include <multimedia/player_framework/native_avformat.h>
#include <multimedia/player_framework/native_avcodec_base.h>
#include <multimedia/player_framework/native_avcapability.h>

static uint32_t width   = 720;
static uint32_t height  = 480;
static uint64_t bitrate = 3'000'000L;
static double frameRate = 30.0;

static void OnError(OH_AVCodec* codec, int32_t errorCode, void* userData);
static void OnStreamChanged(OH_AVCodec* codec, OH_AVFormat* format, void* userData);
static void OnNeedInputBuffer(OH_AVCodec* codec, uint32_t index, OH_AVBuffer* buffer, void* userData);
static void OnNewOutputBuffer(OH_AVCodec* codec, uint32_t index, OH_AVBuffer* buffer, void* userData);
    
acgist::VideoEncoder::VideoEncoder() {
    OH_AVCapability* capability = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_AVC, true);
    const char* codecName = OH_AVCapability_GetName(capability);
    this->avCodec = OH_VideoEncoder_CreateByName(codecName);
    OH_LOG_INFO(LOG_APP, "视频编码格式：%s", codecName);
    // 注册回调
    OH_AVCodecCallback callback = { &OnError, &OnStreamChanged, &OnNeedInputBuffer, &OnNewOutputBuffer };
    OH_AVErrCode ret = OH_VideoEncoder_RegisterCallback(this->avCodec, callback, this);
    OH_LOG_INFO(LOG_APP, "注册编码回调：%o", ret);
    // 配置编码参数：https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/avcodec/video-encoding.md
    OH_AVFormat* format = OH_AVFormat_Create();
    // 配置视频帧宽度（必须）
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_WIDTH, width);
    // 配置视频帧高度（必须）
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_HEIGHT, height);
    // 配置视频颜色格式（必须）
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_PIXEL_FORMAT, AV_PIXEL_FORMAT_YUVI420);
    // OH_AVFormat_SetIntValue(format, OH_MD_KEY_PIXEL_FORMAT, AV_PIXEL_FORMAT_NV12);
    // 配置视频帧速率
    OH_AVFormat_SetDoubleValue(format, OH_MD_KEY_FRAME_RATE, frameRate);
    // 配置视频YUV值范围标志
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_RANGE_FLAG, false);
    // 配置视频原色
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_COLOR_PRIMARIES, static_cast<int32_t>(OH_ColorPrimary::COLOR_PRIMARY_BT709));
    // 配置传输特性
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_TRANSFER_CHARACTERISTICS, static_cast<int32_t>(OH_TransferCharacteristic::TRANSFER_CHARACTERISTIC_BT709));
    // 配置最大矩阵系数
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_MATRIX_COEFFICIENTS, static_cast<int32_t>(OH_MatrixCoefficient::MATRIX_COEFFICIENT_IDENTITY));
    // 配置关键帧的间隔（单位为：毫秒）
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_I_FRAME_INTERVAL, 5000);
    // 配置编码Profile
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_PROFILE, static_cast<int32_t>(OH_AVCProfile::AVC_PROFILE_BASELINE));
    // 配置编码比特率模式
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_VIDEO_ENCODE_BITRATE_MODE, static_cast<int32_t>(OH_VideoEncodeBitrateMode::CBR));
    // 配置比特率
    OH_AVFormat_SetLongValue(format, OH_MD_KEY_BITRATE, bitrate);
    // 配置所需的编码质量：只有在恒定质量模式下配置的编码器才支持此配置
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_QUALITY, 0);
    ret = OH_VideoEncoder_Configure(this->avCodec, format);
    OH_AVFormat_Destroy(format);
    OH_LOG_INFO(LOG_APP, "配置编码参数：%o %d %d %f %ld", ret, width, height, frameRate, bitrate);
    ret = OH_VideoEncoder_Prepare(this->avCodec);
    OH_LOG_INFO(LOG_APP, "视频编码准备就绪：%o", ret);
    ret = OH_VideoEncoder_GetSurface(this->avCodec, &this->nativeWindow);
    OH_LOG_INFO(LOG_APP, "配置surface：%o", ret);
}

acgist::VideoEncoder::~VideoEncoder() {
    OH_AVErrCode ret = OH_VideoEncoder_Destroy(this->avCodec);
    this->avCodec = nullptr;
    OH_LOG_INFO(LOG_APP, "释放视频编码器：%o", ret);
    // TODO: delete nativeWindow
}

void acgist::VideoEncoder::restart() {
    OH_AVErrCode ret = OH_VideoEncoder_Flush(this->avCodec);
    OH_LOG_INFO(LOG_APP, "清空编码队列：%o", ret);
    ret = OH_VideoEncoder_Start(this->avCodec);
    OH_LOG_INFO(LOG_APP, "开始视频编码：%o", ret);
}

void acgist::VideoEncoder::reset(OH_AVFormat* format) {
    OH_AVErrCode ret = OH_VideoEncoder_Reset(this->avCodec);
    OH_LOG_INFO(LOG_APP, "重置视频编码：%o", ret);
    ret = OH_VideoEncoder_Configure(this->avCodec, format);
    OH_LOG_INFO(LOG_APP, "配置视频编码：%o", ret);
}

void acgist::VideoEncoder::resetIntConfig(const char* key, int32_t value) {
    OH_AVFormat* format = OH_AVFormat_Create();
    OH_AVFormat_SetIntValue(format, key, value);
    OH_AVErrCode ret = OH_VideoEncoder_SetParameter(this->avCodec, format);
    OH_LOG_INFO(LOG_APP, "动态配置视频编码：%o %s %d", ret, key, value);
    OH_AVFormat_Destroy(format);
}

void acgist::VideoEncoder::resetLongConfig(const char* key, int64_t value) {
    OH_AVFormat* format = OH_AVFormat_Create();
    OH_AVFormat_SetLongValue(format, key, value);
    OH_AVErrCode ret = OH_VideoEncoder_SetParameter(this->avCodec, format);
    OH_LOG_INFO(LOG_APP, "动态配置视频编码：%o %s %ld", ret, key, value);
    OH_AVFormat_Destroy(format);
}

void acgist::VideoEncoder::resetDoubleConfig(const char* key, double value) {
    OH_AVFormat* format = OH_AVFormat_Create();
    OH_AVFormat_SetDoubleValue(format, key, value);
    OH_AVErrCode ret = OH_VideoEncoder_SetParameter(this->avCodec, format);
    OH_LOG_INFO(LOG_APP, "动态配置视频编码：%o %s %f", ret, key, value);
    OH_AVFormat_Destroy(format);
}

bool acgist::VideoEncoder::start() {
    OH_AVErrCode ret = OH_VideoEncoder_Start(this->avCodec);
    OH_LOG_INFO(LOG_APP, "开始视频编码：%o", ret);
    return true;
}

bool acgist::VideoEncoder::stop() {
    OH_AVErrCode ret = OH_VideoEncoder_NotifyEndOfStream(this->avCodec);
    OH_LOG_INFO(LOG_APP, "通知视频编码结束：%o", ret);
    ret = OH_VideoEncoder_Stop(this->avCodec);
    OH_LOG_INFO(LOG_APP, "结束视频编码：%o", ret);
    return true;
}

static void OnError(OH_AVCodec* codec, int32_t errorCode, void* userData) {
    OH_LOG_WARN(LOG_APP, "视频编码发送错误：%d", errorCode);
}

static void OnStreamChanged(OH_AVCodec* codec, OH_AVFormat* format, void* userData) {
    OH_LOG_DEBUG(LOG_APP, "视频编码配置变化");
}

static void OnNeedInputBuffer(OH_AVCodec* codec, uint32_t index, OH_AVBuffer* buffer, void* userData) {
    // 忽略
}

static void OnNewOutputBuffer(OH_AVCodec* codec, uint32_t index, OH_AVBuffer* buffer, void* userData) {
    acgist::VideoCapturer* videoCapturer = (acgist::VideoCapturer*) userData;
    // TODO: 全局是否性能更好
    OH_AVCodecBufferAttr info;
    OH_AVErrCode ret = OH_AVBuffer_GetBufferAttr(buffer, &info);
    char* data = reinterpret_cast<char*>(OH_AVBuffer_GetAddr(buffer));
    // TODO: 解析
    rtc::scoped_refptr<webrtc::I420Buffer> videoFrameBuffer = webrtc::I420Buffer::Copy(width, height, (uint8_t*)data, 0, (uint8_t*)data, 0, (uint8_t*)data, 0);
    // webrtc::NV12Buffer::Create(width, height);
    webrtc::VideoFrame::Builder builder;
    webrtc::VideoFrame videoFrame = builder
        .set_timestamp_ms(rtc::TimeMillis())
        .set_video_frame_buffer(videoFrameBuffer)
        .set_rotation(webrtc::VideoRotation::kVideoRotation_0)
        .build();
    for (auto iterator = videoCapturer->map.begin(); iterator != videoCapturer->map.end(); ++iterator) {
        iterator->second->OnFrame(videoFrame);
    }
    // TODO: 释放webrtc
    videoFrameBuffer->Release();
    ret = OH_VideoEncoder_FreeOutputBuffer(codec, index);
}
