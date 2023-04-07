package com.acgist.taoyao.media;

/**
 * 路由回调
 *
 * @author acgist
 */
public interface RouterCallback {

    default void enterCallback() {};
    default void newRemoteClientCallback() {};
    default void closeRemoteClientCallback() {};
    default void consumerPauseCallback() {};
    default void consumerResumeCallback() {};
    default void producerPauseCallback() {};
    default void producerResumeCallback() {};

}
