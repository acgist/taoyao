package com.acgist.taoyao.signal.party.media;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 房间管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class RoomManager {

	private final IdService idService;
	private final ClientManager clientManager;
	
	/**
	 * 房间列表
	 */
	private final List<Room> rooms;
	
	public RoomManager(IdService idService, ClientManager clientManager) {
        this.idService = idService;
        this.clientManager = clientManager;
        this.rooms = new CopyOnWriteArrayList<>();
    }
	
    @Scheduled(cron = "${taoyao.scheduled.room:0 0/5 * * * ?}")
    public void scheduled() {
        this.releaseUnknowClient();
    }

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
		return room == null ? null : room.getRoomStatus();
	}
	
	/**
	 * @return 所有房间状态列表
	 */
	public List<RoomStatus> status() {
		return this.rooms().stream()
			.map(Room::getRoomStatus)
			.toList();
	}

	/**
	 * 重建房间
	 * 
	 * @param mediaClient 媒体服务终端
	 * @param message 消息
	 */
	public void recreate(Client mediaClient, Message message) {
	    this.rooms.stream()
	    .filter(room -> mediaClient.getClientId().equals(room.getMediaClient().getClientId()))
	    .forEach(room -> {
	        log.info("重建房间：{}", room.getRoomId());
	        final Message clone = message.cloneWithoutBody();
	        clone.getHeader().setId(this.idService.buildId());
	        clone.setBody(Map.of(Constant.ROOM_ID, room.getRoomId()));
	        // 异步发送防止线程卡死
	        mediaClient.push(clone);
	        // 同步需要添加异步注解
//	        mediaClient.request(clone);
	        // 更新媒体服务
	        room.setMediaClient(mediaClient);
	        // TODO：通知重建房间
	    });
	}

	/**
	 * 创建房间
	 * 
	 * @param name 名称
	 * @param password 密码
	 * @param mediaClientId 媒体服务终端标识
	 * @param message 消息
	 * 
	 * @return 房间信息
	 */
	public Room create(String name, String password, String mediaClientId, Message message) {
		final Client mediaClient = this.clientManager.clients(mediaClientId);
		if(mediaClient == null) {
			throw MessageCodeException.of("无效媒体服务：" + mediaClientId);
		}
		final String roomId = this.idService.buildUuid();
		// 房间
		final Room room = new Room(mediaClient, this);
		room.setRoomId(roomId);
		room.setPassword(password);
		// 状态
		final RoomStatus roomStatus = room.getRoomStatus();
		roomStatus.setRoomId(roomId);
		roomStatus.setName(name);
		roomStatus.setMediaClientId(mediaClientId);
		roomStatus.setClientSize(0L);
		// 创建媒体服务房间
		message.setBody(Map.of(Constant.ROOM_ID, roomId));
		mediaClient.request(message);
		log.info("创建房间：{}-{}", roomId, name);
		this.rooms.add(room);
		return room;
	}

	/**
	 * 离开房间
	 * 
	 * @param client 终端
	 */
	public void leave(Client client) {
		this.rooms.forEach(v -> v.leave(client));
	}
	
	/**
	 * 删除房间
	 * 
	 * @param room 房间
	 */
	public void remove(Room room) {
	    this.rooms.remove(room);
	}
	
	/**
	 * 记录日志
	 */
	public void log() {
	    log.info("""
	        当前房间数量：{}""",
	        this.rooms.size()
	    );
	    this.rooms.forEach(Room::log);
	}

	/**
	 * 清理没有关联终端的资源
	 */
	private void releaseUnknowClient() {
	    this.rooms.forEach(Room::releaseUnknowClient);
	}
	
}
