#include "../include/Client.hpp"

acgist::Client::Client(acgist::MediaManager* mediaManager) : mediaManager(mediaManager) {
}

acgist::Client::~Client() {
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
