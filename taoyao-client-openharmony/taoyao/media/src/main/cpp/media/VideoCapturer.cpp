/**
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/apis-camera-kit/camera_8h.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/apis-camera-kit/video__output_8h.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/apis-camera-kit/camera__manager_8h.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/apis-camera-kit/capture__session_8h.md
 */
#include "../include/Capturer.hpp"

#include "hilog/log.h"

#include "ohcamera/camera_input.h"
#include "ohcamera/camera_manager.h"
#include "ohcamera/capture_session.h"

static void onError(Camera_VideoOutput* videoOutput, Camera_ErrorCode errorCode);
static void onFrameEnd(Camera_VideoOutput* videoOutput, int32_t frameCount);
static void onFrameStart(Camera_VideoOutput* videoOutput);

acgist::VideoCapturer::VideoCapturer() {
    Camera_ErrorCode ret = OH_Camera_GetCameraManager(&this->cameraManager);
    OH_LOG_INFO(LOG_APP, "构造摄像头管理器：%o", ret);
    ret = OH_CameraManager_GetSupportedCameras(this->cameraManager, &this->cameraDevice, &this->size);
    OH_LOG_INFO(LOG_APP, "摄像头设备列表：%o %d", ret, size);
    ret = OH_CameraManager_GetSupportedCameraOutputCapability(this->cameraManager, &this->cameraDevice[this->cameraIndex], &this->cameraOutputCapability);
    OH_LOG_INFO(LOG_APP, "摄像头输出功能：%o %d %d", ret, this->cameraIndex, this->cameraOutputCapability->videoProfilesSize);
    // 二次处理：createImageReceiver/getReceivingSurfaceId
//    ret = OH_CameraManager_CreateVideoOutput(this->cameraManager, this->cameraOutputCapability->videoProfiles[0],
//    this->nativeWindow, &this->cameraVideoOutput);
}

acgist::VideoCapturer::~VideoCapturer() {
    Camera_ErrorCode ret = OH_VideoOutput_Release(this->cameraVideoOutput);
    this->cameraVideoOutput = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头视频输出：%o", ret);
    ret = OH_CameraManager_DeleteSupportedCameraOutputCapability(this->cameraManager, this->cameraOutputCapability);
    this->cameraOutputCapability = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头输出能力：%o", ret);
    ret = OH_CameraManager_DeleteSupportedCameras(this->cameraManager, this->cameraDevice, this->size);
    this->cameraDevice = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头设备列表：%o", ret);
    ret = OH_Camera_DeleteCameraManager(this->cameraManager);
    this->cameraManager = nullptr;
    OH_LOG_INFO(LOG_APP, "释放摄像头管理器：%o", ret);
}

bool acgist::VideoCapturer::start() {
    Camera_ErrorCode ret = OH_VideoOutput_Start(this->cameraVideoOutput);
    OH_LOG_INFO(LOG_APP, "开始视频捕获：%o", ret);
    VideoOutput_Callbacks callbacks;
    callbacks.onError      = onError;
    callbacks.onFrameEnd   = onFrameEnd;
    callbacks.onFrameStart = onFrameStart;
    ret = OH_VideoOutput_RegisterCallback(this->cameraVideoOutput, &callbacks);
    OH_LOG_INFO(LOG_APP, "视频捕获回调：%o", ret);
    return true;
}

bool acgist::VideoCapturer::stop() {
    Camera_ErrorCode ret = OH_VideoOutput_Stop(this->cameraVideoOutput);
    OH_LOG_INFO(LOG_APP, "结束视频捕获：%o", ret);
    return true;
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
