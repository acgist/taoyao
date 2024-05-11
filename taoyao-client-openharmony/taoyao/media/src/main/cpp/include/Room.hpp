/**
 * 房间
 * 
 * @author acgist
 */

#ifndef TAOYAO_ROOM_HPP
#define TAOYAO_ROOM_HPP

#include <map>
#include <string>

#include "./Client.hpp"
#include "./Signal.hpp"
#include "./MediaManager.hpp"

#include "mediasoupclient.hpp"

namespace acgist {

class Room;

/**
 * 发送通道监听器
 */
class SendListener : public mediasoupclient::SendTransport::Listener {

public:
    // 房间指针
    Room* room;

public:
    explicit SendListener(Room* room);
    virtual ~SendListener()/*  override */;

public:
    // 连接通道
    std::future<void> OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) override;
    // 通道状态改变
    void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override;
    // 通道生产媒体
    std::future<std::string> OnProduce(mediasoupclient::SendTransport* transport, const std::string& kind, nlohmann::json rtpParameters, const nlohmann::json& appData) override;
    // 通道生产数据
    std::future<std::string> OnProduceData(mediasoupclient::SendTransport* transport, const nlohmann::json& sctpStreamParameters, const std::string& label, const std::string& protocol, const nlohmann::json& appData) override;

};

/**
 * 接收通道监听器
 */
class RecvListener : public mediasoupclient::RecvTransport::Listener {

public:
    // 房间指针
    Room* room;

public:
    explicit RecvListener(Room* room);
    virtual ~RecvListener()/*  override */;

public:
    // 连接通道
    std::future<void> OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) override;
    // 通道状态改变
    void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override;

};

/**
 * 生产者监听器
 */
class ProducerListener : public mediasoupclient::Producer::Listener {

public:
    // 房间指针
    Room* room;

public:
    explicit ProducerListener(Room* room);
    virtual ~ProducerListener()/*  override */;

public:
    // 通道关闭
    void OnTransportClose(mediasoupclient::Producer* producer) override;

};

/**
 * 消费者监听器
 */
class ConsumerListener : public mediasoupclient::Consumer::Listener {

public:
    // 房间指针
    Room* room;

public:
    explicit ConsumerListener(Room* room);
    virtual ~ConsumerListener()/*  override */;

public:
    // 通道关闭
    void OnTransportClose(mediasoupclient::Consumer* consumer) override;

};

/**
 * 房间
 */
class Room {
    
private:
    // 是否进入
    bool enterd = false;
    // 是否关闭
    bool closed = false;

public:
    // 生产消息：没有实现
    bool dataProduce  = false;
    // 生产音频
    bool audioProduce = true;
    // 生产视频
    bool videoProduce = true;
    // 消费音频
    bool audioConsume = true;
    // 消费视频
    bool videoConsume = true;
    // 房间ID
    std::string roomId = "";
    // 媒体管理
    acgist::MediaManager* mediaManager = nullptr;
    // 本地终端
    acgist::LocalClient* client = nullptr;
    // 消费者ID和终端ID映射：consumerId = clientId
    std::map<std::string, std::string> consumerIdClientId;
    // 远程终端：clientId = RemoteClient
    std::map<std::string, acgist::RemoteClient*> clients;
    // WebRTC配置
    webrtc::PeerConnectionInterface::RTCConfiguration* rtcConfiguration;
    // 房间Device
    mediasoupclient::Device* device = nullptr;
    // 发送通道
    mediasoupclient::SendTransport* sendTransport = nullptr;
    // 接收通道
    mediasoupclient::RecvTransport* recvTransport = nullptr;
    // 发送监听器
    mediasoupclient::SendTransport::Listener* sendListener = nullptr;
    // 接收监听器
    mediasoupclient::RecvTransport::Listener* recvListener = nullptr;
    // 音频生产者
    mediasoupclient::Producer* audioProducer = nullptr;
    // 视频生产者
    mediasoupclient::Producer* videoProducer = nullptr;
    // 生产者监听器
    mediasoupclient::Producer::Listener* producerListener = nullptr;
    // 消费者监听器
    mediasoupclient::Consumer::Listener* consumerListener = nullptr;

public:
    Room(const std::string& roomId, acgist::MediaManager* mediaManager);
    virtual ~Room();
    
public:
    // 进入房间
    int enter(const std::string& password);
    // 生成媒体
    int produceMedia();
    // 创建发送通道
    int createSendTransport();
    // 创建接收通道
    int createRecvTransport();
    // 生产音频
    int produceAudio();
    // 生产视频
    int produceVideo();
    // 关闭房间
    int close();
    // 关闭本地消费者
    int closeClient();
    // 关闭远程消费者
    int closeClients();
    // 关闭音频生产者
    int closeAudioProducer();
    // 关闭视频生产者
    int closeVideoProducer();
    // 关闭通道
    int closeTransport();
    // 新建远程终端
    int newRemoteClient(const std::string& clientId, const std::string& name);
    // 删除远程终端
    int closeRemoteClient(const std::string& clientId);
    // 新增消费者
    int newConsumer(nlohmann::json& body);
    // 删除消费者
    int closeConsumer(const std::string& consumerId);
    // 暂停消费者
    int pauseConsumer(const std::string& consumerId);
    // 恢复消费者
    int resumeConsumer(const std::string& consumerId);
    // 关闭生产者
    int closeProducer(const std::string& producerId);
    // 暂停生产者
    int pauseProducer(const std::string& producerId);
    // 恢复生产者
    int resumeProducer(const std::string& producerId);
    
};

}

#endif // TAOYAO_ROOM_HPP
