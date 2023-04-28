#include "Room.hpp"

namespace acgist {

    class SendListener : public mediasoupclient::SendTransport::Listener {

    public:
        Room* room;

    public:
        explicit SendListener(Room* room) {
            this->room = room;
        }
        virtual ~SendListener() {
        }

    public:
        std::future<void> OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) override {
            const std::string cTransportId    = transport->GetId();
            const std::string cDtlsParameters = dtlsParameters.dump();
            JNIEnv* env = nullptr;
            if(taoyaoJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) == JNI_EDETACHED) {
                bindJavaThread(&env);
                this->room->sendTransportConnectCallback(env, cTransportId, cDtlsParameters);
                unbindJavaThread();
            } else {
                this->room->sendTransportConnectCallback(env, cTransportId, cDtlsParameters);
            }
            std::promise <void> promise;
            promise.set_value();
            return promise.get_future();
        }

        void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override {
        }

        std::future<std::string> OnProduce(mediasoupclient::SendTransport* transport, const std::string& kind, nlohmann::json rtpParameters, const nlohmann::json& appData) override {
            std::string result;
            const std::string cTransportId   = transport->GetId();
            const std::string cRtpParameters = rtpParameters.dump();
            JNIEnv* env = nullptr;
            if(taoyaoJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) == JNI_EDETACHED) {
                bindJavaThread(&env);
                result = this->room->sendTransportProduceCallback(env, kind, cTransportId, cRtpParameters);
                unbindJavaThread();
            } else {
                result = this->room->sendTransportProduceCallback(env, kind, cTransportId, cRtpParameters);
            }
            std::promise <std::string> promise;
            promise.set_value(result);
            return promise.get_future();
        }

        std::future<std::string> OnProduceData(mediasoupclient::SendTransport* transport, const nlohmann::json& sctpStreamParameters, const std::string& label, const std::string& protocol, const nlohmann::json& appData) override {
            std::promise <std::string> promise;
            // TODO：如果需要自行实现
            return promise.get_future();
        }

    };

    class RecvListener : public mediasoupclient::RecvTransport::Listener {

    public:
        Room* room;

    public:
        explicit RecvListener(Room* room) {
            this->room = room;
        }
        virtual ~RecvListener() {
        }

        std::future<void> OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) override {
            const std::string cTransportId    = transport->GetId();
            const std::string cDtlsParameters = dtlsParameters.dump();
            JNIEnv* env = nullptr;
            if(taoyaoJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) == JNI_EDETACHED) {
                bindJavaThread(&env);
                this->room->recvTransportConnectCallback(env, cTransportId, cDtlsParameters);
                unbindJavaThread();
            } else {
                this->room->recvTransportConnectCallback(env, cTransportId, cDtlsParameters);
            }
            std::promise <void> promise;
            promise.set_value();
            return promise.get_future();
        }

        void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override {
        }

    };

    class ProducerListener : public mediasoupclient::Producer::Listener {

    public:
        Room* room;

    public:
        explicit ProducerListener(Room* room) {
            this->room = room;
        }
        virtual ~ProducerListener() {
        }

        void OnTransportClose(mediasoupclient::Producer* producer) override {
            producer->Close();
            JNIEnv* env = nullptr;
            if(taoyaoJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) == JNI_EDETACHED) {
                bindJavaThread(&env);
                this->room->producerCloseCallback(env, producer->GetId());
                unbindJavaThread();
            } else {
                this->room->producerCloseCallback(env, producer->GetId());
            }
        }

    };

    class ConsumerListener : public mediasoupclient::Consumer::Listener {

    public:
        Room* room;

    public:
        explicit ConsumerListener(Room* room) {
            this->room = room;
        }
        virtual ~ConsumerListener() {
//          mediasoupclient::Consumer::Listener::~Listener();
        }

        void OnTransportClose(mediasoupclient::Consumer* consumer) override {
            consumer->Close();
            JNIEnv* env = nullptr;
            if(taoyaoJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) == JNI_EDETACHED) {
                bindJavaThread(&env);
                this->room->consumerCloseCallback(env, consumer->GetId());
                unbindJavaThread();
            } else {
                this->room->consumerCloseCallback(env, consumer->GetId());
            }
        }

    };

    Room::Room(
        const std::string& roomId,
        const jobject& routerCallback
    ) : RouterCallback(routerCallback) {
        this->roomId           = roomId;
        this->factory          = nullptr;
        this->rtcConfiguration = nullptr;
        this->device           = new mediasoupclient::Device();
        this->sendTransport    = nullptr;
        this->recvTransport    = nullptr;
        this->sendListener     = new SendListener(this);
        this->recvListener     = new RecvListener(this);
        this->audioProducer    = nullptr;
        this->videoProducer    = nullptr;
        this->producerListener = new ProducerListener(this);
        this->consumerListener = new ConsumerListener(this);
    }

    Room::~Room() {
        delete this->rtcConfiguration;
        delete this->device;
        delete this->sendListener;
        delete this->sendTransport;
        delete this->recvListener;
        delete this->recvTransport;
        delete this->audioProducer;
        delete this->videoProducer;
        delete this->producerListener;
        delete this->consumerListener;
    }

    void Room::enterRoom(
        JNIEnv* env,
        const std::string& rtpCapabilities,
        webrtc::PeerConnectionFactoryInterface* factory,
        webrtc::PeerConnectionInterface::RTCConfiguration& rtcConfiguration
    ) {
        this->factory          = factory;
        this->rtcConfiguration = new webrtc::PeerConnectionInterface::RTCConfiguration(rtcConfiguration);
        mediasoupclient::PeerConnection::Options options;
        options.config  = rtcConfiguration;
        options.factory = factory;
        nlohmann::json json = nlohmann::json::parse(rtpCapabilities);
        this->device->Load(json, &options);
        const std::string cRtpCapabilities  = this->device->GetRtpCapabilities().dump();
        const std::string cSctpCapabilities = this->device->GetSctpCapabilities().dump();
        this->enterRoomCallback(env, cRtpCapabilities, cSctpCapabilities);
    }

    void Room::createSendTransport(JNIEnv* env, const std::string& body) {
        nlohmann::json json = nlohmann::json::parse(body);
        mediasoupclient::PeerConnection::Options options;
        options.config  = *this->rtcConfiguration;
        options.factory = this->factory;
        this->sendTransport = this->device->CreateSendTransport(
            this->sendListener,
            json["transportId"],
            json["iceParameters"],
            json["iceCandidates"],
            json["dtlsParameters"],
            json["sctpParameters"],
            &options
        );
    }

    void Room::createRecvTransport(JNIEnv* env, const std::string& body) {
        nlohmann::json json = nlohmann::json::parse(body);
        mediasoupclient::PeerConnection::Options options;
        options.config  = *this->rtcConfiguration;
        options.factory = this->factory;
        this->recvTransport = this->device->CreateRecvTransport(
            this->recvListener,
            json["transportId"],
            json["iceParameters"],
            json["iceCandidates"],
            json["dtlsParameters"],
            json["sctpParameters"],
            &options
        );
    }

    void Room::mediaProduceAudio(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream) {
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
        this->producerNewCallback(env, this->audioProducer->GetKind(), this->audioProducer->GetId(), this->audioProducer, this->audioProducer->GetTrack());
    }

    void Room::mediaProduceVideo(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream) {
        if(!this->device->CanProduce("video")) {
            return;
        }
        // TODO：配置读取
        nlohmann::json codecOptions =
            {
                { "videoGoogleStartBitrate", 400  },
                { "videoGoogleMinBitrate",   800  },
                { "videoGoogleMaxBitrate",   1600 }
            };
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
//      强制设置编码器
//      nlohmann::json codec = this->device->GetRtpCapabilities()["codec"];
        this->videoProducer = this->sendTransport->Produce(
            this->producerListener,
            mediaStream->GetVideoTracks()[0],
            nullptr,
            &codecOptions,
            nullptr
        );
        this->producerNewCallback(env, this->videoProducer->GetKind(), this->videoProducer->GetId(), this->videoProducer, this->videoProducer->GetTrack());
    }

    void Room::mediaConsume(JNIEnv* env, const std::string& message) {
        nlohmann::json json = nlohmann::json::parse(message);
        nlohmann::json body = json["body"];
        mediasoupclient::Consumer* consumer = this->recvTransport->Consume(
            this->consumerListener,
            body["consumerId"],
            body["producerId"],
            body["kind"],
            &body["rtpParameters"]
        );
        this->consumers.insert({ consumer->GetId(), consumer });
        webrtc::MediaStreamTrackInterface* trackPointer = consumer->GetTrack();
        this->consumerNewCallback(env, message, consumer, trackPointer);
    };

    void Room::mediaProducerPause(JNIEnv* env, const std::string& producerId) {
        if(this->audioProducer->GetId() == producerId) {
            this->audioProducer->Pause();
        } else if(this->videoProducer->GetId() == producerId) {
            this->videoProducer->Pause();
        } else {
        }
        this->producerPauseCallback(env, producerId);
    }

    void Room::mediaProducerResume(JNIEnv* env, const std::string& producerId) {
        if(this->audioProducer->GetId() == producerId) {
            this->audioProducer->Resume();
        } else if(this->videoProducer->GetId() == producerId) {
            this->videoProducer->Resume();
        } else {
        }
        this->producerResumeCallback(env, producerId);
    }

    void Room::mediaProducerClose(JNIEnv* env, const std::string& producerId) {
        if(this->audioProducer->GetId() == producerId) {
            this->audioProducer->Close();
        } else if(this->videoProducer->GetId() == producerId) {
            this->videoProducer->Close();
        } else {
        }
        this->producerCloseCallback(env, producerId);
    }

    void Room::mediaConsumerPause(JNIEnv* env, const std::string& consumerId) {
        mediasoupclient::Consumer* consumer = this->consumers[consumerId];
        if(consumer == nullptr) {
            return;
        }
        consumer->Pause();
        this->consumerPauseCallback(env, consumerId);
    }

    void Room::mediaConsumerResume(JNIEnv* env, const std::string& consumerId) {
        mediasoupclient::Consumer* consumer = this->consumers[consumerId];
        if(consumer == nullptr) {
            return;
        }
        consumer->Resume();
        this->consumerResumeCallback(env, consumerId);
    }

    void Room::mediaConsumerClose(JNIEnv* env, const std::string& consumerId) {
        mediasoupclient::Consumer* consumer = this->consumers[consumerId];
        if(consumer == nullptr) {
            return;
        }
        consumer->Close();
        this->consumerCloseCallback(env, consumerId);
    }

    void Room::closeRoom(JNIEnv* env) {
        std::map<std::string, mediasoupclient::Consumer*>::iterator iterator;
        for (iterator = this->consumers.begin(); iterator != this->consumers.end(); iterator++) {
            if(iterator->second == nullptr) {
                continue;
            }
            iterator->second->Close();
            delete iterator->second;
        }
        this->consumers.clear();
        if(this->audioProducer != nullptr) {
            this->audioProducer->Close();
        }
        if(this->videoProducer != nullptr) {
            this->videoProducer->Close();
        }
        if(this->sendTransport != nullptr) {
            this->sendTransport->Close();
        }
        if(this->recvTransport != nullptr) {
            this->recvTransport->Close();
        }
        this->closeRoomCallback(env);
    }

    extern "C" JNIEXPORT jlong JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeNewRoom(
        JNIEnv* env, jobject me,
        jstring jRoomId, jobject jRouterCallback
    ) {
        jobject routerCallback = env->NewGlobalRef(jRouterCallback);
        const char* roomId = env->GetStringUTFChars(jRoomId, nullptr);
        Room* room = new Room(roomId, routerCallback);
        env->DeleteLocalRef(jRoomId);
        env->ReleaseStringUTFChars(jRoomId, roomId);
        return (jlong) room;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeEnterRoom(
        JNIEnv* env, jobject me,
        jlong nativeRoomPointer, jstring jRtpCapabilities,
        jlong factoryPointer, jobject jRtcConfiguration
    ) {
        Room* room = (Room*) nativeRoomPointer;
//      webrtc::PeerConnectionInterface::RTCConfiguration rtcConfiguration(webrtc::PeerConnectionInterface::RTCConfigurationType::kSafe);
        webrtc::PeerConnectionInterface::RTCConfiguration rtcConfiguration(webrtc::PeerConnectionInterface::RTCConfigurationType::kAggressive);
        webrtc::JavaParamRef<jobject> jRtcConfigurationRef(env, jRtcConfiguration);
        webrtc::jni::JavaToNativeRTCConfiguration(env, jRtcConfigurationRef, &rtcConfiguration);
        const char* rtpCapabilities = env->GetStringUTFChars(jRtpCapabilities, nullptr);
        room->enterRoom(
            env,
            rtpCapabilities,
            (webrtc::PeerConnectionFactoryInterface*) factoryPointer,
            rtcConfiguration
        );
        env->DeleteLocalRef(jRtpCapabilities);
        env->ReleaseStringUTFChars(jRtpCapabilities, rtpCapabilities);
        env->DeleteLocalRef(jRtcConfiguration);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCloseRoom(JNIEnv* env, jobject me, jlong nativeRoomPointer) {
        Room* room = (Room*) nativeRoomPointer;
        room->closeRoom(env);
        delete room;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCreateSendTransport(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jBody) {
        Room* room = (Room*) nativeRoomPointer;
        const char* body = env->GetStringUTFChars(jBody, nullptr);
        room->createSendTransport(env, body);
        env->DeleteLocalRef(jBody);
        env->ReleaseStringUTFChars(jBody, body);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCreateRecvTransport(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jBody) {
        Room* room = (Room*) nativeRoomPointer;
        const char* body = env->GetStringUTFChars(jBody, nullptr);
        room->createRecvTransport(env, body);
        env->DeleteLocalRef(jBody);
        env->ReleaseStringUTFChars(jBody, body);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProduceAudio(JNIEnv* env, jobject me, jlong nativeRoomPointer, jlong mediaStreamPointer) {
        Room* room = (Room*) nativeRoomPointer;
        room->mediaProduceAudio(env, (webrtc::MediaStreamInterface*) mediaStreamPointer);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProduceVideo(JNIEnv* env, jobject me, jlong nativeRoomPointer, jlong mediaStreamPointer) {
        Room* room = (Room*) nativeRoomPointer;
        room->mediaProduceVideo(env, (webrtc::MediaStreamInterface*) mediaStreamPointer);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsume(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jMessage) {
        Room* room = (Room*) nativeRoomPointer;
        const char* message = env->GetStringUTFChars(jMessage, nullptr);
        room->mediaConsume(env, message);
        env->DeleteLocalRef(jMessage);
        env->ReleaseStringUTFChars(jMessage, message);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerPause(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char* producerId = env->GetStringUTFChars(jProducerId, nullptr);
        room->mediaProducerPause(env, producerId);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerResume(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char* producerId = env->GetStringUTFChars(jProducerId, nullptr);
        room->mediaProducerResume(env, producerId);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerClose(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char* producerId = env->GetStringUTFChars(jProducerId, nullptr);
        room->mediaProducerClose(env, producerId);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerPause(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char* consumerId = env->GetStringUTFChars(jConsumerId, nullptr);
        room->mediaConsumerPause(env, consumerId);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerResume(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char* consumerId = env->GetStringUTFChars(jConsumerId, nullptr);
        room->mediaConsumerResume(env, consumerId);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerClose(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        const char* consumerId = env->GetStringUTFChars(jConsumerId, nullptr);
        room->mediaConsumerClose(env, consumerId);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

}
