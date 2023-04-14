package com.acgist.taoyao.media.signal;

import com.acgist.taoyao.boot.model.Message;

/**
 * 信令监听
 *
 * @author acgist
 */
public interface ITaoyaoListener {

    /**
     * 前置信令处理
     *
     * @param message 信令消息
     *
     * @return 是否继续处理信令
     */
    default boolean preOnMessage(Message message) {
        return false;
    }

    /**
     * 后置信令处理
     *
     * @param message 信令消息
     */
    default void postOnMessage(Message message) {
    }

    /**
     * 信令正在连接
     */
    default void onConnect() {
    }

    /**
     * 信令连接成功
     */
    default void onConnected() {
    }

    /**
     * 信令断开连接
     */
    default void onDisconnect() {
    }

    /**
     * 信令异常
     *
     * @param throwable 异常
     */
    default void onError(Throwable throwable) {
    }

}
