package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.MediaProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 媒体服务列表信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
        [
            {
                "name": "名称",
                "enabled": "是否启用",
                "host": "主机",
                "port": "端口",
                "schema": "协议",
                "address": "完整地址"
            }
        ]
        """,
    flow = "终端->信令服务->终端"
)
public class MediaListProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "media::list";
	
	@Autowired
	private MediaProperties mediaProperties;
	
	public MediaListProtocol() {
		super("媒体服务列表信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		message.setBody(this.mediaProperties.getMediaServerList());
		client.push(message);
	}
	
}
