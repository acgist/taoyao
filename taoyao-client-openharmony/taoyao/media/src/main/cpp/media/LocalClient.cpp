#include "../include/Client.hpp"

acgist::LocalClient::LocalClient(acgist::MediaManager* mediaManager) : acgist::RoomClient(mediaManager) {
    this->mediaManager->newLocalClient();
    // this->audioTrack = this->mediaManager->getAudioTrack();
    this->videoTrack = this->mediaManager->getVideoTrack();
}

acgist::LocalClient::~LocalClient() {
    this->mediaManager->releaseLocalClient();
}
