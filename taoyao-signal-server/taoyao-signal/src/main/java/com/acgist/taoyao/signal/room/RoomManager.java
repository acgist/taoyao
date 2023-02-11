package com.acgist.taoyao.signal.room;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.mediasoup.MediasoupClient;
import com.acgist.taoyao.signal.mediasoup.MediasoupClientManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 房间管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class RoomManager {

	@Autowired
	private IdService idService;
	@Autowired
	private MediasoupClientManager mediasoupClientManager;

	/**
	 * 房间列表
	 */
	private List<Room> rooms = new CopyOnWriteArrayList<>();
	
	/**
	 * @param id ID
	 * 
	 * @return 房间信息
	 */
	public Room room(Long id) {
		return this.rooms.stream()
			.filter(v -> v.getId().equals(id))
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * @param id ID
	 * 
	 * @return 房间信息
	 */
	public RoomStatus status(Long id) {
		final Room room = this.room(id);
		return room == null ? null : room.getStatus();
	}
	
	/**
	 * @return 所有房间列表
	 */
	public List<Room> rooms() {
		return this.rooms;
	}
	
	/**
	 * @return 所有房间状态
	 */
	public List<RoomStatus> status() {
		return this.rooms().stream()
			.map(Room::getStatus)
			.toList();
	}

	/**
	 * 创建房间
	 * 
	 * @param sn 创建终端标识
	 * @param name 名称
	 * @param password 密码
	 * @param mediasoup 媒体服务名称
	 * 
	 * @return 房间信息
	 */
	public Room create(String sn, String name, String password, String mediasoup) {
		final MediasoupClient mediasoupClient = this.mediasoupClientManager.mediasoupClient(mediasoup);
		if(mediasoupClient == null) {
			throw MessageCodeException.of("无效媒体服务：" + mediasoup);
		}
		final Long id = this.idService.buildId();
		// 状态
		final RoomStatus roomStatus = new RoomStatus();
		roomStatus.setId(id);
		roomStatus.setName(name);
		roomStatus.setSnSize(0L);
		roomStatus.setMediasoup(mediasoup);
		// 房间
		final Room room = new Room();
		room.setId(id);
		room.setPassword(password);
		room.setStatus(roomStatus);
		room.setMediasoupClient(mediasoupClient);
		room.setClients(new CopyOnWriteArrayList<>());
		// 加入
		this.rooms.add(room);
		// TODO:媒体服务
		log.info("创建房间：{}-{}", id, name);
		return room;
	}
	
	/**
	 * 关闭房间
	 * 
	 * @param id ID
	 */
	public void close(Long id) {
		final Room room = this.room(id);
		if(room == null) {
			log.warn("房间无效：{}", id);
			return;
		}
		if(this.rooms.remove(room)) {
			// TODO:媒体服务
		}
	}

	/**
	 * 释放房间
	 * 
	 * @param client 终端
	 */
	public void leave(Client client) {
		this.rooms.forEach(v -> v.leave(client));
	}
	
}
