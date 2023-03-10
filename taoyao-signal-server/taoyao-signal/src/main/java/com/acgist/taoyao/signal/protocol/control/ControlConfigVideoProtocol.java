package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.MediaVideoProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 配置视频信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    memo = "如果没有指定参数使用默认参数配置",
    body = """
    {
        "to": "目标终端ID",
        ...MediaVideoProperties
    }
    """,
    flow = {
        "信令服务->终端",
        "终端=>信令服务->终端"
    }
)
public class ControlConfigVideoProtocol extends ProtocolControlAdapter {

    public static final String SIGNAL = "control::config::video";
    
    public ControlConfigVideoProtocol() {
        super("配置视频信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        client.push(targetClient.request(message));
    }
    
    /**
     * @param clientId 终端ID
     * @param mediaVideoProperties 视频配置
     * 
     * @return 执行结果
     */
    public Message execute(String clientId, MediaVideoProperties mediaVideoProperties) {
        return this.request(clientId, this.build(mediaVideoProperties));
    }

}
