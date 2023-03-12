package com.acgist.taoyao.signal.party.media;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.room.RoomCloseEvent;
import com.acgist.taoyao.signal.event.room.RoomLeaveEvent;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 房间
 * 房间和媒体路由一对一关联
 * 房间媒体媒体路由规则：订阅类型 + 路由类型
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class Room extends OperatorAdapter {
	
	/**
	 * 房间标识
	 */
	private String roomId;
	/**
	 * 密码
	 * 设置密码之后进入房间需要验证密码
	 */
	private String password;
	/**
	 * 状态
	 */
	private final RoomStatus roomStatus;
	/**
	 * 媒体服务
	 */
	private Client mediaClient;
	/**
	 * 房间管理
	 */
	private final RoomManager roomManager;
	/**
	 * 终端
	 */
	private final Map<Client, ClientWrapper> clients;
	/**
	 * 通道
	 */
	private final Map<String, Transport> transports;
    /**
     * 生产者
     */
    private final Map<String, Producer> producers;
    /**
     * 消费者
     */
    private final Map<String, Consumer> consumers;
    /**
     * 数据生产者
     */
    private final Map<String, DataProducer> dataProducers;
    /**
     * 数据消费者
     */
    private final Map<String, DataConsumer> dataConsumers;
	
	/**
	 * @param mediaClient 媒体服务
	 */
	public Room(Client mediaClient, RoomManager roomManager) {
	    this.roomStatus = new RoomStatus();
	    this.mediaClient = mediaClient;
	    this.roomManager = roomManager;
	    this.clients = new ConcurrentHashMap<>();
	    this.transports = new ConcurrentHashMap<>();
	    this.producers = new ConcurrentHashMap<>();
	    this.consumers = new ConcurrentHashMap<>();
	    this.dataProducers = new ConcurrentHashMap<>();
	    this.dataConsumers = new ConcurrentHashMap<>();
	}
	
	/**
	 * @param client 终端
	 * 
	 * @return 是否授权
	 */
	public boolean authenticate(Client client) {
	    return
	        this.mediaClient == client ||
	        this.clients.containsKey(client);
	}
	
	/**
	 * @return 终端状态列表
	 */
	public List<ClientStatus> clientStatus() {
		return this.clients.keySet().stream()
			.map(Client::status)
			.toList();
	}
	
	/**
	 * 终端进入
	 * 
	 * @param client 终端
	 * 
	 * @return 终端封装器
	 */
	public ClientWrapper enter(Client client) {
		synchronized (this.clients) {
		    ClientWrapper clientWrapper = this.clients.get(client);
			if(clientWrapper != null) {
				return clientWrapper;
			}
			log.info("终端进入房间：{} - {}", this.roomId, client.clientId());
			clientWrapper = new ClientWrapper(this, client);
			this.clients.put(client, clientWrapper);
			this.roomStatus.setClientSize(this.roomStatus.getClientSize() + 1);
			return clientWrapper;
		}
	}
	
	/**
	 * 终端离开
	 * 
	 * @param client 终端
	 */
	public void leave(Client client) {
		synchronized (this.clients) {
		    final ClientWrapper wrapper = this.clients.remove(client);
			if(wrapper != null) {
			    try {
                    wrapper.close();
                } catch (Exception e) {
                    log.error("关闭终端代理异常：{}", wrapper.getClientId(), e);
                }
			    this.roomStatus.setClientSize(this.roomStatus.getClientSize() - 1);
			    EventPublisher.publishEvent(new RoomLeaveEvent(this, client));
			}
		}
	}
	
	/**
	 * 媒体服务推送消息
	 * 
	 * @param message 消息
	 */
    public void push(Message message) {
        this.mediaClient.push(message);
    }
    
    /**
     * 媒体服务请求消息
     * 
     * @param message 消息
     * 
     * @return 响应
     */
    public Message request(Message message) {
        return this.mediaClient.request(message);
    }
    
    /**
     * 单播消息
     * 
     * @param to 接收终端
     * @param message 消息
     */
    public void unicast(String to, Message message) {
        this.clients.keySet().stream()
        .filter(v -> Objects.equals(to, v.clientId()))
        .forEach(v -> v.push(message));
    }
    
	/**
	 * 广播消息
	 * 
	 * @param message 消息
	 */
	public void broadcast(Message message) {
		this.clients.keySet().forEach(v -> v.push(message));
	}
	
	/**
	 * 广播消息
	 * 
	 * @param from 发送终端
	 * @param message 消息
	 */
	public void broadcast(String from, Message message) {
		this.clients.keySet().stream()
		.filter(v -> !Objects.equals(from, v.clientId()))
		.forEach(v -> v.push(message));
	}
	
	/**
	 * 广播消息
	 * 
	 * @param from 发送终端
	 * @param message 消息
	 */
	public void broadcast(Client from, Message message) {
		this.clients.keySet().stream()
		.filter(v -> v != from)
		.forEach(v -> v.push(message));
	}
	
	/**
	 * 
	 * @param client
	 * @return
	 */
	public ClientWrapper clientWrapper(Client client) {
	    return this.clients.get(client);
	}
	
	/**
	 * 
	 * @param client
	 * @return
	 */
	public ClientWrapper clientWrapper(String clientId) {
	    return this.clients.values().stream()
	        .filter(v -> clientId.equals(v.getClientId()))
	        .findFirst()
	        .orElse(null);
	}
	
	/**
	 * @param transportId 通道ID
	 * 
	 * @return 通道
	 */
	public Transport transport(String transportId) {
	    return this.transports.get(transportId);
	}
	
	/**
	 * @param producerId 生产者ID
	 * 
	 * @return 生产者
	 */
	public Producer producer(String producerId) {
	    return this.producers.get(producerId);
	}
	
	/**
	 * @param consumerId 消费者ID
	 * 
	 * @return 消费者
	 */
	public Consumer consumer(String consumerId) {
	    return this.consumers.get(consumerId);
	}
	
	/**
	 * @param producerId 数据生产者ID
	 * 
	 * @return 数据生产者
	 */
	public DataProducer dataProducer(String producerId) {
	    return this.dataProducers.get(producerId);
	}
	
	/**
	 * @param consumerId 数据消费者ID
	 * 
	 * @return 数据消费者
	 */
	public DataConsumer dataConsumer(String consumerId) {
	    return this.dataConsumers.get(consumerId);
	}
	
	@Override
	public void close() {
	    if(this.markClose()) {
	        return;
	    }
		log.info("关闭房间：{}", this.roomId);
		this.clients.values().forEach(ClientWrapper::close);
		EventPublisher.publishEvent(new RoomCloseEvent(this));
	}
	
	@Override
	public void remove() {
        log.info("移除房间：{}", this.roomId);
        this.roomManager.remove(this);
	}

	@Override
	public void log() {
	    log.info("""
	        当前房间：{}
	        终端数量：{}
	        通道数量：{}
	        消费者数量：{}
	        生产者数量：{}
	        数据消费者数量：{}
	        数据生产者数量：{}""",
	        this.roomId,
	        this.clients.size(),
	        this.transports.size(),
	        this.consumers.size(),
	        this.producers.size(),
	        this.dataConsumers.size(),
	        this.dataProducers.size()
	    );
	    this.clients.values().forEach(ClientWrapper::log);
	}

	/**
	 * 清理没有关联终端的资源
	 */
	public void releaseUnknowClient() {
	    this.transports.values().stream().filter(v -> !this.clients.containsKey(v.getClient())).forEach(Transport::close);
	    this.consumers.values().stream().filter(v -> !this.clients.containsValue(v.getConsumerClient())).forEach(Consumer::close);
	    this.producers.values().stream().filter(v -> !this.clients.containsValue(v.getProducerClient())).forEach(Producer::close);
	    this.dataConsumers.values().stream().filter(v -> !this.clients.containsValue(v.getConsumerClient())).forEach(DataConsumer::close);
	    this.dataProducers.values().stream().filter(v -> !this.clients.containsValue(v.getProducerClient())).forEach(DataProducer::close);
	}
	
}
