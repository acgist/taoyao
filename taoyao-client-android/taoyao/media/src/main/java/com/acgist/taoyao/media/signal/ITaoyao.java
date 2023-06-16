package com.acgist.taoyao.media.signal;

import android.util.Log;

import com.acgist.taoyao.boot.model.Message;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 桃夭信令
 *
 * @author acgist
 */
public interface ITaoyao {

    /**
     * @param signal 信令标识
     * @param args   消息主体
     *
     * @return 信令消息
     */
    Message buildMessage(String signal, Object ... args);

    /**
     * @param signal 信令标识
     * @param body   消息主体
     *
     * @return 信令消息
     */
    Message buildMessage(String signal, Object body);

    /**
     * 推送信令消息
     *
     * @param message 信令消息
     */
    void push(Message message);

    /**
     * 请求信令消息
     *
     * @param request 请求信令消息
     *
     * @return 响应信令消息
     */
    Message request(Message request);

    /**
     * 请求信令消息
     *
     * @param request 请求信令消息
     * @param success 成功请求执行
     *
     * @return 执行结果
     */
    default <T> T requestFuture(Message request, Function<Message, T> success) {
        return this.requestFuture(request, success, null);
    }

    /**
     * 请求信令消息
     *
     * @param request 请求信令消息
     * @param success 成功请求执行
     * @param failure 失败请求执行
     *
     * @return 执行结果
     */
    default <T> T requestFuture(Message request, Function<Message, T> success, Function<Message, T> failure) {
        final Message response = this.request(request);
        if(response != null && response.isSuccess()) {
            return success.apply(response);
        } else {
            Log.w(ITaoyao.class.getSimpleName(), "信令响应失败：" + response);
            if(failure != null) {
                return failure.apply(response);
            }
            return null;
        }
    }

    /**
     * 请求信令消息
     *
     * @param request 请求信令消息
     * @param success 成功请求执行
     */
    default void requestFuture(Message request, Consumer<Message> success) {
        this.requestFuture(request, success, null);
    }

    /**
     * 请求信令消息
     *
     * @param request 信令请求消息
     * @param success 成功请求执行
     * @param failure 失败请求执行
     */
    default void requestFuture(Message request, Consumer<Message> success, Consumer<Message> failure) {
        final Message response = this.request(request);
        if(response != null && response.isSuccess()) {
            success.accept(response);
        } else {
            Log.w(ITaoyao.class.getSimpleName(), "信令响应失败：" + response);
            if(failure != null) {
                failure.accept(response);
            }
        }
    }

}
