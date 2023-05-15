#pragma once

/**
 * 日志头文件
 */
namespace acgist {

#include "android/log.h"

// TODO：优化INFO提示
#ifndef LOG_TAG_TAOYAO
#define LOG_TAG_TAOYAO "libtaoyao"
#define LOG_D(format, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG_TAOYAO, "%s " format, __func__, ##__VA_ARGS__)
#define LOG_I(format, ...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG_TAOYAO, "%s " format, __func__, ##__VA_ARGS__)
#define LOG_W(format, ...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG_TAOYAO, "%s " format, __func__, ##__VA_ARGS__)
#define LOG_E(format, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG_TAOYAO, "%s " format, __func__, ##__VA_ARGS__)
#endif

}
