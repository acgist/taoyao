/**
 * 采集器
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/media/audio-encoding.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/media/video-encoding.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/media/obtain-supported-codecs.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/media/using-ohaudio-for-recording.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/reference/native-lib/third_party_opengl/opengles.md
 * https://docs.openharmony.cn/pages/v4.0/zh-cn/application-dev/reference/native-lib/third_party_opensles/opensles.md
 */

#ifndef taoyao_Capturer_HPP
#define taoyao_Capturer_HPP

#include "api/media_stream_track.h"

namespace acgist {

class Capturer {
    
public:
    virtual bool start();
    virtual bool stop();
    
};

class AudioCapturer {};

class VideoCapturer {};

}

#endif // taoyao_Capturer_HPP
