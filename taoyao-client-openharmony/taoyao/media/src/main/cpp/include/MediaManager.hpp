/**
 * WebRTC媒体服务
 * 提供通道等等创建
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/avcodec/obtain-supported-codecs.md
 */
#ifndef taoyao_MediaManager_HPP
#define taoyao_MediaManager_HPP

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
    ~MediaManager();
public:
    int localClientRef = 0;
    std::unique_ptr<rtc::Thread> networkThread   = nullptr;
    std::unique_ptr<rtc::Thread> signalingThread = nullptr;
    std::unique_ptr<rtc::Thread> workerThread    = nullptr;
    rtc::scoped_refptr<webrtc::PeerConnectionFactoryInterface> peerConnectionFactory = nullptr;
public:
    // 加载PC工厂
    bool initPeerConnectionFactory();
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

#endif // taoyao_MediaManager_HPP
