package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.Constant;
import com.acgist.taoyao.boot.property.MediaServerProperties;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.protocol.ProtocolMediaAdapter;
import com.acgist.taoyao.signal.protocol.room.RoomCreateProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务注册信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
        {
            "username": "媒体用户",
            "password": "媒体密码"
        }
        """,
    flow = "信令服务->媒体服务->信令服务"
)
public class MediaRegisterProtocol extends ProtocolMediaAdapter {

	public static final String SIGNAL = "media::register";
	
	@Autowired
	private RoomCreateProtocol roomCreateProtocol;
	
	public MediaRegisterProtocol() {
		super("媒体服务注册信令", SIGNAL);
	}
	
	/**
	 * 创建信令消息
	 * 
	 * @param mediaServerProperties 媒体服务配置
	 * 
	 * @return 信令消息
	 */
	public Message build(MediaServerProperties mediaServerProperties) {
		return super.build(Map.of(
			Constant.USERNAME, mediaServerProperties.getUsername(),
			Constant.PASSWORD, mediaServerProperties.getPassword()
		));
	}
	
	@Override
	public void execute(Map<?, ?> body, MediaClient mediaClient, Message message) {
		log.info("媒体终端注册结果：{}", message);
		this.roomManager.recreate(mediaClient, this.roomCreateProtocol.build());
	}
	
	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
	}

}
