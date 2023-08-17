package com.acgist.taoyao.signal.service;

/**
 * 帐号密码认证服务
 * 
 * 用于验证帐号密码，如果没有提供实现默认使用配置中的帐号密码。
 * 
 * @author acgist
 */
public interface UsernamePasswordService {

    /**
     * 帐号密码认证
     * 
     * @param username 帐号
     * @param password 密码
     * 
     * @return 是否成功
     */
    boolean authenticate(String username, String password);
    
}
