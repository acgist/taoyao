package com.acgist.taoyao.signal.protocol.control;

import com.acgist.taoyao.boot.model.Message;

/**
 * 服务端录像信令接口
 * 
 * @author acgist
 */
public interface IControlServerRecordProtocol {

    /**
     * @param roomId   房间ID
     * @param clientId 终端ID
     * @param enabled  状态
     * 
     * @return 执行结果
     */
    Message execute(String roomId, String clientId, Boolean enabled);
    
}
