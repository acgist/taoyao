package com.acgist.taoyao.signal.client;

import com.acgist.taoyao.boot.model.Message;

/**
 * 终端
 * 
 * @author acgist
 */
public interface Client extends AutoCloseable {

    /**
     * @return IP
     */
    String getIP();
    
    /**
     * @return 终端名称
     */
    String getName();
    
    /**
     * @return 终端ID
     */
    String getClientId();
    
    /**
     * @return 终端类型
     */
    ClientType getClientType();
    
    /**
     * @return 终端状态
     */
    ClientStatus getStatus();
    
    /**
     * @return 终端实例
     */
    AutoCloseable getInstance();
    
    /**
     * 推送消息
     * 
     * @param message 消息
     */
    void push(Message message);
    
    /**
     * 请求消息
     * 
     * @param request 消息
     * 
     * @return 响应
     */
    Message request(Message request);
    
    /**
     * 响应消息
     * 
     * @param id      消息ID
     * @param message 消息
     * 
     * @return 是否响应消息
     */
    boolean response(Long id, Message message);
    
    /**
     * @return 授权是否超时
     */
    boolean timeout();
    
    /**
     * 设置授权
     * 
     * @param clientId 终端ID
     */
    void authorize(String clientId);
    
    /**
     * @return 是否授权
     */
    boolean authorized();
    
    /**
     * @return 是否没有授权
     */
    default boolean unauthorized() {
        return !this.authorized();
    }
    
}
