#include "../include/Room.hpp"

#include <mutex>

#include "hilog/log.h"

static std::mutex roomMutex;

acgist::Room::Room(const std::string& roomId, acgist::MediaManager* mediaManager) : roomId(roomId), mediaManager(mediaManager) {
    this->device = new mediasoupclient::Device();
    this->sendListener = new acgist::SendListener(this);
    this->recvListener = new acgist::RecvListener(this);
    this->producerListener = new acgist::ProducerListener(this);
    this->consumerListener = new acgist::ConsumerListener(this);
}

acgist::Room::~Room() {
    this->close();
    // rtcConfiguration
    if (this->device != nullptr) {
        delete this->device;
        this->device = nullptr;
    }
    if (this->sendListener != nullptr) {
        delete this->sendListener;
        this->sendListener = nullptr;
    }
    if (this->sendTransport != nullptr) {
        delete this->sendTransport;
        this->sendTransport = nullptr;
    }
    if (this->recvListener != nullptr) {
        delete this->recvListener;
        this->recvListener = nullptr;
    }
    if (this->recvTransport != nullptr) {
        delete this->recvTransport;
        this->recvTransport = nullptr;
    }
    if (this->audioProducer != nullptr) {
        delete this->audioProducer;
        this->audioProducer = nullptr;
    }
    if (this->videoProducer != nullptr) {
        delete this->videoProducer;
        this->videoProducer = nullptr;
    }
    if (this->producerListener != nullptr) {
        delete this->producerListener;
        this->producerListener = nullptr;
    }
    if (this->consumerListener != nullptr) {
        delete this->consumerListener;
        this->consumerListener = nullptr;
    }
    if(this->client != nullptr) {
        delete this->client;
        this->client = nullptr;
    }
}

int acgist::Room::enter(const std::string& password) {
    if (this->device->IsLoaded()) {
        OH_LOG_WARN(LOG_APP, "Device配置已经加载：%s", this->roomId.data());
        return -1;
    }
    // 本地终端
    this->client = new acgist::LocalClient(this->mediaManager);
    // 房间能力
    nlohmann::json requestBody = {
        { "roomId", this->roomId }
    };
    std::string response = acgist::request("media::router::rtp::capabilities", requestBody.dump());
    nlohmann::json json = nlohmann::json::parse(response);
    nlohmann::json responseBody = json["body"];
    nlohmann::json rtpCapabilities = responseBody["rtpCapabilities"];
    // 加载设备
    // this->rtcConfiguration->set_cpu_adaptation(false);
    mediasoupclient::PeerConnection::Options options;
    options.config  = this->rtcConfiguration;
    options.factory = this->mediaManager->peerConnectionFactory.get();
    this->device->Load(rtpCapabilities, &options);
    // 进入房间
    requestBody = {
        { "roomId",   this->roomId },
        { "password", password     },
        { "rtpCapabilities",  this->device->GetRtpCapabilities()  },
        { "sctpCapabilities", this->device->GetSctpCapabilities() }
    };
    response = acgist::request("room::enter", requestBody.dump());
    OH_LOG_INFO(LOG_APP, "进入房间：%s", this->roomId.data());
    return 0;
}

int acgist::Room::produceMedia() {
    OH_LOG_INFO(LOG_APP, "生成媒体：%s", this->roomId.data());
    if(this->audioProduce || this->videoProduce) {
        this->createSendTransport();
    }
    if(this->audioConsume || this->videoConsume) {
        this->createRecvTransport();
    }
    if(this->audioProduce) {
        this->produceAudio();
    }
    if(this->videoProduce) {
        this->produceVideo();
    }
}

int acgist::Room::createSendTransport() {
    nlohmann::json requestBody = {
        { "roomId",           this->roomId     },
        { "forceTcp",         false            },
        { "producing",        true             },
        { "consuming",        false            },
    //  { "sctpCapabilities", sctpCapabilities },
    };
    std::string response = acgist::request("media::transport::webrtc::create", requestBody.dump());
    nlohmann::json json = nlohmann::json::parse(response);
    nlohmann::json responseBody = json["body"];
    mediasoupclient::PeerConnection::Options options;
    options.config  = this->rtcConfiguration;
    options.factory = this->mediaManager->peerConnectionFactory.get();
    this->sendTransport = this->device->CreateSendTransport(
        this->sendListener,
        responseBody["transportId"],
        responseBody["iceParameters"],
        responseBody["iceCandidates"],
        responseBody["dtlsParameters"],
        responseBody["sctpParameters"],
        &options
    );
    return 0;
}

int acgist::Room::createRecvTransport() {
    nlohmann::json requestBody = {
        { "roomId",           this->roomId     },
        { "forceTcp",         false            },
        { "producing",        false            },
        { "consuming",        true             },
    //  { "sctpCapabilities", sctpCapabilities },
    };
    std::string response = acgist::request("media::transport::webrtc::create", requestBody.dump());
    nlohmann::json json = nlohmann::json::parse(response);
    nlohmann::json responseBody = json["body"];
    mediasoupclient::PeerConnection::Options options;
    options.config      = this->rtcConfiguration;
    options.factory     = this->mediaManager->peerConnectionFactory.get();
    this->recvTransport = this->device->CreateRecvTransport(
        this->recvListener,
        json["transportId"],
        json["iceParameters"],
        json["iceCandidates"],
        json["dtlsParameters"],
        json["sctpParameters"],
        &options
    );
    return 0;
}

int acgist::Room::produceAudio() {
    return 0;
}

int acgist::Room::produceVideo() {
    return 0;
}

int acgist::Room::close() {
    OH_LOG_INFO(LOG_APP, "关闭房间：%s", this->roomId.data());
    this->closeConsumer();
    this->closeAudioProducer();
    this->closeVideoProducer();
    this->closeTransport();
    return 0;
}

int acgist::Room::closeConsumer() {
    std::lock_guard<std::mutex> lockRoom(roomMutex);
    for (auto iterator = this->consumers.begin(); iterator != this->consumers.end(); ++iterator) {
        if (iterator->second == nullptr) {
            continue;
        }
        OH_LOG_INFO(LOG_APP, "关闭消费者：%s", iterator->second->GetId().data());
        iterator->second->Close();
        delete iterator->second;
        iterator->second = nullptr;
    }
    this->consumers.clear();
    return 0;
}

int acgist::Room::closeAudioProducer() {
    std::lock_guard<std::mutex> lockRoom(roomMutex);
    if (this->audioProducer != nullptr) {
        this->audioProducer->Close();
    }
    return 0;
}

int acgist::Room::closeVideoProducer() {
    std::lock_guard<std::mutex> lockRoom(roomMutex);
    if (this->videoProducer != nullptr) {
        this->videoProducer->Close();
    }
    return 0;
}

int acgist::Room::closeTransport() {
    std::lock_guard<std::mutex> lockRoom(roomMutex);
    if (this->sendTransport != nullptr) {
        this->sendTransport->Close();
    }
    if (this->recvTransport != nullptr) {
        this->recvTransport->Close();
    }
    return 0;
}

acgist::SendListener::SendListener(Room* room) : room(room) {
}

acgist::SendListener::~SendListener() {
}

std::future<void> acgist::SendListener::OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) {
    OH_LOG_INFO(LOG_APP, "连接发送通道：%s - %s", this->room->roomId.data(), transport->GetId().data());
    nlohmann::json requestBody = {
        { "roomId",           this->roomId       },
        { "transportId",      transport->GetId() },
        { "dtlsParameters",   dtlsParameters     },
    };
    std::string response = acgist::send("media::transport::webrtc::connect", requestBody.dump());
    std::promise<void> promise;
    promise.set_value();
    return promise.get_future();
}

void acgist::SendListener::OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) {
    OH_LOG_INFO(LOG_APP, "发送通道状态改变：%s - %s - %s", this->room->roomId.data(), transport->GetId().data(), connectionState.data());
    // TODO: 异常关闭逻辑
}

std::future<std::string> acgist::SendListener::OnProduce(mediasoupclient::SendTransport* transport, const std::string& kind, nlohmann::json rtpParameters, const nlohmann::json& appData) override {
    OH_LOG_INFO(LOG_APP, "生产媒体：%s - %s - %s", this->room->roomId.data(), transport->GetId().data(), kind.data());
    nlohmann::json requestBody = {
        { "kind",          kind               },
        { "roomId",        this.roomId        },
        { "transportId",   transport->GetId() },
        { "rtpParameters", rtpParameters      },
    };
    std::string response = acgist::request("media::produce", requestBody.dump());
    nlohmann::json json = nlohmann::json::parse(response);
    nlohmann::json responseBody = json["body"];
    std::string producerId = responseBody["producerId"];
    std::promise<std::string> promise;
    promise.set_value(producerId);
    return promise.get_future();
}

std::future<std::string> acgist::SendListener::OnProduceData(mediasoupclient::SendTransport* transport, const nlohmann::json& sctpStreamParameters, const std::string& label, const std::string& protocol, const nlohmann::json& appData) override; {
    OH_LOG_INFO(LOG_APP, "生产数据：%s - %s - %s - %s", this->room->roomId.data(), transport->GetId().data(), label.data(), protocol.data());
    // TODO: 代码实现
    std::promise<std::string> promise;
    // promise.set_value("producerId");
    return promise.get_future();
}

acgist::RecvListener::RecvListener(acgist::Room* room) : room(room) {
}

acgist::RecvListener::~RecvListener() {
}

std::future<void> acgist::RecvListener::OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) {
    OH_LOG_INFO(LOG_APP, "连接接收通道：%s - %s", this->room->roomId.data(), transport->GetId().data());
    nlohmann::json requestBody = {
        { "roomId",           this->roomId       },
        { "transportId",      transport->GetId() },
        { "dtlsParameters",   dtlsParameters     },
    };
    std::string response = acgist::send("media::transport::webrtc::connect", requestBody.dump());
    std::promise<void> promise;
    promise.set_value();
    return promise.get_future();
}

void acgist::RecvListener::OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) {
    OH_LOG_INFO(LOG_APP, "接收通道状态改变：%s - %s - %s", this->room->roomId.data(), transport->GetId().data(), connectionState.data());
    // TODO: 异常关闭逻辑
}

acgist::ProducerListener::ProducerListener(acgist::Room* room) : room(room) {
}

acgist::ProducerListener::~ProducerListener() {
}

void acgist::ProducerListener::OnTransportClose(mediasoupclient::Producer* producer) {
    OH_LOG_INFO(LOG_APP, "生产者通道关闭：%s - %s", this->room->roomId.data(), producer->GetId().data());
    producer->Close();
    // TODO: 异常关闭逻辑
}

acgist::ConsumerListener::ConsumerListener(acgist::Room* room) : room(room) {
}

acgist::ConsumerListener::~ConsumerListener() {
}

void acgist::ConsumerListener::OnTransportClose(mediasoupclient::Consumer* consumer) {
    OH_LOG_INFO(LOG_APP, "消费者通道关闭：%s - %s", this->room->roomId.data(), consumer->GetId().data());
    consumer->Close();
    // TODO: 异常关闭逻辑
}
