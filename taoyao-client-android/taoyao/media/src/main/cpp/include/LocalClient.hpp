#pragma once

#include <string>

#include "jni.h"
#include "mediasoupclient.hpp"

namespace acgist {

    class LocalClient {
    public:
        std::string name;
        std::string clientId;
        mediasoupclient::Producer* producer;
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