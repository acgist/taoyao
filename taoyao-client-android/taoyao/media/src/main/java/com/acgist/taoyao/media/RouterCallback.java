package com.acgist.taoyao.media;

/**
 * 路由回调
 *
 * @author acgist
 */
public interface RouterCallback {

    default void enterRoomCallback(String rtpCapabilities, String sctpCapabilities) {};
    default void closeRoomCallback() {};
    default void sendTransportConnectCallback(String transportId, String dtlsParameters) {};
    default void recvTransportConnectCallback(String transportId, String dtlsParameters) {};
    default String sendTransportProduceCallback(String kind, String transportId, String rtpParameters) { return null; };
    default void producerNewCallback(String kind, String producerId, long producerPointer, long producerMediaTrackPointer) {};
    default void producerCloseCallback(String producerId) {};
    default void producerPauseCallback(String producerId) {};
    default void producerResumeCallback(String producerId) {};
    default void consumerNewCallback(String message, long consumerPointer, long consumerMediaTrackPointer) {};
    default void consumerCloseCallback(String consumerId) {};
    default void consumerPauseCallback(String consumerId) {};
    default void consumerResumeCallback(String consumerId) {};

}
