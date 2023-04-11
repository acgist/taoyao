#include "RouterCallback.hpp"

namespace acgist {

    void RouterCallback::enterCallback(std::string rtpCapabilities, std::string sctpCapabilities) {
        jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
        jmethodID recvTransportConnectCallback = this->env->GetMethodID(jCallbackClazz, "enterCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
        const char* cRtpCapabilities  = rtpCapabilities.data();
        const char* cSctpCapabilities = sctpCapabilities.data();
        jstring jRtpCapabilities  = this->env->NewStringUTF(cRtpCapabilities);
        jstring jScrpCapabilities = this->env->NewStringUTF(cSctpCapabilities);
        this->env->CallVoidMethod(
            this->routerCallback,
            recvTransportConnectCallback,
            jRtpCapabilities,
            jScrpCapabilities
        );
        this->env->DeleteLocalRef(jRtpCapabilities);
        this->env->DeleteLocalRef(jScrpCapabilities);
        this->env->DeleteLocalRef(jCallbackClazz);
    }

    void RouterCallback::sendTransportConnectCallback(std::string transportId, std::string dtlsParameters) {
        jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
        jmethodID sendTransportConnectCallback = this->env->GetMethodID(jCallbackClazz, "sendTransportConnectCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
        const char* cTransportId    = transportId.data();
        const char* cDtlsParameters = dtlsParameters.data();
        jstring jTransportId    = this->env->NewStringUTF(cTransportId);
        jstring jDtlsParameters = this->env->NewStringUTF(cDtlsParameters);
        this->env->CallVoidMethod(
            this->routerCallback,
            sendTransportConnectCallback,
            jTransportId,
            jDtlsParameters
        );
        this->env->DeleteLocalRef(jTransportId);
        this->env->DeleteLocalRef(jDtlsParameters);
        this->env->DeleteLocalRef(jCallbackClazz);
    }

    std::string RouterCallback::sendTransportProduceCallback(std::string kind, std::string transportId, std::string rtpParameters) {
        jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
        jmethodID sendTransportProduceCallback = this->env->GetMethodID(jCallbackClazz, "sendTransportProduceCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
        const char* cKind          = kind.data();
        const char* cTransportId   = transportId.data();
        const char* cRtpParameters = rtpParameters.data();
        jstring jKind          = this->env->NewStringUTF(cKind);
        jstring jTransportId   = this->env->NewStringUTF(cTransportId);
        jstring jRtpParameters = this->env->NewStringUTF(cRtpParameters);
        jstring jResult = (jstring) this->env->CallObjectMethod(
            this->routerCallback,
            sendTransportProduceCallback,
            jKind,
            jTransportId,
            jRtpParameters
        );
        const char* result = this->env->GetStringUTFChars(jResult, nullptr);
        this->env->DeleteLocalRef(jResult);
        this->env->ReleaseStringUTFChars(jResult, result);
        this->env->DeleteLocalRef(jKind);
        this->env->DeleteLocalRef(jTransportId);
        this->env->DeleteLocalRef(jRtpParameters);
        this->env->DeleteLocalRef(jCallbackClazz);
        return result;
    }

    void RouterCallback::recvTransportConnectCallback(std::string transportId, std::string dtlsParameters) {
        jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
        jmethodID recvTransportConnectCallback = this->env->GetMethodID(jCallbackClazz, "recvTransportConnectCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
        const char* cTransportId    = transportId.data();
        const char* cDtlsParameters = dtlsParameters.data();
        jstring jTransportId    = this->env->NewStringUTF(cTransportId);
        jstring jDtlsParameters = this->env->NewStringUTF(cDtlsParameters);
        this->env->CallVoidMethod(
            this->routerCallback,
            recvTransportConnectCallback,
            jTransportId,
            jDtlsParameters
        );
        this->env->DeleteLocalRef(jTransportId);
        this->env->DeleteLocalRef(jDtlsParameters);
        this->env->DeleteLocalRef(jCallbackClazz);
    }

    void RouterCallback::consumerNewCallback(std::string message, webrtc::MediaStreamTrackInterface* consumerMediaTrackPointer) {
        jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
        jmethodID consumerNewCallback = this->env->GetMethodID(jCallbackClazz, "consumerNewCallback", "(Ljava/lang/String;J;)V");
        const char* cMessage = message.data();
        jstring jMessage = this->env->NewStringUTF(cMessage);
        this->env->CallVoidMethod(
            this->routerCallback,
            consumerNewCallback,
            jMessage,
            (jlong) consumerMediaTrackPointer
        );
        this->env->DeleteLocalRef(jMessage);
        this->env->DeleteLocalRef(jCallbackClazz);
    }

}