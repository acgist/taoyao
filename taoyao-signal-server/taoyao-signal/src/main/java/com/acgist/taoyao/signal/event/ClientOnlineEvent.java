package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.client.Client;

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
