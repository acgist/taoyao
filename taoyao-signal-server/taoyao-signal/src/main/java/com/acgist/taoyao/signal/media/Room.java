package com.acgist.taoyao.signal.media;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 房间
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class Room implements Closeable {
	
	/**
	 * ID
	 */
	private Long roomId;
	/**
	 * 密码
	 */
	private String password;
	/**
	 * 状态
	 */
	private RoomStatus status;
	/**
	 * 媒体服务
	 */
	private MediaClient mediaClient;
	/**
	 * 终端列表
	 */
	private List<Client> clients;
	/**
	 * 传输通道列表
	 */
	private List<Transport> transports;
	
	/**
	 * @return 终端状态列表
	 */
	public List<ClientStatus> clientStatus() {
		return this.clients.stream()
			.map(Client::status)
			.toList();
	}
	
	/**
	 * 终端进入
	 * 
	 * @param client 终端
	 */
	public void enter(Client client) {
		synchronized (this.clients) {
			if(this.clients.contains(client)) {
				return;
			}
			if(this.clients.add(client)) {
				this.status.setClientSize(this.status.getClientSize() + 1);
			}
		}
	}
	
	/**
	 * 终端离开
	 * 
	 * @param client 终端
	 */
	public void leave(Client client) {
		synchronized (this.clients) {
			if(this.clients.remove(client)) {
				this.status.setClientSize(this.status.getClientSize() - 1);
			}
		}
	}
	
	/**
	 * 广播消息
	 * 
	 * @param message 消息
	 */
	public void broadcast(Message message) {
		this.clients.forEach(v -> v.push(message));
	}
	
	/**
	 * 广播消息
	 * 
	 * @param from 发送终端
	 * @param message 消息
	 */
	public void broadcast(String from, Message message) {
		this.clients.stream()
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
		this.clients.stream()
		.filter(v -> v != from)
		.forEach(v -> v.push(message));
	}
	
	/**
	 * @see MediaClient#send(Message)
	 */
	public void send(Message message) {
		this.mediaClient.send(message);
	}
	
	/**
	 * @see MediaClient#sendSync(Message)
	 */
	public Message sendSync(Message message) {
		return this.mediaClient.sendSync(message);
	}
	
	@Override
	public void close() {
		log.info("关闭房间：{}", this.roomId);
		this.mediaClient.send(null);
	}
	
}
