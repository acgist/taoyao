#pragma once

#include <string>

#include "jni.h"
#include "mediasoupclient.hpp"

namespace acgist {

    class RemoteClient {
    public:
        std::string name;
        std::string clientId;
        mediasoupclient::Consumer* consumer;
    public:
        jmethodID newCallback;
        jmethodID pauseCallback;
        jmethodID resumeCallback;
        jmethodID closeCallback;
    public:
        void pause();
        void resume();
        void close();
    };

}