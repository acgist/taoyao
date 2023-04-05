#pragma once

namespace acgist {

#include "android/log.h"

#define LOG_TAG "libtaoyao"

#define LOG_D(message, ...) acgist::__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, message, __VA_ARGS__)
#define LOG_I(message, ...) acgist::__android_log_print(ANDROID_LOG_INFO,  LOG_TAG, message, __VA_ARGS__)
#define LOG_W(message, ...) acgist::__android_log_print(ANDROID_LOG_WARN,  LOG_TAG, message, __VA_ARGS__)
#define LOG_E(message, ...) acgist::__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, message, __VA_ARGS__)
}
