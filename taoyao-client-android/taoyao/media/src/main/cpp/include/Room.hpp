#pragma once

#include <map>
#include <string>

#include "jni.h"
#include "sdk/android/src/jni/pc/peer_connection.h"
#include "sdk/android/native_api/jni/scoped_java_ref.h"

#include "Log.hpp"
#include "RouterCallback.hpp"

/**
 * 视频房间头文件
 */
namespace acgist {

    /**
     * 视频房间
     */
    class Room : public RouterCallback {
    public:
        /**
         * 房间ID
         */
        std::string roomId;
        /**
         * 全局共用PeerConnectionFactory
         */
        webrtc::PeerConnectionFactoryInterface* factory;
        /**
         * 全局共用RTCConfiguration
         */
        webrtc::PeerConnectionInterface::RTCConfiguration* rtcConfiguration;
        /**
         * 房间Device
         */
        mediasoupclient::Device* device;
        /**
         * 发送通道
         */
        mediasoupclient::SendTransport* sendTransport;
        /**
         * 接收通道
         */
        mediasoupclient::RecvTransport* recvTransport;
        /**
         * 发送监听器
         */
        mediasoupclient::SendTransport::Listener* sendListener;
        /**
         * 接收监听器
         */
        mediasoupclient::RecvTransport::Listener* recvListener;
        /**
         * 音频生产者
         */
        mediasoupclient::Producer* audioProducer;
        /**
         * 视频生产者
         */
        mediasoupclient::Producer* videoProducer;
        /**
         * 生产者监听器
         */
        mediasoupclient::Producer::Listener* producerListener;
        /**
         * 消费者监听器
         */
        mediasoupclient::Consumer::Listener* consumerListener;
        /**
         * 消费者列表
         */
        std::map<std::string, mediasoupclient::Consumer*> consumers;
    public:
        /**
         * @param roomId         房间ID
         * @param routerCallback 房间路由回调
         */
        Room(const std::string& roomId, const jobject& routerCallback);
        /**
         * 析构函数
         */
        virtual ~Room();
    public:
        /**
         * 进入房间
         *
         * @param env              JNIEnv
         * @param rtpCapabilities  RTP协商
         * @param factory          PeerConnectionFactory
         * @param rtcConfiguration RTCConfiguration
         */
        void enterRoom(JNIEnv* env, const std::string& rtpCapabilities, webrtc::PeerConnectionFactoryInterface* factory, webrtc::PeerConnectionInterface::RTCConfiguration& rtcConfiguration);
        /**
         * 创建发送通道
         *
         * @param env  JNIEnv
         * @param body 信令主体
         */
        void createSendTransport(JNIEnv* env, const std::string& body);
        /**
         * 创建接收通道
         *
         * @param env  JNIEnv
         * @param body 信令主体
         */
        void createRecvTransport(JNIEnv* env, const std::string& body);
        /**
         * 生成音频
         *
         * @param env         JNIEnv
         * @param mediaStream 媒体流
         */
        void mediaProduceAudio(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream);
        /**
         * 生成视频
         *
         * @param env         JNIEnv
         * @param mediaStream 媒体流
         */
        void mediaProduceVideo(JNIEnv* env, webrtc::MediaStreamInterface* mediaStream);
        /**
         * 消费媒体
         *
         * @param env     JNIEnv
         * @param message 信令消息
         */
        void mediaConsume(JNIEnv* env, const std::string& message);
        /**
         * 暂停生产者
         *
         * @param env        JNIEnv
         * @param producerId 生产者ID
         */
        void mediaProducerPause(JNIEnv* env, const std::string& producerId);
        /**
         * 恢复生产者
         *
         * @param env        JNIEnv
         * @param producerId 生产者ID
         */
        void mediaProducerResume(JNIEnv* env, const std::string& producerId);
        /**
         * 关闭生产者
         *
         * @param env        JNIEnv
         * @param producerId 生产者ID
         */
        void mediaProducerClose(JNIEnv* env, const std::string& producerId);
        /**
         * 暂停消费者
         *
         * @param env        JNIEnv
         * @param consumerId 消费者ID
         */
        void mediaConsumerPause(JNIEnv* env, const std::string& consumerId);
        /**
         * 恢复消费者
         *
         * @param env        JNIEnv
         * @param consumerId 消费者ID
         */
        void mediaConsumerResume(JNIEnv* env, const std::string& consumerId);
        /**
         * 关闭消费者
         *
         * @param env        JNIEnv
         * @param consumerId 消费者ID
         */
        void mediaConsumerClose(JNIEnv* env, const std::string& consumerId);
        /**
         * 关闭房间
         *
         * @param env JNIEnv
         */
        void closeRoom(JNIEnv* env);
        /**
         * 设置码率
         *
         * @param maxFramerate 最大帧率
         * @param minBitrate   最小码率
         * @param maxBitrate   最大码率
         */
        void setBitrate(int maxFramerate, int minBitrate, int maxBitrate);
    };

}