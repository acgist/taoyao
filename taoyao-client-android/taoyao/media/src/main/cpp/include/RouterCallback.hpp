#pragma once

#include <string>

#include "jni.h"
#include "mediasoupclient.hpp"

namespace acgist {

    class RouterCallback {
    public:
        JNIEnv* env;
        jobject routerCallback;
    public:
        void enterCallback(std::string rtpCapabilities, std::string sctpCapabilities);
        void sendTransportConnectCallback(std::string transportId, std::string dtlsParameters);
        std::string sendTransportProduceCallback(std::string kind, std::string transportId, std::string rtpParameters);
        void recvTransportConnectCallback(std::string transportId, std::string dtlsParameters);
        void producerNewCallback(std::string kind, std::string producerId, webrtc::MediaStreamTrackInterface* producerMediaTrackPointer);
        void producerPauseCallback(std::string producerId);
        void producerResumeCallback(std::string producerId);
        void producerCloseCallback(std::string producerId);
        void consumerNewCallback(std::string message, webrtc::MediaStreamTrackInterface* consumerMediaTrackPointer);
        void consumerPauseCallback(std::string consumerId);
        void consumerResumeCallback(std::string consumerId);
        void consumerCloseCallback(std::string consumerId);
    };

}