/**
 * WebRTC功能
 * 
 * 视频编码解码
 * 
 * @author acgist
 */

#ifndef TAOYAO_WEBRTC_HPP
#define TAOYAO_WEBRTC_HPP

#include "./Signal.hpp"

//#include <ohaudio/native_audiorenderer.h>
//#include <ohaudio/native_audiostreambuilder.h>

//#include "api/audio_codecs/audio_encoder.h"
//#include "api/audio_codecs/audio_decoder.h"
//#include "api/audio_codecs/audio_encoder_factory.h"
//#include "api/audio_codecs/audio_decoder_factory.h"

#include "api/media_stream_track.h"
#include "api/media_stream_interface.h"
#include "api/video/video_sink_interface.h"
#include "api/video/video_source_interface.h"

#include "media/base/video_broadcaster.h"
#include "media/base/adapted_video_track_source.h"

#include "api/video_codecs/video_encoder.h"
#include "api/video_codecs/video_decoder.h"
#include "api/video_codecs/video_encoder_factory.h"
#include "api/video_codecs/video_decoder_factory.h"

#include <multimedia/player_framework/native_avformat.h>
#include <multimedia/player_framework/native_avbuffer.h>
#include <multimedia/player_framework/native_avcodec_base.h>
#include <multimedia/player_framework/native_avcapability.h>
#include <multimedia/player_framework/native_avcodec_videoencoder.h>
#include <multimedia/player_framework/native_avcodec_videodecoder.h>

namespace acgist {

/**
 * 音频轨道来源
 */
class TaoyaoAudioTrackSource : public webrtc::AudioTrackSinkInterface {

public:
    virtual void OnData(const void* audio_data, int bits_per_sample, int sample_rate, size_t number_of_channels, size_t number_of_frames) override;

};

/**
 * 视频管道
 */
class VideoTrackSinkInterface {

public:
    virtual void OnData(const webrtc::VideoFrame& videoFrame) = 0;

};

/**
 * 视频轨道来源
 */
class TaoyaoVideoTrackSource : public VideoTrackSinkInterface, public rtc::AdaptedVideoTrackSource {

public:
    TaoyaoVideoTrackSource();
    virtual ~TaoyaoVideoTrackSource() override;

public:
    virtual webrtc::MediaSourceInterface::SourceState state() const override;
    virtual bool remote() const override;
    virtual bool is_screencast() const override;
    virtual absl::optional<bool> needs_denoising() const override;
    virtual void OnData(const webrtc::VideoFrame& videoFrame) override;

};

/**
 * 视频编码
 */
class TaoyaoVideoEncoder : public webrtc::VideoEncoder {
    
public:
    // 视频编码器
    OH_AVCodec* avCodec = nullptr;
    // 视频窗口
    OHNativeWindow* nativeWindow = nullptr;
    
public:
    TaoyaoVideoEncoder();
    virtual ~TaoyaoVideoEncoder() override;
    
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
    virtual int32_t Release() override;
    virtual int32_t RegisterEncodeCompleteCallback(webrtc::EncodedImageCallback* callback) override;
    virtual void SetRates(const webrtc::VideoEncoder::RateControlParameters& parameters) override;
    virtual webrtc::VideoEncoder::EncoderInfo GetEncoderInfo() const override;
    virtual int32_t Encode(const webrtc::VideoFrame& frame, const std::vector<webrtc::VideoFrameType>* frame_types) override;
    
};

/**
 * 视频解码器
 */
class TaoyaoVideoDecoder : public webrtc::VideoDecoder {
    
public:
    TaoyaoVideoDecoder();
    virtual ~TaoyaoVideoDecoder() override;
    
public:
    virtual bool start();
    virtual bool stop();
    virtual int32_t Release() override;
    virtual int32_t RegisterDecodeCompleteCallback(webrtc::DecodedImageCallback* callback) override;
    virtual bool Configure(const webrtc::VideoDecoder::Settings& settings) override;
    virtual int32_t Decode(const webrtc::EncodedImage& input_image, bool missing_frames, int64_t render_time_ms) override;
    
};

class TaoyaoVideoEncoderFactory : public webrtc::VideoEncoderFactory {
    
public:
    TaoyaoVideoEncoderFactory();
    virtual ~TaoyaoVideoEncoderFactory();
    
public:
    virtual std::vector<webrtc::SdpVideoFormat> GetSupportedFormats() const override;
    virtual std::unique_ptr<webrtc::VideoEncoder> CreateVideoEncoder(const webrtc::SdpVideoFormat& format) override;
    
};

class TaoyaoVideoDecoderFactory : public webrtc::VideoDecoderFactory {
    
public:
    TaoyaoVideoDecoderFactory();
    virtual ~TaoyaoVideoDecoderFactory();
    
public:
    virtual std::vector<webrtc::SdpVideoFormat> GetSupportedFormats() const override;
    virtual std::unique_ptr<webrtc::VideoDecoder> CreateVideoDecoder(const webrtc::SdpVideoFormat& format) override;

};

}

#endif //TAOYAO_WEBRTC_HPP
