package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.client.Client;

/**
 * 终端关闭事件
 * 
 * @author acgist
 */
public class ClientCloseEvent extends ClientEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public ClientCloseEvent(Client client) {
        super(client);
    }

}
