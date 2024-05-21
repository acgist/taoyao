#include "../include/MediaManager.hpp"

#include <mutex>

#include <hilog/log.h>

#include "api/create_peerconnection_factory.h"
#include "api/audio_codecs/audio_decoder_factory.h"
#include "api/audio_codecs/audio_encoder_factory.h"
#include "api/video_codecs/video_decoder_factory.h"
#include "api/video_codecs/video_encoder_factory.h"
#include <api/audio_codecs/builtin_audio_decoder_factory.h>
#include <api/audio_codecs/builtin_audio_encoder_factory.h>
#include <api/video_codecs/builtin_video_decoder_factory.h>
#include <api/video_codecs/builtin_video_encoder_factory.h>

static std::recursive_mutex mediaMutex;

acgist::MediaManager::MediaManager() {
}

acgist::MediaManager::~MediaManager() {
}

bool acgist::MediaManager::newPeerConnectionFactory() {
    if(this->peerConnectionFactory != nullptr) {
        return true;
    }
    OH_LOG_INFO(LOG_APP, "加载PeerConnectionFactory");
    this->networkThread   = rtc::Thread::CreateWithSocketServer();
    this->workerThread    = rtc::Thread::Create();
    this->signalingThread = rtc::Thread::Create();
    this->networkThread->SetName("network_thread", nullptr);
    this->workerThread->SetName("worker_thread", nullptr);
    this->signalingThread->SetName("signaling_thread", nullptr);
    if (
        !this->networkThread->Start()   ||
        !this->signalingThread->Start() ||
        !this->workerThread->Start()
    ) {
        OH_LOG_WARN(LOG_APP, "WebRTC线程启动失败");
        this->networkThread   = nullptr;
        this->workerThread    = nullptr;
        this->signalingThread = nullptr;
        return false;
    }
    this->peerConnectionFactory = webrtc::CreatePeerConnectionFactory(
        // 网络线程
        this->networkThread.get(),
        // 工作线程
        this->workerThread.get(),
        // 信令线程
        this->signalingThread.get(),
        // 音频设备
        nullptr,
        // 音频编码
        webrtc::CreateBuiltinAudioEncoderFactory(),
        // 音频解码
        webrtc::CreateBuiltinAudioDecoderFactory(),
        // 视频编码
        webrtc::CreateBuiltinVideoEncoderFactory(),
        // 视频解码
        webrtc::CreateBuiltinVideoDecoderFactory(),
        // 混音处理
        nullptr,
        // 音频处理
        nullptr
    );
    return true;
}

bool acgist::MediaManager::releasePeerConnectionFactory() {
    if(this->peerConnectionFactory == nullptr) {
        return true;
    }
    OH_LOG_INFO(LOG_APP, "释放PeerConnectionFactory");
    this->peerConnectionFactory->Release();
    // delete this->peerConnectionFactory;
    this->peerConnectionFactory = nullptr;
    return true;
}

int acgist::MediaManager::newLocalClient() {
    {
        std::lock_guard<std::recursive_mutex> mediaLock(mediaMutex);
        this->localClientRef++;
        this->newPeerConnectionFactory();
        this->startCapture();
    }
    OH_LOG_INFO(LOG_APP, "打开本地终端：%{public}d", this->localClientRef);
    return this->localClientRef;
}

int acgist::MediaManager::releaseLocalClient() {
    {
        std::lock_guard<std::recursive_mutex> mediaLock(mediaMutex);
        this->localClientRef--;
        if(this->localClientRef <= 0) {
            if(this->localClientRef < 0) {
                this->localClientRef = 0;
            } else {
                this->stopCapture();
                this->releasePeerConnectionFactory();
            }
        }
    }
    OH_LOG_INFO(LOG_APP, "关闭本地终端：%d", this->localClientRef);
    return this->localClientRef;
}

bool acgist::MediaManager::startCapture() {
    // this->startAudioCapture();
    this->startVideoCapture();
    return true;
}

bool acgist::MediaManager::startAudioCapture() {
    #if __TAOYAO_AUDIO_LOCAL__
        if (this->audioCapturer == nullptr) {
            OH_LOG_INFO(LOG_APP, "开始音频采集");
            this->audioCapturer = new acgist::AudioCapturer();
            this->audioCapturer->start();
        }
        if(this->audioTrackSource == nullptr) {
            OH_LOG_INFO(LOG_APP, "设置音频来源");
            this->audioTrackSource = new rtc::RefCountedObject<acgist::TaoyaoAudioTrackSource>();
            this->audioCapturer->source = this->audioTrackSource;
        }
    #else
        if(this->audioTrackSource == nullptr) {
            OH_LOG_INFO(LOG_APP, "设置音频来源");
            cricket::AudioOptions options;
//            options.highpass_filter   = true;
//            options.auto_gain_control = true;
//            options.echo_cancellation = true;
//            options.noise_suppression = true;
            this->audioTrackSource = this->peerConnectionFactory->CreateAudioSource(options);
        }
    #endif
    return true;
}

bool acgist::MediaManager::startVideoCapture() {
    if(this->videoCapturer == nullptr) {
        OH_LOG_INFO(LOG_APP, "开始视频采集");
        #if TAOYAO_VIDEO_SOURCE_SCREEN
        OH_LOG_INFO(LOG_APP, "开始屏幕采集");
        this->videoCapturer = new acgist::ScreenCapturer();
        #endif
        #if TAOYAO_VIDEO_SOURCE_CAMERA
        OH_LOG_INFO(LOG_APP, "开始相机采集");
        this->videoCapturer = new acgist::CameraCapturer();
        #endif
        this->videoCapturer->start();
    }
    if(this->videoTrackSource == nullptr) {
        OH_LOG_INFO(LOG_APP, "设置视频来源");
        this->videoTrackSource = new rtc::RefCountedObject<acgist::TaoyaoVideoTrackSource>();
        this->videoCapturer->source = this->videoTrackSource;
    }
    return true;
}

bool acgist::MediaManager::stopCapture() {
    this->stopAudioCapture();
    this->stopVideoCapture();
    return true;
}

bool acgist::MediaManager::stopAudioCapture() {
    #if __TAOYAO_AUDIO_LOCAL__
        if(this->audioCapturer != nullptr) {
            OH_LOG_INFO(LOG_APP, "停止音频采集");
            this->audioCapturer->stop();
            delete this->audioCapturer;
            this->audioCapturer = nullptr;
        }
        if(this->audioTrackSource != nullptr) {
            OH_LOG_INFO(LOG_APP, "释放音频来源");
            this->audioTrackSource->Release();
            // delete this->AudioTrackSource;
            this->audioTrackSource = nullptr;
        }
    #else
        if(this->audioTrackSource != nullptr) {
            OH_LOG_INFO(LOG_APP, "释放音频来源");
            this->audioTrackSource->Release();
            // delete this->audioTrackSource;
            this->audioTrackSource = nullptr;
        }
    #endif
    return true;
}

bool acgist::MediaManager::stopVideoCapture() {
    if(this->videoCapturer != nullptr) {
        OH_LOG_INFO(LOG_APP, "停止视频采集");
        this->videoCapturer->stop();
        delete this->videoCapturer;
        this->videoCapturer = nullptr;
    }
    if(this->videoTrackSource != nullptr) {
        OH_LOG_INFO(LOG_APP, "释放视频来源");
        this->videoTrackSource->Release();
        // delete this->videoTrackSource;
        this->videoTrackSource = nullptr;
    }
    return true;
}

rtc::scoped_refptr<webrtc::AudioTrackInterface> acgist::MediaManager::getAudioTrack() {
    #if __TAOYAO_AUDIO_LOCAL__
    return this->peerConnectionFactory->CreateAudioTrack("taoyao-audio", this->audioTrackSource);
    #else
    return this->peerConnectionFactory->CreateAudioTrack("taoyao-audio", this->audioTrackSource.get());
    #endif
}

rtc::scoped_refptr<webrtc::VideoTrackInterface> acgist::MediaManager::getVideoTrack() {
    return this->peerConnectionFactory->CreateVideoTrack("taoyao-video", this->videoTrackSource);
}
