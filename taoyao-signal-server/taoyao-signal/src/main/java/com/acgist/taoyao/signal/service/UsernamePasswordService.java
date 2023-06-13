package com.acgist.taoyao.signal.service;

/**
 * 帐号密码认证服务
 * 
 * @author acgist
 */
public interface UsernamePasswordService {

    /**
     * 认证
     * 
     * @param username 帐号
     * @param password 密码
     * 
     * @return 是否成功
     */
    boolean authenticate(String username, String password);
    
}
