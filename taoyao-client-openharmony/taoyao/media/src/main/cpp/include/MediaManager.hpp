/**
 * WebRTC媒体服务
 * 提供通道等等创建
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/media/obtain-supported-codecs.md
 */

#ifndef taoyao_MediaManager_HPP
#define taoyao_MediaManager_HPP

#include <memory>
#include <thread>

#include "pc/peer_connection.h"
#include "pc/peer_connection_factory.h"

namespace acgist {

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
    void startCapture();
    void startAudioCapture();
    void startVideoCapture();
    void stopCapture();
    void stopAudioCapture();
    void stopVideoCapture();
};

}

#endif // taoyao_MediaManager_HPP
