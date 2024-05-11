#include "../include/Client.hpp"

#include <mutex>

#include "hilog/log.h"

static std::recursive_mutex clientMutex;

acgist::RemoteClient::RemoteClient(acgist::MediaManager* mediaManager) : RoomClient(mediaManager) {
}

acgist::RemoteClient::~RemoteClient() {
    for(auto iterator = this->consumers.begin(); iterator != this->consumers.end(); ++iterator) {
        iterator->second->Close();
        delete iterator->second;
        iterator->second = nullptr;
    }
    this->consumers.clear();
}

bool acgist::RemoteClient::addConsumer(const std::string& consumerId, mediasoupclient::Consumer* consumer) {
    std::lock_guard<std::recursive_mutex> clientLock(clientMutex);
    auto oldConsumer = this->consumers.find(clientId);
    if(oldConsumer != this->consumers.end()) {
        OH_LOG_INFO(LOG_APP, "关闭旧的消费者：%s", consumerId.data());
        oldConsumer->second->Close();
        delete oldConsumer->second;
        oldConsumer->second = nullptr;
        this->consumers.erase(oldConsumer);
    }
    this->consumers.insert({ consumerId, consumer });
    webrtc::MediaStreamTrackInterface* track = consumer->GetTrack();
    OH_LOG_INFO(LOG_APP, "添加新的消费者：%s %s", consumerId.data(), track->kind().data());
    if(track->kind() == webrtc::MediaStreamTrackInterface::kAudioKind) {
        this->audioTrack = (webrtc::AudioTrackInterface*) track;
        // TODO: old track
    }
    if(track->kind() == webrtc::MediaStreamTrackInterface::kVideoKind) {
        this->videoTrack = (webrtc::VideoTrackInterface*) track;
        // TODO: old track
    }
    return true;
}

bool acgist::RemoteClient::closeConsumer(const std::string& consumerId) {
    std::lock_guard<std::recursive_mutex> clientLock(clientMutex);
    auto consumer = this->consumers.find(consumerId);
    if(consumer == this->consumers.end()) {
        OH_LOG_INFO(LOG_APP, "消费者已经关闭：%s", consumerId.data());
        return false;
    }
    OH_LOG_INFO(LOG_APP, "关闭消费者：%s", consumerId.data());
    consumer->second->Close();
    delete consumer->second;
    consumer->second = nullptr;
    this->consumers.erase(consumer);
    return true;
}

bool acgist::RemoteClient::pauseConsumer(const std::string& consumerId) {
    std::lock_guard<std::recursive_mutex> clientLock(clientMutex);
    auto consumer = this->consumers.find(consumerId);
    if(consumer == this->consumers.end()) {
        OH_LOG_INFO(LOG_APP, "无效消费者：%s", consumerId.data());
        return false;
    }
    OH_LOG_INFO(LOG_APP, "暂停消费者：%s", consumerId.data());
    consumer->second->Pause();
    return true;
}

bool acgist::RemoteClient::resumeConsumer(const std::string& consumerId) {
    std::lock_guard<std::recursive_mutex> clientLock(clientMutex);
    auto consumer = this->consumers.find(consumerId);
    if(consumer == this->consumers.end()) {
        OH_LOG_INFO(LOG_APP, "无效消费者：%s", consumerId.data());
        return false;
    }
    OH_LOG_INFO(LOG_APP, "恢复消费者：%s", consumerId.data());
    consumer->second->Resume();
    return true;
}
