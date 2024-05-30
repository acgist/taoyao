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

#include "rtc_base/time_utils.h"

#include "api/video/nv12_buffer.h"
#include "api/video/i420_buffer.h"

#include <ohcamera/camera_manager.h>
#include <ohcamera/capture_session.h>

#include <multimedia/image_framework/image_mdk.h>

static std::recursive_mutex videoMutex;

// 相机状态回调
static void onCameraStatus(Camera_Manager* cameraManager, Camera_StatusInfo* status);

// 相机输入回调
static void onInputOnError(const Camera_Input* cameraInput, Camera_ErrorCode errorCode);

// 预览采集回调
static void onPreviewError(Camera_PreviewOutput* previewOutput, Camera_ErrorCode errorCode);
static void onPreviewFrameEnd(Camera_PreviewOutput* previewOutput, int32_t frameCount);
static void onPreviewFrameStart(Camera_PreviewOutput* previewOutput);

// 视频采集回调
static void onVideoError(Camera_VideoOutput* videoOutput, Camera_ErrorCode errorCode);
static void onVideoFrameEnd(Camera_VideoOutput* videoOutput, int32_t frameCount);
static void onVideoFrameStart(Camera_VideoOutput* videoOutput);

// 相机会话回调
static void onSessionError(Camera_CaptureSession* session, Camera_ErrorCode errorCode);
static void onSessionFocusStateChange(Camera_CaptureSession* session, Camera_FocusState focusState);

// 视频数据回调
static void onFrame();

acgist::CameraCapturer::CameraCapturer() {
    // 装备视频
    char* surfaceId = new char[sizeof(char)];
    int32_t code = OH_Image_Receiver_GetReceivingSurfaceId(acgist::imageReceiverNative, surfaceId, sizeof(char));
    OH_LOG_DEBUG(LOG_APP, "配置图片接收SurfaceId：%{public}d %{public}d", code, surfaceId[0]);
    OH_Image_Receiver_On_Callback callback = &onFrame;
    OH_Image_Receiver_On(acgist::imageReceiverNative, callback);
    // 装备相机
    Camera_ErrorCode ret = OH_Camera_GetCameraManager(&this->cameraManager);
    TAOYAO_VIDEO_RET_LOG("配置相机管理：%{public}d", ret);
    CameraManager_Callbacks cameraManager_Callbacks = { onCameraStatus };
    ret = OH_CameraManager_RegisterCallback(this->cameraManager, &cameraManager_Callbacks);
    TAOYAO_VIDEO_RET_LOG("注册相机管理回调：%{public}d", ret);
    ret = OH_CameraManager_GetSupportedCameras(this->cameraManager, &this->cameraDevice, &this->cameraSize);
    TAOYAO_VIDEO_RET_LOG("相机设备列表：%{public}d %{public}d", ret, this->cameraSize);
    for (int index = 0; index < this->cameraSize; ++index) {
       OH_LOG_DEBUG(LOG_APP, "相机cameraId       = %{public}s", this->cameraDevice[index].cameraId);
       OH_LOG_DEBUG(LOG_APP, "相机cameraPosition = %{public}d", this->cameraDevice[index].cameraPosition);
       OH_LOG_DEBUG(LOG_APP, "相机cameraType     = %{public}d", this->cameraDevice[index].cameraType);
       OH_LOG_DEBUG(LOG_APP, "相机connectionType = %{public}d", this->cameraDevice[index].connectionType);
    }
    ret = OH_CameraManager_GetSupportedCameraOutputCapability(this->cameraManager, &this->cameraDevice[this->cameraIndex], &this->cameraOutputCapability);
    TAOYAO_VIDEO_RET_LOG("相机输出能力：%{public}d %{public}d %{public}d %{public}d", ret, this->cameraIndex, this->cameraOutputCapability->previewProfilesSize, this->cameraOutputCapability->videoProfilesSize);
    ret = OH_CameraManager_CreateCameraInput(this->cameraManager, &this->cameraDevice[this->cameraIndex], &this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("配置相机输入：%{public}d %{public}d", ret, this->cameraIndex);
    CameraInput_Callbacks cameraInput_Callbacks = { onInputOnError };
    ret = OH_CameraInput_RegisterCallback(this->cameraInput, &cameraInput_Callbacks);
    TAOYAO_VIDEO_RET_LOG("注册相机输入回调：%{public}d %{public}d", ret, this->cameraIndex);
    auto& previewProfile = this->cameraOutputCapability->previewProfiles[0];
    // previewProfile->format = CAMERA_FORMAT_JPEG;
    // previewProfile->format = CAMERA_FORMAT_YUV_420_SP;
    ret = OH_CameraManager_CreatePreviewOutput(this->cameraManager, previewProfile, acgist::surfaceId.data(), &this->cameraPreviewOutput);
    TAOYAO_VIDEO_RET_LOG("创建相机预览输出：%{public}d %{public}d %{public}s", ret, previewProfile->format, acgist::surfaceId.data());
    PreviewOutput_Callbacks previewOutput_Callbacks = { onPreviewFrameStart, onPreviewFrameEnd, onPreviewError };
    ret = OH_PreviewOutput_RegisterCallback(this->cameraPreviewOutput, &previewOutput_Callbacks);
    TAOYAO_VIDEO_RET_LOG("注册相机预览输出回调：%{public}d", ret);
    auto& videoProfile = this->cameraOutputCapability->videoProfiles[0];
    // videoProfile->format = CAMERA_FORMAT_JPEG;
    // videoProfile->format = CAMERA_FORMAT_YUV_420_SP;
    videoProfile->range.min = 20;
    videoProfile->range.max = 30;
    OH_LOG_DEBUG(LOG_APP, "相机视频配置：%{public}d %{public}d %{public}d %{public}d %{public}d", videoProfile->format, videoProfile->size.width, videoProfile->size.height, videoProfile->range.min, videoProfile->range.max);
    ret = OH_CameraManager_CreateVideoOutput(this->cameraManager, videoProfile, surfaceId, &this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("创建相机视频输出：%{public}d %{public}d", ret, surfaceId[0]);
    VideoOutput_Callbacks videoOutput_Callbacks = { onVideoFrameStart, onVideoFrameEnd, onVideoError };
    ret = OH_VideoOutput_RegisterCallback(this->cameraVideoOutput, &videoOutput_Callbacks);
    TAOYAO_VIDEO_RET_LOG("注册相机视频输出回调：%{public}d", ret);
    ret = OH_CameraManager_CreateCaptureSession(this->cameraManager, &this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("创建相机视频会话：%{public}d", ret);
    CaptureSession_Callbacks captureSession_Callbacks = { onSessionFocusStateChange, onSessionError };
    OH_CaptureSession_RegisterCallback(this->cameraCaptureSession, &captureSession_Callbacks);
    TAOYAO_VIDEO_RET_LOG("注册相机视频会话：%{public}d", ret);
    // 设置相机：闪光、变焦、质量、高宽、补光、防抖
    // 设置缩放比例
    // OH_CaptureSession_SetZoomRatio(this->cameraCaptureSession, 0.5F);
    delete[] surfaceId;
}

acgist::CameraCapturer::~CameraCapturer() {
    CaptureSession_Callbacks captureSession_Callbacks = { onSessionFocusStateChange, onSessionError };
    Camera_ErrorCode ret = OH_CaptureSession_UnregisterCallback(this->cameraCaptureSession, &captureSession_Callbacks);
    TAOYAO_VIDEO_RET_LOG("取消相机视频会话回调：%{public}d", ret);
    ret = OH_CaptureSession_Release(this->cameraCaptureSession);
    this->cameraCaptureSession = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放相机视频会话：%{public}d", ret);
    PreviewOutput_Callbacks previewOutput_Callbacks = { onPreviewFrameStart, onPreviewFrameEnd, onPreviewError };
    ret = OH_PreviewOutput_UnregisterCallback(this->cameraPreviewOutput, &previewOutput_Callbacks);
    TAOYAO_VIDEO_RET_LOG("取消相机预览输出回调：%{public}d", ret);
    ret = OH_PreviewOutput_Release(this->cameraPreviewOutput);
    this->cameraPreviewOutput = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放相机预览输出：%{public}d", ret);
    VideoOutput_Callbacks videoOutput_Callbacks = { onVideoFrameStart, onVideoFrameEnd, onVideoError };
    ret = OH_VideoOutput_UnregisterCallback(this->cameraVideoOutput, &videoOutput_Callbacks);
    TAOYAO_VIDEO_RET_LOG("取消相机视频输出回调：%{public}d", ret);
    ret = OH_VideoOutput_Release(this->cameraVideoOutput);
    this->cameraVideoOutput = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放相机视频输出：%{public}d", ret);
    CameraInput_Callbacks cameraInput_Callbacks = { onInputOnError };
    ret = OH_CameraInput_UnregisterCallback(this->cameraInput, &cameraInput_Callbacks);
    TAOYAO_VIDEO_RET_LOG("取消相机输入回调：%{public}d", ret);
    ret = OH_CameraInput_Release(this->cameraInput);
    this->cameraInput = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放相机输入：%{public}d", ret);
    ret = OH_CameraManager_DeleteSupportedCameraOutputCapability(this->cameraManager, this->cameraOutputCapability);
    this->cameraOutputCapability = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放相机输出能力：%{public}d", ret);
    ret = OH_CameraManager_DeleteSupportedCameras(this->cameraManager, this->cameraDevice, this->cameraSize);
    this->cameraDevice = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放相机设备列表：%{public}d", ret);
    CameraManager_Callbacks cameraManager_Callbacks = { onCameraStatus };
    ret = OH_CameraManager_UnregisterCallback(this->cameraManager, &cameraManager_Callbacks);
    TAOYAO_VIDEO_RET_LOG("取消相机管理回调：%{public}d", ret);
    ret = OH_Camera_DeleteCameraManager(this->cameraManager);
    this->cameraManager = nullptr;
    TAOYAO_VIDEO_RET_LOG("释放相机管理：%{public}d", ret);
}

bool acgist::CameraCapturer::start() {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    if (this->running) {
        return true;
    }
    this->running = true;
    Camera_ErrorCode ret = OH_CameraInput_Open(this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("打开相机输入：%{public}d", ret);
    ret = OH_CaptureSession_BeginConfig(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("开始配置视频会话：%{public}d", ret);
    ret = OH_CaptureSession_AddInput(this->cameraCaptureSession, this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("绑定视频输入会话：%{public}d", ret);
    ret = OH_CaptureSession_AddPreviewOutput(cameraCaptureSession, this->cameraPreviewOutput);
    TAOYAO_VIDEO_RET_LOG("绑定相机预览输出会话：%{public}d", ret);
    ret = OH_CaptureSession_AddVideoOutput(this->cameraCaptureSession, this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("绑定相机视频输出会话：%{public}d", ret);
    ret = OH_CaptureSession_CommitConfig(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("结束配置视频会话：%{public}d", ret);
    ret = OH_CaptureSession_Start(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("开始相机视频会话：%{public}d", ret);
//    ret = OH_PreviewOutput_Start(this->cameraPreviewOutput);
//    TAOYAO_VIDEO_RET_LOG("开始相机预览输出：%{public}d", ret);
    ret = OH_VideoOutput_Start(this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("开始相机视频输出：%{public}d", ret);
    return ret = Camera_ErrorCode::CAMERA_OK;
}

bool acgist::CameraCapturer::stop() {
    std::lock_guard<std::recursive_mutex> videoLock(videoMutex);
    if (!this->running) {
        return true;
    }
    this->running = false;
    Camera_ErrorCode ret = OH_CaptureSession_BeginConfig(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("开始配置视频会话：%{public}d", ret);
    ret = OH_CaptureSession_RemoveInput(this->cameraCaptureSession, this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("取消绑定视频输入会话：%{public}d", ret);
    ret = OH_CaptureSession_RemovePreviewOutput(this->cameraCaptureSession, this->cameraPreviewOutput);
    TAOYAO_VIDEO_RET_LOG("绑定相机预览输出会话：%{public}d", ret);
    ret = OH_CaptureSession_RemoveVideoOutput(this->cameraCaptureSession, this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("绑定相机视频输出会话：%{public}d", ret);
    ret = OH_CaptureSession_CommitConfig(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("结束配置视频会话：%{public}d", ret);
    ret = OH_CaptureSession_Stop(this->cameraCaptureSession);
    TAOYAO_VIDEO_RET_LOG("结束视频会话：%{public}d", ret);
    ret = OH_PreviewOutput_Stop(this->cameraPreviewOutput);
    TAOYAO_VIDEO_RET_LOG("结束相机预览输出：%{public}d", ret);
    ret = OH_VideoOutput_Stop(this->cameraVideoOutput);
    TAOYAO_VIDEO_RET_LOG("结束相机视频输出：%{public}d", ret);
    ret = OH_CameraInput_Close(this->cameraInput);
    TAOYAO_VIDEO_RET_LOG("关闭相机输入：%{public}d", ret);
    return ret = Camera_ErrorCode::CAMERA_OK;
}

static void onCameraStatus(Camera_Manager* cameraManager, Camera_StatusInfo* status) {
   OH_LOG_INFO(LOG_APP, "相机状态变化：%{public}d", status->status);
}

static void onInputOnError(const Camera_Input* cameraInput, Camera_ErrorCode errorCode) {
   OH_LOG_ERROR(LOG_APP, "相机输入错误：%{public}d", errorCode);
}

static void onPreviewError(Camera_PreviewOutput* previewOutput, Camera_ErrorCode errorCode) {
    OH_LOG_ERROR(LOG_APP, "预览捕获数据帧失败：%{public}d", errorCode);
}

static void onPreviewFrameEnd(Camera_PreviewOutput* previewOutput, int32_t frameCount) {
    OH_LOG_DEBUG(LOG_APP, "结束预览捕获数据帧");
}

static void onPreviewFrameStart(Camera_PreviewOutput* previewOutput) {
    OH_LOG_DEBUG(LOG_APP, "开始预览捕获数据帧");
}

static void onVideoError(Camera_VideoOutput* videoOutput, Camera_ErrorCode errorCode) {
    OH_LOG_ERROR(LOG_APP, "视频捕获数据帧失败：%{public}d", errorCode);
}

static void onVideoFrameEnd(Camera_VideoOutput* videoOutput, int32_t frameCount) {
    OH_LOG_DEBUG(LOG_APP, "结束视频捕获数据帧");
}

static void onVideoFrameStart(Camera_VideoOutput* videoOutput) {
    OH_LOG_DEBUG(LOG_APP, "开始视频捕获数据帧");
}

static void onSessionError(Camera_CaptureSession* session, Camera_ErrorCode errorCode) {
    OH_LOG_ERROR(LOG_APP, "打开相机会话失败：%{public}d", errorCode);
}

static void onSessionFocusStateChange(Camera_CaptureSession* session, Camera_FocusState focusState) {
    OH_LOG_DEBUG(LOG_APP, "相机会话焦点改变：%{public}d", focusState);
}

static void onFrame() {
    napi_value nextImage;
    int32_t ret = OH_Image_Receiver_ReadNextImage(acgist::imageReceiverNative, &nextImage);
    OH_LOG_DEBUG(LOG_APP, "视频帧数据采集回调：%{public}d", ret);
          OhosImageSize imageSize;
    ImageNative * nextImage_native = OH_Image_InitImageNative(acgist::env, nextImage);
      OH_Image_Size(nextImage_native, &imageSize);
      OH_LOG_Print(LOG_APP, LOG_INFO, 0xFF00, "[receiver]", "OH_Image_Size  width: %{public}d, height:%{public}d", imageSize.width, imageSize.height);
      OhosImageComponent imgComponent;
       OH_Image_GetComponent(nextImage_native, 4, &imgComponent); // 4=jpeg
      
      uint8_t *img_buffer = imgComponent.byteBuffer;
    OH_Image_Release(nextImage_native);
//HWTEST_F(NativeImageTest, OHNativeImageSetOnFrameAvailableListener001, Function | MediumTest | Level1)
//{
//    if (image == nullptr) {
//        image = OH_NativeImage_Create(textureId, GL_TEXTURE_2D);
//        ASSERT_NE(image, nullptr);
//    }
//
//    if (nativeWindow == nullptr) {
//        nativeWindow = OH_NativeImage_AcquireNativeWindow(image);
//        ASSERT_NE(nativeWindow, nullptr);
//    }
//
//    OH_OnFrameAvailableListener listener;
//    listener.context = this;
//    listener.onFrameAvailable = NativeImageTest::OnFrameAvailable;
//    int32_t ret = OH_NativeImage_SetOnFrameAvailableListener(image, listener);
//    ASSERT_EQ(ret, GSERROR_OK);
//
//    NativeWindowBuffer* nativeWindowBuffer = nullptr;
//    int fenceFd = -1;
//    ret = OH_NativeWindow_NativeWindowRequestBuffer(nativeWindow, &nativeWindowBuffer, &fenceFd);
//    ASSERT_EQ(ret, GSERROR_OK);
//
//    struct Region *region = new Region();
//    struct Region::Rect *rect = new Region::Rect();
//    rect->x = 0x100;
//    rect->y = 0x100;
//    rect->w = 0x100;
//    rect->h = 0x100;
//    region->rects = rect;
//    ret = OH_NativeWindow_NativeWindowFlushBuffer(nativeWindow, nativeWindowBuffer, fenceFd, *region);
//    ASSERT_EQ(ret, GSERROR_OK);
//    delete region;
//
//    ret = OH_NativeImage_UpdateSurfaceImage(image);
//    ASSERT_EQ(ret, SURFACE_ERROR_OK);
//}
//    OHNativeWindow* nativeWindow = OH_NativeImage_AcquireNativeWindow(cameraCapturer->nativeImage);
//    int code = SET_BUFFER_GEOMETRY;
//    int32_t width = 800;
//    int32_t height = 600;
//    int32_t ret = OH_NativeWindow_NativeWindowHandleOpt(nativeWindow, code, width, height);
//    OHNativeWindowBuffer *buffer = nullptr;
//    int fenceFd;
//    // 通过 OH_NativeWindow_NativeWindowRequestBuffer 获取 OHNativeWindowBuffer 实例
//    OH_NativeWindow_NativeWindowRequestBuffer(nativeWindow, &buffer, &fenceFd);
//    BufferHandle *handle = OH_NativeWindow_GetBufferHandleFromNative(buffer);
//    // 设置刷新区域，如果Region中的Rect为nullptr,或者rectNumber为0，则认为NativeWindowBuffer全部有内容更改。
//Region region{nullptr, 0};
//// 通过OH_NativeWindow_NativeWindowFlushBuffer 提交给消费者使用，例如：显示在屏幕上。
//OH_NativeWindow_NativeWindowFlushBuffer(nativeWindow, buffer, fenceFd, region);
//    // 更新内容到OpenGL纹理。
// ret = OH_NativeImage_UpdateSurfaceImage(cameraCapturer->nativeImage);
//if (ret != 0) {
//}
//// 获取最近调用OH_NativeImage_UpdateSurfaceImage的纹理图像的时间戳和变化矩阵。
//int64_t timeStamp = OH_NativeImage_GetTimestamp(cameraCapturer->nativeImage);
//float matrix[16];
//ret = OH_NativeImage_GetTransformMatrix(cameraCapturer->nativeImage, matrix);
//if (ret != 0) {
//}
//
//// 对update绑定到对应textureId的纹理做对应的opengl后处理后，将纹理上屏
//EGLBoolean eglRet = eglSwapBuffers(cameraCapturer->eglDisplay, cameraCapturer->eglSurface);
//if (eglRet == EGL_FALSE) {
//}

//    eglMakeCurrent(cameraCapturer->eglDisplay, cameraCapturer->eglSurface, cameraCapturer->eglSurface, cameraCapturer->eglContext);
//    OH_NativeImage_UpdateSurfaceImage(cameraCapturer->nativeImage);
//    OH_NativeImage_GetTimestamp(cameraCapturer->nativeImage);
//    float matrix[16];
//    OH_NativeImage_GetTransformMatrix(cameraCapturer->nativeImage, matrix);
//    eglSwapBuffers(cameraCapturer->eglDisplay, cameraCapturer->eglSurface);
//    char* chars = new char[1024];
//    eglQueryNativePixmapNV(cameraCapturer->eglDisplay, cameraCapturer->eglSurface, &pixmap);
//    EGLBoolean ret = eglCopyBuffers(cameraCapturer->eglDisplay, cameraCapturer->eglSurface, (EGLNativePixmapType) chars);
//    char buffer[1024];
//    glReadPixels(0, 0, 10, 10, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
//    OH_LOG_DEBUG(LOG_APP, "视频帧数据采集回调读取：%{public}d", ret);
//    glFlush();
//    glFinish();
//    // TODO: 解析
//    rtc::scoped_refptr<webrtc::I420Buffer> videoFrameBuffer =
//        webrtc::I420Buffer::Copy(width, height, (uint8_t *)data, 0, (uint8_t *)data, 0, (uint8_t *)data, 0);
//    // webrtc::NV12Buffer::Create(width, height);
//    webrtc::VideoFrame::Builder builder;
//    webrtc::VideoFrame videoFrame =
//        builder.set_timestamp_ms(rtc::TimeMillis()).set_video_frame_buffer(videoFrameBuffer).build();
//    for (auto iterator = cameraCapturer->map.begin(); iterator != cameraCapturer->map.end(); ++iterator) {
//        iterator->second->OnFrame(videoFrame);
//    }
//    // TODO: 释放webrtc
//    videoFrameBuffer->Release();
    
//      height = (height > 0) ? height : -height; // abs
//  width = stride;
//  size_t size = stride * height + stride * height / 2;
//  if (bufferSize < size) {
//    return false;
//  }
//
//  rtc::scoped_refptr<webrtc::I420Buffer> i420_buffer = webrtc::I420Buffer::Create(width, height);
//  libyuv::NV21ToI420(buffer, width, buffer + width * height, width, i420_buffer.get()->MutableDataY(),
//                     i420_buffer.get()->StrideY(), i420_buffer.get()->MutableDataU(), i420_buffer.get()->StrideU(),
//                     i420_buffer.get()->MutableDataV(), i420_buffer.get()->StrideV(), width, height);
//
//  webrtc::VideoFrame video_frame = webrtc::VideoFrame::Builder()
//                                     .set_video_frame_buffer(i420_buffer)
//                                     .set_timestamp_rtp(0)
//                                     .set_timestamp_ms(rtc::TimeMillis())
//                                     .set_rotation(webrtc::kVideoRotation_90)
//                                     .build();
}
