#pragma once

#include <map>

#include "jni.h"
#include "sdk/android/src/jni/pc/peer_connection.h"
#include "sdk/android/native_api/jni/scoped_java_ref.h"

#include "Log.hpp"
#include "RouterCallback.hpp"

namespace acgist {

    class Room : public RouterCallback {
    public:
        std::string roomId;
        mediasoupclient::Device* device;
        mediasoupclient::PeerConnection* peerConnection;
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
        Room(std::string roomId, JavaVM* javaVM, jobject routerCallback);
        virtual ~Room();
    public:
        void enterRoom(JNIEnv* env, std::string rtpCapabilities, webrtc::PeerConnectionFactoryInterface* factory, webrtc::PeerConnectionInterface::RTCConfiguration& rtcConfiguration);
        void createSendTransport(JNIEnv* env, std::string body);
        void createRecvTransport(JNIEnv* env, std::string body);
        void mediaProduceAudio(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream);
        void mediaProduceVideo(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream);
        void mediaConsume(JNIEnv* env, std::string message);
        void mediaProducerPause(JNIEnv* env, std::string producerId);
        void mediaProducerResume(JNIEnv* env, std::string producerId);
        void mediaProducerClose(JNIEnv* env, std::string producerId);
        void mediaConsumerPause(JNIEnv* env, std::string consumerId);
        void mediaConsumerResume(JNIEnv* env, std::string consumerId);
        void mediaConsumerClose(JNIEnv* env, std::string consumerId);
        void closeRoom();
    };

}