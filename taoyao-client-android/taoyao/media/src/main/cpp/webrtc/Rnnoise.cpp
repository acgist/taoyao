#include "Rnnoise.hpp"

#include "jni.h"
#include "android/log.h"

#include <limits>

#ifndef RNNOISE_TAG
#define RNNOISE_TAG "rnnoise"
#endif

acgist::RnnoiseConfig::RnnoiseConfig() {
}

acgist::RnnoiseConfig::~RnnoiseConfig() {
    delete[] this->rnnoiseData;
    rnnoise_destroy(this->denoiseState);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_acgist_taoyao_media_audio_RnnoiseProcesser_Init(
    JNIEnv* env,
    jobject processer,
    jint bits,
    jint size,
    jint rate
) {
    __android_log_print(ANDROID_LOG_DEBUG, RNNOISE_TAG, "加载Rnnoise");
    acgist::RnnoiseConfig* config = new acgist::RnnoiseConfig();
    config->bits         = bits;
    config->size         = size;
    config->rate         = rate;
    config->rnnoiseSize  = size / 2;
    config->rnnoiseData  = new float[config->rnnoiseSize];
    config->denoiseState = rnnoise_create(NULL);
    return (jlong) config;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_acgist_taoyao_media_audio_RnnoiseProcesser_Rnnoise(
    JNIEnv* env,
    jobject processer,
    jlong pointer,
    jbyteArray pcm
) {
    acgist::RnnoiseConfig* config = (acgist::RnnoiseConfig*) pointer;
    jbyte* srcBytes = env->GetByteArrayElements(pcm, 0);
    short* srcBuffer = (short*) srcBytes;
    for (int i = 0; i < config->rnnoiseSize; i++) {
        config->rnnoiseData[i] = srcBuffer[i];
    }
    rnnoise_process_frame(config->denoiseState, config->rnnoiseData, config->rnnoiseData);
    // 返回值不用释放否则需要手动释放
    const jbyteArray result = env->NewByteArray(config->size);
    jbyte dstBytes[config->size];
    for (int i = 0; i < config->rnnoiseSize; i++) {
        short v = config->rnnoiseData[i];
        if(v > std::numeric_limits<short>::max()) {
            v = std::numeric_limits<short>::max();
        } else if(v < std::numeric_limits<short>::min()) {
            v = std::numeric_limits<short>::min();
        }
        dstBytes[2 * i]     = (int8_t) (v >> 0);
        dstBytes[2 * i + 1] = (int8_t) (v >> 8);
    }
    env->SetByteArrayRegion(result, 0, config->size, dstBytes);
    env->ReleaseByteArrayElements(pcm, srcBytes, 0);
//  env->DeleteLocalRef(result);
//  env->ReleaseByteArrayElements(result, dstBytes, 0);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_acgist_taoyao_media_audio_RnnoiseProcesser_Release(
    JNIEnv* env,
    jobject processer,
    jlong pointer
) {
    __android_log_print(ANDROID_LOG_DEBUG, RNNOISE_TAG, "释放Rnnoise");
    acgist::RnnoiseConfig* config = (acgist::RnnoiseConfig*) pointer;
    delete config;
}
