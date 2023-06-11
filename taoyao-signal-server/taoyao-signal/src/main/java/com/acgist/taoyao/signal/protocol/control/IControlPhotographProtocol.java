package com.acgist.taoyao.signal.protocol.control;

import com.acgist.taoyao.boot.model.Message;

/**
 * 拍照信令接口
 * 
 * @author acgist
 */
public interface IControlPhotographProtocol {

    /**
     * @param clientId 终端标识
     * 
     * @return 执行结果
     */
    Message execute(String clientId);
    
}
