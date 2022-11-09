package com.acgist.taoyao.meeting.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
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

	@Operation(summary = "进入房间", description = "进入房间，如果房间不存在时自动创建。")
	@GetMapping("/enter")
	public void enter(String roomId) {
	}
	
}
