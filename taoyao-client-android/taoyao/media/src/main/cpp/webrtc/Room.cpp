#include <iostream>

#include "Room.hpp"

namespace acgist {

Room::Room() {
}

Room::~Room() {
}

}

void init() {
    std::cout << "加载MediasoupClient：" << mediasoupclient::Version() << std::endl;
    std::cout << "加载libwebrtc" << std::endl;
    mediasoupclient::Initialize();
//    mediasoupclient::parseScalabilityMode("L2T3");
    // => { spatialLayers: 2, temporalLayers: 3 }
//    mediasoupclient::parseScalabilityMode("L4T7_KEY_SHIFT");
    // => { spatialLayers: 4, temporalLayers: 7 }
}

void load() {
    // TODO：JNI信令交互
//    if(acgist::Room::pDevice == nullptr) {
//      acgist::Room::pDevice = new mediasoupclient::Device();
//      acgist::Room::pDevice->Load();
//    }
}

void stop() {
    std::cout << "释放libwebrtc" << std::endl;
    mediasoupclient::Cleanup();
}