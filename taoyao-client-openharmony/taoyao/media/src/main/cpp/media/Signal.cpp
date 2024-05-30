#include "../include/Signal.hpp"

namespace acgist {

uint32_t width   = 640;
uint32_t height  = 480;
uint64_t bitrate = 3'000'000L;
uint32_t iFrameInterval = 5'000;
double frameRate = 10.0;
int32_t samplingRate  = 48'000;
int32_t channelCount  = 2;
int32_t bitsPerSample = 16;
std::string clientId  = "";
std::string name      = "";
std::string surfaceId = "";
napi_env env = nullptr;
ImageReceiverNative* imageReceiverNative = nullptr;

}
