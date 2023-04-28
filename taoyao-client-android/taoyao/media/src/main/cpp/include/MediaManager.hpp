#pragma once

#include "jni.h"
#include "Log.hpp"
#include "mediasoupclient.hpp"

#include "sdk/android/src/jni/jvm.h"

namespace acgist {

    /**
     * 全局JavaVM指针
     */
    extern JavaVM* taoyaoJavaVM;

    extern void bindJavaThread(JNIEnv** env, const char* name = "C++Thread");

    extern void unbindJavaThread();

}