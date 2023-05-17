#include "MediaManager.hpp"

namespace acgist {

    JavaVM* taoyaoJavaVM = nullptr;

    void bindJavaThread(JNIEnv** env, const char* name) {
        JavaVMAttachArgs args;
        args.name    = name;
        args.version = JNI_VERSION_1_6;
        if(taoyaoJavaVM == nullptr) {
            LOG_W("绑定线程失败：JavaVM为空");
            return;
        }
        taoyaoJavaVM->AttachCurrentThreadAsDaemon(env, &args);
    }

    void unbindJavaThread() {
        if(taoyaoJavaVM == nullptr) {
            LOG_W("解绑线程失败：JavaVM为空");
            return;
        }
        taoyaoJavaVM->DetachCurrentThread();
    }

    /**
     * 加载库文件时加载WebRTC，JNI自动调用。
     */
    extern "C" JNIEXPORT jint JNICALL
    JNI_OnLoad(JavaVM* javaVM, void* reserved) {
        LOG_I("加载WebRTC");
        taoyaoJavaVM = javaVM;
//      JNIEnv* env = webrtc::jni::GetEnv();
        // 下面非常重要
        webrtc::jni::InitGlobalJniVariables(javaVM);
        return JNI_VERSION_1_6;
    }

    extern "C" JNIEXPORT void JNICALL
    Java_com_acgist_taoyao_media_MediaManager_nativeInit(JNIEnv* env, jobject me) {
        std::string version = mediasoupclient::Version();
        LOG_I("加载MediasoupClient：%s", version.data());
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
