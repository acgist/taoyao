package com.acgist.taoyao.signal.room;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.service.IdService;

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

	/**
	 * 房间列表
	 */
	private List<Room> roomList = new CopyOnWriteArrayList<>();
	
	/**
	 * @return 所有房间列表
	 */
	public List<Room> roomList() {
		return this.roomList;
	}
	
	/**
	 * @param id 房间标识
	 * 
	 * @return 房间信息
	 */
	public Room room(String id) {
		return this.roomList.stream()
			.filter(v -> v.getId().equals(id))
			.findFirst()
			.orElse(null);
	}

	/**
	 * @param id 房间标识
	 * 
	 * @return 房间所有终端标识
	 */
	public List<String> snList(String id) {
		final Room room = this.room(id);
		return room == null ? List.of() : room.getSnList();
	}

	/**
	 * 创建房间
	 * 
	 * @param sn 创建房间终端标识
	 * 
	 * @return 房间信息
	 */
	public Room create(String sn) {
		final Room room = new Room();
		room.setId(this.idService.buildIdToString());
		room.setSnList(new CopyOnWriteArrayList<>());
		room.setCreator(sn);
		room.addSn(sn);
		this.roomList.add(room);
		log.info("创建房间：{}", room.getId());
		return room;
	}
	
}
