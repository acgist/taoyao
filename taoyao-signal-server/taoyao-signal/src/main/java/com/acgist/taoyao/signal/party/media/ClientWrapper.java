package com.acgist.taoyao.signal.party.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * 终端包装器
 * 
 * @author acgist
 */
@Getter
@Setter
public class ClientWrapper implements AutoCloseable {

    /**
     * 媒体订阅类型
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
        
        public static final SubscribeType of(String value) {
            for (SubscribeType type : SubscribeType.values()) {
                if(type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return SubscribeType.ALL;
        }
        
        public boolean consume(Producer producer) {
            return switch (this) {
            case NONE -> false;
            case ALL_AUDIO -> producer.getKind() == Kind.AUDIO;
            case ALL_VIDEO -> producer.getKind() == Kind.VIDEO;
            default -> true;
            };
        }
        
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
	/**
	 * 媒体订阅类型
	 * 指定订阅类型终端注册或者生成媒体后会自动进行媒体推流拉流
	 * 没有订阅任何媒体时需要用户自己对媒体进行消费控制
	 */
	private SubscribeType subscribeType;
	/**
	 * RTP协商
	 */
	private Object rtpCapabilities;
	/**
	 * SCTP协商
	 */
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
     * 消费者
     */
    private final Map<String, Consumer> consumers;
	/**
	 * 数据通道生产者
	 */
	private final Map<String, DataProducer> dataProducers;
	/**
	 * 数据通道消费者
	 */
	private final Map<String, DataProducer> dataConsumers;
	
    public ClientWrapper(Room room, Client client) {
        this.room = room;
        this.client = client;
        this.roomId = room.getRoomId();
        this.clientId = client.clientId();
        this.producers = new ConcurrentHashMap<>();
        this.consumers = new ConcurrentHashMap<>();
        this.dataProducers = new ConcurrentHashMap<>();
        this.dataConsumers = new ConcurrentHashMap<>();
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
    
    /**
     * @param producer 生产者
     * 
     * @return 是否已经消费
     */
    public boolean consume(Producer producer) {
        return this.consumers.values().stream()
            .anyMatch(v -> v.getProducer() == producer);
    }
    
    /**
     * 删除消费者
     * 
     * @param wrapper 消费者终端保证期
     */
    public void remove(ClientWrapper wrapper) {
        this.consumers.entrySet().stream()
        .filter(v -> v.getValue().getConsumeClient() == wrapper)
        .map(Map.Entry::getKey)
        .forEach(this.consumers::remove);
        // TODO：资源释放
        this.producers.values().forEach(v -> v.remove(wrapper));
    }
    
    @Override
    public void close() throws Exception {
        // TODO：释放资源
    }
    
}
