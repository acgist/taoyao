#include "../include/WebRTC.hpp"

#include <hilog/log.h>

#include "media/base/codec.h"
#include "api/video_codecs/sdp_video_format.h"
#include "modules/video_coding/codecs/vp8/include/vp8.h"
#include "modules/video_coding/codecs/vp9/include/vp9.h"
#include "modules/video_coding/codecs/h264/include/h264.h"

//#include "api/audio_codecs/builtin_audio_decoder_factory.h"
//#include "api/audio_codecs/builtin_audio_encoder_factory.h"

#include "api/video_codecs/builtin_video_decoder_factory.h"
#include "api/video_codecs/builtin_video_encoder_factory.h"

static const std::string h264ProfileLevelId = "42e01f";

acgist::TaoyaoAudioTrackSource::TaoyaoAudioTrackSource() {
}

acgist::TaoyaoAudioTrackSource::~TaoyaoAudioTrackSource() {
}

webrtc::MediaSourceInterface::SourceState acgist::TaoyaoAudioTrackSource::state() const {
    return webrtc::MediaSourceInterface::SourceState::kLive;
}

bool acgist::TaoyaoAudioTrackSource::remote() const {
    return false;
}

void acgist::TaoyaoAudioTrackSource::OnData(const void* audio_data, int bits_per_sample, int sample_rate, size_t number_of_channels, size_t number_of_frames) {
    // TODO: 转发媒体
}

acgist::TaoyaoVideoTrackSource::TaoyaoVideoTrackSource() {
}

acgist::TaoyaoVideoTrackSource::~TaoyaoVideoTrackSource() {
}

webrtc::MediaSourceInterface::SourceState acgist::TaoyaoVideoTrackSource::state() const {
    return webrtc::MediaSourceInterface::SourceState::kLive;
}

bool acgist::TaoyaoVideoTrackSource::remote() const {
    return false;
}

bool acgist::TaoyaoVideoTrackSource::is_screencast() const {
    return false;
}

absl::optional<bool> acgist::TaoyaoVideoTrackSource::needs_denoising() const {
    return false;
}

void acgist::TaoyaoVideoTrackSource::OnData(const webrtc::VideoFrame& videoFrame) {
    // TODO
}

acgist::TaoyaoVideoEncoderFactory::TaoyaoVideoEncoderFactory() {
}

acgist::TaoyaoVideoEncoderFactory::~TaoyaoVideoEncoderFactory() {
}

std::vector<webrtc::SdpVideoFormat> acgist::TaoyaoVideoEncoderFactory::GetSupportedFormats() const {
    std::vector<webrtc::SdpVideoFormat> supported_codecs;
    std::map<std::string, std::string> params;
    params["profile-level-id"]        = h264ProfileLevelId;
    params["packetization-mode"]      = "1";
    params["level-asymmetry-allowed"] = "1";
    webrtc::SdpVideoFormat dst_format("H264", params);
    for (const webrtc::SdpVideoFormat& format : webrtc::SupportedH264Codecs()) {
        if (format == dst_format) {
            supported_codecs.push_back(format);
            break;
        }
    }
    supported_codecs.push_back(webrtc::SdpVideoFormat(cricket::kVp8CodecName));
    supported_codecs.push_back(webrtc::SdpVideoFormat(cricket::kVp9CodecName));
    supported_codecs.push_back(webrtc::SdpVideoFormat(cricket::kH264CodecName));
    return supported_codecs;
}

std::unique_ptr<webrtc::VideoEncoder> acgist::TaoyaoVideoEncoderFactory::CreateVideoEncoder(const webrtc::SdpVideoFormat& format) {
    OH_LOG_DEBUG(LOG_APP, "返回WebRTC编码器：%s", format.name.data());
    // 硬编
    if (absl::EqualsIgnoreCase(format.name.data(), "H264") == 0) {
        return std::unique_ptr<webrtc::VideoEncoder>(new acgist::TaoyaoVideoEncoder());
    }
    // 软便
    if (absl::EqualsIgnoreCase(format.name.data(), cricket::kVp8CodecName) == 0) {
        return webrtc::VP8Encoder::Create();
    }
    if (absl::EqualsIgnoreCase(format.name.data(), cricket::kVp9CodecName) == 0) {
        // return webrtc::VP9Encoder::Create();
        return webrtc::VP9Encoder::Create(cricket::CreateVideoCodec(format));
    }
    if (absl::EqualsIgnoreCase(format.name.data(), cricket::kH264CodecName) == 0) {
        // return webrtc::H264Encoder::Create();
        return webrtc::H264Encoder::Create(cricket::CreateVideoCodec(format));
    }
    return nullptr;
}

acgist::TaoyaoVideoDecoderFactory::TaoyaoVideoDecoderFactory() {
}

acgist::TaoyaoVideoDecoderFactory::~TaoyaoVideoDecoderFactory() {
}

std::vector<webrtc::SdpVideoFormat> acgist::TaoyaoVideoDecoderFactory::GetSupportedFormats() const {
    std::vector<webrtc::SdpVideoFormat> supported_codecs;
    std::map<std::string, std::string> params;
    params["profile-level-id"]        = h264ProfileLevelId;
    params["packetization-mode"]      = "1";
    params["level-asymmetry-allowed"] = "1";
    webrtc::SdpVideoFormat dst_format("H264", params);
    for (const webrtc::SdpVideoFormat& format : webrtc::SupportedH264Codecs()) {
        if (format == dst_format) {
            supported_codecs.push_back(format);
            break;
        }
    }
    supported_codecs.push_back(webrtc::SdpVideoFormat(cricket::kVp8CodecName));
    supported_codecs.push_back(webrtc::SdpVideoFormat(cricket::kVp9CodecName));
    supported_codecs.push_back(webrtc::SdpVideoFormat(cricket::kH264CodecName));
    return supported_codecs;
}

std::unique_ptr<webrtc::VideoDecoder> acgist::TaoyaoVideoDecoderFactory::CreateVideoDecoder(const webrtc::SdpVideoFormat& format) {
    OH_LOG_DEBUG(LOG_APP, "返回WebRTC解码器：%s", format.name.data());
    // 硬解
    if (absl::EqualsIgnoreCase(format.name.data(), "H264") == 0) {
        return std::unique_ptr<webrtc::VideoDecoder>(new acgist::TaoyaoVideoDecoder());
    }
    // 软解
    if (absl::EqualsIgnoreCase(format.name.data(), cricket::kVp8CodecName) == 0) {
        return webrtc::VP8Decoder::Create();
    }
    if (absl::EqualsIgnoreCase(format.name.data(), cricket::kVp9CodecName) == 0) {
        return webrtc::VP9Decoder::Create();
    }
    if (absl::EqualsIgnoreCase(format.name.data(), cricket::kH264CodecName) == 0) {
        return webrtc::H264Decoder::Create();
    }
    return nullptr;
}

std::unique_ptr<webrtc::VideoEncoderFactory>  webrtc::CreateBuiltinVideoEncoderFactory() {
    return std::unique_ptr<webrtc::VideoEncoderFactory>(new acgist::TaoyaoVideoEncoderFactory());
}

std::unique_ptr<webrtc::VideoDecoderFactory> webrtc::CreateBuiltinVideoDecoderFactory() {
    return std::unique_ptr<webrtc::VideoDecoderFactory>(new acgist::TaoyaoVideoDecoderFactory());
}
