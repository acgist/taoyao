package com.acgist.taoyao.signal.service;

/**
 * 用户密码认证服务
 * 
 * @author acgist
 */
public interface UsernamePasswordService {

    /**
     * 认证
     * 
     * @param username 用户名称
     * @param password 用户密码
     * 
     * @return 是否成功
     */
    boolean authenticate(String username, String password);
    
}
