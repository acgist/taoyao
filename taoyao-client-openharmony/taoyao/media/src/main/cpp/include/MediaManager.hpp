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

namespace acgist {

class MediaManager {

public:
    MediaManager();
    virtual ~MediaManager();

public:
    int localClientRef = 0;
    #if __TAOYAO_AUDIO_LOCAL__
    acgist::TaoyaoAudioTrackSource* audioTrackSource = nullptr;
    #else
    rtc::scoped_refptr<webrtc::AudioSourceInterface> audioTrackSource = nullptr;
    #endif
    acgist::TaoyaoVideoTrackSource* videoTrackSource = nullptr;
    acgist::AudioCapturer* audioCapturer = nullptr;
    acgist::VideoCapturer* videoCapturer = nullptr;
    std::unique_ptr<rtc::Thread> networkThread   = nullptr;
    std::unique_ptr<rtc::Thread> signalingThread = nullptr;
    std::unique_ptr<rtc::Thread> workerThread    = nullptr;
    rtc::scoped_refptr<webrtc::PeerConnectionFactoryInterface> peerConnectionFactory = nullptr;

protected:
    // 加载PC工厂
    bool newPeerConnectionFactory();
    // 释放PC工厂
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
    // 加载媒体
    bool init();
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
