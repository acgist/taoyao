#pragma once

#include <map>
#include <string>
#include <iostream>

#include "jni.h"
#include "sdk/android/src/jni/pc/peer_connection.h"
#include "sdk/android/native_api/jni/scoped_java_ref.h"

#include "Log.hpp"
#include "mediasoupclient.hpp"

namespace acgist {

    class Room {
    public:
        mediasoupclient::Device *device;
        mediasoupclient::SendTransport *sendTransport;
        mediasoupclient::RecvTransport *recvTransport;
        mediasoupclient::PeerConnection *peerConnection;
        mediasoupclient::SendTransport::Listener *sendListener;
        mediasoupclient::RecvTransport::Listener *recvListener;
        std::string roomId;
    public:
        JNIEnv *env;
        jobject routerCallback;
    public:
        Room(std::string roomId, JNIEnv *env, jobject routerCallback);
        virtual ~Room();
    public:
        void enter(
            std::string rtpCapabilities,
            webrtc::PeerConnectionFactoryInterface *factory,
            webrtc::PeerConnectionInterface::RTCConfiguration &rtcConfiguration
        );
        void createSendTransport(std::string body);
        void createRecvTransport(std::string body);
        void produceMedia(webrtc::MediaStreamInterface mediaStream);
        void closeLocalClient();
        void closeRemoteClient();
        void close();
    };

}