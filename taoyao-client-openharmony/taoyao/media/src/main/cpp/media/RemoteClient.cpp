#include "../include/Client.hpp"

acgist::RemoteClient::RemoteClient(acgist::MediaManager* mediaManager) : RoomClient(mediaManager) {}

acgist::RemoteClient::~RemoteClient() {}

bool acgist::RemoteClient::release() { return true; }
