#pragma once

#include "rnnoise.h"

namespace acgist {

/**
 * 降噪配置
 */
class RnnoiseConfig {

public:
    // 采样位深：16
    int bits = 16;
    // 数据大小：960
    int size = 960;
    // 采样率：48000
    int rate = 48000;
    // 降噪数据大小
    int rnnoiseSize = 480;
    // 降噪数据
    float* rnnoiseData = nullptr;
    // 降噪对象
    DenoiseState* denoiseState = nullptr;

public:
    RnnoiseConfig();
    virtual ~RnnoiseConfig();

};

}
