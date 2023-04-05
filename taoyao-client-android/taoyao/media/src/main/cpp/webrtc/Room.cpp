#include "Room.hpp"

namespace acgist {

    Room::Room(jstring roomId) {
        this->roomId = roomId;
        this->device = new mediasoupclient::Device();
    }

    Room::~Room() {
    }

    void Room::load(std::string rtpCapabilities, webrtc::PeerConnectionFactoryInterface* factory, webrtc::PeerConnectionInterface::RTCConfiguration& configuration) {
        // TODO：PeerConnectionFactory复用
        nlohmann::json json;
        mediasoupclient::PeerConnection::Options options;
        options.config = configuration;
        options.factory = factory;
        json["routerRtpCapabilities"] = nlohmann::json::parse(rtpCapabilities);
        this->device->Load(json, &options);
    }

    void Room::close() {
        delete this->device;
        this->device = nullptr;
    }

    extern "C" JNIEXPORT void JNICALL Java_com_acgist_taoyao_media_Room_nativeLoad(
        JNIEnv* env, jobject me,
        jlong nativePointer, jstring rtpCapabilities,
        jlong factoryPointer, jobject configuration
    ) {
        Room *room = (Room *) nativePointer;
        webrtc::PeerConnectionInterface::RTCConfiguration rtcConfiguration(webrtc::PeerConnectionInterface::RTCConfigurationType::kAggressive);
        // TODO：为什么不能转换
        jobject configurationGlobal = webrtc::jni::NewGlobalRef(env, configuration);
        webrtc::JavaParamRef<jobject> configurationRef(configurationGlobal);
//      webrtc::jni::JavaToNativeMediaConstraints()
        webrtc::jni::JavaToNativeRTCConfiguration(env, webrtc::JavaParamRef(env, configurationGlobal), &rtcConfiguration);
        webrtc::jni::DeleteGlobalRef(env, configurationGlobal);
        const char* routerRtpCapabilities = env->GetStringUTFChars(rtpCapabilities, 0);
        room->load(
            routerRtpCapabilities,
            reinterpret_cast<webrtc::PeerConnectionFactoryInterface*>(factoryPointer),
//          (webrtc::PeerConnectionFactoryInterface*) factoryPointer,
            rtcConfiguration
        );
//      env->ReleaseStringUTFChars(rtpCapabilities, routerRtpCapabilities);
//      delete routerRtpCapabilities;
    }

    extern "C" JNIEXPORT void JNICALL Java_com_acgist_taoyao_media_Room_nativeNewClient(JNIEnv * env, jobject me) {

    }

    extern "C" JNIEXPORT void JNICALL Java_com_acgist_taoyao_media_Room_nativeCloseClient(JNIEnv * env, jobject me) {
    }

    extern "C" JNIEXPORT void JNICALL Java_com_acgist_taoyao_media_Room_nativeCloseRoom(JNIEnv * env, jobject me, jlong nativePointer) {
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

        Room* room = (Room*) nativePointer;
        room->close();
        delete room;
    }

}
