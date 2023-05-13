package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 录像信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "to": "目标终端ID",
            "enabled": 是否录像（true|false）
        }
        """,
        """
        {
            "enabled": 是否录像（true|false）,
            "filepath": "视频文件路径"
        }
        """
    },
    flow = {
        "信令服务->目标终端->信令服务",
        "终端=>信令服务->目标终端->信令服务->终端"
    }
)
public class ControlRecordProtocol extends ProtocolControlAdapter {

    public static final String SIGNAL = "control::record";
    
    public ControlRecordProtocol() {
        super("录像信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        client.push(targetClient.request(message));
    }
    
    /**
     * @param clientId 终端ID
     * @param enabled  状态
     * 
     * @return 执行结果
     */
    public Message execute(String clientId, Boolean enabled) {
        return this.request(clientId, this.build(Map.of(Constant.ENABLED, enabled)));
    }

}
