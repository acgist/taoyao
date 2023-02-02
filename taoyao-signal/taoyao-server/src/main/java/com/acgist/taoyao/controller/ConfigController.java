package com.acgist.taoyao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.MediaProperties;
import com.acgist.taoyao.boot.property.WebrtcProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 配置
 * 
 * @author acgist
 */
@Tag(name = "配置", description = "配置管理")
@RestController
@RequestMapping("/config")
public class ConfigController {

	@Autowired
	private MediaProperties mediaProperties;
	@Autowired
	private WebrtcProperties webrtcProperties;
	
	@Operation(summary = "媒体配置", description = "媒体配置")
	@GetMapping("/media")
	@ApiResponse(content = @Content(schema = @Schema(implementation = MediaProperties.class)))
	public Message media() {
		return Message.success(this.mediaProperties);
	}
	
	@Operation(summary = "WebRTC配置", description = "WebRTC配置")
	@GetMapping("/webrtc")
	@ApiResponse(content = @Content(schema = @Schema(implementation = WebrtcProperties.class)))
	public Message webrtc() {
		return Message.success(this.webrtcProperties);
	}
	
}
