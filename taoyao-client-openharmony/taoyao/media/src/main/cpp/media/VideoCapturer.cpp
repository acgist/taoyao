/**
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/vulkan.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/camera/camera-overview.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/opengles.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/egl-symbol.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/graphics/native-image-guidelines.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/graphics/native-window-guidelines.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/vulkan-guidelines.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/openglesv3-symbol.md
 * 
 * https://gitee.com/openharmony/third_party_vulkan-headers/blob/master/include/vulkan/vulkan.hpp
 * https://gitee.com/openharmony/graphic_graphic_surface/blob/master/surface/src/native_window.cpp
 * https://gitee.com/openharmony/graphic_graphic_2d/blob/master/frameworks/surfaceimage/src/native_image.cpp
 * https://gitee.com/openharmony/arkui_ace_engine/blob/master/interfaces/native/native_interface_xcomponent.cpp
 * https://gitee.com/openharmony/multimedia_camera_framework/blob/master/frameworks/native/ndk/camera_manager.cpp
 * https://gitee.com/openharmony/multimedia_camera_framework/blob/master/frameworks/native/ndk/impl/camera_manager_impl.cpp
 * https://gitee.com/openharmony/multimedia_camera_framework/blob/master/frameworks/native/camera/src/session/capture_session.cpp
 */

#include "../include/Capturer.hpp"

#include <mutex>

#include "hilog/log.h"

#include <EGL/eglplatform.h>

#include "rtc_base/time_utils.h"

#include "api/video/nv12_buffer.h"
#include "api/video/i420_buffer.h"

#include <ohcamera/camera_manager.h>
#include <ohcamera/capture_session.h>

static std::recursive_mutex videoMutex;

// 采集回调
static void onError(Camera_VideoOutput* videoOutput, Camera_ErrorCode errorCode);
static void onFrameEnd(Camera_VideoOutput* videoOutput, int32_t frameCount);
static void onFrameStart(Camera_VideoOutput* videoOutput);

// 数据回调
static void onFrame(void* context);

// 检查EGL扩展
static bool CheckEglExtension(const char* extensions, const char* extension);

acgist::VideoCapturer::VideoCapturer() {
    #if __TAOYAO_VULKAN__
    initVulkan();
    #endif
    #if __TAOYAO_OPENGL__
    initOpenGLES();
    #endif
    Camera_ErrorCode ret = OH_Camera_GetCameraManager(&this->cameraManager);
    TAOYAO_VIDEO_RET_LOG("配置摄像头管理器：%{public}d", ret);
    ret = OH_CameraManager_GetSupportedCameras(this->cameraManager, &this->cameraDevice, &this->cameraSize);
    TAOYAO_VIDEO_RET_LOG("摄像头设备列表数量：%{public}d %{public}d", ret, this->cameraSize);
    ret = OH_CameraManager_CreateCameraInput(this->cameraManager, &this->cameraDevice[this->cameraIndex], &this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("配置摄像头输入：%{public}d %{public}d", ret, this->cameraIndex);
    ret = OH_CameraInput_Open(cameraInput);
    TAOYAO_VIDEO_RET_LOG("打开摄像头：%{public}d", ret);
    ret = OH_CameraManager_GetSupportedCameraOutputCapability(this->cameraManager, &this->cameraDevice[this->cameraIndex], &this->cameraOutputCapability);
    TAOYAO_VIDEO_RET_LOG("摄像头输出功能：%{public}d %{public}d %{public}d", ret, this->cameraIndex, this->cameraOutputCapability->videoProfilesSize);
    // 注册相机状态回调：可以取消注册
    // OH_CameraManager_RegisterCallback(this->cameraManager, CameraManager_Callbacks* callback);
    ret = OH_CameraManager_CreateVideoOutput(this->cameraManager, this->cameraOutputCapability->videoProfiles[0], std::to_string(this->surfaceId).data(), &this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("创建摄像头视频输出：%{public}d %{public}lld %{public}s", ret, this->surfaceId, std::to_string(this->surfaceId).data());
    ret = OH_CameraManager_CreateCaptureSession(this->cameraManager, &this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("创建摄像头视频会话：%{public}d", ret);
    // 设置相机：闪光、变焦、质量、高宽、补光、防抖
    // 设置缩放比例
    // OH_CaptureSession_SetZoomRatio(this->cameraCaptureSession, 0.5F);
}

acgist::VideoCapturer::~VideoCapturer() {
    #if __TAOYAO_VULKAN__
    releaseVulkan();
    #endif
    #if __TAOYAO_OPENGL__
    releaseOpenGLES();
    #endif
    Camera_ErrorCode ret = OH_CaptureSession_Release(this->cameraCaptureSession);
    this->cameraCaptureSession = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放摄像头视频会话：%{public}o", ret);
    ret = OH_VideoOutput_Release(this->cameraVideoOutput);
    this->cameraVideoOutput = nullptr;
    ret = OH_CameraInput_Release(this->cameraInput);
    this->cameraInput = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放摄像头：%{public}o", ret);
    TAOYAO_VIDEO_RET_LOG("释放摄像头视频输出：%{public}o", ret);
    ret = OH_CameraManager_DeleteSupportedCameraOutputCapability(this->cameraManager, this->cameraOutputCapability);
    this->cameraOutputCapability = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放摄像头输出能力：%{public}d", ret);
    ret = OH_CameraManager_DeleteSupportedCameras(this->cameraManager, this->cameraDevice, this->cameraSize);
    this->cameraDevice = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放摄像头设备列表：%{public}d", ret);
    ret = OH_Camera_DeleteCameraManager(this->cameraManager);
    this->cameraManager = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放摄像头管理器：%{public}d", ret);
}

bool acgist::VideoCapturer::start() {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    if (this->running) {
        return true;
    }
    this->running = true;
    OH_NativeImage_AttachContext(this->nativeImage, this->textureId);
    Camera_ErrorCode ret = OH_CaptureSession_BeginConfig(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("开始配置视频会话：%{public}o", ret);
    ret = OH_CaptureSession_AddInput(this->cameraCaptureSession, this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("视频输入绑定会话：%{public}d", ret);
    ret = OH_CaptureSession_AddVideoOutput(this->cameraCaptureSession, this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("视频捕获绑定会话：%{public}d", ret);
    ret = OH_CaptureSession_CommitConfig(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("结束配置视频会话：%{public}o", ret);
    ret = OH_CaptureSession_Start(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("开始视频会话：%{public}d", ret);
    ret = OH_VideoOutput_Start(this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("开始视频捕获：%{public}d", ret);
    VideoOutput_Callbacks callbacks;
    callbacks.onError      = onError;
    callbacks.onFrameEnd   = onFrameEnd;
    callbacks.onFrameStart = onFrameStart;
    // 可以取消注册
    ret = OH_VideoOutput_RegisterCallback(this->cameraVideoOutput, &callbacks);
    TAOYAO_VIDEO_RET_LOG("视频捕获回调：%{public}d", ret);
    return ret = Camera_ErrorCode::CAMERA_OK;
}

bool acgist::VideoCapturer::stop() {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    if (!this->running) {
        return true;
    }
    this->running = false;
    Camera_ErrorCode ret = OH_CaptureSession_BeginConfig(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("开始配置视频会话：%{public}o", ret);
    ret = OH_CaptureSession_RemoveInput(this->cameraCaptureSession, this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("视频输入取消绑定会话：%{public}o", ret);
    ret = OH_CaptureSession_RemoveVideoOutput(this->cameraCaptureSession, this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("视频捕获取消绑定会话：%{public}o", ret);
    ret = OH_CaptureSession_CommitConfig(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("结束配置视频会话：%{public}o", ret);
    ret = OH_CaptureSession_Stop(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("结束视频会话：%{public}o", ret);
    ret = OH_VideoOutput_Stop(this->cameraVideoOutput);
    // OH_VideoOutput_UnregisterCallback(this->cameraVideoOutput, callback);
    TAOYAO_VIDEO_RET_LOG("结束视频捕获：%{public}o", ret);
    ret = OH_CameraInput_Close(this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("关闭摄像头：%{public}o", ret);
    OH_NativeImage_DetachContext(this->nativeImage);
    return ret = Camera_ErrorCode::CAMERA_OK;
}

void acgist::VideoCapturer::initVulkan() {
    // vkApplicationInfo
    this->vkApplicationInfo.sType            = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    this->vkApplicationInfo.apiVersion       = VK_API_VERSION_1_3;
    this->vkApplicationInfo.pEngineName      = "vulkan-taoyao";
    this->vkApplicationInfo.pApplicationName = "vulkan-taoyao";
    OH_LOG_INFO(LOG_APP, "配置vkApplicationInfo");
    // vkInstanceCreateInfo
    this->vkInstanceCreateInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    this->vkInstanceCreateInfo.pNext = NULL;
    this->vkInstanceCreateInfo.pApplicationInfo = &this->vkApplicationInfo;
    OH_LOG_INFO(LOG_APP, "配置vkInstanceCreateInfo");
    // vkInstanceCreateInfo
    std::vector<const char*> instanceExtensions = {
        VK_KHR_SURFACE_EXTENSION_NAME,
        VK_OHOS_SURFACE_EXTENSION_NAME
    };
    vkInstanceCreateInfo.enabledExtensionCount   = static_cast<uint32_t>(instanceExtensions.size());
    vkInstanceCreateInfo.ppEnabledExtensionNames = instanceExtensions.data();
    OH_LOG_INFO(LOG_APP, "配置vkInstanceCreateInfo");
    VkResult ret = vkCreateInstance(&this->vkInstanceCreateInfo, nullptr, &this->vkInstance);
    OH_LOG_INFO(LOG_APP, "加载vkInstance：%d", ret);
    this->vkSurfaceCreateInfoOHOS.sType  = VK_STRUCTURE_TYPE_SURFACE_CREATE_INFO_OHOS;
    this->vkSurfaceCreateInfoOHOS.window = this->nativeWindow;
    OH_LOG_INFO(LOG_APP, "配置vkSurfaceCreateInfoOHOS");
    ret = vkCreateSurfaceOHOS(this->vkInstance, &this->vkSurfaceCreateInfoOHOS, nullptr, &this->vkSurfaceKHR);
    OH_LOG_INFO(LOG_APP, "加载vkSurfaceKHR：%d", ret);
}

void acgist::VideoCapturer::releaseVulkan() {
    if(this->vkSurfaceKHR != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(this->vkInstance, this->vkSurfaceKHR, nullptr);
        this->vkSurfaceKHR = VK_NULL_HANDLE;
    }
    if(this->vkInstance != VK_NULL_HANDLE) {
        vkDestroyInstance(this->vkInstance, nullptr);
        this->vkInstance = VK_NULL_HANDLE;
    }
}

void acgist::VideoCapturer::initOpenGLES() {
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
            OH_LOG_INFO(LOG_APP, "扩展EGLDisplay");
        }
    }
    // 新建
    if(this->eglDisplay == EGL_NO_DISPLAY) {
        this->eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        OH_LOG_INFO(LOG_APP, "新建EGLDisplay");
    }
    // 配置
    EGLint count;
    EGLConfig config;
    EGLint config_attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
        EGL_SURFACE_TYPE,    EGL_PBUFFER_BIT,
        EGL_RED_SIZE,        8,
        EGL_GREEN_SIZE,      8,
        EGL_BLUE_SIZE,       8,
        EGL_ALPHA_SIZE,      8,
        EGL_DEPTH_SIZE,      0,
        EGL_STENCIL_SIZE,    0,
        EGL_NONE
    };
    EGLint context_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };
    EGLBoolean ret = eglChooseConfig(this->eglDisplay, config_attribs, &config, 1, &count);
    TAOYAO_OPENGL_RET_LOG("EGL选择配置：%{public}d", ret);
    // 当前
    this->eglContext = eglGetCurrentContext();
    // 新建
    if(this->eglContext == EGL_NO_CONTEXT) {
        EGLint major;
        EGLint minor;
        ret = eglInitialize(this->eglDisplay, &major, &minor);
        TAOYAO_OPENGL_RET_LOG("加载EGL：%{public}d", ret);
        ret = eglBindAPI(EGL_OPENGL_ES_API);
        TAOYAO_OPENGL_RET_LOG("绑定EGL：%{public}d", ret);
        this->eglContext = eglCreateContext(this->eglDisplay, config, EGL_NO_CONTEXT, context_attribs);
    }
    const EGLint surfaceAttr[] = {
        EGL_WIDTH, 512,
        EGL_HEIGHT, 512,
        EGL_LARGEST_PBUFFER, EGL_TRUE,
        EGL_NONE
    };
        // TODO: 验证强转
    this->eglSurface = eglCreatePbufferSurface(this->eglDisplay, config, surfaceAttr);
    
//    this->eglSurface = eglCreateWindowSurface(this->eglDisplay, config, (EGLNativeWindowType) this->nativeWindow, context_attribs);

    
    eglMakeCurrent(this->eglDisplay, this->eglSurface, this->eglSurface, this->eglContext);
    
    // IMAGE WINDOW
    glGenTextures(this->textureSize, &this->textureId);
    this->nativeImage = OH_NativeImage_Create(this->textureId, GL_TEXTURE_2D);
    OH_LOG_INFO(LOG_APP, "创建NativeImage：%{public}d", this->textureId);
    // TODO: 验证是否需要window
//    this->nativeWindow = OH_NativeImage_AcquireNativeWindow(this->nativeImage);
//    OH_LOG_INFO(LOG_APP, "视频采集Window：%{public}d", this->nativeWindow);
    // this->eglSurface = eglCreatePlatformWindowSurface(this->eglDisplay, config, this->nativeWindow, context_attribs);
//    OH_NativeWindow_NativeWindowHandleOpt(this->nativeWindow, SET_BUFFER_GEOMETRY, acgist::width, acgist::height);
    // OH_NativeWindow_NativeWindowHandleOpt(this->nativeWindow, SET_TIMEOUT, 10'000);
    // OH_NativeWindow_NativeWindowHandleOpt(this->nativeWindow, GET_TIMEOUT, 10'000);
    OH_OnFrameAvailableListener listener = { this, onFrame };
    OH_NativeImage_SetOnFrameAvailableListener(this->nativeImage, listener);
    
    OH_NativeImage_GetSurfaceId(this->nativeImage, &this->surfaceId);
    OH_LOG_INFO(LOG_APP, "视频采集SurfaceId：%{public}lld", this->surfaceId);
}

void acgist::VideoCapturer::releaseOpenGLES() {
    if(this->textureId != 0) {
        glDeleteTextures(this->textureSize, &this->textureId);
        this->textureId = 0;
    }
    if(this->eglSurface != EGL_NO_SURFACE) {
        EGLBoolean ret = eglDestroySurface(this->eglDisplay, this->eglSurface);
        this->eglSurface = EGL_NO_SURFACE;
        TAOYAO_OPENGL_RET_LOG("销毁EGLSurface：%d", ret);
    }
    if(this->eglContext != EGL_NO_CONTEXT) {
        EGLBoolean ret = eglDestroyContext(this->eglDisplay, this->eglContext);
        this->eglContext = EGL_NO_CONTEXT;
        TAOYAO_OPENGL_RET_LOG("销毁EGLContext：%d", ret);
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
    OH_LOG_ERROR(LOG_APP, "视频捕获数据帧失败：%d", errorCode);
}

static void onFrameEnd(Camera_VideoOutput* videoOutput, int32_t frameCount) {
    OH_LOG_DEBUG(LOG_APP, "结束视频捕获数据帧");
}

static void onFrameStart(Camera_VideoOutput* videoOutput) {
    OH_LOG_DEBUG(LOG_APP, "开始视频捕获数据帧");
}

static void onFrame(void* context) {
    OH_LOG_ERROR(LOG_APP, "视频数据采集回调：%{public}d", 1);
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
