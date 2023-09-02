package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;

/**
 * 控制信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolControlAdapter extends ProtocolClientAdapter {

    protected ProtocolControlAdapter(String name, String signal) {
        super(name, signal);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final String to = MapUtils.remove(body, Constant.TO);
        final Client targetClient = this.clientManager.getClients(to);
        if(targetClient == null) {
            throw MessageCodeException.of("目标终端无效：" + to);
        }
        this.execute(clientId, clientType, client, targetClient, message, body);
    }
    
    /**
     * 处理终端控制信令
     * 
     * @param clientId     终端ID
     * @param clientType   终端类型
     * @param client       终端
     * @param targetClient 目标终端
     * @param message      信令消息
     * @param body         消息主体
     */
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
    }
    
    /**
     * 请求终端执行控制信令
     * 
     * @param clientId 终端ID
     * @param request  请求
     * 
     * @return 响应
     */
    protected Message request(String clientId, Message request) {
        final Client client = this.clientManager.getClients(clientId);
        if(client == null) {
            return Message.fail("无效终端：" + clientId);
        } else {
            return client.request(request);
        }
    }

}
