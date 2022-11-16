package com.acgist.taoyao.live.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.live.Live;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 直播
 * 
 * @author acgist
 */
@Tag(name = "直播", description = "直播管理")
@RestController
@RequestMapping("/live")
public class LiveController {

	@Operation(summary = "直播列表", description = "直播列表")
	@GetMapping("/list")
	@ApiResponse(content = @Content(schema = @Schema(implementation = Live.class)))
	public Message list() {
		return Message.success();
	}
	
}
