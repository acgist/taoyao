#include "../include/WebRTC.hpp"

#include "api/video_codecs/video_encoder.h"
#include "api/video_codecs/video_decoder.h"

acgist::TaoyaoVideoDecoder::TaoyaoVideoDecoder() {
}

acgist::TaoyaoVideoDecoder::~TaoyaoVideoDecoder() {
}

bool acgist::TaoyaoVideoDecoder::start() {
    return true;
}

bool acgist::TaoyaoVideoDecoder::stop() {
    return true;
}

int32_t acgist::TaoyaoVideoDecoder::Release() {
    return 0;
}

int32_t acgist::TaoyaoVideoDecoder::RegisterDecodeCompleteCallback(webrtc::DecodedImageCallback* callback) {
    return 0;
}

bool acgist::TaoyaoVideoDecoder::Configure(const webrtc::VideoDecoder::Settings& settings) {
    return true;
}

int32_t acgist::TaoyaoVideoDecoder::Decode(const webrtc::EncodedImage& input_image, bool missing_frames, int64_t render_time_ms) {
    return 0;
}
