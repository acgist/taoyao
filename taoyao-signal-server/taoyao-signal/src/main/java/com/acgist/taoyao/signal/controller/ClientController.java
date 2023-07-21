package com.acgist.taoyao.signal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.protocol.client.ClientRebootProtocol;
import com.acgist.taoyao.signal.protocol.client.ClientShutdownProtocol;
import com.acgist.taoyao.signal.protocol.client.ClientWakeupProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 终端
 * 
 * @author acgist
 */
@Tag(name = "终端", description = "终端管理")
@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientManager          clientManager;
    private final ClientWakeupProtocol   clientWakeupProtocol;
    private final ClientRebootProtocol   clientRebootProtocol;
    private final ClientShutdownProtocol clientShutdownProtocol;
    
    @Operation(summary = "终端列表", description = "终端列表")
    @GetMapping("/list")
    @ApiResponse(content = @Content(schema = @Schema(implementation = ClientStatus.class)))
    public Message list() {
        return Message.success(this.clientManager.status());
    }
    
    @Operation(summary = "终端状态", description = "终端状态")
    @GetMapping("/status/{clientId}")
    @ApiResponse(content = @Content(schema = @Schema(implementation = ClientStatus.class)))
    public Message status(@PathVariable String clientId) {
        return Message.success(this.clientManager.status(clientId));
    }
    
    @Operation(summary = "唤醒终端", description = "唤醒终端")
    @GetMapping("/wakeup/{clientId}")
    public Message wakeup(@PathVariable String clientId) {
        this.clientWakeupProtocol.execute(clientId);
        return Message.success();
    }
    
    @Operation(summary = "重启终端", description = "重启终端")
    @GetMapping("/reboot/{clientId}")
    public Message reboot(@PathVariable String clientId) {
        this.clientRebootProtocol.execute(clientId);
        return Message.success();
    }

    @Operation(summary = "关闭终端", description = "关闭终端")
    @GetMapping("/shutdown/{clientId}")
    public Message shutdown(@PathVariable String clientId) {
        this.clientShutdownProtocol.execute(clientId);
        return Message.success();
    }
    
}
