package com.acgist.taoyao.signal.event.client;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ClientEventAdapter;

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
