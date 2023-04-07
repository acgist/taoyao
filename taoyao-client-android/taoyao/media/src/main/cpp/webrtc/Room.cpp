#include "Room.hpp"

namespace acgist {

    class SendListener : public mediasoupclient::SendTransport::Listener {

        std::future<void> OnConnect(mediasoupclient::Transport *transport, const nlohmann::json &dtlsParameters) override {
            return std::future<void>();
        }

        void OnConnectionStateChange(mediasoupclient::Transport *transport, const std::string &connectionState) override {
        }

        std::future<std::string> OnProduce(mediasoupclient::SendTransport *transport, const std::string &kind, nlohmann::json rtpParameters, const nlohmann::json &appData) override {
            return std::future<std::string>();
        }

        std::future<std::string> OnProduceData(mediasoupclient::SendTransport *transport, const nlohmann::json &sctpStreamParameters, const std::string &label, const std::string &protocol, const nlohmann::json &appData) override {
            return std::future<std::string>();
        }

    };

    class RecvListener : public mediasoupclient::RecvTransport::Listener {

        std::future<void> OnConnect(mediasoupclient::Transport *transport, const nlohmann::json &dtlsParameters) override {
            return std::future<void>();
        }

        void OnConnectionStateChange(mediasoupclient::Transport *transport, const std::string &connectionState) override {
        }

    };

    Room::Room(jstring roomId) {
        this->roomId = roomId;
        this->device = new mediasoupclient::Device();
        this->sendListener = new SendListener();
        this->recvListener = new RecvListener();
    }

    Room::~Room() {
        delete this->device;
        delete this->sendListener;
        delete this->sendTransport;
        delete this->recvListener;
        delete this->recvTransport;
    }

    void Room::load(
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
        this->device = nullptr;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeLoad(
        JNIEnv* env, jobject me,
        jlong nativeRoomPointer, jstring jRtpCapabilities,
        jlong factoryPointer, jobject jRtcConfiguration
    ) {
        Room* room = (Room*) nativeRoomPointer;
        webrtc::PeerConnectionInterface::RTCConfiguration rtcConfiguration(webrtc::PeerConnectionInterface::RTCConfigurationType::kAggressive);
        // TODO：为什么不能转换？测试是否因为stun配置问题
        jobject jRtcConfigurationGlobal = webrtc::jni::NewGlobalRef(env, jRtcConfiguration);
        webrtc::JavaParamRef<jobject> jRtcConfigurationRef(jRtcConfigurationGlobal);
//      webrtc::jni::JavaToNativeMediaConstraints()
        webrtc::jni::JavaToNativeRTCConfiguration(env, jRtcConfigurationRef, &rtcConfiguration);
        webrtc::jni::DeleteGlobalRef(env, jRtcConfigurationGlobal);
        const char* rtpCapabilities = env->GetStringUTFChars(jRtpCapabilities, 0);
        room->load(
            rtpCapabilities,
            reinterpret_cast<webrtc::PeerConnectionFactoryInterface*>(factoryPointer),
//          (webrtc::PeerConnectionFactoryInterface*) factoryPointer,
            rtcConfiguration
        );
        env->ReleaseStringUTFChars(jRtpCapabilities, rtpCapabilities);
//      delete rtpCapabilities;
    }

    extern "C" JNIEXPORT jlong JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeNewRoom(JNIEnv *env, jobject me, jstring roomId) {
        const Room* room = new Room(roomId);
        return (jlong) room;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeCloseRoom(JNIEnv *env, jobject me, jlong nativeRoomPointer) {
//        JNIEXPORT void JNICALL
//        Java_nativeMethod
//            (JNIEnv *env, jobject thiz) {
//            MyCPlusObj *obj = new MyCPlusObj();
//            jclass clazz = (jclass)(*env).GetObjectClass(thiz);
//            jfieldID fid = (jfieldID)(*env).GetFieldID(clazz, "mObj", "I");
//            (*env).SetIntField(thiz, fid, (jint)obj);
//        }

//        jclass objClazz = (jclass)env->GetObjectClass(obj);//obj为对应的JAVA对象
//        jfieldID fid = env->GetFieldID(objClazz, "mObj", "I");
//        jlong p = (jlong)env->GetObjectField(obj, fid);
//        MyCPlusObj *cPlusObj = (MyCPlusObj *)p;
////cPlusObj 为JAVA对象对应的C++对象

//        jobject gThiz = (jobject)env->NewGlobalRef(thiz);//thiz为JAVA对象
//        (*obj).javaObj = (jint)gThiz;

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
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_client_Room_nativeProduceMedia(JNIEnv *env, jobject me, jlong nativeRoomPointer, jlong mediaStreamPointer) {
        Room* room = (Room*) nativeRoomPointer;
        webrtc::MediaStreamInterface* mediaStream = reinterpret_cast<webrtc::MediaStreamInterface*>(mediaStreamPointer);
    }

}
