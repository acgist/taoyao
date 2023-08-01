package com.acgist.taoyao.signal.protocol.control;

import com.acgist.taoyao.boot.config.MediaAudioProperties;
import com.acgist.taoyao.boot.model.Message;

/**
 * 配置音频信令
 * 
 * @author acgist
 */
public interface IControlConfigAudioProtocol {

    /**
     * @param clientId             终端ID
     * @param mediaAudioProperties 音频配置
     * 
     * @return 执行结果
     */
    Message execute(String clientId, MediaAudioProperties mediaAudioProperties);
    
}
