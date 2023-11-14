package com.acgist.taoyao.signal.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.party.room.RoomManager;
import com.acgist.taoyao.signal.party.room.RoomStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 房间
 * 
 * @author acgist
 */
@Tag(name = "房间", description = "房间管理")
@Validated
@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {
    
    private final RoomManager roomManager;
    
    @Operation(summary = "房间信息", description = "房间信息")
    @GetMapping("/log")
    public Message log() {
        this.roomManager.log();
        return Message.success();
    }
    
    @Operation(summary = "房间列表", description = "房间列表")
    @GetMapping("/list")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RoomStatus.class)))
    public Message list() {
        return Message.success(this.roomManager.getStatus());
    }

    @Operation(summary = "房间状态", description = "房间状态")
    @GetMapping("/status/{roomId}")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RoomStatus.class)))
    public Message status(@PathVariable String roomId) {
        return Message.success(this.roomManager.getStatus(roomId));
    }
    
    @Operation(summary = "房间终端列表", description = "房间终端列表")
    @GetMapping("/list/client/{roomId}")
    @ApiResponse(content = @Content(schema = @Schema(implementation = ClientStatus.class)))
    public Message listClient(@PathVariable String roomId) {
        final Room room = this.roomManager.getRoom(roomId);
        final List<ClientStatus> list = room == null ? List.of() : room.getClientStatus();
        return Message.success(list);
    }
    
}
