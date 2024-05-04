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

#include "pc/peer_connection.h"
#include "pc/peer_connection_factory.h"

namespace acgist {

class MediaManager {
public:
    MediaManager();
    ~MediaManager();
public:
    webrtc::PeerConnectionFactory* peerConnectionFactoryPtr;
public:
    void initPeerConnectionFactory();
    webrtc::PeerConnection* buildPeerConnection();
    void startCapture();
    void startAudioCapture();
    void startVideoCapture();
};

}

#endif // taoyao_MediaManager_HPP