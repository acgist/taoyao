/**
 * 房间
 * 
 * @author acgist
 */
#ifndef taoyao_Room_HPP
#define taoyao_Room_HPP

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
    /**
     * 房间指针
     */
    Room* room;

public:
    /**
     * 发送通道监听器
     *
     * @param room 房间指针
     */
    explicit SendListener(Room* room);
    /**
     * 析构函数
     */
    virtual ~SendListener();

public:
    /**
     * 连接通道
     *
     * @param transport      通道指针
     * @param dtlsParameters DTLS参数
     *
     * @return future
     */
    std::future<void> OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) override;

    /**
     * 通道状态改变
     *
     * @param transport       通道指针
     * @param connectionState 当前状态
     */
    void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override;

    /**
     * 通道生产媒体
     *
     * @param transport     通道指针
     * @param kind          媒体类型
     * @param rtpParameters RTP参数
     * @param appData       应用数据
     *
     * @return 生产者ID
     */
    std::future<std::string> OnProduce(mediasoupclient::SendTransport* transport, const std::string& kind, nlohmann::json rtpParameters, const nlohmann::json& appData) override;

    /**
     * 通道生产数据
     * 注意：需要自己实现
     *
     * @param transport            通道指针
     * @param sctpStreamParameters SCTP参数
     * @param label                标记
     * @param protocol             协议
     * @param appData              应用数据
     *
     * @return 生产者ID
     */
    std::future<std::string> OnProduceData(mediasoupclient::SendTransport* transport, const nlohmann::json& sctpStreamParameters, const std::string& label, const std::string& protocol, const nlohmann::json& appData) override;

};

/**
 * 接收通道监听器
 */
class RecvListener : public mediasoupclient::RecvTransport::Listener {

public:
    /**
     * 房间指针
     */
    Room* room;

public:
    /**
     * 接收通道监听器
     *
     * @param room 房间指针
     */
    explicit RecvListener(Room* room);
    /**
     * 析构函数
     */
    virtual ~RecvListener();

    /**
     * 连接通道
     *
     * @param transport      通道指针
     * @param dtlsParameters DTLS参数
     *
     * @return future
     */
    std::future<void> OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) override;

    /**
     * 通道状态改变
     *
     * @param transport       通道指针
     * @param connectionState 通道状态
     */
    void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override;

};

/**
 * 生产者监听器
 */
class ProducerListener : public mediasoupclient::Producer::Listener {

public:
    /**
     * 房间指针
     */
    Room* room;

public:
    /**
     * 生产者监听器
     *
     * @param room 房间指针
     */
    explicit ProducerListener(Room* room);
    /**
     * 析构函数
     */
    virtual ~ProducerListener();

    /**
     * 通道关闭
     *
     * @param producer 生产者
     */
    void OnTransportClose(mediasoupclient::Producer* producer) override;

};

/**
 * 消费者监听器
 */
class ConsumerListener : public mediasoupclient::Consumer::Listener {

public:
    /**
     * 房间指针
     */
    Room* room;

public:
    /**
     * 消费者监听器
     *
     * @param room 房间指针
     */
    explicit ConsumerListener(Room* room);
    /**
     * 析构函数
     */
    virtual ~ConsumerListener();

    /**
     * 通道关闭
     *
     * @param consumer 消费者
     */
    void OnTransportClose(mediasoupclient::Consumer* consumer) override;

};

/**
 * 房间
 */
class Room {
    
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
    // 远程终端
    std::map<std::string, acgist::RemoteClient*> clients;
    // WebRTC配置
    webrtc::PeerConnectionInterface::RTCConfiguration rtcConfiguration;
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
    // 消费者列表
    std::map<std::string, mediasoupclient::Consumer*> consumers;

public:
    Room(const std::string& roomId, acgist::MediaManager* mediaManager);
    virtual ~Room();
    
public:
    /**
     * 进入房间
     * 
     * @param password 密码
     * 
     * @return 状态
     */
    int enter(const std::string& password);
    /**
     * 生成媒体
     * 
     * @return 状态
     */
    int produceMedia();
    int createSendTransport();
    int createRecvTransport();
    int produceAudio();
    int produceVideo();
    int close();
    int closeConsumer();
    int closeAudioProducer();
    int closeVideoProducer();
    int closeTransport();
    int newRemoteClient();
    
};

}

#endif // taoyao_Room_HPP
