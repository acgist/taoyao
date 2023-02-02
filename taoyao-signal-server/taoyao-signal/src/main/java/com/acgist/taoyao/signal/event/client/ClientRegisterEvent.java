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

	public ClientRegisterEvent(String sn, Map<?, ?> body, Message message, ClientSession session) {
		super(sn, body, message, session);
	}
	
	/**
	 * @return IP
	 */
	public String getIp() {
		return this.get(ClientSessionStatus.IP);
	}
	
	/**
	 * @return Mac
	 */
	public String getMac() {
		return this.get(ClientSessionStatus.MAC);
	}
	
	/**
	 * @return Signal
	 */
	public Integer getSignal() {
		return this.get(ClientSessionStatus.SIGNAL);
	}
	
	/**
	 * @return Battery
	 */
	public Integer getBattery() {
		return this.get(ClientSessionStatus.BATTERY);
	}

}
