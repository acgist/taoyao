/**
 * 终端
 * 
 * @author acgist
 */
#ifndef taoyao_Client_HPP
#define taoyao_Client_HPP

#include "MediaManager.hpp"

namespace acgist {

/**
 * 终端
 */
class Client {

public:
    // 媒体管理
    acgist::MediaManager* mediaManager = nullptr;
    // 音频轨道
    webrtc::AudioTrackInterface* audioTrack = nullptr;
    // 视频轨道
    webrtc::VideoTrackInterface* videoTrack = nullptr;

public:
    Client(acgist::MediaManager* mediaManager);
    virtual ~Client();
    
public:
    /**
     * 资源释放
     * 
     * @return 是否成功
     */
    virtual bool release() = 0;
    
};

/**
 * 房间终端
 */
class RoomClient : public Client {
    
public:
    RoomClient(acgist::MediaManager* mediaManager);
    virtual ~RoomClient();
    
public:
    virtual bool release() override;
    
};

/**
 * 本地终端
 */
class LocalClient : public RoomClient {

public:
    LocalClient(acgist::MediaManager* mediaManager);
    virtual ~LocalClient();

public:
    virtual bool release() override;
    
};

/**
 * 远程终端
 */
class RemoteClient : public RoomClient {

public:
    RemoteClient(acgist::MediaManager* mediaManager);
    virtual ~RemoteClient();

public:
    virtual bool release() override;
    
};

}

#endif // taoyao_Client_HPP
