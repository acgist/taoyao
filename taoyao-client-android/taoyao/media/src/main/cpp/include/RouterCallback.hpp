#pragma once

#include <string>

#include "jni.h"
#include "MediaManager.hpp"
#include "mediasoupclient.hpp"

/**
 * 路由回调头文件
 */
namespace acgist {

    /**
     * 路由回调
     */
    class RouterCallback {
    public:
        /**
         * 回调对象
         */
        jobject routerCallback;
    public:
        /**
         * @param routerCallback 回调对象
         */
        RouterCallback(jobject routerCallback);
        /**
         * 析构函数
         */
        virtual ~RouterCallback();
    public:
        /**
         * 进入房间回调
         *
         * @param env              JNIEnv
         * @param rtpCapabilities  RTP协商
         * @param sctpCapabilities SCTP协商
         */
        void enterRoomCallback(JNIEnv* env, const std::string& rtpCapabilities, const std::string& sctpCapabilities);
        /**
         * 关闭房间回调
         *
         * @param env JNIEnv
         */
        void closeRoomCallback(JNIEnv* env);
        /**
         * 发送通道连接回调
         *
         * @param env            JNIEnv
         * @param transportId    通道ID
         * @param dtlsParameters DTLS参数
         */
        void sendTransportConnectCallback(JNIEnv* env, const std::string& transportId, const std::string& dtlsParameters);
        /**
         * 接收通道连接回调
         *
         * @param env            JNIEnv
         * @param transportId    通道ID
         * @param dtlsParameters DTLS参数
         */
        void recvTransportConnectCallback(JNIEnv* env, const std::string& transportId, const std::string& dtlsParameters);
        /**
         * 发送通道生产回调
         *
         * @param env           JNIEnv
         * @param kind          类型
         * @param transportId   通道ID
         * @param rtpParameters RTP参数
         *
         * @return 生产者ID
         */
        std::string sendTransportProduceCallback(JNIEnv* env, const std::string& kind, const std::string& transportId, const std::string& rtpParameters);
        /**
         * 新建生产者回调
         *
         * @param env                       JNIEnv
         * @param kind                      生产者类型
         * @param producerId                生产者ID
         * @param producerPointer           生产者指针
         * @param producerMediaTrackPointer 媒体Tracker指针
         */
        void producerNewCallback(JNIEnv* env, const std::string& kind, const std::string& producerId, mediasoupclient::Producer* producerPointer, webrtc::MediaStreamTrackInterface* producerMediaTrackPointer);
        /**
         * 关闭生产者回调
         *
         * @param env        JNIEnv
         * @param producerId 生产者ID
         */
        void producerCloseCallback(JNIEnv* env, const std::string& producerId);
        /**
         * 暂停生产者回调
         *
         * @param env        JNIEnv
         * @param producerId 生产者ID
         */
        void producerPauseCallback(JNIEnv* env, const std::string& producerId);
        /**
         * 恢复生产者回调
         *
         * @param env        JNIEnv
         * @param producerId 生产者ID
         */
        void producerResumeCallback(JNIEnv* env, const std::string& producerId);
        /**
         * 新建消费者回调
         *
         * @param env                       JNIEnv
         * @param message                   信令消息
         * @param consumerPointer           消费者指针
         * @param consumerMediaTrackPointer 媒体Tracker指针
         */
        void consumerNewCallback(JNIEnv* env, const std::string& message, mediasoupclient::Consumer* consumerPointer, webrtc::MediaStreamTrackInterface* consumerMediaTrackPointer);
        /**
         * 关闭消费者回调
         *
         * @param env        JNIEnv
         * @param consumerId 消费者ID
         */
        void consumerCloseCallback(JNIEnv* env, const std::string& consumerId);
        /**
         * 暂停消费者回调
         *
         * @param env        JNIEnv
         * @param consumerId 消费者ID
         */
        void consumerPauseCallback(JNIEnv* env, const std::string& consumerId);
        /**
         * 恢复消费者回调
         *
         * @param env        JNIEnv
         * @param consumerId 消费者ID
         */
        void consumerResumeCallback(JNIEnv* env, const std::string& consumerId);

    };

}