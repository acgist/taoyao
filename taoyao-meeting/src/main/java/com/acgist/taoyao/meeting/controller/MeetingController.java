package com.acgist.taoyao.meeting.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.meeting.Meeting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 会议
 * 
 * @author acgist
 */
@Tag(name = "会议", description = "会议管理")
@RestController
@RequestMapping("/meeting")
public class MeetingController {
	
	@Operation(summary = "会议列表", description = "会议列表")
	@GetMapping("/list")
	@ApiResponse(content = @Content(schema = @Schema(implementation = Meeting.class)))
	public Message list() {
		return Message.success();
	}

}
