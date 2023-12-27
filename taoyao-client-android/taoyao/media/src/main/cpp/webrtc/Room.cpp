#include "Room.hpp"

namespace acgist {

    /**
     * 发送通道监听器
     */
    class SendListener : public mediasoupclient::SendTransport::Listener {

    public:
        /**
         * 房间指针
         */
        Room* room;

    public:
        /**
         * 发送通道监听器
         *
         * @param room 房间指针
         */
        explicit SendListener(Room* room) {
            this->room = room;
        }
        /**
         * 析构函数
         */
        virtual ~SendListener() {
        }

    public:
        /**
         * 连接通道
         *
         * @param transport      通道指针
         * @param dtlsParameters DTLS参数
         *
         * @return future
         */
        std::future<void> OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) override {
            const std::string cTransportId    = transport->GetId();
            const std::string cDtlsParameters = dtlsParameters.dump();
            LOG_I("连接发送通道：%s - %s", this->room->roomId.data(), cTransportId.data());
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

        /**
         * 通道状态改变
         *
         * @param transport       通道指针
         * @param connectionState 当前状态
         */
        void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override {
            LOG_I("发送通道状态改变：%s - %s - %s", this->room->roomId.data(), transport->GetId().data(), connectionState.data());
        }

        /**
         * 通道生产媒体
         *
         * @param transport     通道指针
         * @param kind          媒体类型
         * @param rtpParameters RTP参数
         * @param appData       应用数据
         *
         * @return 生产者ID
         */
        std::future<std::string> OnProduce(mediasoupclient::SendTransport* transport, const std::string& kind, nlohmann::json rtpParameters, const nlohmann::json& appData) override {
            std::string result;
            const std::string cTransportId   = transport->GetId();
            const std::string cRtpParameters = rtpParameters.dump();
            LOG_I("生产媒体：%s - %s - %s", this->room->roomId.data(), cTransportId.data(), kind.data());
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

        /**
         * 通道生产数据
         * 注意：需要自己实现
         *
         * @param transport            通道指针
         * @param sctpStreamParameters SCTP参数
         * @param label                标记
         * @param protocol             协议
         * @param appData              应用数据
         *
         * @return 生产者ID
         */
        std::future<std::string> OnProduceData(mediasoupclient::SendTransport* transport, const nlohmann::json& sctpStreamParameters, const std::string& label, const std::string& protocol, const nlohmann::json& appData) override {
            const std::string cTransportId = transport->GetId();
            LOG_I("生产数据：%s - %s - %s -%s", this->room->roomId.data(), cTransportId.data(), label.data(), protocol.data());
            std::promise <std::string> promise;
            return promise.get_future();
        }

    };

    /**
     * 接收通道监听器
     */
    class RecvListener : public mediasoupclient::RecvTransport::Listener {

    public:
        /**
         * 房间指针
         */
        Room* room;

    public:
        /**
         * 接收通道监听器
         *
         * @param room 房间指针
         */
        explicit RecvListener(Room* room) {
            this->room = room;
        }
        /**
         * 析构函数
         */
        virtual ~RecvListener() {
        }

        /**
         * 连接通道
         *
         * @param transport      通道指针
         * @param dtlsParameters DTLS参数
         *
         * @return future
         */
        std::future<void> OnConnect(mediasoupclient::Transport* transport, const nlohmann::json& dtlsParameters) override {
            const std::string cTransportId    = transport->GetId();
            const std::string cDtlsParameters = dtlsParameters.dump();
            LOG_I("连接接收通道：%s - %s", this->room->roomId.data(), cTransportId.data());
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

        /**
         * 通道状态改变
         *
         * @param transport       通道指针
         * @param connectionState 通道状态
         */
        void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override {
            LOG_I("接收通道状态改变：%s - %s - %s", this->room->roomId.data(), transport->GetId().data(), connectionState.data());
        }

    };

    /**
     * 生产者监听器
     */
    class ProducerListener : public mediasoupclient::Producer::Listener {

    public:
        /**
         * 房间指针
         */
        Room* room;

    public:
        /**
         * 生产者监听器
         *
         * @param room 房间指针
         */
        explicit ProducerListener(Room* room) {
            this->room = room;
        }
        /**
         * 析构函数
         */
        virtual ~ProducerListener() {
        }

        /**
         * 通道关闭
         *
         * @param producer 生产者
         */
        void OnTransportClose(mediasoupclient::Producer* producer) override {
            LOG_I("生产者通道关闭：%s - %s", this->room->roomId.data(), producer->GetId().data());
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

    /**
     * 消费者监听器
     */
    class ConsumerListener : public mediasoupclient::Consumer::Listener {

    public:
        /**
         * 房间指针
         */
        Room* room;

    public:
        /**
         * 消费者监听器
         *
         * @param room 房间指针
         */
        explicit ConsumerListener(Room* room) {
            this->room = room;
        }
        /**
         * 析构函数
         */
        virtual ~ConsumerListener() {
        }

        /**
         * 通道关闭
         *
         * @param consumer 消费者
         */
        void OnTransportClose(mediasoupclient::Consumer* consumer) override {
            LOG_I("消费者通道关闭：%s - %s", this->room->roomId.data(), consumer->GetId().data());
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
        // TODO：解决析构函数不是虚函数的问题
        if(this->rtcConfiguration != nullptr) {
            delete this->rtcConfiguration;
            this->rtcConfiguration = nullptr;
        }
        if(this->device != nullptr) {
            delete this->device;
            this->device = nullptr;
        }
        if(this->sendListener != nullptr) {
            delete this->sendListener;
            this->sendListener = nullptr;
        }
        if(this->sendTransport != nullptr) {
            delete this->sendTransport;
            this->sendTransport = nullptr;
        }
        if(this->recvListener != nullptr) {
            delete this->recvListener;
            this->recvListener = nullptr;
        }
        if(this->recvTransport != nullptr) {
            delete this->recvTransport;
            this->recvTransport = nullptr;
        }
        if(this->audioProducer != nullptr) {
            delete this->audioProducer;
            this->audioProducer = nullptr;
        }
        if(this->videoProducer != nullptr) {
            delete this->videoProducer;
            this->videoProducer = nullptr;
        }
        if(this->producerListener != nullptr) {
            delete this->producerListener;
            this->producerListener = nullptr;
        }
        if(this->consumerListener != nullptr) {
            delete this->consumerListener;
            this->consumerListener = nullptr;
        }
    }

    void Room::enterRoom(
        JNIEnv* env,
        const std::string& rtpCapabilities,
        webrtc::PeerConnectionFactoryInterface* factory,
        webrtc::PeerConnectionInterface::RTCConfiguration& rtcConfiguration
    ) {
        if(this->device->IsLoaded()) {
            LOG_W("配置已经加载");
            return;
        }
        this->factory          = factory;
        this->rtcConfiguration = new webrtc::PeerConnectionInterface::RTCConfiguration(rtcConfiguration);
        mediasoupclient::PeerConnection::Options options;
        options.config      = rtcConfiguration;
        options.factory     = factory;
        nlohmann::json json = nlohmann::json::parse(rtpCapabilities);
        this->device->Load(json, &options);
        const std::string cRtpCapabilities  = this->device->GetRtpCapabilities().dump();
        const std::string cSctpCapabilities = this->device->GetSctpCapabilities().dump();
        this->enterRoomCallback(env, cRtpCapabilities, cSctpCapabilities);
    }

    void Room::createSendTransport(JNIEnv* env, const std::string& body) {
        nlohmann::json json = nlohmann::json::parse(body);
        mediasoupclient::PeerConnection::Options options;
        options.config      = *this->rtcConfiguration;
        options.factory     = this->factory;
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
        options.config      = *this->rtcConfiguration;
        options.factory     = this->factory;
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
            LOG_I("不能生产音频媒体：%s", this->roomId.data());
            return;
        }
        nlohmann::json codecOptions =
            {
                { "opusStereo", true },
                { "opusDtx",    true }
            };
        rtc::scoped_refptr<webrtc::AudioTrackInterface> track = mediaStream->GetAudioTracks()[0];
        if(track->state() == webrtc::MediaStreamTrackInterface::TrackState::kEnded) {
            LOG_W("音频媒体状态错误");
            return;
        }
        this->audioProducer = this->sendTransport->Produce(
            this->producerListener,
            track,
            nullptr,
            &codecOptions,
            nullptr
        );
        this->producerNewCallback(env, this->audioProducer->GetKind(), this->audioProducer->GetId(), this->audioProducer, this->audioProducer->GetTrack());
    }

    void Room::mediaProduceVideo(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream) {
        if(!this->device->CanProduce("video")) {
            LOG_I("不能生产视频媒体：%s", this->roomId.data());
            return;
        }
        // TODO：配置读取同时测试效果
        nlohmann::json codecOptions =
            {
                // x-google-start-bitrate
                { "videoGoogleStartBitrate", 400  },
                // x-google-min-bitrate
                { "videoGoogleMinBitrate",   800  },
                // x-google-max-bitrate
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
        rtc::scoped_refptr<webrtc::VideoTrackInterface> track = mediaStream->GetVideoTracks()[0];
        if(track->state() == webrtc::MediaStreamTrackInterface::TrackState::kEnded) {
            LOG_W("视频媒体状态错误");
            return;
        }
        this->videoProducer = this->sendTransport->Produce(
            this->producerListener,
            track,
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
            this->producerCloseCallback(env, producerId);
        } else if(this->videoProducer->GetId() == producerId) {
            this->videoProducer->Close();
            this->producerCloseCallback(env, producerId);
        } else {
        }
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
        this->consumers.erase(consumerId);
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
        const char* roomId     = env->GetStringUTFChars(jRoomId, nullptr);
        Room* room             = new Room(roomId, routerCallback);
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
        if(room == nullptr) {
            return;
        }
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
        if(room == nullptr) {
            return;
        }
        room->closeRoom(env);
        delete room;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCreateSendTransport(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jBody) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* body = env->GetStringUTFChars(jBody, nullptr);
        room->createSendTransport(env, body);
        env->DeleteLocalRef(jBody);
        env->ReleaseStringUTFChars(jBody, body);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCreateRecvTransport(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jBody) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* body = env->GetStringUTFChars(jBody, nullptr);
        room->createRecvTransport(env, body);
        env->DeleteLocalRef(jBody);
        env->ReleaseStringUTFChars(jBody, body);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProduceAudio(JNIEnv* env, jobject me, jlong nativeRoomPointer, jlong mediaStreamPointer) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        room->mediaProduceAudio(env, (webrtc::MediaStreamInterface*) mediaStreamPointer);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProduceVideo(JNIEnv* env, jobject me, jlong nativeRoomPointer, jlong mediaStreamPointer) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        room->mediaProduceVideo(env, (webrtc::MediaStreamInterface*) mediaStreamPointer);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsume(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jMessage) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* message = env->GetStringUTFChars(jMessage, nullptr);
        room->mediaConsume(env, message);
        env->DeleteLocalRef(jMessage);
        env->ReleaseStringUTFChars(jMessage, message);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerPause(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* producerId = env->GetStringUTFChars(jProducerId, nullptr);
        room->mediaProducerPause(env, producerId);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerResume(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* producerId = env->GetStringUTFChars(jProducerId, nullptr);
        room->mediaProducerResume(env, producerId);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaProducerClose(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jProducerId) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* producerId = env->GetStringUTFChars(jProducerId, nullptr);
        room->mediaProducerClose(env, producerId);
        env->DeleteLocalRef(jProducerId);
        env->ReleaseStringUTFChars(jProducerId, producerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerPause(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* consumerId = env->GetStringUTFChars(jConsumerId, nullptr);
        room->mediaConsumerPause(env, consumerId);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerResume(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* consumerId = env->GetStringUTFChars(jConsumerId, nullptr);
        room->mediaConsumerResume(env, consumerId);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeMediaConsumerClose(JNIEnv* env, jobject me, jlong nativeRoomPointer, jstring jConsumerId) {
        Room* room = (Room*) nativeRoomPointer;
        if(room == nullptr) {
            return;
        }
        const char* consumerId = env->GetStringUTFChars(jConsumerId, nullptr);
        room->mediaConsumerClose(env, consumerId);
        env->DeleteLocalRef(jConsumerId);
        env->ReleaseStringUTFChars(jConsumerId, consumerId);
    }

}
