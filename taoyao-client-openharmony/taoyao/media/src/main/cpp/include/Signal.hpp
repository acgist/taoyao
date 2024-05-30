/**
 * 信令
 * 
 * @author acgist
 */

#ifndef TAOYAO_SIGNAL_HPP
#define TAOYAO_SIGNAL_HPP

#include <napi/native_api.h>

#include <multimedia/image_framework/image_receiver_mdk.h>

#ifndef TAOYAO_AUDIO_RET_LOG
#define TAOYAO_AUDIO_RET_LOG(format, ret, ...)          \
if(ret == OH_AudioStream_Result::AUDIOSTREAM_SUCCESS) { \
    OH_LOG_INFO(LOG_APP, format, ret, ##__VA_ARGS__);   \
} else {                                                \
    OH_LOG_ERROR(LOG_APP, format, ret, ##__VA_ARGS__);  \
}
#endif

#ifndef TAOYAO_VIDEO_RET_LOG
#define TAOYAO_VIDEO_RET_LOG(format, ret, ...)         \
if(ret == Camera_ErrorCode::CAMERA_OK) {               \
    OH_LOG_INFO(LOG_APP, format, ret, ##__VA_ARGS__);  \
} else {                                               \
    OH_LOG_ERROR(LOG_APP, format, ret, ##__VA_ARGS__); \
}
#endif

#ifndef TAOYAO_OPENGL_RET_LOG
#define TAOYAO_OPENGL_RET_LOG(format, ret, ...)        \
if(ret) {                                              \
    OH_LOG_INFO(LOG_APP, format, ret, ##__VA_ARGS__);  \
} else {                                               \
    OH_LOG_ERROR(LOG_APP, format, ret, ##__VA_ARGS__); \
}
#endif

#include <string>

#include <json.hpp>

namespace acgist {

// 成功状态编码
const int SUCCESS_CODE = 0;
// 视频宽度
extern uint32_t width;
// 视频高度
extern uint32_t height;
// 视频码率
extern uint64_t bitrate;
// 关键帧的频率
extern uint32_t iFrameInterval;
// 视频帧率
extern double frameRate;
// 采样率
extern int32_t samplingRate;
// 声道数
extern int32_t channelCount;
// 采样位数
extern int32_t bitsPerSample;
// 终端ID
extern std::string clientId;
// 终端名称
extern std::string name;
// 预览句柄
extern std::string surfaceId;
// 环境
extern napi_env env;
// 图片接收
extern ImageReceiverNative* imageReceiverNative;

/**
 * 发送消息
 *
 * @param signal 信令
 * @param body   主体
 * @param id     ID
 */
extern void push(const std::string& signal, const std::string& body, uint64_t id = 0L);

/**
 * 发送请求
 * 
 * @param signal 信令
 * @param body   主体
 * @param id     ID
 * 
 * @return 响应
 */
extern nlohmann::json request(const std::string& signal, const std::string& body, uint64_t id = 0L);

}

#endif //TAOYAO_SIGNAL_HPP
