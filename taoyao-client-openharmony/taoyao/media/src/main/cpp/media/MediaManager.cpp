#include "../include/MediaManager.hpp"

#include <mutex>

#include "hilog/log.h"

#include "api/create_peerconnection_factory.h"
#include "api/audio_codecs/audio_decoder_factory.h"
#include "api/audio_codecs/audio_encoder_factory.h"
#include "api/video_codecs/video_decoder_factory.h"
#include "api/video_codecs/video_encoder_factory.h"
#include <api/audio_codecs/builtin_audio_decoder_factory.h>
#include <api/audio_codecs/builtin_audio_encoder_factory.h>
#include <api/video_codecs/builtin_video_decoder_factory.h>
#include <api/video_codecs/builtin_video_encoder_factory.h>

static std::mutex refMutex;

acgist::MediaManager::MediaManager() {
}

acgist::MediaManager::~MediaManager() {
    // TODO：验证是否需要释放线程和工厂
}

bool acgist::MediaManager::initPeerConnectionFactory() {
    OH_LOG_INFO(LOG_APP, "加载PeerConnectionFactory");
    this->networkThread   = rtc::Thread::CreateWithSocketServer();
    this->signalingThread = rtc::Thread::Create();
    this->workerThread    = rtc::Thread::Create();
    this->networkThread->SetName("network_thread", nullptr);
    this->signalingThread->SetName("signaling_thread", nullptr);
    this->workerThread->SetName("worker_thread", nullptr);
    if (!this->networkThread->Start() || !this->signalingThread->Start() || !this->workerThread->Start()) {
        OH_LOG_WARN(LOG_APP, "WebRTC线程启动失败");
        return false;
    }
    this->peerConnectionFactory = webrtc::CreatePeerConnectionFactory(
        this->networkThread.get(),
        // worker和signaling使用相同线程
        this->workerThread.get(),
        // this->signalingThread.get(),
        this->signalingThread.get(),
        nullptr /* default_adm */,
        webrtc::CreateBuiltinAudioEncoderFactory(),
        webrtc::CreateBuiltinAudioDecoderFactory(),
        nullptr,
        nullptr,
        // TODO: 视频工厂
//         webrtc::CreateBuiltinVideoEncoderFactory(),
//         webrtc::CreateBuiltinVideoDecoderFactory(),
        nullptr /* audio_mixer      */,
        nullptr /* audio_processing */
    );
    return this->peerConnectionFactory != nullptr;
}

int acgist::MediaManager::newLocalClient() {
    {
        std::lock_guard<std::mutex> guard(refMutex);
        this->localClientRef++;
    }
}

int acgist::MediaManager::releaseLocalClient() {
    {
        std::lock_guard<std::mutex> guard(refMutex);
        this->localClientRef--;
    }
}

bool acgist::MediaManager::startCapture() {
    this->startAudioCapture();
    this->startVideoCapture();
}

bool acgist::MediaManager::startAudioCapture() {

    return true;
}

bool acgist::MediaManager::startVideoCapture() {
    return true;
}

rtc::scoped_refptr<webrtc::AudioTrackInterface> acgist::MediaManager::getAudioTrack() {
    cricket::AudioOptions options;
    options.highpass_filter   = true;
    options.auto_gain_control = true;
    options.echo_cancellation = true;
    options.noise_suppression = true;
    auto audioSource = this->peerConnectionFactory->CreateAudioSource(options);
    return this->peerConnectionFactory->CreateAudioTrack("taoyao-audio", audioSource.get());
}

rtc::scoped_refptr<webrtc::VideoTrackInterface> acgist::MediaManager::getVideoTrack() {
//     webrtc::VideoTrackSourceInterface videoSource;
//     this->peerConnectionFactory->CreateVideoTrack("taoyao-video", videoSource);
    return nullptr;
}
