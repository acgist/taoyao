#pragma once

#include <map>

#include "jni.h"
#include "sdk/android/src/jni/pc/peer_connection.h"
#include "sdk/android/native_api/jni/scoped_java_ref.h"

#include "Log.hpp"
#include "MediaManager.hpp"
#include "RouterCallback.hpp"

namespace acgist {

    class Room : public RouterCallback {
    public:
        std::string roomId;
        webrtc::PeerConnectionFactoryInterface* factory;
        mediasoupclient::Device* device;
        mediasoupclient::SendTransport* sendTransport;
        mediasoupclient::RecvTransport* recvTransport;
        mediasoupclient::SendTransport::Listener* sendListener;
        mediasoupclient::RecvTransport::Listener* recvListener;
        mediasoupclient::Producer* audioProducer;
        mediasoupclient::Producer* videoProducer;
        mediasoupclient::Producer::Listener* producerListener;
        mediasoupclient::Consumer::Listener* consumerListener;
        std::map<std::string, mediasoupclient::Consumer*> consumers;
    public:
        Room(const std::string& roomId, const jobject& routerCallback);
        virtual ~Room();
    public:
        void enterRoom(JNIEnv* env, const std::string& rtpCapabilities, webrtc::PeerConnectionFactoryInterface* factory, webrtc::PeerConnectionInterface::RTCConfiguration& rtcConfiguration);
        void createSendTransport(JNIEnv* env, const std::string& body);
        void createRecvTransport(JNIEnv* env, const std::string& body);
        void mediaProduceAudio(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream);
        void mediaProduceVideo(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream);
        void mediaConsume(JNIEnv* env, const std::string& message);
        void mediaProducerPause(JNIEnv* env, const std::string& producerId);
        void mediaProducerResume(JNIEnv* env, const std::string& producerId);
        void mediaProducerClose(JNIEnv* env, const std::string& producerId);
        void mediaConsumerPause(JNIEnv* env, const std::string& consumerId);
        void mediaConsumerResume(JNIEnv* env, const std::string& consumerId);
        void mediaConsumerClose(JNIEnv* env, const std::string& consumerId);
        void closeRoom();
    };

}