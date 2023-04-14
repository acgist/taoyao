#include "RouterCallback.hpp"

namespace acgist {

    void RouterCallback::enterRoomCallback(JNIEnv* env, std::string rtpCapabilities, std::string sctpCapabilities) {
        jclass jCallbackClazz = env->GetObjectClass(this->routerCallback);
        jmethodID recvTransportConnectCallback = env->GetMethodID(jCallbackClazz, "enterRoomCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
        const char* cRtpCapabilities  = rtpCapabilities.data();
        const char* cSctpCapabilities = sctpCapabilities.data();
        jstring jRtpCapabilities  = env->NewStringUTF(cRtpCapabilities);
        jstring jScrpCapabilities = env->NewStringUTF(cSctpCapabilities);
        env->CallVoidMethod(
            this->routerCallback,
            recvTransportConnectCallback,
            jRtpCapabilities,
            jScrpCapabilities
        );
        env->DeleteLocalRef(jRtpCapabilities);
        env->DeleteLocalRef(jScrpCapabilities);
        env->DeleteLocalRef(jCallbackClazz);
    }

    void RouterCallback::closeRoomCallback(JNIEnv* env) {
        jclass jCallbackClazz = env->GetObjectClass(this->routerCallback);
        jmethodID closeRoomCallback = env->GetMethodID(jCallbackClazz, "closeRoomCallback", "()V");
        env->CallVoidMethod(
            this->routerCallback,
            closeRoomCallback
        );
        env->DeleteLocalRef(jCallbackClazz);
    }

    void RouterCallback::sendTransportConnectCallback(JNIEnv* env, std::string transportId, std::string dtlsParameters) {
        jclass jCallbackClazz = env->GetObjectClass(this->routerCallback);
        jmethodID sendTransportConnectCallback = env->GetMethodID(jCallbackClazz, "sendTransportConnectCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
        const char* cTransportId    = transportId.data();
        const char* cDtlsParameters = dtlsParameters.data();
        jstring jTransportId    = env->NewStringUTF(cTransportId);
        jstring jDtlsParameters = env->NewStringUTF(cDtlsParameters);
        env->CallVoidMethod(
            this->routerCallback,
            sendTransportConnectCallback,
            jTransportId,
            jDtlsParameters
        );
        env->DeleteLocalRef(jTransportId);
        env->DeleteLocalRef(jDtlsParameters);
        env->DeleteLocalRef(jCallbackClazz);
    }

    void RouterCallback::recvTransportConnectCallback(JNIEnv* env, std::string transportId, std::string dtlsParameters) {
        jclass jCallbackClazz = env->GetObjectClass(this->routerCallback);
        jmethodID recvTransportConnectCallback = env->GetMethodID(jCallbackClazz, "recvTransportConnectCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
        const char* cTransportId    = transportId.data();
        const char* cDtlsParameters = dtlsParameters.data();
        jstring jTransportId    = env->NewStringUTF(cTransportId);
        jstring jDtlsParameters = env->NewStringUTF(cDtlsParameters);
        env->CallVoidMethod(
            this->routerCallback,
            recvTransportConnectCallback,
            jTransportId,
            jDtlsParameters
        );
        env->DeleteLocalRef(jTransportId);
        env->DeleteLocalRef(jDtlsParameters);
        env->DeleteLocalRef(jCallbackClazz);
    }

    std::string RouterCallback::sendTransportProduceCallback(JNIEnv* env, std::string kind, std::string transportId, std::string rtpParameters) {
        jclass jCallbackClazz = env->GetObjectClass(this->routerCallback);
        jmethodID sendTransportProduceCallback = env->GetMethodID(jCallbackClazz, "sendTransportProduceCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
        const char* cKind          = kind.data();
        const char* cTransportId   = transportId.data();
        const char* cRtpParameters = rtpParameters.data();
        jstring jKind          = env->NewStringUTF(cKind);
        jstring jTransportId   = env->NewStringUTF(cTransportId);
        jstring jRtpParameters = env->NewStringUTF(cRtpParameters);
        jstring jResult = (jstring) env->CallObjectMethod(
            this->routerCallback,
            sendTransportProduceCallback,
            jKind,
            jTransportId,
            jRtpParameters
        );
        const char* result = env->GetStringUTFChars(jResult, nullptr);
        env->DeleteLocalRef(jResult);
        env->ReleaseStringUTFChars(jResult, result);
        env->DeleteLocalRef(jKind);
        env->DeleteLocalRef(jTransportId);
        env->DeleteLocalRef(jRtpParameters);
        env->DeleteLocalRef(jCallbackClazz);
        return result;
    }

    void RouterCallback::producerNewCallback(JNIEnv* env, std::string kind, std::string producerId, mediasoupclient::Producer* producerPointer, webrtc::MediaStreamTrackInterface* producerMediaTrackPointer) {
        jclass jCallbackClazz = env->GetObjectClass(this->routerCallback);
        jmethodID producerNewCallback = env->GetMethodID(jCallbackClazz, "producerNewCallback", "(Ljava/lang/String;java/lang/String;J;J;)V");
        const char* cKind = kind.data();
        jstring jKind     = env->NewStringUTF(cKind);
        const char* cProducerId = producerId.data();
        jstring jProducerId     = env->NewStringUTF(cProducerId);
        env->CallVoidMethod(
            this->routerCallback,
            producerNewCallback,
            jKind,
            jProducerId,
            (jlong) producerPointer,
            (jlong) producerMediaTrackPointer
        );
        env->DeleteLocalRef(jKind);
        env->DeleteLocalRef(jProducerId);
        env->DeleteLocalRef(jCallbackClazz);
    }

    void RouterCallback::producerCloseCallback(JNIEnv* env, std::string producerId) {}

    void RouterCallback::producerPauseCallback(JNIEnv* env, std::string producerId) {}

    void RouterCallback::producerResumeCallback(JNIEnv* env, std::string producerId) {}

    void RouterCallback::consumerNewCallback(JNIEnv* env, std::string message, mediasoupclient::Consumer* consumerPointer, webrtc::MediaStreamTrackInterface* consumerMediaTrackPointer) {
        jclass jCallbackClazz = env->GetObjectClass(this->routerCallback);
        jmethodID consumerNewCallback = env->GetMethodID(jCallbackClazz, "consumerNewCallback", "(Ljava/lang/String;J;J;)V");
        const char* cMessage = message.data();
        jstring jMessage     = env->NewStringUTF(cMessage);
        env->CallVoidMethod(
            this->routerCallback,
            consumerNewCallback,
            jMessage,
            (jlong) consumerPointer,
            (jlong) consumerMediaTrackPointer
        );
        env->DeleteLocalRef(jMessage);
        env->DeleteLocalRef(jCallbackClazz);
    }

    void RouterCallback::consumerCloseCallback(JNIEnv* env, std::string producerId) {}

    void RouterCallback::consumerPauseCallback(JNIEnv* env, std::string producerId) {}

    void RouterCallback::consumerResumeCallback(JNIEnv* env, std::string producerId) {}

}