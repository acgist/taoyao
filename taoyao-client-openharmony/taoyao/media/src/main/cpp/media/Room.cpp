#include "../include/Room.hpp"
#include "Client.hpp"

acgist::Room::Room(const std::string& roomId, acgist::MediaManager* mediaManager) : roomId(roomId), mediaManager(mediaManager) {
    
}

acgist::Room::~Room() {
    if(this->client != nullptr) {
        this->client->release();
        delete this->client;
        this->client = nullptr;
    }
}

int acgist::Room::enter(const std::string& password) {
    this->mediaManager->newLocalClient();
//     this->client = new LocalClient();
    return 0;
}
