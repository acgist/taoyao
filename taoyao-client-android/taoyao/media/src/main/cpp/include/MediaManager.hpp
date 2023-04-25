#pragma once

#include "jni.h"
#include "Log.hpp"
#include "mediasoupclient.hpp"

#include "sdk/android/src/jni/jvm.h"

namespace acgist {

#ifndef TAOYAO_JAVA_VM
#define TAOYAO_JAVA_VM
    /**
     * 全局JavaVM指针
     */
    extern JavaVM* taoyaoJavaVM;
#endif

}