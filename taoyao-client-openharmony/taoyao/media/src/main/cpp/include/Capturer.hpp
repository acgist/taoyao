/**
 * 采集器
 * 
 * 音频默认使用OHOS实现：modules/audio_device/ohos/audio_device_template.h
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

// 本地音频采集
#define __TAOYAO_AUDIO_LOCAL__ false
// 本地视频采集
#define __TAOYAO_VIDEO_LOCAL__ true

#include <map>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <EGL/eglplatform.h>
#include <GLES3/gl32.h>

#include "./Signal.hpp"
#include "./WebRTC.hpp"

#include <native_image/native_image.h>
#include <native_buffer/native_buffer.h>
#include <native_window/external_window.h>

#include <ohcamera/camera.h>
#include "ohcamera/camera_input.h"
#include <ohcamera/video_output.h>
#include <ohcamera/capture_session.h>

#include "api/media_stream_track.h"
#include "api/media_stream_interface.h"

#include <ohaudio/native_audiocapturer.h>
#include <ohaudio/native_audiostreambuilder.h>

#include <multimedia/player_framework/native_avscreen_capture.h>
#include <multimedia/player_framework/native_avscreen_capture_base.h>
#include <multimedia/player_framework/native_avscreen_capture_errors.h>

namespace acgist {

/**
 * 采集器
 * 
 * @tparam Source 输出管道
 */
template <typename Source>
class Capturer {
    
protected:
    // 是否运行
    bool running = false;
    
public:
    Source* source;

public:
    Capturer();
    virtual ~Capturer();
    
public:
    // 开始采集
    virtual bool start() = 0;
    // 结束采集
    virtual bool stop()  = 0;
    
};

template <typename Source>
acgist::Capturer<Source>::Capturer() {}

template <typename Source>
acgist::Capturer<Source>::~Capturer() {
    if(this->source != nullptr) {
        this->source->Release();
        // delete this->source;
        this->source = nullptr;
    }
}

/**
 * 音频采集器
 */
class AudioCapturer: public Capturer<acgist::TaoyaoAudioTrackSource> {

public:
    // 音频构造器
    OH_AudioStreamBuilder* builder  = nullptr;
    // 音频采集器
    OH_AudioCapturer* audioCapturer = nullptr;

public:
    AudioCapturer();
    virtual ~AudioCapturer() override;
    
public:
    virtual bool start() override;
    virtual bool stop()  override;
    
};

/**
 * 视频采集器
 */
class VideoCapturer : public Capturer<acgist::TaoyaoVideoTrackSource> {
    
public:
    VideoCapturer() {};
    virtual ~VideoCapturer() override {};
    
public:
    virtual bool start() override = 0;
    virtual bool stop()  override = 0;
    
};

/**
 * 相机采集器
 */
class CameraCapturer : public VideoCapturer {
    
public:
    // ================ OpenGL ES ================
    // OpenGL ES SurfaceId
    uint64_t surfaceId = 0;
    // OpenGL ES纹理指针
    GLuint textureId = 0;
    // OpenGL ES纹理数量
    GLsizei textureSize = 1;
    // EGL显示设备
    EGLDisplay eglDisplay = EGL_NO_DISPLAY;
    // EGL上下文
    EGLContext eglContext = EGL_NO_CONTEXT;
    // EGL Surface
    EGLSurface eglSurface = EGL_NO_SURFACE;
    // ================ Camera ================
    // 相机设备数量
    uint32_t cameraSize = 0;
    // 相机索引
    uint32_t cameraIndex = 0;
    // NativeImage
    OH_NativeImage* nativeImage = nullptr;
    // 相机输入
    Camera_Input* cameraInput = nullptr;
    // 相机设备列表
    Camera_Device* cameraDevice = nullptr;
    // 相机管理
    Camera_Manager* cameraManager = nullptr;
    // 相机视频输出
    Camera_VideoOutput* cameraVideoOutput = nullptr;
    // 相机预览输出
    Camera_PreviewOutput* cameraPreviewOutput = nullptr;
    // 相机视频会话
    Camera_CaptureSession* cameraCaptureSession = nullptr;
    // 相机输出能力
    Camera_OutputCapability* cameraOutputCapability = nullptr;

public:
    CameraCapturer();
    virtual ~CameraCapturer() override;
    
public:
    // 加载OpenGL ES
    void initOpenGLES();
    // 释放OpenGL ES
    void releaseOpenGLES();
    virtual bool start() override;
    virtual bool stop()  override;
    
};

/**
 * 屏幕采集器
 */
class ScreenCapturer : public VideoCapturer {
    
public:
    OH_AVScreenCapture* avScreenCapture = nullptr;
    
public:
    ScreenCapturer();
    virtual ~ScreenCapturer() override;
    
public:
    virtual bool start() override;
    virtual bool stop()  override;
    
};

}

#endif // TAOYAO_CAPTURER_HPP
