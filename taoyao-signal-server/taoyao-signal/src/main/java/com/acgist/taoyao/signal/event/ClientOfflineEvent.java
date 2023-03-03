package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * 终端下线事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class ClientOfflineEvent extends ClientEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public ClientOfflineEvent(Client client) {
        super(client);
    }
    
}
