/**
 * 播放器
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/media/video-playback.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/media/media-kit-intro.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/audio/audio-playback-overview.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/media/using-ndk-avplayer-for-playerback.md
 */

#ifndef TAOYAO_PALYER_HPP
#define TAOYAO_PALYER_HPP

#include "./Signal.hpp"

#include <ohaudio/native_audiorenderer.h>
#include <ohaudio/native_audiostreambuilder.h>

namespace acgist {

/**
 * 播放器
 */
class Player {
    
protected:
    bool running = false;
    
public:
    Player();
    virtual ~Player();

public:
    // 开始播放
    virtual bool start() = 0;
    // 结束播放
    virtual bool stop()  = 0;
    
};

/**
 * 音频播放器
 * 
 * 默认交给音频设备播放
 */
class AudioPlayer : public Player {

public:
    // 音频构造器
    OH_AudioStreamBuilder* builder  = nullptr;
    // 音频播放器
    OH_AudioRenderer* audioRenderer = nullptr;;

public:
    AudioPlayer();
    virtual ~AudioPlayer() override;

public:
    virtual bool start() override;
    virtual bool stop()  override;
    
};

/**
 * 视频播放器
 * 
 * TODO: 如果需要自行实现
 */
class VideoPlayer : public Player {

public:
    VideoPlayer();
    virtual ~VideoPlayer() override;

public:
    virtual bool start() override;
    virtual bool stop()  override;
    
};

}

#endif //TAOYAO_PALYER_HPP