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
     * @param signal 信令
     * @param args   消息主体内容
     *
     * @return 消息
     */
    Message buildMessage(String signal, Object ... args);

    /**
     * @param signal 信令
     * @param body   消息主体
     *
     * @return 消息
     */
    Message buildMessage(String signal, Object body);

    /**
     * @param message 信令消息
     */
    void push(Message message);

    /**
     * @param request 信令请求消息
     *
     * @return 信令响应消息
     */
    Message request(Message request);

    /**
     * @param request 信令请求消息
     * @param success 成功
     *
     * @return 结果
     */
    default <T> T requestFuture(Message request, Function<Message, T> success) {
        return this.requestFuture(request, success, null);
    }

    /**
     * @param request 信令请求消息
     * @param success 成功
     * @param failure 失败
     *
     * @return 结果
     */
    default <T> T requestFuture(Message request, Function<Message, T> success, Function<Message, T> failure) {
//      final CompletableFuture<Message> completableFuture = new CompletableFuture<>();
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
     * @param request 信令请求消息
     * @param success 成功
     */
    default void requestFuture(Message request, Consumer<Message> success) {
        this.requestFuture(request, success, null);
    }

    /**
     * @param request 信令请求消息
     * @param success 成功
     * @param failure 失败
     */
    default void requestFuture(Message request, Consumer<Message> success, Consumer<Message> failure) {
//      final CompletableFuture<Message> completableFuture = new CompletableFuture<>();
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
