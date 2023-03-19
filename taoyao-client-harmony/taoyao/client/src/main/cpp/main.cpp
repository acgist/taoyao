#include <jni.h>
#include <string>
#include <hilog/log.h>

#include "main.hpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_acgist_taoyao_slice_MainAbilitySlice_stringFromJNI(JNIEnv *env, jobject obj) {
    std::string hello = "Hello from JNI C++ codes";
    int len = hello.size();
    jchar res[len];
    for (int i = 0; i < len; i++) {
        res[i] = (jchar)hello[i];
    }
    return env->NewString(res, len);
}
