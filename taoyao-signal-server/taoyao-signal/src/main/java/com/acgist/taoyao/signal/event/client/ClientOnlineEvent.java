package com.acgist.taoyao.signal.event.client;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ClientEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 终端上线事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class ClientOnlineEvent extends ClientEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public ClientOnlineEvent(Client client) {
        super(client);
    }

}
