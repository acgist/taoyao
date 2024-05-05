#include "../include/MediaManager.hpp"

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
    this->localClientRef++;
}
