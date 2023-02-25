package com.acgist.taoyao.signal.terminal.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
public class ClientWrapper {

    /**
     * 订阅类型
     * 如果需要订阅指定终端需要调用媒体消费信令
     * 
     * @author acgist
     */
    public enum SubscribeType {
        
        // 订阅所有媒体
        ALL,
        // 订阅所有音频媒体
        ALL_AUDIO,
        // 订阅所有视频媒体
        ALL_VIDEO,
        // 没有订阅任何媒体
        NONE;
        
    }

    /**
     * 房间
     */
    private final Room room;
	/**
	 * 终端
	 */
	private final Client client;
	/**
	 * 房间标识
	 */
	private final String roomId;
	/**
	 * 终端标识
	 */
	private final String clientId;
	private Object rtpCapabilities;
	private Object sctpCapabilities;
	/**
	 * 发送通道
	 */
	private Transport sendTransport;
	/**
	 * 接收通道
	 */
	private Transport recvTransport;
	/**
	 * 生产者
	 */
	private final Map<String, Producer> producers;
	/**
	 * 数据通道生产者
	 */
	private final Map<String, DataProducer> dataProducers;
	
    public ClientWrapper(Room room, Client client) {
        this.room = room;
        this.client = client;
        this.roomId = room.getRoomId();
        this.clientId = client.clientId();
        this.producers = new ConcurrentHashMap<>();
        this.dataProducers = new ConcurrentHashMap<>();
    }
	
    /**
     * @return 生产者数量
     */
    public Integer producerSize() {
        return this.producers.size();
    }
    
    /**
     * @return 消费者数量
     */
    public Integer consumerSize() {
        return this.producers.values().stream()
            .map(producer -> producer.getConsumers().size())
            .collect(Collectors.counting())
            .intValue();
    }
    
}
