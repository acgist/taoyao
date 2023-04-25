#pragma once

#include <string>

#include "jni.h"
#include "mediasoupclient.hpp"

namespace acgist {

    class RouterCallback {
    public:
        jobject routerCallback;
    public:
        void enterRoomCallback(JNIEnv* env, std::string rtpCapabilities, std::string sctpCapabilities);
        void closeRoomCallback(JNIEnv* env);
        void sendTransportConnectCallback(JNIEnv* env, std::string transportId, std::string dtlsParameters);
        void recvTransportConnectCallback(JNIEnv* env, std::string transportId, std::string dtlsParameters);
        std::string sendTransportProduceCallback(JNIEnv* env, std::string kind, std::string transportId, std::string rtpParameters);
        void producerNewCallback(JNIEnv* env, std::string kind, std::string producerId, mediasoupclient::Producer* producerPointer, webrtc::MediaStreamTrackInterface* producerMediaTrackPointer);
        void producerCloseCallback(JNIEnv* env, std::string producerId);
        void producerPauseCallback(JNIEnv* env, std::string producerId);
        void producerResumeCallback(JNIEnv* env, std::string producerId);
        void consumerNewCallback(JNIEnv* env, std::string message, mediasoupclient::Consumer* consumerPointer, webrtc::MediaStreamTrackInterface* consumerMediaTrackPointer);
        void consumerCloseCallback(JNIEnv* env, std::string consumerId);
        void consumerPauseCallback(JNIEnv* env, std::string consumerId);
        void consumerResumeCallback(JNIEnv* env, std::string consumerId);
    };

}