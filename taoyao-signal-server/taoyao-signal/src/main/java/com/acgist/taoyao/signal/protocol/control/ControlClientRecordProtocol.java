package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 终端录像信令
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
public class ControlClientRecordProtocol extends ProtocolControlAdapter implements IControlClientRecordProtocol {

    public static final String SIGNAL = "control::client::record";
    
    public ControlClientRecordProtocol() {
        super("终端录像信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        this.updateRecordStatus(targetClient, MapUtils.getBoolean(body, Constant.ENABLED));
        client.push(targetClient.request(message));
    }
    
    @Override
    public Message execute(String clientId, Boolean enabled) {
        this.updateRecordStatus(this.clientManager.getClients(clientId), enabled);
        return this.request(clientId, this.build(Map.of(Constant.ENABLED, enabled)));
    }
    
    /**
     * 设置录像状态
     * 
     * @param client  终端
     * @param enabled 录像状态
     */
    private void updateRecordStatus(Client client, Boolean enabled) {
        client.getStatus().setClientRecording(enabled);
    }
    
}
