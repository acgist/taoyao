package com.acgist.taoyao.signal.party.media;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.room.RoomLeaveEvent;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 房间
 * 房间和媒体路由一对一关联
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class Room implements Closeable {
	
    /**
     * 是否关闭
     */
    private volatile boolean close = false;
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
	 * @param mediaClient 媒体服务
	 */
	public Room(Client mediaClient, RoomManager roomManager) {
	    this.roomStatus = new RoomStatus();
	    this.mediaClient = mediaClient;
	    this.roomManager = roomManager;
	    this.clients = new ConcurrentHashMap<>();
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
     * 广播消息
     * 所有终端以及媒体服务
     * 
     * @param message 消息
     */
    public void broadcastAll(Message message) {
        this.broadcast(message);
        this.mediaClient.push(message);
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
	 * @param producerId 生产者ID
	 * 
	 * @return 生产者
	 */
	public Producer producer(String producerId) {
	    return this.clients.values().stream()
	        .map(wrapper -> wrapper.getProducers().get(producerId))
	        .filter(Objects::nonNull)
	        .findFirst()
	        .orElse(null);
	}
	
	/**
	 * @param consumerId 消费者ID
	 * 
	 * @return 消费者
	 */
	public Consumer consumer(String consumerId) {
	    return this.clients.values().stream()
	        .map(wrapper -> wrapper.getConsumers().get(consumerId))
	        .filter(Objects::nonNull)
	        .findFirst()
	        .orElse(null);
	}
	
	@Override
	public void close() {
	    if(this.close) {
	        return;
	    }
	    this.close = true;
		log.info("关闭房间：{}", this.roomId);
		// TODO：关闭房间
		// TODO：媒体服务：直接没提服务关闭所有资源（通道、生产者、消费者）
		this.roomManager.remove(this);
	}
	
}
