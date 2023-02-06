package com.acgist.taoyao.signal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.room.Room;
import com.acgist.taoyao.signal.room.RoomManager;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 房间
 * 
 * @author acgist
 */
@Tag(name = "房间", description = "房间管理")
@RestController
@RequestMapping("/room")
public class RoomController {
	
	@Autowired
	private RoomManager roomManager;
	
	@Operation(summary = "房间列表", description = "房间列表")
	@GetMapping("/list")
	@ApiResponse(content = @Content(schema = @Schema(implementation = Room.class)))
	public Message list() {
		return Message.success(this.roomManager.roomList());
	}

	@Operation(summary = "房间状态", description = "房间状态")
	@GetMapping("/status/{id}")
	public Message status(@PathVariable String id) {
		return Message.success(this.roomManager.room(id));
	}
	
	@Operation(summary = "房间终端列表", description = "房间终端列表")
	@GetMapping("/list/client/{id}")
	public Message listClient(@PathVariable String id) {
		return Message.success(this.roomManager.snList(id));
	}
	
}
