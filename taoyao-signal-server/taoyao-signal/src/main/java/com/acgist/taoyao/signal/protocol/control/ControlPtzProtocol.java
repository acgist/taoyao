package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * PTZ信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "to": "目标终端ID",
        "type": "PTZ类型（PAN|TILT|ZOOM）",
        "value": PTZ参数
    }
    """,
    flow = {
        "信令服务->终端",
        "终端->信令服务->终端"
    }
)
public class ControlPtzProtocol extends ProtocolControlAdapter {

    public static final String SIGNAL = "control::ptz";
    
    public ControlPtzProtocol() {
        super("PTZ信令", SIGNAL);
    }

    /**
     * PTZ类型
     * 
     * @author acgist
     */
    public enum Type {
        
        // 水平
        PAN,
        // 垂直
        TILT,
        // 变焦
        ZOOM;
        
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        targetClient.push(message);
    }
    
    /**
     * @param type PTZ类型
     * @param value PTZ参数
     * @param clientId 终端标识
     */
    public void execute(Type type, Double value, String clientId) {
        this.clientManager.unicast(clientId, this.build(Map.of(type, value)));
    }
    
}
