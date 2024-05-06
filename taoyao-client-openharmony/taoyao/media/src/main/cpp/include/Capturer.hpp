/**
 * 采集器
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/media/audio-encoding.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/media/video-encoding.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/media/avscreen-capture.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/media/obtain-supported-codecs.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/camera/native-camera-recording.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/audio/using-ohaudio-for-recording.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/reference/native-lib/third_party_opengl/opengles.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/reference/native-lib/third_party_opensles/opensles.md
 */

#ifndef taoyao_Capturer_HPP
#define taoyao_Capturer_HPP

#include <map>

#include "ohcamera/camera.h"
#include "ohcamera/video_output.h"

#include "api/media_stream_track.h"

#include <ohaudio/native_audiocapturer.h>
#include <ohaudio/native_audiostreambuilder.h>

namespace acgist {

/**
 * 采集器
 * 
 * @tparam Sink 输出管道
 */
template <typename Sink>
class Capturer {
    
public:
    std::map<std::string, Sink*> map;

public:
    Capturer();
    virtual ~Capturer();
    
public:
    // 开始采集
    virtual bool start() = 0;
    // 结束采集
    virtual bool stop() = 0;
    
};

/**
 * 音频采集器
 */
class AudioCapturer: public Capturer<webrtc::AudioTrackSinkInterface> {

public:
    // 音频流构造器
    OH_AudioStreamBuilder* builder = nullptr;
    // 音频采集器
    OH_AudioCapturer* audioCapturer = nullptr;

public:
    AudioCapturer();
    virtual ~AudioCapturer();
    
public:
    virtual bool start();
    virtual bool stop();
    
};

/**
 * 视频采集器
 */
class VideoCapturer: public Capturer<rtc::VideoSinkInterface<webrtc::RecordableEncodedFrame>> {
    
public:
    Camera_Device* camera_Device;
    Camera_Manager* camera_Manager;
    Camera_VideoOutput* camera_VideoOutput;
    Camera_VideoProfile* camera_VideoProfile;
    Camera_OutputCapability* camera_OutputCapability;

public:
    VideoCapturer();
    virtual ~VideoCapturer();
    
public:
    virtual bool start();
    virtual bool stop();
};

}

#endif // taoyao_Capturer_HPP
