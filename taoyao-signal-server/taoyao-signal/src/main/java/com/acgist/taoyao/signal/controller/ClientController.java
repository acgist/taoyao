package com.acgist.taoyao.signal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSessionManager;
import com.acgist.taoyao.signal.protocol.client.ClientRebootProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 终端
 * 
 * @author acgist
 */
@Tag(name = "终端", description = "终端管理")
@RestController
@RequestMapping("/client")
public class ClientController {

	@Autowired
	private ClientSessionManager clientSessionManager;
	@Autowired
	private ClientRebootProtocol clientRebootProtocol;
	
	@Operation(summary = "终端列表", description = "终端列表")
	@GetMapping("/list")
	public Message list() {
		return Message.success(this.clientSessionManager.status());
	}
	
	@Operation(summary = "终端状态", description = "终端状态")
	@GetMapping("/status/{sn}")
	public Message status(@PathVariable String sn) {
		return Message.success(this.clientSessionManager.status(sn));
	}
	
	@Operation(summary = "重启终端", description = "重启终端")
	@GetMapping("/reboot/{sn}")
	public Message reboot(@PathVariable String sn) {
		this.clientSessionManager.unicast(sn, this.clientRebootProtocol.build());
		return Message.success();
	}
	
}
