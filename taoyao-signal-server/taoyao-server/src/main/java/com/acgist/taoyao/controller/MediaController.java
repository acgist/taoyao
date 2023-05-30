package com.acgist.taoyao.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.protocol.media.MediaRecordProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * 媒体
 * 
 * @author acgist
 */
@Tag(name = "媒体", description = "媒体管理")
@Validated
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaRecordProtocol mediaRecordProtocol;
    
    @Operation(summary = "录像", description = "媒体录像")
    @GetMapping("/record/{roomId}/{clientId}")
    public Message record(@PathVariable String roomId, @PathVariable String clientId, @NotNull(message = "没有指定操作状态") Boolean enabled) {
        return this.mediaRecordProtocol.execute(roomId, clientId, enabled);
    }
    
}
