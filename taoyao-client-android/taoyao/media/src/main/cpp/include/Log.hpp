#pragma once

namespace acgist {

#include "android/log.h"

#define LOG_TAG "libtaoyao"

#define LOG_D(format, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%s " format, __func__, ##__VA_ARGS__)
#define LOG_I(format, ...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, "%s " format, __func__, ##__VA_ARGS__)
#define LOG_W(format, ...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, "%s " format, __func__, ##__VA_ARGS__)
#define LOG_E(format, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s " format, __func__, ##__VA_ARGS__)

//#define LOG_D(...) acgist::__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
//#define LOG_I(...) acgist::__android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
//#define LOG_W(...) acgist::__android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
//#define LOG_E(...) acgist::__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

}
