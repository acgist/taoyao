/**
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/vulkan.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/camera/camera-overview.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/opengles.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/egl-symbol.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/graphics/native-image-guidelines.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/graphics/native-window-guidelines.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/vulkan-guidelines.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/openglesv3-symbol.md
 */

#include "../include/Capturer.hpp"

#include "hilog/log.h"

#include "rtc_base/time_utils.h"

#include "api/video/nv12_buffer.h"
#include "api/video/i420_buffer.h"

#include <ohcamera/camera_manager.h>
#include <ohcamera/capture_session.h>

// 采集回调
static void onError(Camera_VideoOutput* videoOutput, Camera_ErrorCode errorCode);
static void onFrameEnd(Camera_VideoOutput* videoOutput, int32_t frameCount);
static void onFrameStart(Camera_VideoOutput* videoOutput);

// 数据回调
static void onFrame(void* context);

// 检查EGL扩展
static bool CheckEglExtension(const char* extensions, const char* extension);

acgist::VideoCapturer::VideoCapturer() {
    initOpenGLES();
    Camera_ErrorCode ret = OH_Camera_GetCameraManager(&this->cameraManager);
    OH_LOG_INFO(LOG_APP, "获取摄像头管理器：%o", ret);
    ret = OH_CameraManager_GetSupportedCameras(this->cameraManager, &this->cameraDevice, &this->cameraSize);
    OH_LOG_INFO(LOG_APP, "获取摄像头设备列表：%o %d", ret, this->cameraSize);
    ret = OH_CameraManager_GetSupportedCameraOutputCapability(this->cameraManager, &this->cameraDevice[this->cameraIndex], &this->cameraOutputCapability);
    OH_LOG_INFO(LOG_APP, "获取摄像头输出功能：%o %d %d", ret, this->cameraIndex, this->cameraOutputCapability->videoProfilesSize);
    // 注册相机状态回调
    // OH_CameraManager_RegisterCallback(this->cameraManager, CameraManager_Callbacks* callback);
    ret = OH_CameraManager_CreateVideoOutput(this->cameraManager, this->cameraOutputCapability->videoProfiles[0], (char*) this->surfaceId, &this->cameraVideoOutput);
    OH_LOG_INFO(LOG_APP, "创建摄像头视频输出：%o", ret);
    ret = OH_CameraManager_CreateCaptureSession(this->cameraManager, &this->cameraCaptureSession);
    OH_LOG_INFO(LOG_APP, "创建摄像头视频会话：%o", ret);
    // 设置相机：闪光、变焦、质量、高宽、补光、防抖
    // 设置缩放比例
    // OH_CaptureSession_SetZoomRatio(this->cameraCaptureSession, 0.5F);
}

acgist::VideoCapturer::~VideoCapturer() {
    releaseOpenGLES();
    Camera_ErrorCode ret = OH_CaptureSession_Release(this->cameraCaptureSession);
    this->cameraCaptureSession = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头视频会话：%o", ret);
    ret = OH_VideoOutput_Release(this->cameraVideoOutput);
    this->cameraVideoOutput = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头视频输出：%o", ret);
    ret = OH_CameraManager_DeleteSupportedCameraOutputCapability(this->cameraManager, this->cameraOutputCapability);
    this->cameraOutputCapability = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头输出能力：%o", ret);
    ret = OH_CameraManager_DeleteSupportedCameras(this->cameraManager, this->cameraDevice, this->cameraSize);
    this->cameraDevice = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头设备列表：%o", ret);
    ret = OH_Camera_DeleteCameraManager(this->cameraManager);
    this->cameraManager = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头管理器：%o", ret);
}

bool acgist::VideoCapturer::start() {
    if (this->running) {
        return true;
    }
    this->running = true;
    OH_CaptureSession_BeginConfig(this->cameraCaptureSession);
    Camera_ErrorCode ret = OH_CaptureSession_AddVideoOutput(this->cameraCaptureSession, this->cameraVideoOutput);
    OH_LOG_INFO(LOG_APP, "视频捕获绑定会话：%o", ret);
    OH_CaptureSession_CommitConfig(this->cameraCaptureSession);
    ret = OH_CaptureSession_Start(this->cameraCaptureSession);
    OH_LOG_INFO(LOG_APP, "开始视频会话：%o", ret);
    ret = OH_VideoOutput_Start(this->cameraVideoOutput);
    OH_LOG_INFO(LOG_APP, "开始视频捕获：%o", ret);
    VideoOutput_Callbacks callbacks;
    callbacks.onError      = onError;
    callbacks.onFrameEnd   = onFrameEnd;
    callbacks.onFrameStart = onFrameStart;
    ret = OH_VideoOutput_RegisterCallback(this->cameraVideoOutput, &callbacks);
    OH_LOG_INFO(LOG_APP, "视频捕获回调：%o", ret);
    OH_NativeImage_AttachContext(this->nativeImage, this->textureId);
    return ret = Camera_ErrorCode::CAMERA_OK;
}

bool acgist::VideoCapturer::stop() {
    if (!this->running) {
        return true;
    }
    this->running = false;
    OH_NativeImage_DetachContext(this->nativeImage);
    OH_CaptureSession_BeginConfig(this->cameraCaptureSession);
    Camera_ErrorCode ret = OH_CaptureSession_RemoveVideoOutput(this->cameraCaptureSession, this->cameraVideoOutput);
    OH_LOG_INFO(LOG_APP, "视频捕获取消绑定会话：%o", ret);
    OH_CaptureSession_CommitConfig(this->cameraCaptureSession);
    ret = OH_CaptureSession_Stop(this->cameraCaptureSession);
    OH_LOG_INFO(LOG_APP, "结束视频会话：%o", ret);
    ret = OH_VideoOutput_Stop(this->cameraVideoOutput);
    // OH_VideoOutput_UnregisterCallback(this->cameraVideoOutput, callback);
    OH_LOG_INFO(LOG_APP, "结束视频捕获：%o", ret);
    return ret = Camera_ErrorCode::CAMERA_OK;
}

void acgist::VideoCapturer::initOpenGLES() {
    // IMAGE WINDOW
    glGenTextures(this->textureSize, &this->textureId);
    this->nativeImage = OH_NativeImage_Create(this->textureId, GL_TEXTURE_2D);
    // TODO: 验证是否需要window
//    this->nativeWindow = OH_NativeImage_AcquireNativeWindow(this->nativeImage);
//    OH_NativeWindow_NativeWindowHandleOpt(this->nativeWindow, SET_BUFFER_GEOMETRY, acgist::width, acgist::height);
//    OH_NativeWindow_NativeWindowHandleOpt(this->nativeWindow, SET_TIMEOUT, 100'000);
//    OH_NativeWindow_NativeWindowHandleOpt(this->nativeWindow, GET_TIMEOUT, 100'000);
    OH_OnFrameAvailableListener listener = { this, onFrame };
    OH_NativeImage_SetOnFrameAvailableListener(this->nativeImage, listener);
    // EGL
    static const char* EGL_EXT_PLATFORM_WAYLAND     = "EGL_EXT_platform_wayland";
    static const char* EGL_KHR_PLATFORM_WAYLAND     = "EGL_KHR_platform_wayland";
    static const char* EGL_GET_PLATFORM_DISPLAY_EXT = "eglGetPlatformDisplayEXT";
    // 当前
    this->eglDisplay = eglGetCurrentDisplay();
    // 扩展
    if(this->eglDisplay == EGL_NO_DISPLAY) {
        const char* extensions = eglQueryString(EGL_NO_DISPLAY, EGL_EXTENSIONS);
        if (extensions && (CheckEglExtension(extensions, EGL_EXT_PLATFORM_WAYLAND) || CheckEglExtension(extensions, EGL_KHR_PLATFORM_WAYLAND))) {
            PFNEGLGETPLATFORMDISPLAYEXTPROC eglGetPlatformDisplayExt = (PFNEGLGETPLATFORMDISPLAYEXTPROC) eglGetProcAddress(EGL_GET_PLATFORM_DISPLAY_EXT);
            this->eglDisplay = eglGetPlatformDisplayExt(EGL_PLATFORM_OHOS_KHR, EGL_DEFAULT_DISPLAY, nullptr);
        }
    }
    // 新建
    if(this->eglDisplay == EGL_NO_DISPLAY) {
        this->eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    }
    EGLint count;
    EGLConfig config;
    EGLint config_attribs[] = {
        EGL_SURFACE_TYPE,    EGL_WINDOW_BIT,
        EGL_RED_SIZE,        8,
        EGL_GREEN_SIZE,      8,
        EGL_BLUE_SIZE,       8,
        EGL_ALPHA_SIZE,      8,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_NONE
    };
    EGLint context_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };
    EGLBoolean ret = eglChooseConfig(this->eglDisplay, config_attribs, &config, 1, &count);
    OH_LOG_INFO(LOG_APP, "EGL选择配置：%d", ret);
    // 当前
    this->eglContext = eglGetCurrentContext();
    // 新建
    if(this->eglContext == EGL_NO_CONTEXT) {
        EGLint major;
        EGLint minor;
        ret = eglInitialize(this->eglDisplay, &major, &minor);
        OH_LOG_INFO(LOG_APP, "加载EGL：%d", ret);
        ret = eglBindAPI(EGL_OPENGL_ES_API);
        OH_LOG_INFO(LOG_APP, "绑定EGL：%d", ret);
        this->eglContext = eglCreateContext(this->eglDisplay, config, EGL_NO_CONTEXT, context_attribs);
    }
    this->eglSurface = eglCreatePbufferSurface(this->eglDisplay, config, context_attribs);
    // TODO: 验证强转
//    this->eglSurface = eglCreateWindowSurface(this->eglDisplay, config, (EGLNativeWindowType) this->nativeWindow, context_attribs);
    eglMakeCurrent(this->eglDisplay, this->eglSurface, this->eglSurface, this->eglContext);
    OH_NativeImage_GetSurfaceId(this->nativeImage, &this->surfaceId);
}

void acgist::VideoCapturer::releaseOpenGLES() {
    if(this->textureId != 0) {
        glDeleteTextures(this->textureSize, &this->textureId);
        this->textureId = 0;
    }
    if(this->eglSurface != EGL_NO_SURFACE) {
        EGLBoolean ret = eglDestroySurface(this->eglDisplay, this->eglSurface);
        this->eglSurface = EGL_NO_SURFACE;
        OH_LOG_INFO(LOG_APP, "销毁EGLSurface：%d", ret);
    }
    if(this->eglContext != EGL_NO_CONTEXT) {
        EGLBoolean ret = eglDestroyContext(this->eglDisplay, this->eglContext);
        this->eglContext = EGL_NO_CONTEXT;
        OH_LOG_INFO(LOG_APP, "销毁EGLContext：%d", ret);
    }
    if(this->nativeImage != nullptr) {
        OH_NativeImage_Destroy(&this->nativeImage);
        this->nativeImage = nullptr;
        OH_LOG_INFO(LOG_APP, "销毁nativeImage");
    }
    if(this->nativeWindow != nullptr) {
        OH_NativeWindow_DestroyNativeWindow(this->nativeWindow);
        this->nativeWindow = nullptr;
        OH_LOG_INFO(LOG_APP, "销毁nativeWindow");
    }
}

static void onError(Camera_VideoOutput* videoOutput, Camera_ErrorCode errorCode) {
    OH_LOG_WARN(LOG_APP, "视频捕获数据帧失败：%d", errorCode);
}

static void onFrameEnd(Camera_VideoOutput* videoOutput, int32_t frameCount) {
    OH_LOG_DEBUG(LOG_APP, "结束视频捕获数据帧");
}

static void onFrameStart(Camera_VideoOutput* videoOutput) {
    OH_LOG_DEBUG(LOG_APP, "开始视频捕获数据帧");
}

static void onFrame(void* context) {
//    // TODO: 解析
//    rtc::scoped_refptr<webrtc::I420Buffer> videoFrameBuffer =
//        webrtc::I420Buffer::Copy(width, height, (uint8_t *)data, 0, (uint8_t *)data, 0, (uint8_t *)data, 0);
//    // webrtc::NV12Buffer::Create(width, height);
//    webrtc::VideoFrame::Builder builder;
//    webrtc::VideoFrame videoFrame =
//        builder.set_timestamp_ms(rtc::TimeMillis()).set_video_frame_buffer(videoFrameBuffer).build();
//    for (auto iterator = videoCapturer->map.begin(); iterator != videoCapturer->map.end(); ++iterator) {
//        iterator->second->OnFrame(videoFrame);
//    }
//    // TODO: 释放webrtc
//    videoFrameBuffer->Release();
}

static bool CheckEglExtension(const char* extensions, const char* extension) {
    char CHARACTER_WHITESPACE = ' ';
    size_t extlen = strlen(extension);
    const char* CHARACTER_STRING_WHITESPACE = " ";
    const char* end = extensions + strlen(extensions);
    while (extensions < end) {
        size_t index = 0;
        if (*extensions == CHARACTER_WHITESPACE) {
            extensions++;
            continue;
        }
        index = strcspn(extensions, CHARACTER_STRING_WHITESPACE);
        if (index == extlen && strncmp(extension, extensions, index) == 0) {
            return true;
        }
        extensions += index;
    }
    return false;
}
