package com.acgist.taoyao.signal.event.room;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ClientEventAdapter;

/**
 * 媒体服务注册事件
 * 需要重新创建房间
 * 
 * 媒体服务掉线两种方案
 * 
 * 1. 注册相同名称媒体服务，注册成功之后通知媒体服务终端重新连接。
 * 2. 自动转移媒体服务终端到个新的媒体服务，然后通知媒体服务终端重新连接。
 * 
 * 本项目采用第一种方案
 * 
 * @author acgist
 */
public class MediaServerRegisterEvent extends ClientEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public MediaServerRegisterEvent(Client client) {
        super(client);
    }

}
