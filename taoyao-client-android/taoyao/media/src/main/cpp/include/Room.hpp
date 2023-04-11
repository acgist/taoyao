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
        Room(std::string roomId, JNIEnv* env, jobject routerCallback);
        virtual ~Room();
    public:
        void enter(std::string rtpCapabilities, webrtc::PeerConnectionFactoryInterface* factory, webrtc::PeerConnectionInterface::RTCConfiguration& rtcConfiguration);
        void createSendTransport(std::string body);
        void createRecvTransport(std::string body);
        void mediaProduceAudio(webrtc::MediaStreamInterface* mediaStream);
        void mediaProduceVideo(webrtc::MediaStreamInterface* mediaStream);
        void mediaConsume(std::string message);
        void mediaProducerPause(std::string producerId);
        void mediaProducerResume(std::string producerId);
        void mediaProducerClose(std::string producerId);
        void mediaConsumerPause(std::string consumerId);
        void mediaConsumerResume(std::string consumerId);
        void mediaConsumerClose(std::string consumerId);
        void close();
    };

}