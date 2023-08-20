package com.acgist.taoyao.signal.service;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.Protocol;

/**
 * 信令安全
 * 
 * @author acgist
 */
public interface SecurityService {

    /**
     * 认证
     * 
     * @param username 用户名称
     * @param password 用户密码
     * 
     * @return 是否成功
     * 
     * @see UsernamePasswordService
     */
    boolean authenticate(String username, String password);
    
    /**
     * 鉴权
     * 
     * @param client   终端
     * @param message  信令消息
     * @param protocol 协议
     * 
     * @return 是否成功
     */
    boolean authenticate(Client client, Message message, Protocol protocol);
    
}
