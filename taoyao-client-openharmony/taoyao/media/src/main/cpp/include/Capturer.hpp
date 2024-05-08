/**
 * 采集器
 * 
 * TODO: 释放资源判断空指针
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/camera/camera-overview.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/avcodec/audio-encoding.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/avcodec/video-encoding.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/media/avscreen-capture.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/opengles.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/opensles.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/camera/native-camera-recording.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/avcodec/obtain-supported-codecs.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/audio/using-ohaudio-for-recording.md
 */

#ifndef TAOYAO_CAPTURER_HPP
#define TAOYAO_CAPTURER_HPP

#include <map>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl32.h>

#include "./Signal.hpp"

#include <native_image/native_image.h>
#include <native_buffer/native_buffer.h>
#include <native_window/external_window.h>

#include <ohcamera/camera.h>
#include <ohcamera/video_output.h>
#include <ohcamera/capture_session.h>

#include "api/media_stream_track.h"

#include <ohaudio/native_audiocapturer.h>
#include <ohaudio/native_audiostreambuilder.h>

#include <multimedia/player_framework/native_avcodec_videoencoder.h>

namespace acgist {

/**
 * 采集器
 * 
 * @tparam Sink 输出管道
 */
template <typename Sink>
class Capturer {
    
protected:
    bool running = false;
    
public:
    // id = rtc::scoped_refptr
    std::map<std::string, Sink*> map;

public:
    Capturer();
    virtual ~Capturer();
    
public:
    // 添加管道
    virtual bool add(const std::string& id, Sink* sink);
    // 删除管道
    virtual bool remove(const std::string& id);
    // 开始采集
    virtual bool start() = 0;
    // 结束采集
    virtual bool stop()  = 0;
    
};

template <typename Sink>
acgist::Capturer<Sink>::Capturer() {}

template <typename Sink>
acgist::Capturer<Sink>::~Capturer() {
    for (auto iterator = this->map.begin(); iterator != this->map.end(); ++iterator) {
        // TODO：释放
//        delete iterator->second;
//        iterator->second = nullptr;
    }
    this->map.clear();
}

template <typename Sink>
bool acgist::Capturer<Sink>::add(const std::string& id, Sink* sink) {
    this->map.insert({ id, sink });
    return true;
}

template <typename Sink>
bool acgist::Capturer<Sink>::remove(const std::string& id) {
    auto iterator = this->map.find(id);
    if (iterator == this->map.end()) {
        return false;
    }
    // TODO：释放
//    delete iterator->second;
//    iterator->second = nullptr;
    this->map.erase(iterator);
    return true;
}

/**
 * 音频采集器
 */
class AudioCapturer: public Capturer<webrtc::AudioTrackSinkInterface> {

public:
    // 音频构造器
    OH_AudioStreamBuilder* builder  = nullptr;
    // 音频采集器
    OH_AudioCapturer* audioCapturer = nullptr;

public:
    AudioCapturer();
    virtual ~AudioCapturer();
    
public:
    virtual bool start() override;
    virtual bool stop()  override;
    
};

/**
 * 视频编码
 */
class VideoEncoder {
    
public:
    // 视频编码器
    OH_AVCodec* avCodec = nullptr;
    // 视频窗口
    OHNativeWindow* nativeWindow = nullptr;
    
public:
    VideoEncoder();
    virtual ~VideoEncoder();
    
public:
    // 初始配置
    void initFormatConfig(OH_AVFormat* format);
    // 重新开始
    void restart();
    // 动态配置
    void reset(OH_AVFormat* format);
    // 动态配置
    void resetIntConfig(const char* key, int32_t value);
    // 动态配置
    void resetLongConfig(const char* key, int64_t value);
    // 动态配置
    void resetDoubleConfig(const char* key, double value);
    // 开始编码
    virtual bool start();
    // 结束编码
    virtual bool stop();
    
};

/**
 * 视频采集器
 */
class VideoCapturer: public Capturer<rtc::VideoSinkInterface<webrtc::VideoFrame>> {
    
public:
    // SurfaceId
    uint64_t surfaceId = 0;
    // OpenGL纹理指针
    GLuint textureId = 0;
    // OpenGL纹理数量
    GLsizei textureSize = 1;
    // NativeImage
    OH_NativeImage* nativeImage = nullptr;
    // OHNativeWindow
    OHNativeWindow* nativeWindow = nullptr;
    // EGL显示设备
    EGLDisplay eglDisplay = EGL_NO_DISPLAY;
    // EGL上下文
    EGLContext eglContext = EGL_NO_CONTEXT;
    // EGL Surface
    EGLSurface eglSurface = EGL_NO_SURFACE;
    // 摄像头设备数量
    uint32_t cameraSize = 0;
    // 摄像头索引
    uint32_t cameraIndex = 0;
    // 摄像头设备列表
    Camera_Device* cameraDevice = nullptr;
    // 摄像头管理器
    Camera_Manager* cameraManager = nullptr;
    // 摄像头视频输出
    Camera_VideoOutput* cameraVideoOutput = nullptr;
    // 摄像头视频会话
    Camera_CaptureSession* cameraCaptureSession = nullptr;
    // 摄像头输出能力
    Camera_OutputCapability *cameraOutputCapability = nullptr;

public:
    VideoCapturer();
    virtual ~VideoCapturer();
    
public:
    void initOpenGLES();
    void releaseOpenGLES();
    virtual bool start() override;
    virtual bool stop()  override;
    
};

}

#endif // TAOYAO_CAPTURER_HPP
