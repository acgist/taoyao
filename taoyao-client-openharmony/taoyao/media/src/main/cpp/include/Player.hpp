/**
 * 播放器
 * 
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/media/video-playback.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/audio/audio-playback-overview.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/media/media/using-ndk-avplayer-for-playerback.md
 */
#ifndef TAOYAO_PALYER_HPP
#define TAOYAO_PALYER_HPP

namespace acgist {

/**
 * 播放器
 */
class Player {
    
public:
    Player();
    virtual ~Player();

public:
    // 开始播放
    virtual bool start() = 0;
    // 结束播放
    virtual bool stop() = 0;
    
};

/**
 * 音频播放器
 */
class AudioPlayer: public Player {

public:
    AudioPlayer();
    virtual ~AudioPlayer();

public:
    virtual bool start() override;
    virtual bool stop() override;
    
};

/**
 * 视频播放器
 * 
 * TODO: 实现留给有缘人了~.~!
 */
class VideoPlayer: public Player {

public:
    VideoPlayer();
    virtual ~VideoPlayer();

public:
    virtual bool start() override;
    virtual bool stop() override;
    
};

}

#endif //TAOYAO_PALYER_HPP
