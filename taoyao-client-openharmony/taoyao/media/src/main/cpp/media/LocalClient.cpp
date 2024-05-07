#include "../include/Client.hpp"

acgist::LocalClient::LocalClient(acgist::MediaManager* mediaManager) : acgist::RoomClient(mediaManager) {
    this->mediaManager->newLocalClient();
}

acgist::LocalClient::~LocalClient() {
    this->release();
    this->mediaManager->releaseLocalClient();
}

bool acgist::LocalClient::release() {
    return true;
}
