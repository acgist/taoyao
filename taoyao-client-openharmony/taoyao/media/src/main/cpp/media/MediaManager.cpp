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

static std::mutex mediaMutex;

acgist::MediaManager::MediaManager() {
}

acgist::MediaManager::~MediaManager() {
    // TODO：验证是否需要释放线程和工厂
}

bool acgist::MediaManager::init() {
    return true;
}

bool acgist::MediaManager::newPeerConnectionFactory() {
    if(this->peerConnectionFactory != nullptr) {
        return true;
    }
    OH_LOG_INFO(LOG_APP, "加载PeerConnectionFactory");
    this->networkThread   = rtc::Thread::CreateWithSocketServer();
    this->signalingThread = rtc::Thread::Create();
    this->workerThread    = rtc::Thread::Create();
    this->networkThread->SetName("network_thread", nullptr);
    this->signalingThread->SetName("signaling_thread", nullptr);
    this->workerThread->SetName("worker_thread", nullptr);
    if (!this->networkThread->Start() || !this->signalingThread->Start() || !this->workerThread->Start()) {
        OH_LOG_WARN(LOG_APP, "WebRTC线程启动失败");
        // TODO: 释放线程
        return false;
    }
    this->peerConnectionFactory = webrtc::CreatePeerConnectionFactory(
        this->networkThread.get(),
        this->workerThread.get(),
        // this->signalingThread.get(),
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
        // 混音
        nullptr,
        // 音频处理
        nullptr
    );
    return this->peerConnectionFactory != nullptr;
}

bool acgist::MediaManager::releasePeerConnectionFactory() {
    if(this->peerConnectionFactory == nullptr) {
        return true;
    }
    OH_LOG_INFO(LOG_APP, "释放PeerConnectionFactory");
    if(this->peerConnectionFactory != nullptr) {
        this->peerConnectionFactory->Release();
        // delete this->peerConnectionFactory;
        this->peerConnectionFactory = nullptr;
    }
    return true;
}

int acgist::MediaManager::newLocalClient() {
    {
        std::lock_guard<std::mutex> mediaLock(mediaMutex);
        this->localClientRef++;
        if(this->localClientRef > 0) {
            this->newPeerConnectionFactory();
            this->startCapture();
        }
    }
    return this->localClientRef;
}

int acgist::MediaManager::releaseLocalClient() {
    {
        std::lock_guard<std::mutex> mediaLock(mediaMutex);
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
    return this->localClientRef;
}

bool acgist::MediaManager::startCapture() {
    this->startAudioCapture();
    this->startVideoCapture();
    return true;
}

bool acgist::MediaManager::startAudioCapture() {
    if(this->audioCapturer != nullptr) {
        return true;
    }
    this->audioCapturer = new acgist::AudioCapturer();
    this->audioCapturer->start();
    return true;
}

bool acgist::MediaManager::startVideoCapture() {
    if(this->videoCapturer != nullptr) {
        return true;
    }
    this->videoCapturer = new acgist::VideoCapturer();
    this->videoCapturer->start();
    return true;
}

bool acgist::MediaManager::stopCapture() {
    this->stopAudioCapture();
    this->stopVideoCapture();
    return true;
}

bool acgist::MediaManager::stopAudioCapture() {
    if(this->audioCapturer == nullptr) {
        return true;
    }
    this->audioCapturer->stop();
    delete this->audioCapturer;
    this->audioCapturer = nullptr;
    return true;
}

bool acgist::MediaManager::stopVideoCapture() {
    if(this->videoCapturer == nullptr) {
        return true;
    }
    this->videoCapturer->stop();
    delete this->videoCapturer;
    this->videoCapturer = nullptr;
    return true;
}

rtc::scoped_refptr<webrtc::AudioTrackInterface> acgist::MediaManager::getAudioTrack() {
    cricket::AudioOptions options;
    options.highpass_filter   = true;
    options.auto_gain_control = true;
    options.echo_cancellation = true;
    options.noise_suppression = true;
    this->audioTrackSource = this->peerConnectionFactory->CreateAudioSource(options);
    return this->peerConnectionFactory->CreateAudioTrack("taoyao-audio", audioTrackSource.get());
}

rtc::scoped_refptr<webrtc::VideoTrackInterface> acgist::MediaManager::getVideoTrack() {
    this->videoTrackSource = new rtc::RefCountedObject<acgist::TaoyaoVideoTrackSource>();
    return this->peerConnectionFactory->CreateVideoTrack("taoyao-video", this->videoTrackSource);
}
