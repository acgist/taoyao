#include "../include/WebRTC.hpp"

#include <hilog/log.h>

static const std::string h264ProfileLevelId = "42e01f";

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
    OH_LOG_DEBUG(LOG_APP, "返回WebRTC编码器：%o", format.name.data());
    // 硬编
    
    // 软便
    if (cricket::CodecNamesEq(format.name, cricket::kVp8CodecName)) {
        return VP8Encoder::Create();
    }
    if (cricket::CodecNamesEq(format.name, cricket::kVp9CodecName)) {
        return VP9Encoder::Create(cricket::VideoCodec(format));
    }
    if (cricket::CodecNamesEq(format.name, cricket::kH264CodecName)) {
        return H264Encoder::Create(cricket::VideoCodec(format));
    }
    return nullptr;    
}

std::unique_ptr<webrtc::VideoEncoderFactory>  webrtc::CreateBuiltinVideoEncoderFactory() {
    return std::unique_ptr(new acgist::TaoyaoVideoEncoderFactory());
}

std::unique_ptr<webrtc::VideoDecoderFactory> webrtc::CreateBuiltinVideoDecoderFactory() {
    return std::unique_ptr(new acgist::TaoyaoVideoDecoderFactory());
}
