package com.acgist.taoyao.media;

import org.webrtc.MediaStream;

/**
 * 路由回调
 *
 * @author acgist
 */
public interface RouterCallback {

    default void enterCallback(String rtpCapabilities, String sctpCapabilities) {};
    default void sendTransportConnectCallback(String transportId, String dtlsParameters) {};
    default String sendTransportProduceCallback(String kind, String transportId, String rtpParameters) { return null; };
    default void recvTransportConnectCallback(String transportId, String dtlsParameters) {};
    default void producerNewCallback(String kind, String producerId, long producerMediaTrackPointer) {};
    default void producerPauseCallback(String producerId) {};
    default void producerResumeCallback(String producerId) {};
    default void producerCloseCallback(String producerId) {};
    default void consumerNewCallback(String message, long consumerMediaTrackPointer) {};
    default void consumerPauseCallback(String consumerId) {};
    default void consumerResumeCallback(String consumerId) {};
    default void consumerCloseCallback(String consumerId) {};

}
