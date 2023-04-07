#include "Room.hpp"

namespace acgist {

    class SendListener : public mediasoupclient::SendTransport::Listener {

    public:
        Room *room;
        JNIEnv *env;
        jobject routerCallback;

    public:
        SendListener(Room *room, JNIEnv *env, jobject routerCallback) {
            this->room = room;
            this->env = env;
            this->routerCallback = routerCallback;
        }

    public:
        std::future<void> OnConnect(mediasoupclient::Transport *transport, const nlohmann::json &dtlsParameters) override {
            jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
            jmethodID sendTransportConnectCallback = this->env->GetMethodID(jCallbackClazz, "sendTransportConnectCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
            const char *cTransportId = transport->GetId().data();
            jstring jTransportId = env->NewStringUTF(cTransportId);
            const char *cDtlsParameters = dtlsParameters.dump().data();
            jstring jDtlsParameters = env->NewStringUTF(cDtlsParameters);
            this->env->CallVoidMethod(
                this->routerCallback,
                sendTransportConnectCallback,
                jTransportId,
                jDtlsParameters
            );
            env->DeleteLocalRef(jTransportId);
            env->ReleaseStringUTFChars(jTransportId, cTransportId);
            env->DeleteLocalRef(jDtlsParameters);
            env->ReleaseStringUTFChars(jDtlsParameters, cDtlsParameters);
            env->DeleteLocalRef(jCallbackClazz);
            return std::future<void>();
        }

        void OnConnectionStateChange(mediasoupclient::Transport *transport, const std::string &connectionState) override {
            // 状态变化
        }

        std::future<std::string> OnProduce(mediasoupclient::SendTransport *transport, const std::string &kind, nlohmann::json rtpParameters, const nlohmann::json &appData) override {
            jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
            jmethodID sendTransportProduceCallback = this->env->GetMethodID(jCallbackClazz, "sendTransportProduceCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
            const char *cKind = kind.data();
            jstring jKind = env->NewStringUTF(cKind);
            const char *cTransportId = transport->GetId().data();
            jstring jTransportId = env->NewStringUTF(cTransportId);
            const char *cRtpParameters = rtpParameters.dump().data();
            jstring jRtpParameters = env->NewStringUTF(cRtpParameters);
            jstring jResult = (jstring) this->env->CallObjectMethod(
                this->routerCallback,
                sendTransportProduceCallback,
                jKind,
                jTransportId,
                jRtpParameters
            );
            const char *cResult = env->GetStringUTFChars(jResult, 0);
            std::string result(cResult);
            env->DeleteLocalRef(jResult);
            env->DeleteLocalRef(jKind);
            env->ReleaseStringUTFChars(jKind, cKind);
            env->DeleteLocalRef(jResult);
            env->ReleaseStringUTFChars(jResult, cResult);
            env->DeleteLocalRef(jTransportId);
            env->ReleaseStringUTFChars(jTransportId, cTransportId);
            env->DeleteLocalRef(jRtpParameters);
            env->ReleaseStringUTFChars(jRtpParameters, cRtpParameters);
            env->DeleteLocalRef(jCallbackClazz);
            return std::future<std::string>();
        }

        std::future<std::string> OnProduceData(mediasoupclient::SendTransport *transport, const nlohmann::json &sctpStreamParameters, const std::string &label, const std::string &protocol, const nlohmann::json &appData) override {
            // 数据生产
            return std::future<std::string>();
        }

    };

    class RecvListener : public mediasoupclient::RecvTransport::Listener {

    public:
        Room *room;
        JNIEnv *env;
        jobject routerCallback;

    public:
        RecvListener(Room *room, JNIEnv *env, jobject routerCallback) {
            this->room = room;
            this->env = env;
            this->routerCallback = routerCallback;
        }

        std::future<void> OnConnect(mediasoupclient::Transport *transport, const nlohmann::json &dtlsParameters) override {
            jclass jCallbackClazz = env->GetObjectClass(this->routerCallback);
            jmethodID recvTransportConnectCallback = this->env->GetMethodID(jCallbackClazz, "recvTransportConnectCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
            const char *cTransportId = transport->GetId().data();
            jstring jTransportId = env->NewStringUTF(cTransportId);
            const char *cDtlsParameters = dtlsParameters.dump().data();
            jstring jDtlsParameters = env->NewStringUTF(cDtlsParameters);
            this->env->CallVoidMethod(
                this->routerCallback,
                recvTransportConnectCallback,
                jTransportId,
                jDtlsParameters
            );
            env->DeleteLocalRef(jTransportId);
            env->ReleaseStringUTFChars(jTransportId, cTransportId);
            env->DeleteLocalRef(jDtlsParameters);
            env->ReleaseStringUTFChars(jDtlsParameters, cDtlsParameters);
            env->DeleteLocalRef(jCallbackClazz);
            return std::future<void>();
        }

        void OnConnectionStateChange(mediasoupclient::Transport *transport, const std::string &connectionState) override {
            // 状态变化
        }

    };

    Room::Room(
        std::string roomId,
        JNIEnv *env,
        jobject routerCallback
    ) {
        this->roomId = roomId;
        this->env = env;
        this->routerCallback = routerCallback;
        this->device = new mediasoupclient::Device();
        this->sendListener = new SendListener(this, env, routerCallback);
        this->recvListener = new RecvListener(this, env, routerCallback);
    }

    Room::~Room() {
        delete this->device;
        delete this->sendListener;
        delete this->sendTransport;
        delete this->recvListener;
        delete this->recvTransport;
        env->DeleteLocalRef(this->routerCallback);
        env->DeleteGlobalRef(this->routerCallback);
    }

    void Room::enter(
        std::string rtpCapabilities,
        webrtc::PeerConnectionFactoryInterface *factory,
        webrtc::PeerConnectionInterface::RTCConfiguration &rtcConfiguration
    ) {
        nlohmann::json json;
        // TODO：全局
        mediasoupclient::PeerConnection::Options options;
        options.config = rtcConfiguration;
        options.factory = factory;
        json["routerRtpCapabilities"] = nlohmann::json::parse(rtpCapabilities);
        this->device->Load(json, &options);
    }

    void Room::createSendTransport(std::string body) {
        nlohmann::json json = nlohmann::json::parse(body);
        this->sendTransport = this->device->CreateSendTransport(
            this->sendListener,
            json["transportId"],
            json["iceCandidates"],
            json["iceParameters"],
            json["dtlsParameters"],
            json["sctpParameters"]
            // TODO：全局options
        );
    }

    void Room::createRecvTransport(std::string body) {
        nlohmann::json json = nlohmann::json::parse(body);
        this->recvTransport = this->device->CreateRecvTransport(
            this->recvListener,
            json["transportId"],
            json["iceCandidates"],
            json["iceParameters"],
            json["dtlsParameters"],
            json["sctpParameters"]
            // TODO：全局options
        );
    }

    void Room::produceMedia(webrtc::MediaStreamInterface mediaStream) {
//        this->device->CanProduce();
    }

    void Room::close() {
        delete this->device;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeEnter(
        JNIEnv* env, jobject me,
        jlong nativeRoomPointer, jstring jRtpCapabilities,
        jlong factoryPointer, jobject jRtcConfiguration
    ) {
        Room* room = (Room*) nativeRoomPointer;
        webrtc::PeerConnectionInterface::RTCConfiguration rtcConfiguration(webrtc::PeerConnectionInterface::RTCConfigurationType::kAggressive);
        // TODO：为什么不能转换？测试是否因为stun配置问题
        webrtc::JavaParamRef<jobject> jRtcConfigurationRef(jRtcConfiguration);
//      webrtc::jni::JavaToNativeMediaConstraints()
        webrtc::jni::JavaToNativeRTCConfiguration(env, jRtcConfigurationRef, &rtcConfiguration);
        const char* rtpCapabilities = env->GetStringUTFChars(jRtpCapabilities, 0);
        room->enter(
            rtpCapabilities,
            reinterpret_cast<webrtc::PeerConnectionFactoryInterface*>(factoryPointer),
//          (webrtc::PeerConnectionFactoryInterface*) factoryPointer,
            rtcConfiguration
        );
        env->ReleaseStringUTFChars(jRtpCapabilities, rtpCapabilities);
        env->DeleteLocalRef(jRtpCapabilities);
        env->DeleteLocalRef(jRtcConfiguration);
//      delete rtpCapabilities;
    }

    extern "C" JNIEXPORT jlong JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeNewRoom(
        JNIEnv *env, jobject me,
        jstring jRoomId, jobject jRouterCallback
    ) {
        const char* roomId = env->GetStringUTFChars(jRoomId, 0);
        jobject routerCallback = env->NewGlobalRef(jRouterCallback);
        Room* room = new Room(roomId, env, routerCallback);
        env->ReleaseStringUTFChars(jRoomId, roomId);
        return (jlong) room;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCloseRoom(JNIEnv *env, jobject me, jlong nativeRoomPointer) {
        Room* room = (Room*) nativeRoomPointer;
        room->close();
        delete room;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCreateSendTransport(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jBody) {
        Room* room = (Room*) nativeRoomPointer;
        const char* body = env->GetStringUTFChars(jBody, 0);
        room->createSendTransport(body);
        env->ReleaseStringUTFChars(jBody, body);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCreateRecvTransport(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jBody) {
        Room* room = (Room*) nativeRoomPointer;
        const char* body = env->GetStringUTFChars(jBody, 0);
        room->createRecvTransport(body);
        env->ReleaseStringUTFChars(jBody, body);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeProduceMedia(JNIEnv *env, jobject me, jlong nativeRoomPointer, jlong mediaStreamPointer) {
        Room* room = (Room*) nativeRoomPointer;
        webrtc::MediaStreamInterface* mediaStream = reinterpret_cast<webrtc::MediaStreamInterface*>(mediaStreamPointer);
    }

}
