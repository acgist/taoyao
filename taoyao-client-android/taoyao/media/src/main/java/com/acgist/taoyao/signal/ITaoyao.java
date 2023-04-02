package com.acgist.taoyao.signal;

import com.acgist.taoyao.boot.model.Message;

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

}
