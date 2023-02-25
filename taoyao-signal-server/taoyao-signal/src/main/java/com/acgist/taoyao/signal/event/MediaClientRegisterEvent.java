package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.client.Client;

/**
 * 媒体服务终端注册事件
 * 
 * @author acgist
 */
public class MediaClientRegisterEvent extends ClientEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public MediaClientRegisterEvent(Client client) {
        super(client);
    }

}
