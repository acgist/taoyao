/**
 * 采集器
 * 
 * @author acgist
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
