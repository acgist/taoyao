#include <jni.h>
#include <string>

#include "Device.hpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_acgist_taoyao_client_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    mediasoupclient::Device device;
    if(device.IsLoaded()) {
        std::string hello = "Hello from C++ true";
        return env->NewStringUTF(hello.c_str());
    } else {
        std::string hello = "Hello from C++ false";
        return env->NewStringUTF(hello.c_str());
    }
}