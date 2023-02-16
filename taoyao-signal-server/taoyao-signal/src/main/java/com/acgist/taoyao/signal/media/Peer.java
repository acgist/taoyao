package com.acgist.taoyao.signal.media;

import java.util.Map;

import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * Peer
 * 
 * @author acgist
 */
@Getter
@Setter
public class Peer {

	/**
	 * 终端会话
	 */
	private Client client;
	private String device;
	private String produce;
	private String consume;
	private String rtpCapabilities;
	private String sctpCapabilities;
	private Map<String, Transport> transports;
	private Map<String, Transport> producers;
	private Map<String, Transport> consumers;
	private Map<String, Transport> dataProducers;
	private Map<String, Transport> dataConsumers;
	
}
