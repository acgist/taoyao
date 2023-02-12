package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.MediaProperties;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 媒体服务列表信令
 * 
 * @author acgist
 */
@Protocol
public class MediaListProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "media::list";
	
	@Autowired
	private MediaProperties mediaProperties;
	
	public MediaListProtocol() {
		super("媒体服务列表信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Client client, Message message) {
		message.setBody(this.mediaProperties.getMediaServerList());
		client.push(message);
	}
	
}
