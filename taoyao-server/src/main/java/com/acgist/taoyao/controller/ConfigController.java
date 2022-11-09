package com.acgist.taoyao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.config.WebrtcProperties;

import io.swagger.v3.oas.annotations.Operation;
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
	private WebrtcProperties webrtcProperties;
	
	@Operation(summary = "WebRTC配置", description = "WebRTC配置")
	@GetMapping("/webrtc")
	public WebrtcProperties webrtc() {
		return this.webrtcProperties;
	}
	
}
