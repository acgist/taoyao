package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 拍照信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "to": "目标终端ID"
        }
        """,
        """
        {
            "filepath": "图片文件路径"
        }
        """
    },
    flow = {
        "信令服务=>终端",
        "终端=>信令服务->终端"
    }
)
public class ControlPhotographProtocol extends ProtocolControlAdapter implements IControlPhotographProtocol {

    public static final String SIGNAL = "control::photograph";
    
    public ControlPhotographProtocol() {
        super("拍照信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        client.push(targetClient.request(message));
    }
    
    @Override
    public Message execute(String clientId) {
        return this.request(clientId, this.build());
    }
    
}
