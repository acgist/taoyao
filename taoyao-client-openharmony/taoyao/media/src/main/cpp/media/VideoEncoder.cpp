#include "../include/Capturer.hpp"

#include "hilog/log.h"

#include <native_window/external_window.h>
#include <multimedia/player_framework/native_avbuffer.h>
#include <multimedia/player_framework/native_averrors.h>
#include <multimedia/player_framework/native_avformat.h>
#include <multimedia/player_framework/native_avcodec_base.h>
#include <multimedia/player_framework/native_avcapability.h>

// 编码回调
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
    // 配置编码参数
    OH_AVFormat* format = OH_AVFormat_Create();
    this->initFormatConfig(format);
    ret = OH_VideoEncoder_Configure(this->avCodec, format);
    OH_AVFormat_Destroy(format);
    OH_LOG_INFO(LOG_APP, "配置编码参数：%o %d %d %f %ld", ret, acgist::width, acgist::height, acgist::frameRate, acgist::bitrate);
    // 准备就绪
    ret = OH_VideoEncoder_Prepare(this->avCodec);
    OH_LOG_INFO(LOG_APP, "视频编码准备就绪：%o", ret);
    // 配置surface
    ret = OH_VideoEncoder_GetSurface(this->avCodec, &this->nativeWindow);
    OH_LOG_INFO(LOG_APP, "配置视频窗口：%o", ret);
}

acgist::VideoEncoder::~VideoEncoder() {
    if(this->avCodec != nullptr) {
        OH_AVErrCode ret = OH_VideoEncoder_Destroy(this->avCodec);
        this->avCodec = nullptr;
        OH_LOG_INFO(LOG_APP, "释放视频编码器：%o", ret);
    }
    if(this->nativeWindow != nullptr) {
        OH_NativeWindow_DestroyNativeWindow(this->nativeWindow);
        this->nativeWindow = nullptr;
        OH_LOG_INFO(LOG_APP, "释放视频窗口");
    }
}

void acgist::VideoEncoder::initFormatConfig(OH_AVFormat* format) {
    // https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/avcodec/video-encoding.md
    // 配置视频宽度
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_WIDTH, acgist::width);
    // 配置视频高度
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_HEIGHT, acgist::height);
    // 配置视频比特率
    OH_AVFormat_SetLongValue(format, OH_MD_KEY_BITRATE, acgist::bitrate);
    // 配置视频帧率
    OH_AVFormat_SetDoubleValue(format, OH_MD_KEY_FRAME_RATE, acgist::frameRate);
    // 配置视频颜色格式
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_PIXEL_FORMAT, AV_PIXEL_FORMAT_YUVI420);
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
    // 配置所需的编码质量：只有在恒定质量模式下配置的编码器才支持此配置
    OH_AVFormat_SetIntValue(format, OH_MD_KEY_QUALITY, 0);
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
    return ret == OH_AVErrCode::AV_ERR_OK;
}

bool acgist::VideoEncoder::stop() {
    OH_AVErrCode ret = OH_VideoEncoder_NotifyEndOfStream(this->avCodec);
    OH_LOG_INFO(LOG_APP, "通知视频编码结束：%o", ret);
    ret = OH_VideoEncoder_Stop(this->avCodec);
    OH_LOG_INFO(LOG_APP, "结束视频编码：%o", ret);
    return ret == OH_AVErrCode::AV_ERR_OK;
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
    // TODO: 继续处理
    ret = OH_VideoEncoder_FreeOutputBuffer(codec, index);
}
