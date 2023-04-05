package com.acgist.taoyao;

/**
 * 路由回调
 */
public interface RouterCallback {

    void enterCallback();
    void newRemoteClientCallback();
    void closeRemoteClientCallback();
    void consumerPauseCallback();
    void consumerResumeCallback();
    void producerPauseCallback();
    void producerResumeCallback();

}
