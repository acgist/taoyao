package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.config.camera.BeautyProperties;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 打开美颜信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "to": "目标终端ID",
        ...BeautyProperties
    }
    """,
    flow = {
        "信令服务->终端",
        "终端=>信令服务->终端"
    }
)
public class ControlBeautyProtocol extends ProtocolControlAdapter {

    public static final String SIGNAL = "control::beauty";
    
    public ControlBeautyProtocol() {
        super("打开美颜信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        client.push(targetClient.request(message));
    }

    /**
     * @param clientId 终端ID
     * @param beautyProperties 美颜配置
     * 
     * @return 执行结果
     */
    public Message execute(String clientId, BeautyProperties beautyProperties) {
        return this.request(clientId, this.build(beautyProperties));
    }
    
}
