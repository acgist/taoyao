package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.config.camera.AiProperties;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 打开AI识别信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "to": "目标终端ID",
        ...AiProperties
    }
    """,
    flow = {
        "信令服务->终端",
        "终端=>信令服务->终端"
    }
)
public class ControlAiProtocol extends ProtocolControlAdapter {

    public static final String SIGNAL = "control::ai";
    
    public ControlAiProtocol() {
        super("打开AI识别信令", SIGNAL);
    }
    
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        client.push(targetClient.request(message));
    }
    
    /**
     * @param clientId 终端标识
     * @param aiProperties AI识别配置
     * 
     * @return 执行结果
     */
    public Message execute(String clientId, AiProperties aiProperties) {
        return this.request(clientId, this.build(aiProperties));
    }

}
