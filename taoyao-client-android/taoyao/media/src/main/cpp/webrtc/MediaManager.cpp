#include "MediaManager.hpp"

namespace acgist {

    JavaVM* taoyaoJavaVM = nullptr;

    void bindJavaThread(JNIEnv** env, const char* name) {
        JavaVMAttachArgs args;
        args.name    = name;
        args.version = JNI_VERSION_1_6;
        taoyaoJavaVM->AttachCurrentThreadAsDaemon(env, &args);
    }

    void unbindJavaThread() {
        taoyaoJavaVM->DetachCurrentThread();
    }

    /**
     * 非常重要
     */
    extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* javaVM, void* reserved) {
        taoyaoJavaVM = javaVM;
    //  JNIEnv* env = webrtc::jni::GetEnv();
        webrtc::jni::InitGlobalJniVariables(javaVM);
        return JNI_VERSION_1_6;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_MediaManager_nativeInit(JNIEnv* env, jobject me) {
        std::string version = mediasoupclient::Version();
        LOG_I("加载MediasoupClient", version.data());
        mediasoupclient::Initialize();
        // => { spatialLayers: 2, temporalLayers: 3 }
        // mediasoupclient::parseScalabilityMode("L2T3");
        // => { spatialLayers: 4, temporalLayers: 7 }
        // mediasoupclient::parseScalabilityMode("L4T7_KEY_SHIFT");
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_MediaManager_nativeStop(JNIEnv* env, jobject me) {
        LOG_I("释放mediasoupclient");
        mediasoupclient::Cleanup();
    }

}
