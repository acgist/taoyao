package com.acgist.taoyao.signal.event.client;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;
import com.acgist.taoyao.signal.protocol.Constant;

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

	public ClientRegisterEvent(Map<?, ?> body, Client client, Message message) {
		super(body, client, message);
	}
	
	/**
	 * @return {@link Constant#IP}
	 */
	public String getIp() {
		return this.get(Constant.IP);
	}
	
	/**
	 * @return {@link Constant#SIGNAL}
	 */
	public Integer getSignal() {
		return this.get(Constant.SIGNAL);
	}
	
	/**
	 * @return {@link Constant#BATTERY}
	 */
	public Integer getBattery() {
		return this.get(Constant.BATTERY);
	}
	
	/**
	 * @return {@link Constant#CHARGING}
	 */
	public Boolean getCharging() {
		return this.get(Constant.CHARGING);
	}

}
