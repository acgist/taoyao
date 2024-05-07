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

#include "api/media_stream_interface.h"
#include "api/peer_connection_interface.h"
#include "api/video/video_sink_interface.h"
#include "api/video/video_source_interface.h"

namespace acgist {

class TaoyaoAudioSink : public webrtc::AudioTrackSinkInterface {
};

class TaoyaoVideoSource : public webrtc::VideoTrackSourceInterface {
};

class TaoyaoVideoSink : public rtc::VideoSinkInterface<webrtc::RecordableEncodedFrame> {
};

class MediaManager {

public:
    MediaManager();
    virtual ~MediaManager();

public:
    int localClientRef = 0;
    std::unique_ptr<rtc::Thread> networkThread   = nullptr;
    std::unique_ptr<rtc::Thread> signalingThread = nullptr;
    std::unique_ptr<rtc::Thread> workerThread    = nullptr;
    rtc::scoped_refptr<webrtc::PeerConnectionFactoryInterface> peerConnectionFactory = nullptr;

protected:
    // 加载PC工厂
    bool newPeerConnectionFactory();
    // 释放PC工厂
    bool releasePeerConnectionFactory();

public:
    // 加载媒体
    bool init();
    // 新增本地终端
    int newLocalClient();
    // 释放本地终端
    int releaseLocalClient();
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
    // 音频来源
    rtc::scoped_refptr<webrtc::AudioTrackInterface> getAudioTrack();
    // 视频来源
    rtc::scoped_refptr<webrtc::VideoTrackInterface> getVideoTrack();

};

}

#endif // TAOYAO_MEDIAMANAGER_HPP
