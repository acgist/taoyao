#include "../include/Room.hpp"

#include <mutex>
#include <thread>

#include "hilog/log.h"

static std::recursive_mutex roomMutex;
static std::recursive_mutex mediaMutex;
static std::recursive_mutex clientMutex;

acgist::Room::Room(const std::string& roomId, acgist::MediaManager* mediaManager) : roomId(roomId), mediaManager(mediaManager) {
    this->device = new mediasoupclient::Device();
    this->sendListener = new acgist::SendListener(this);
    this->recvListener = new acgist::RecvListener(this);
    this->producerListener = new acgist::ProducerListener(this);
    this->consumerListener = new acgist::ConsumerListener(this);
}

acgist::Room::~Room() {
    this->close();
    if(this->rtcConfiguration != nullptr) {
        delete this->rtcConfiguration;
        this->rtcConfiguration = nullptr;
    }
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
}

int acgist::Room::enter(const std::string& password) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->enterd) {
        return 0;
    }
    this->enterd = true;
    if (this->device->IsLoaded()) {
        OH_LOG_WARN(LOG_APP, "Device配置已经加载：%{public}s", this->roomId.data());
        return -1;
    }
    // 本地终端
    this->client = new acgist::LocalClient(this->mediaManager);
    this->client->clientId = acgist::clientId;
    this->client->name     = acgist::name;
    this->client->roomId   = this->roomId;
    // 配置连接
    this->rtcConfiguration = new webrtc::PeerConnectionInterface::RTCConfiguration();
    // this->rtcConfiguration->set_cpu_adaptation(false);
    // 房间能力
    nlohmann::json requestBody = {
        { "roomId", this->roomId }
    };
    nlohmann::json json = acgist::request("media::router::rtp::capabilities", requestBody.dump());
    if(json.find("body") == json.end()) {
        OH_LOG_WARN(LOG_APP, "进入房间失败：%{public}s", this->roomId.data());
        return -1;
    }
    nlohmann::json responseBody = json["body"];
    nlohmann::json rtpCapabilities = responseBody["rtpCapabilities"];
    // 加载设备
    OH_LOG_INFO(LOG_APP, "加载设备：%{public}s", this->roomId.data());
    mediasoupclient::PeerConnection::Options options;
    options.config  = *this->rtcConfiguration;
    options.factory = this->mediaManager->peerConnectionFactory.get();
    this->device->Load(rtpCapabilities, &options);
    // 进入房间
    OH_LOG_INFO(LOG_APP, "进入房间：%{public}s", this->roomId.data());
    requestBody = {
        { "roomId",           this->roomId                        },
        { "password",         password                            },
        { "rtpCapabilities",  this->device->GetRtpCapabilities()  },
        { "sctpCapabilities", this->device->GetSctpCapabilities() }
    };
    acgist::request("room::enter", requestBody.dump());
    return 0;
}

int acgist::Room::produceMedia() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    OH_LOG_INFO(LOG_APP, "生成媒体：%{public}s", this->roomId.data());
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
//        this->produceVideo();
    }
    return 0;
}

int acgist::Room::createSendTransport() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->sendTransport != nullptr) {
        OH_LOG_INFO(LOG_APP, "发送通道已经存在：%{public}s", this->roomId.data());
        return -1;
    }
    nlohmann::json requestBody = {
        { "roomId",           this->roomId     },
        { "forceTcp",         false            },
        { "producing",        true             },
        { "consuming",        false            },
    //  { "sctpCapabilities", sctpCapabilities },
    };
    nlohmann::json json = acgist::request("media::transport::webrtc::create", requestBody.dump());
    if(json.find("body") == json.end()) {
        OH_LOG_WARN(LOG_APP, "创建发送通道失败：%{public}s", this->roomId.data());
        return -1;
    }
    OH_LOG_INFO(LOG_APP, "创建发送通道：%{public}s", this->roomId.data());
    nlohmann::json responseBody = json["body"];
    mediasoupclient::PeerConnection::Options options;
    options.config  = *this->rtcConfiguration;
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
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->recvTransport != nullptr) {
        OH_LOG_INFO(LOG_APP, "接收通道已经存在：%{public}s", this->roomId.data());
        return -1;
    }
    nlohmann::json requestBody = {
        { "roomId",           this->roomId     },
        { "forceTcp",         false            },
        { "producing",        false            },
        { "consuming",        true             },
    //  { "sctpCapabilities", sctpCapabilities },
    };
    nlohmann::json json = acgist::request("media::transport::webrtc::create", requestBody.dump());
    if(json.find("body") == json.end()) {
        OH_LOG_WARN(LOG_APP, "创建接收通道失败：%{public}s", this->roomId.data());
        return -1;
    }
    OH_LOG_INFO(LOG_APP, "创建接收通道：%{public}s", this->roomId.data());
    nlohmann::json responseBody = json["body"];
    mediasoupclient::PeerConnection::Options options;
    options.config      = *this->rtcConfiguration;
    options.factory     = this->mediaManager->peerConnectionFactory.get();
    this->recvTransport = this->device->CreateRecvTransport(
        this->recvListener,
        responseBody["transportId"],
        responseBody["iceParameters"],
        responseBody["iceCandidates"],
        responseBody["dtlsParameters"],
        responseBody["sctpParameters"],
        &options
    );
    return 0;
}

int acgist::Room::produceAudio() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->audioProducer != nullptr) {
        OH_LOG_INFO(LOG_APP, "音频媒体已经生产：%{public}s", this->roomId.data());
        return -1;
    }
    if(!this->device->CanProduce("audio")) {
        OH_LOG_INFO(LOG_APP, "不能生产音频媒体：%{public}s", this->roomId.data());
        return -1;
    }
    if(this->client->audioTrack->state() == webrtc::MediaStreamTrackInterface::TrackState::kEnded) {
        OH_LOG_INFO(LOG_APP, "音频媒体状态错误：%{public}s", this->roomId.data());
        return -2;
    }
    OH_LOG_INFO(LOG_APP, "生产音频媒体：%s", this->roomId.data());
    nlohmann::json codecOptions = {
        { "opusStereo", true },
        { "opusDtx",    true }
    };
    this->audioProducer = this->sendTransport->Produce(
        this->producerListener,
        this->client->audioTrack.get(),
        nullptr,
        &codecOptions,
        nullptr
    );
    return 0;
}

int acgist::Room::produceVideo() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->videoProducer != nullptr) {
        OH_LOG_INFO(LOG_APP, "视频媒体已经生产：%s", this->roomId.data());
        return -1;
    }
    if(!this->device->CanProduce("video")) {
        OH_LOG_INFO(LOG_APP, "不能生产视频媒体：%s", this->roomId.data());
        return -1;
    }
    if(this->client->videoTrack->state() == webrtc::MediaStreamTrackInterface::TrackState::kEnded) {
        OH_LOG_INFO(LOG_APP, "视频媒体状态错误：%s", this->roomId.data());
        return -2;
    }
    OH_LOG_INFO(LOG_APP, "生产视频媒体：%s", this->roomId.data());
    nlohmann::json codecOptions = {
        // x-google-start-bitrate
        { "videoGoogleStartBitrate", 1200 },
        // x-google-min-bitrate
        { "videoGoogleMinBitrate",   800  },
        // x-google-max-bitrate
        { "videoGoogleMaxBitrate",   1600 }
    };
    // 如果需要使用`Simulcast`打开下面配置
    // std::vector<webrtc::RtpEncodingParameters> encodings;
    // webrtc::RtpEncodingParameters min;
    // webrtc::RtpEncodingParameters mid;
    // webrtc::RtpEncodingParameters max;
    // min.active = true;
    // min.max_framerate   = 15;
    // min.min_bitrate_bps = 400;
    // min.max_bitrate_bps = 800;
    // encodings.emplace_back(min);
    // encodings.emplace_back(mid);
    // encodings.emplace_back(max);
    // 强制设置编码器
    // nlohmann::json codec = this->device->GetRtpCapabilities()["codec"];
    this->videoProducer = this->sendTransport->Produce(
        this->producerListener,
        this->client->videoTrack.get(),
        nullptr,
        &codecOptions,
        nullptr
    );
    return 0;
}

int acgist::Room::close() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->closed) {
        return 0;
    }
    this->closed = true;
    OH_LOG_INFO(LOG_APP, "关闭房间：%s", this->roomId.data());
    this->closeClient();
    this->closeClients();
    this->closeAudioProducer();
    this->closeVideoProducer();
    this->closeTransport();
    return 0;
}

int acgist::Room::closeClient() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    OH_LOG_INFO(LOG_APP, "关闭本地终端：%s", this->roomId.data());
    if(this->client != nullptr) {
        delete this->client;
        this->client = nullptr;
    }
    return 0;
}

int acgist::Room::closeClients() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    OH_LOG_INFO(LOG_APP, "关闭远程终端：%s", this->roomId.data());
    for (auto iterator = this->clients.begin(); iterator != this->clients.end(); ++iterator) {
        if (iterator->second == nullptr) {
            continue;
        }
        OH_LOG_INFO(LOG_APP, "关闭消费者：%s", iterator->first.data());
        delete iterator->second;
        iterator->second = nullptr;
    }
    this->clients.clear();
    this->consumerIdClientId.clear();
    return 0;
}

int acgist::Room::closeAudioProducer() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    OH_LOG_INFO(LOG_APP, "关闭音频生产者：%s", this->roomId.data());
    if (this->audioProducer != nullptr) {
        this->audioProducer->Close();
        delete this->audioProducer;
        this->audioProducer = nullptr;
    }
    return 0;
}

int acgist::Room::closeVideoProducer() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    OH_LOG_INFO(LOG_APP, "关闭视频生产者：%s", this->roomId.data());
    if (this->videoProducer != nullptr) {
        this->videoProducer->Close();
        delete this->videoProducer;
        this->videoProducer = nullptr;
    }
    return 0;
}

int acgist::Room::closeTransport() {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    OH_LOG_INFO(LOG_APP, "关闭通道：%s", this->roomId.data());
    if (this->sendTransport != nullptr) {
        this->sendTransport->Close();
        delete this->sendTransport;
        this->sendTransport = nullptr;
    }
    if (this->recvTransport != nullptr) {
        this->recvTransport->Close();
        delete this->recvTransport;
        this->recvTransport = nullptr;
    }
    return 0;
}

static void removeConsumerIdByClientId(const std::string clientId, std::map<std::string, std::string>& consumerIdClientId) {
    std::vector<std::string> keys;
    for(auto iterator = consumerIdClientId.begin(); iterator != consumerIdClientId.end(); ++iterator) {
        if(iterator->second == clientId) {
            keys.push_back(iterator->first);
        }
    }
    for(auto& key : keys) {
        consumerIdClientId.erase(key);
    }
}

int acgist::Room::newRemoteClient(const std::string& clientId, const std::string& name) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    auto oldClient = this->clients.find(clientId);
    if(oldClient != this->clients.end()) {
        OH_LOG_INFO(LOG_APP, "已经存在远程终端：%s", clientId.data());
        delete oldClient->second;
        oldClient->second = nullptr;
        this->clients.erase(oldClient);
        removeConsumerIdByClientId(clientId, this->consumerIdClientId);
    }
    OH_LOG_INFO(LOG_APP, "新增远程终端：%s", clientId.data());
    acgist::RemoteClient* client = new acgist::RemoteClient(this->mediaManager);
    client->clientId = clientId;
    client->name     = name;
    client->roomId   = this->roomId;
    this->clients.insert({ clientId, client });
    return 0;
}

int acgist::Room::closeRemoteClient(const std::string& clientId) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    auto client = this->clients.find(clientId);
    if(client == this->clients.end()) {
        OH_LOG_INFO(LOG_APP, "远程终端已经删除：%s", clientId.data());
        return -1;
    }
    OH_LOG_INFO(LOG_APP, "删除远程终端：%s", clientId.data());
    delete client->second;
    client->second = nullptr;
    this->clients.erase(client);
    removeConsumerIdByClientId(clientId, this->consumerIdClientId);
    return 0;
}

int acgist::Room::newConsumer(nlohmann::json& body) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    mediasoupclient::Consumer* consumer = this->recvTransport->Consume(
        this->consumerListener,
        body["consumerId"],
        body["producerId"],
        body["kind"],
        &body["rtpParameters"]
    );
    std::string kind       = body["kind"];
    std::string sourceId   = body["sourceId"];
    std::string consumerId = body["consumerId"];
    OH_LOG_INFO(LOG_APP, "新增媒体消费：%s %s %s", kind.data(), sourceId.data(), consumerId.data());
    auto oldClient = this->clients.find(sourceId);
    acgist::RemoteClient* client = nullptr;
    if(oldClient == this->clients.end()) {
        // 假如媒体上来时间比进入房间消息快：基本上不可能出现这种情况
        client = new acgist::RemoteClient(this->mediaManager);
        client->clientId = sourceId;
        client->name     = sourceId;
        client->roomId   = this->roomId;
        this->clients.insert({ sourceId, client });
    } else {
        client = oldClient->second;
    }
    client->addConsumer(consumer->GetId(), consumer);
    this->consumerIdClientId.insert({ consumer->GetId(), sourceId });
    return 0;
}

int acgist::Room::closeConsumer(const std::string& consumerId) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    auto clientId = this->consumerIdClientId.find(consumerId);
    if(clientId == this->consumerIdClientId.end()) {
        OH_LOG_INFO(LOG_APP, "关闭消费者无效：%s", consumerId.data());
        return -1;
    }
    this->consumerIdClientId.erase(clientId);
    auto client = this->clients.find(clientId->second);
    if(client == this->clients.end()) {
        OH_LOG_INFO(LOG_APP, "关闭消费者无效：%s %s", consumerId.data(), clientId->second.data());
        return -2;
    }
    OH_LOG_INFO(LOG_APP, "关闭消费者：%s %s", consumerId.data(), clientId->second.data());
    client->second->closeConsumer(consumerId);
    return 0;
}

int acgist::Room::pauseConsumer(const std::string& consumerId) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    auto clientId = this->consumerIdClientId.find(consumerId);
    if(clientId == this->consumerIdClientId.end()) {
        OH_LOG_INFO(LOG_APP, "暂停消费者无效：%s", consumerId.data());
        return -1;
    }
    auto client = this->clients.find(clientId->second);
    if(client == this->clients.end()) {
        OH_LOG_INFO(LOG_APP, "暂停消费者无效：%s %s", consumerId.data(), clientId->second.data());
        return -2;
    }
    OH_LOG_INFO(LOG_APP, "暂停消费者：%s %s", consumerId.data(), clientId->second.data());
    client->second->pauseConsumer(consumerId);
    return 0;
}

int acgist::Room::resumeConsumer(const std::string& consumerId) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    auto clientId = this->consumerIdClientId.find(consumerId);
    if(clientId == this->consumerIdClientId.end()) {
        OH_LOG_INFO(LOG_APP, "恢复消费者无效：%s", consumerId.data());
        return -1;
    }
    auto client = this->clients.find(clientId->second);
    if(client == this->clients.end()) {
        OH_LOG_INFO(LOG_APP, "恢复消费者无效：%s %s", consumerId.data(), clientId->second.data());
        return -2;
    }
    OH_LOG_INFO(LOG_APP, "恢复消费者：%s %s", consumerId.data(), clientId->second.data());
    client->second->resumeConsumer(consumerId);
    return 0;
}

int acgist::Room::closeProducer(const std::string& producerId) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->audioProducer != nullptr && this->audioProducer->GetId() == producerId) {
        OH_LOG_INFO(LOG_APP, "关闭音频生产者：%s", producerId.data());
        this->audioProducer->Close();
        delete this->audioProducer;
        this->audioProducer = nullptr;
    } else if(this->videoProducer != nullptr && this->videoProducer->GetId() == producerId) {
        OH_LOG_INFO(LOG_APP, "关闭视频生产者：%s", producerId.data());
        this->videoProducer->Close();
        delete this->videoProducer;
        this->videoProducer = nullptr;
    } else {
        return -1;
    }
    return 0;
}

int acgist::Room::pauseProducer(const std::string& producerId) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->audioProducer != nullptr && this->audioProducer->GetId() == producerId) {
        OH_LOG_INFO(LOG_APP, "暂停音频生产者：%s", producerId.data());
        this->audioProducer->Pause();
    } else if(this->videoProducer != nullptr && this->videoProducer->GetId() == producerId) {
        OH_LOG_INFO(LOG_APP, "暂停视频生产者：%s", producerId.data());
        this->videoProducer->Pause();
    } else {
        return -1;
    }
    return 0;
}

int acgist::Room::resumeProducer(const std::string& producerId) {
    std::lock_guard<std::recursive_mutex> lockRoom(roomMutex);
    if(this->audioProducer != nullptr && this->audioProducer->GetId() == producerId) {
        OH_LOG_INFO(LOG_APP, "恢复音频生产者：%s", producerId.data());
        this->audioProducer->Resume();
    } else if(this->videoProducer != nullptr && this->videoProducer->GetId() == producerId) {
        OH_LOG_INFO(LOG_APP, "恢复视频生产者：%s", producerId.data());
        this->videoProducer->Resume();
    } else {
        return -1;
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
        { "roomId",           this->room->roomId },
        { "transportId",      transport->GetId() },
        { "dtlsParameters",   dtlsParameters     },
    };
    acgist::push("media::transport::webrtc::connect", requestBody.dump());
    std::promise<void> promise;
    promise.set_value();
    return promise.get_future();
}

void acgist::SendListener::OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) {
    OH_LOG_INFO(LOG_APP, "发送通道状态改变：%s - %s - %s", this->room->roomId.data(), transport->GetId().data(), connectionState.data());
    // TODO: 自行实现异常逻辑
}

std::future<std::string> acgist::SendListener::OnProduce(mediasoupclient::SendTransport* transport, const std::string& kind, nlohmann::json rtpParameters, const nlohmann::json& appData) {
    OH_LOG_INFO(LOG_APP, "生产媒体：%s - %s - %s", this->room->roomId.data(), transport->GetId().data(), kind.data());
    nlohmann::json requestBody = {
        { "kind",          kind               },
        { "roomId",        this->room->roomId },
        { "transportId",   transport->GetId() },
        { "rtpParameters", rtpParameters      },
    };
    std::promise<std::string> promise;
    nlohmann::json json = acgist::request("media::produce", requestBody.dump());
    if(json.find("body") == json.end()) {
        OH_LOG_WARN(LOG_APP, "生产媒体失败：%{public}s", this->room->roomId.data());
        promise.set_value("");
    } else {
        nlohmann::json responseBody = json["body"];
        std::string producerId = responseBody["producerId"];
        promise.set_value(producerId);
    }
    return promise.get_future();
}

std::future<std::string> acgist::SendListener::OnProduceData(mediasoupclient::SendTransport* transport, const nlohmann::json& sctpStreamParameters, const std::string& label, const std::string& protocol, const nlohmann::json& appData) {
    OH_LOG_INFO(LOG_APP, "生产数据：%s - %s - %s - %s", this->room->roomId.data(), transport->GetId().data(), label.data(), protocol.data());
    // TODO: 自行实现实现逻辑
    std::promise<std::string> promise;
    // promise.set_value(producerId);
    return promise.get_future();
}

acgist::RecvListener::RecvListener(acgist::Room* room) : room(room) {
}

acgist::RecvListener::~RecvListener() {
}

std::future<void> acgist::RecvListener::OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) {
    OH_LOG_INFO(LOG_APP, "连接接收通道：%s - %s", this->room->roomId.data(), transport->GetId().data());
    nlohmann::json requestBody = {
        { "roomId",           this->room->roomId },
        { "transportId",      transport->GetId() },
        { "dtlsParameters",   dtlsParameters     },
    };
    acgist::push("media::transport::webrtc::connect", requestBody.dump());
    std::promise<void> promise;
    promise.set_value();
    return promise.get_future();
}

void acgist::RecvListener::OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) {
    OH_LOG_INFO(LOG_APP, "接收通道状态改变：%s - %s - %s", this->room->roomId.data(), transport->GetId().data(), connectionState.data());
    // TODO: 自行实现异常逻辑
}

acgist::ProducerListener::ProducerListener(acgist::Room* room) : room(room) {
}

acgist::ProducerListener::~ProducerListener() {
}

void acgist::ProducerListener::OnTransportClose(mediasoupclient::Producer* producer) {
    OH_LOG_INFO(LOG_APP, "生产者通道关闭：%s - %s", this->room->roomId.data(), producer->GetId().data());
    producer->Close();
    // TODO: 自行实现异常逻辑
}

acgist::ConsumerListener::ConsumerListener(acgist::Room* room) : room(room) {
}

acgist::ConsumerListener::~ConsumerListener() {
}

void acgist::ConsumerListener::OnTransportClose(mediasoupclient::Consumer* consumer) {
    OH_LOG_INFO(LOG_APP, "消费者通道关闭：%s - %s", this->room->roomId.data(), consumer->GetId().data());
    consumer->Close();
    // TODO: 自行实现异常逻辑
}
