#include "../include/Client.hpp"

#include <hilog/log.h>

acgist::Client::Client(acgist::MediaManager* mediaManager) : mediaManager(mediaManager) {
}

acgist::Client::~Client() {
    OH_LOG_INFO(LOG_APP, "释放终端：%s", this->clientId.data());
    if(this->audioTrack != nullptr) {
        this->audioTrack->Release();
        // delete this->audioTrack;
        this->audioTrack = nullptr;
    }
    if(this->videoTrack != nullptr) {
        this->videoTrack->Release();
        // delete this->videoTrack;
        this->videoTrack = nullptr;
    }
}
