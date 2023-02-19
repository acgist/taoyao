package com.acgist.taoyao.signal.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	 * 终端
	 */
	private final Client client;
	private String device;
	private String produce;
	private String consume;
	private Object rtpCapabilities;
	private Object sctpCapabilities;
	private Map<String, Transport> transports;
	private Map<String, Transport> producers;
	private Map<String, Transport> consumers;
	private Map<String, Transport> dataProducers;
	private Map<String, Transport> dataConsumers;
	
    public Peer(Client client) {
        this.client = client;
        this.transports = new ConcurrentHashMap<>();
        this.producers = new ConcurrentHashMap<>();
        this.consumers = new ConcurrentHashMap<>();
        this.dataProducers = new ConcurrentHashMap<>();
        this.dataConsumers = new ConcurrentHashMap<>();
    }
	
}
