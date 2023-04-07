package com.acgist.taoyao.media;

/**
 * 路由回调
 *
 * @author acgist
 */
public interface RouterCallback {

    default void enterCallback(String rtpCapabilities, String sctpCapabilities) {};
    default void sendTransportConnectCallback(String transportId, String dtlsParameters) {};
    default String sendTransportProduceCallback(String kind, String transportId, String rtpParameters) {
        return null;
    };
    default void recvTransportConnectCallback(String transportId, String dtlsParameters) {};
    default void newRemoteClientCallback() {};
    default void closeRemoteClientCallback() {};
    default void consumerPauseCallback() {};
    default void consumerResumeCallback() {};
    default void producerPauseCallback() {};
    default void producerResumeCallback() {};

}
