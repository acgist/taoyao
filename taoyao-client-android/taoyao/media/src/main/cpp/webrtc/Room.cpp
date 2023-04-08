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
            jstring jTransportId = this->env->NewStringUTF(cTransportId);
            const char *cDtlsParameters = dtlsParameters.dump().data();
            jstring jDtlsParameters = this->env->NewStringUTF(cDtlsParameters);
            this->env->CallVoidMethod(
                this->routerCallback,
                sendTransportConnectCallback,
                jTransportId,
                jDtlsParameters
            );
            this->env->DeleteLocalRef(jTransportId);
            this->env->ReleaseStringUTFChars(jTransportId, cTransportId);
            this->env->DeleteLocalRef(jDtlsParameters);
            this->env->ReleaseStringUTFChars(jDtlsParameters, cDtlsParameters);
            this->env->DeleteLocalRef(jCallbackClazz);
            return std::future<void>();
        }

        void OnConnectionStateChange(mediasoupclient::Transport *transport, const std::string &connectionState) override {
            // 状态变化
        }

        std::future<std::string> OnProduce(mediasoupclient::SendTransport *transport, const std::string &kind, nlohmann::json rtpParameters, const nlohmann::json &appData) override {
            jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
            jmethodID sendTransportProduceCallback = this->env->GetMethodID(jCallbackClazz, "sendTransportProduceCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
            const char *cKind = kind.data();
            jstring jKind = this-> env->NewStringUTF(cKind);
            const char *cTransportId = transport->GetId().data();
            jstring jTransportId = this-> env->NewStringUTF(cTransportId);
            const char *cRtpParameters = rtpParameters.dump().data();
            jstring jRtpParameters = this-> env->NewStringUTF(cRtpParameters);
            std::promise<std::string> promise;
            jstring jResult = (jstring) this->env->CallObjectMethod(
                this->routerCallback,
                sendTransportProduceCallback,
                jKind,
                jTransportId,
                jRtpParameters
            );
            const char *cResult = this-> env->GetStringUTFChars(jResult, 0);
            std::string result(cResult);
            promise.set_value(result);
            this-> env->DeleteLocalRef(jResult);
            this-> env->DeleteLocalRef(jKind);
            this-> env->ReleaseStringUTFChars(jKind, cKind);
            this-> env->DeleteLocalRef(jResult);
            this-> env->ReleaseStringUTFChars(jResult, cResult);
            this-> env->DeleteLocalRef(jTransportId);
            this-> env->ReleaseStringUTFChars(jTransportId, cTransportId);
            this-> env->DeleteLocalRef(jRtpParameters);
            this-> env->ReleaseStringUTFChars(jRtpParameters, cRtpParameters);
            this-> env->DeleteLocalRef(jCallbackClazz);
            return promise.get_future();
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
            jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
            jmethodID recvTransportConnectCallback = this->env->GetMethodID(jCallbackClazz, "recvTransportConnectCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
            const char *cTransportId = transport->GetId().data();
            jstring jTransportId = this-> env->NewStringUTF(cTransportId);
            const char *cDtlsParameters = dtlsParameters.dump().data();
            jstring jDtlsParameters = this-> env->NewStringUTF(cDtlsParameters);
            this->env->CallVoidMethod(
                this->routerCallback,
                recvTransportConnectCallback,
                jTransportId,
                jDtlsParameters
            );
            this-> env->DeleteLocalRef(jTransportId);
            this-> env->ReleaseStringUTFChars(jTransportId, cTransportId);
            this-> env->DeleteLocalRef(jDtlsParameters);
            this-> env->ReleaseStringUTFChars(jDtlsParameters, cDtlsParameters);
            this-> env->DeleteLocalRef(jCallbackClazz);
            return std::future<void>();
        }

        void OnConnectionStateChange(mediasoupclient::Transport *transport, const std::string &connectionState) override {
            // 状态变化
        }

    };

    class ProducerListener : public mediasoupclient::Producer::Listener {

    public:
        Room *room;
        JNIEnv *env;
        jobject routerCallback;

    public:
        ProducerListener(Room *room, JNIEnv *env, jobject routerCallback) {
            this->room = room;
            this->env = env;
            this->routerCallback = routerCallback;
        }

        void OnTransportClose(mediasoupclient::Producer *producer) override {

        }

    };

    class ConsumerListener : public mediasoupclient::Consumer::Listener {

    public:
        Room *room;
        JNIEnv *env;
        jobject routerCallback;

    public:
        ConsumerListener(Room *room, JNIEnv *env, jobject routerCallback) {
            this->room = room;
            this->env = env;
            this->routerCallback = routerCallback;
        }

        void OnTransportClose(mediasoupclient::Consumer *consumer) override {

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
        this->producerListener = new ProducerListener(this, env, routerCallback);
        this->consumerListener = new ConsumerListener(this, env, routerCallback);
    }

    Room::~Room() {
        delete this->device;
        delete this->sendListener;
        delete this->sendTransport;
        delete this->recvListener;
        delete this->recvTransport;
        delete this->audioProducer;
        delete this->videoProducer;
        delete this->producerListener;
        delete this->consumerListener;
        this-> env->DeleteLocalRef(this->routerCallback);
        this-> env->DeleteGlobalRef(this->routerCallback);
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

    void Room::mediaProduceAudio(webrtc::MediaStreamInterface *mediaStream) {
        if(!this->device->CanProduce("audio")) {
            return;
        }
        nlohmann::json codecOptions =
            {
                { "opusStereo", true },
                { "opusDtx",    true }
            };
        this->audioProducer = this->sendTransport->Produce(
            this->producerListener,
            mediaStream->GetAudioTracks()[0],
            nullptr,
            &codecOptions,
            nullptr
        );
    }

    void Room::mediaProduceVideo(webrtc::MediaStreamInterface *mediaStream) {
        if(this->device->CanProduce("video")) {
            return;
        }
        // TODO：配置读取
        nlohmann::json codecOptions =
            {
                { "videoGoogleStartBitrate", 400  },
                { "videoGoogleMinBitrate",   800  },
                { "videoGoogleMaxBitrate",   1600 }
            };
        // 设置动态码率，帧率、分辨率在摄像头初始化处设置。
//      如果需要使用`Simulcast`打开下面配置
//      std::vector<webrtc::RtpEncodingParameters> encodings;
//      webrtc::RtpEncodingParameters min;
//      webrtc::RtpEncodingParameters mid;
//      webrtc::RtpEncodingParameters max;
//      min.active = true;
//      min.max_framerate   = 15;
//      min.min_bitrate_bps = 400;
//      min.max_bitrate_bps = 800;
//      encodings.emplace_back(min);
//      encodings.emplace_back(mid);
//      encodings.emplace_back(max);
        // 强制设置编码器
//      nlohmann::json codec = this->device->GetRtpCapabilities()["codec"];
        this->videoProducer = this->sendTransport->Produce(
            this->producerListener,
            mediaStream->GetVideoTracks()[0],
            nullptr,
            &codecOptions,
            nullptr
        );
    }

    void Room::mediaConsume(std::string message) {
        nlohmann::json json = nlohmann::json::parse(message);
        nlohmann::json body = json["body"];
        mediasoupclient::Consumer *consumer = this->recvTransport->Consume(
            this->consumerListener,
            body["consumerId"],
            body["producerId"],
            body["kind"],
            &body["rtpParameters"]
        );
        this->consumers.insert({ consumer->GetId(), consumer });
        webrtc::MediaStreamTrackInterface* trackPointer = consumer->GetTrack();
        jclass jCallbackClazz = this->env->GetObjectClass(this->routerCallback);
        jmethodID consumerNewCallback = this->env->GetMethodID(jCallbackClazz, "consumerNewCallback", "(Ljava/lang/String;J;)V");
        const char *cMessage = message.data();
        jstring jMessage = this-> env->NewStringUTF(cMessage);
        this->env->CallVoidMethod(
            this->routerCallback,
            consumerNewCallback,
            jMessage,
            (jlong) trackPointer
        );
        this-> env->DeleteLocalRef(jMessage);
        this-> env->ReleaseStringUTFChars(jMessage, cMessage);
        this-> env->DeleteLocalRef(jCallbackClazz);
    };

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
        env->DeleteLocalRef(jBody);
        env->ReleaseStringUTFChars(jBody, body);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProduceAudio(JNIEnv *env, jobject me, jlong nativeRoomPointer, jlong mediaStreamPointer) {
        Room* room = (Room*) nativeRoomPointer;
        webrtc::MediaStreamInterface *mediaStream = reinterpret_cast<webrtc::MediaStreamInterface*>(mediaStreamPointer);
        room->mediaProduceAudio(mediaStream);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProduceVideo(JNIEnv *env, jobject me, jlong nativeRoomPointer, jlong mediaStreamPointer) {
        Room* room = (Room*) nativeRoomPointer;
        webrtc::MediaStreamInterface *mediaStream = reinterpret_cast<webrtc::MediaStreamInterface*>(mediaStreamPointer);
        room->mediaProduceVideo(mediaStream);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsume(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jMessage) {
        Room* room = (Room*) nativeRoomPointer;
        const char *message = env->GetStringUTFChars(jMessage, 0);
        room->mediaConsume(message);
        env->DeleteLocalRef(jMessage);
        env->ReleaseStringUTFChars(jMessage, message);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerPause(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char *producerId = env->GetStringUTFChars(jProducerId, 0);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerResume(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char *producerId = env->GetStringUTFChars(jProducerId, 0);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerClose(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char *producerId = env->GetStringUTFChars(jProducerId, 0);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerPause(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char *consumerId = env->GetStringUTFChars(jConsumerId, 0);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerResume(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char *consumerId = env->GetStringUTFChars(jConsumerId, 0);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerClose(JNIEnv *env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char *consumerId = env->GetStringUTFChars(jConsumerId, 0);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

}
