#ifndef TAOYAO_WEBRTC_HPP
#define TAOYAO_WEBRTC_HPP

#include "./Signal.hpp"

#include <ohaudio/native_audiorenderer.h>
#include <ohaudio/native_audiostreambuilder.h>

#include <multimedia/player_framework/native_avformat.h>
#include <multimedia/player_framework/native_avbuffer.h>
#include <multimedia/player_framework/native_avcodec_base.h>
#include <multimedia/player_framework/native_avcapability.h>
#include <multimedia/player_framework/native_avcodec_videoencoder.h>
#include <multimedia/player_framework/native_avcodec_videodecoder.h>

#include "api/rtp_sender_interface.h"
#include "api/video_codecs/sdp_video_format.h"
#include "api/audio_codecs/builtin_audio_decoder_factory.h"
#include "api/audio_codecs/builtin_audio_encoder_factory.h"
#include "api/video_codecs/builtin_video_decoder_factory.h"
#include "api/video_codecs/builtin_video_encoder_factory.h"

#include "modules/video_coding/codecs/vp8/include/vp8.h"
#include "modules/video_coding/codecs/vp9/include/vp9.h"
#include "modules/video_coding/codecs/h264/include/h264.h"

namespace acgist {

/**
 * 视频编码
 */
class VideoEncoder : public webrtc::VideoEncoder {
    
public:
    // 视频编码器
    OH_AVCodec* avCodec = nullptr;
    // 视频窗口
    OHNativeWindow* nativeWindow = nullptr;
    
public:
    VideoEncoder();
    virtual ~VideoEncoder();
    
public:
    // 初始配置
    void initFormatConfig(OH_AVFormat* format);
    // 重新开始
    void restart();
    // 动态配置
    void reset(OH_AVFormat* format);
    // 动态配置
    void resetIntConfig(const char* key, int32_t value);
    // 动态配置
    void resetLongConfig(const char* key, int64_t value);
    // 动态配置
    void resetDoubleConfig(const char* key, double value);
    // 开始编码
    virtual bool start();
    // 结束编码
    virtual bool stop();
    
};

/**
 * 视频解码器
 */
class VideoDecoder : public webrtc::VideoDecoder {
    
public:
    VideoDecoder();
    virtual ~VideoDecoder();
    
public:
    virtual bool start();
    virtual bool stop();
    
};

class TaoyaoVideoEncoderFactory : webrtc::VideoEncoderFactory {
    
public:
    TaoyaoVideoEncoderFactory();
    virtual ~TaoyaoVideoEncoderFactory();
    
public:
    virtual std::vector<webrtc::SdpVideoFormat> GetSupportedFormats() const override;
    virtual std::unique_ptr<webrtc::VideoEncoder> CreateVideoEncoder(const webrtc::SdpVideoFormat& format) override;
    
};

class TaoyaoVideoDecoderFactory : webrtc::VideoDecoderFactory {
    
public:
    TaoyaoVideoDecoderFactory();
    virtual ~TaoyaoVideoDecoderFactory();
    
public:
    virtual std::vector<webrtc::SdpVideoFormat> GetSupportedFormats() const override;
    virtual std::unique_ptr<webrtc::VideoDecoder> CreateVideoDecoder(const webrtc::SdpVideoFormat& format) override;

};

}

#endif //TAOYAO_WEBRTC_HPP
