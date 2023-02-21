package com.acgist.taoyao.signal.media;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.signal.client.Client;

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
	private MediaClientManager mediaClientManager;

	/**
	 * 房间列表
	 */
	private List<Room> rooms = new CopyOnWriteArrayList<>();
	
	/**
	 * @param roomId 房间标识
	 * 
	 * @return 房间
	 */
	public Room room(String roomId) {
		return this.rooms.stream()
			.filter(v -> Objects.equals(roomId, v.getRoomId()))
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * @return 所有房间列表
	 */
	public List<Room> rooms() {
		return this.rooms;
	}
	
	/**
	 * @param roomId 房间标识
	 * 
	 * @return 房间状态
	 */
	public RoomStatus status(String roomId) {
		final Room room = this.room(roomId);
		return room == null ? null : room.getStatus();
	}
	
	/**
	 * @return 所有房间状态列表
	 */
	public List<RoomStatus> status() {
		return this.rooms().stream()
			.map(Room::getStatus)
			.toList();
	}

	/**
	 * 重建房间
	 * 
	 * @param mediaClient 媒体服务终端
	 * @param message 消息
	 */
	public void recreate(MediaClient mediaClient, Message message) {
	    this.rooms.stream()
	    .filter(room -> room.getMediaClient() == mediaClient)
	    .forEach(room -> {
	        log.info("重建房间：{}", room.getRoomId());
	        final Message clone = message.cloneWithoutBody();
	        clone.getHeader().setId(this.idService.buildIdToString());
	        clone.setBody(Map.of(Constant.ROOM_ID, room.getRoomId()));
	        // 异步发送防止线程卡死
	        mediaClient.send(clone);
	        // 同步需要添加异步注解
//	        mediaClient.request(clone);
	    });
	}

	/**
	 * 创建房间
	 * 
	 * @param name 名称
	 * @param password 密码
	 * @param mediaId 媒体服务标识
	 * @param message 消息
	 * 
	 * @return 房间信息
	 */
	public Room create(String name, String password, String mediaId, Message message) {
		final MediaClient mediaClient = this.mediaClientManager.mediaClient(mediaId);
		if(mediaClient == null) {
			throw MessageCodeException.of("无效媒体服务：" + mediaId);
		}
		final String roomId = this.idService.buildIdToString();
		// 状态
		final RoomStatus roomStatus = new RoomStatus();
		roomStatus.setRoomId(roomId);
		roomStatus.setName(name);
		roomStatus.setMediaId(mediaId);
		roomStatus.setClientSize(0L);
		// 房间
		final Room room = new Room();
		room.setRoomId(roomId);
		room.setPassword(password);
		room.setStatus(roomStatus);
		room.setMediaClient(mediaClient);
		room.setClients(new CopyOnWriteArrayList<>());
		// 创建媒体服务房间
		message.setBody(Map.of(Constant.ROOM_ID, roomId));
		mediaClient.request(message);
		log.info("创建房间：{}-{}", roomId, name);
		this.rooms.add(room);
		return room;
	}
	
	/**
	 * 关闭房间
	 * 
	 * @param roomId 房间标识
	 */
	public void close(String roomId) {
		final Room room = this.room(roomId);
		if(room == null) {
			log.warn("关闭房间无效：{}", roomId);
			return;
		}
		if(this.rooms.remove(room)) {
			// TODO:媒体服务
		}
	}

	/**
	 * 离开房间
	 * 
	 * @param client 终端
	 */
	public void leave(Client client) {
		this.rooms.forEach(v -> v.leave(client));
	}
	
}
