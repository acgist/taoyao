/**
 * WebRTC媒体服务
 * 提供通道等等创建
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/avcodec/obtain-supported-codecs.md
 */

#ifndef TAOYAO_MEDIAMANAGER_HPP
#define TAOYAO_MEDIAMANAGER_HPP

#include <memory>
#include <thread>

#include "./WebRTC.hpp"
#include "./Capturer.hpp"

#include "api/peer_connection_interface.h"

// 本地音频采集
#define TAOYAO_AUDIO_LOCAL false
// 本地视频采集
#define TAOYAO_VIDEO_LOCAL true
// 视频来源屏幕
#define TAOYAO_VIDEO_SOURCE_SCREEN true
// 视频来源相机
#define TAOYAO_VIDEO_SOURCE_CAMERA false

namespace acgist {

/**
 * 媒体管理器
 */
class MediaManager {

public:
    MediaManager();
    virtual ~MediaManager();

public:
    // 本地终端数量
    int localClientRef = 0;
    #if __TAOYAO_AUDIO_LOCAL__
    // 音频来源：本地创建
    acgist::TaoyaoAudioTrackSource* audioTrackSource = nullptr;
    #else
    // 音频来源：设备管理
    rtc::scoped_refptr<webrtc::AudioSourceInterface> audioTrackSource = nullptr;
    #endif
    // 视频来源
    acgist::TaoyaoVideoTrackSource* videoTrackSource = nullptr;
    // 音频采集
    acgist::AudioCapturer* audioCapturer = nullptr;
    // 视频采集
    acgist::VideoCapturer* videoCapturer = nullptr;
    // 网络线程
    std::unique_ptr<rtc::Thread> networkThread   = nullptr;
    // 信令线程
    std::unique_ptr<rtc::Thread> signalingThread = nullptr;
    // 工作线程
    std::unique_ptr<rtc::Thread> workerThread    = nullptr;
    // 连接工厂
    rtc::scoped_refptr<webrtc::PeerConnectionFactoryInterface> peerConnectionFactory = nullptr;

protected:
    // 加载连接工厂
    bool newPeerConnectionFactory();
    // 释放连接工厂
    bool releasePeerConnectionFactory();
    // 开始采集
    bool startCapture();
    // 开始采集音频
    bool startAudioCapture();
    // 开始采集视频
    bool startVideoCapture();
    // 结束采集
    bool stopCapture();
    // 结束采集音频
    bool stopAudioCapture();
    // 结束采集视频
    bool stopVideoCapture();

public:
    // 新增本地终端
    int newLocalClient();
    // 释放本地终端
    int releaseLocalClient();
    // 音频来源
    rtc::scoped_refptr<webrtc::AudioTrackInterface> getAudioTrack();
    // 视频来源
    rtc::scoped_refptr<webrtc::VideoTrackInterface> getVideoTrack();

};

}

#endif // TAOYAO_MEDIAMANAGER_HPP
