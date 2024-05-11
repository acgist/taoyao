/**
 * 终端
 * 
 * @author acgist
 */

#ifndef TAOYAO_CLIENT_HPP
#define TAOYAO_CLIENT_HPP

#include "MediaManager.hpp"

#include "mediasoupclient.hpp"

namespace acgist {

/**
 * 终端
 */
class Client {

public:
    // 终端ID
    std::string clientId;
    // 终端名称
    std::string name;
    // 媒体管理
    acgist::MediaManager* mediaManager = nullptr;
    // 音频轨道
    rtc::scoped_refptr<webrtc::AudioTrackInterface> audioTrack = nullptr;
    // 视频轨道
    rtc::scoped_refptr<webrtc::VideoTrackInterface> videoTrack = nullptr;

public:
    Client(acgist::MediaManager* mediaManager);
    virtual ~Client();
    
};

/**
 * 房间终端
 */
class RoomClient : public Client {
    
public:
    // 房间ID
    std::string roomId;
    
public:
    RoomClient(acgist::MediaManager* mediaManager);
    virtual ~RoomClient() override;
    
public:
    
};

/**
 * 本地终端
 */
class LocalClient : public RoomClient {

public:
    LocalClient(acgist::MediaManager* mediaManager);
    virtual ~LocalClient() override;
    
};

/**
 * 远程终端
 */
class RemoteClient : public RoomClient {

public:
    // 消费者列表
    std::map<std::string, mediasoupclient::Consumer*> consumers;

public:
    RemoteClient(acgist::MediaManager* mediaManager);
    virtual ~RemoteClient() override;

public:
    // 添加消费者
    bool addConsumer(const std::string& consuemrId, mediasoupclient::Consumer* consumer);
    // 关闭消费者
    bool closeConsumer(const std::string& consumerId);
    // 暂停消费者
    bool pauseConsumer(const std::string& consumerId);
    // 恢复消费者
    bool resumeConsumer(const std::string& consumerId);
    
};

}

#endif // TAOYAO_CLIENT_HPP
