#pragma once

#include <string>

#include "jni.h"
#include "mediasoupclient.hpp"

namespace acgist {

    class RouterCallback {
    public:
        jobject routerCallback;
    public:
        void enterRoomCallback(JNIEnv* env, const std::string& rtpCapabilities, const std::string& sctpCapabilities);
        void closeRoomCallback(JNIEnv* env);
        void sendTransportConnectCallback(JNIEnv* env, const std::string& transportId, const std::string& dtlsParameters);
        void recvTransportConnectCallback(JNIEnv* env, const std::string& transportId, const std::string& dtlsParameters);
        std::string sendTransportProduceCallback(JNIEnv* env, const std::string& kind, const std::string& transportId, const std::string& rtpParameters);
        void producerNewCallback(JNIEnv* env, const std::string& kind, const std::string& producerId, mediasoupclient::Producer* producerPointer, webrtc::MediaStreamTrackInterface* producerMediaTrackPointer);
        void producerCloseCallback(JNIEnv* env, const std::string& producerId);
        void producerPauseCallback(JNIEnv* env, const std::string& producerId);
        void producerResumeCallback(JNIEnv* env, const std::string& producerId);
        void consumerNewCallback(JNIEnv* env, const std::string& message, mediasoupclient::Consumer* consumerPointer, webrtc::MediaStreamTrackInterface* consumerMediaTrackPointer);
        void consumerCloseCallback(JNIEnv* env, const std::string& consumerId);
        void consumerPauseCallback(JNIEnv* env, const std::string& consumerId);
        void consumerResumeCallback(JNIEnv* env, const std::string& consumerId);
    };

}