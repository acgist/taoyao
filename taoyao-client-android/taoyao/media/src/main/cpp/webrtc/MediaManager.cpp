#include "Room.hpp"
#include "MediaManager.hpp"

namespace acgist {

extern "C" JNIEXPORT void JNICALL Java_com_acgist_taoyao_media_MediaManager_nativeInit(JNIEnv * env, jobject me) {
    LOG_I("加载MediasoupClient：", mediasoupclient::Version().data());
    mediasoupclient::Initialize();
    // => { spatialLayers: 2, temporalLayers: 3 }
//  mediasoupclient::parseScalabilityMode("L2T3");
    // => { spatialLayers: 4, temporalLayers: 7 }
//  mediasoupclient::parseScalabilityMode("L4T7_KEY_SHIFT");
}

extern "C" JNIEXPORT void JNICALL Java_com_acgist_taoyao_media_MediaManager_nativeStop(JNIEnv * env, jobject me) {
    std::cout << "释放mediasoupclient" << std::endl;
    mediasoupclient::Cleanup();
}

extern "C" JNIEXPORT jlong JNICALL Java_com_acgist_taoyao_media_MediaManager_nativeNewRoom(JNIEnv * env, jobject me, jstring roomId) {
    const Room* room = new Room(roomId);
    return (jlong) room;
}

}
