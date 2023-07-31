package com.acgist.taoyao.signal.protocol.control;

import com.acgist.taoyao.boot.model.Message;

/**
 * 终端唤醒信令接口
 * 
 * @author acgist
 */
public interface IControlWakeupProtocol {

    /**
     * @param clientId 终端ID
     * 
     * @return 执行结果
     */
    Message execute(String clientId);
    
}
