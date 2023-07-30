package com.acgist.taoyao.signal.event.client;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ClientEventAdapter;

/**
 * 关闭终端事件
 * 
 * @author acgist
 */
public class ClientCloseEvent extends ClientEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public ClientCloseEvent(Client client) {
        super(client);
    }

}
