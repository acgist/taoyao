package com.acgist.taoyao.signal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.protocol.client.ClientRebootProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
	private ClientManager clientManager;
	@Autowired
	private ClientRebootProtocol clientRebootProtocol;
	
	@Operation(summary = "终端列表", description = "终端列表")
	@GetMapping("/list")
	@ApiResponse(content = @Content(schema = @Schema(implementation = ClientStatus.class)))
	public Message list() {
		return Message.success(this.clientManager.status());
	}
	
	@Operation(summary = "终端状态", description = "终端状态")
	@GetMapping("/status/{sn}")
	@ApiResponse(content = @Content(schema = @Schema(implementation = ClientStatus.class)))
	public Message status(@PathVariable String sn) {
		return Message.success(this.clientManager.status(sn));
	}
	
	@Operation(summary = "重启终端", description = "重启终端")
	@GetMapping("/reboot/{sn}")
	public Message reboot(@PathVariable String sn) {
		this.clientManager.unicast(sn, this.clientRebootProtocol.build());
		return Message.success();
	}
	
}
