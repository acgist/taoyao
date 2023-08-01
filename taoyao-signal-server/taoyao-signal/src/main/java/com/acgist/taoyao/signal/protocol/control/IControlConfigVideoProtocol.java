package com.acgist.taoyao.signal.protocol.control;

import com.acgist.taoyao.boot.config.MediaVideoProperties;
import com.acgist.taoyao.boot.model.Message;

/**
 * 配置视频信令接口
 * 
 * @author acgist
 */
public interface IControlConfigVideoProtocol {

    /**
     * @param clientId             终端ID
     * @param mediaVideoProperties 视频配置
     * 
     * @return 执行结果
     */
    Message execute(String clientId, MediaVideoProperties mediaVideoProperties);
    
}
