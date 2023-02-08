package com.acgist.taoyao.signal.event.client;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionStatus;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 终端注册事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class ClientRegisterEvent extends ApplicationEventAdapter {
	
	private static final long serialVersionUID = 1L;

	public ClientRegisterEvent(Map<?, ?> body, Message message, ClientSession session) {
		super(body, message, session);
	}
	
	/**
	 * @return {@link ClientSessionStatus#IP}
	 */
	public String getIp() {
		return this.get(ClientSessionStatus.IP);
	}
	
	/**
	 * @return {@link ClientSessionStatus#MAC}
	 */
	public String getMac() {
		return this.get(ClientSessionStatus.MAC);
	}
	
	/**
	 * @return {@link ClientSessionStatus#SIGNAL}
	 */
	public Integer getSignal() {
		return this.get(ClientSessionStatus.SIGNAL);
	}
	
	/**
	 * @return {@link ClientSessionStatus#BATTERY}
	 */
	public Integer getBattery() {
		return this.get(ClientSessionStatus.BATTERY);
	}
	
	/**
	 * @return {@link ClientSessionStatus#CHARGING}
	 */
	public Boolean getCharging() {
		return this.get(ClientSessionStatus.CHARGING);
	}
	
	/**
	 * @return {@link ClientSessionStatus#MEDIASOUP}
	 */
	public String getMediasoup() {
		return this.get(ClientSessionStatus.MEDIASOUP);
	}

}
