#include <api/audio_codecs/builtin_audio_decoder_factory.h>
#include <api/audio_codecs/builtin_audio_encoder_factory.h>
#include <api/video_codecs/builtin_video_decoder_factory.h>
#include <api/video_codecs/builtin_video_encoder_factory.h>

std::unique_ptr<webrtc::VideoDecoderFactory>  webrtc::CreateBuiltinVideoDecoderFactory() {
    // TODO: 硬件编解码
    return nullptr;
}

std::unique_ptr<webrtc::VideoEncoderFactory>  webrtc::CreateBuiltinVideoEncoderFactory() {
    // TODO: 硬件编解码
    return nullptr;
}