#include "../include/Client.hpp"

acgist::LocalClient::LocalClient(acgist::MediaManager* mediaManager) : acgist::RoomClient(mediaManager) {
    this->mediaManager->newLocalClient();
}

acgist::LocalClient::~LocalClient() {
    this->mediaManager->releaseLocalClient();
}
