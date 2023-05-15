#pragma once

#include "jni.h"
#include "Log.hpp"
#include "mediasoupclient.hpp"

#include "sdk/android/src/jni/jvm.h"

/**
 * 媒体管理器头文件
 */
namespace acgist {

    /**
     * 全局JavaVM指针
     */
    extern JavaVM* taoyaoJavaVM;
    /**
     * 绑定Java线程
     *
     * @param env  JNIEnv
     * @param name Java线程名称
     */
    extern void bindJavaThread(JNIEnv** env, const char* name = "C++Thread");
    /**
     * 解绑Java线程
     */
    extern void unbindJavaThread();

}