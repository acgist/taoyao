package com.acgist.taoyao.media;

/**
 * 路由回调
 *
 * @author acgist
 */
public interface RouterCallback {

    /**
     * 进入房间回调
     *
     * @param rtpCapabilities  RTP协商
     * @param sctpCapabilities SCTP协商
     */
    default void enterRoomCallback(String rtpCapabilities, String sctpCapabilities) {};

    /**
     * 关闭房间回调
     */
    default void closeRoomCallback() {};

    /**
     * 发送通道连接回调
     *
     * @param transportId    通告ID
     * @param dtlsParameters DTLS参数
     */
    default void sendTransportConnectCallback(String transportId, String dtlsParameters) {};

    /**
     * 接收通道连接回调
     *
     * @param transportId    通道ID
     * @param dtlsParameters DTLS参数
     */
    default void recvTransportConnectCallback(String transportId, String dtlsParameters) {};

    /**
     * 发送通道生产回调
     *
     * @param kind          类型
     * @param transportId   通道ID
     * @param rtpParameters RTP参数
     *
     * @return 生产者ID
     */
    default String sendTransportProduceCallback(String kind, String transportId, String rtpParameters) { return null; };

    /**
     * 新建生产者回调
     *
     * @param kind                      生产者类型
     * @param producerId                生产者ID
     * @param producerPointer           生产者指针
     * @param producerMediaTrackPointer 媒体Tracker指针
     */
    default void producerNewCallback(String kind, String producerId, long producerPointer, long producerMediaTrackPointer) {};

    /**
     * 关闭生产者回调
     *
     * @param producerId 生产者ID
     */
    default void producerCloseCallback(String producerId) {};

    /**
     * 暂停生产者回调
     *
     * @param producerId 生产者ID
     */
    default void producerPauseCallback(String producerId) {};

    /**
     * 恢复生产者回调
     *
     * @param producerId 生产者ID
     */
    default void producerResumeCallback(String producerId) {};

    /**
     * 新建消费者回调
     *
     * @param message                   信令消息
     * @param consumerPointer           消费者指针
     * @param consumerMediaTrackPointer 媒体Tracker指针
     */
    default void consumerNewCallback(String message, long consumerPointer, long consumerMediaTrackPointer) {};

    /**
     * 关闭消费者回调
     *
     * @param consumerId 消费者ID
     */
    default void consumerCloseCallback(String consumerId) {};

    /**
     * 暂停消费者回调
     *
     * @param consumerId 消费者ID
     */
    default void consumerPauseCallback(String consumerId) {};

    /**
     * 恢复消费者回调
     *
     * @param consumerId 消费者ID
     */
    default void consumerResumeCallback(String consumerId) {};

}
