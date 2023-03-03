package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * 终端配置事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class ClientConfigEvent extends ClientEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public ClientConfigEvent(Client client) {
        super(client);
    }
    
}
