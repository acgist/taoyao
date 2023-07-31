package com.acgist.taoyao.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.config.MediaAudioProperties;
import com.acgist.taoyao.boot.config.MediaVideoProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.protocol.control.IControlBellProtocol;
import com.acgist.taoyao.signal.protocol.control.IControlClientRecordProtocol;
import com.acgist.taoyao.signal.protocol.control.IControlConfigAudioProtocol;
import com.acgist.taoyao.signal.protocol.control.IControlConfigVideoProtocol;
import com.acgist.taoyao.signal.protocol.control.IControlPhotographProtocol;
import com.acgist.taoyao.signal.protocol.control.IControlServerRecordProtocol;
import com.acgist.taoyao.signal.protocol.control.IControlWakeupProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * 控制
 * 
 * @author acgist
 */
@Tag(name = "控制", description = "控制管理")
@Validated
@RestController
@RequestMapping("/control")
@RequiredArgsConstructor
public class ControlController {
    
    private final IControlBellProtocol         controlBellProtocol;
    private final IControlWakeupProtocol       controlWakeupProtocol;
    private final IControlPhotographProtocol   controlPhotographProtocol;
    private final IControlConfigAudioProtocol  controlConfigAudioProtocol;
    private final IControlConfigVideoProtocol  controlConfigVideoProtocol;
    private final IControlClientRecordProtocol controlClientRecordProtocol;
    private final IControlServerRecordProtocol controlServerRecordProtocol;
    
    @Operation(summary = "响铃", description = "响铃控制")
    @GetMapping("/bell/{clientId}")
    public Message bell(
        @PathVariable String clientId,
        @NotNull(message = "没有指定操作状态") Boolean enabled
    ) {
        return this.controlBellProtocol.execute(clientId, enabled);
    }
    
    @Operation(summary = "拍照", description = "拍照控制")
    @GetMapping("/photograph/{clientId}")
    public Message photograph(@PathVariable String clientId) {
        return this.controlPhotographProtocol.execute(clientId);
    }
    
    @Operation(summary = "配置音频", description = "配置音频")
    @GetMapping("/config/audio/{clientId}")
    public Message configAudio(
        @PathVariable String clientId,
        @Valid @RequestBody MediaAudioProperties mediaAudioProperties
    ) {
        return this.controlConfigAudioProtocol.execute(clientId, mediaAudioProperties);
    }
    
    @Operation(summary = "配置视频", description = "配置视频")
    @GetMapping("/config/video/{clientId}")
    public Message configVideo(
        @PathVariable String clientId,
        @Valid @RequestBody MediaVideoProperties mediaVideoProperties
    ) {
        return this.controlConfigVideoProtocol.execute(clientId, mediaVideoProperties);
    }
    
    @Operation(summary = "录像", description = "终端录像控制")
    @GetMapping("/client/record/{clientId}")
    public Message record(
        @PathVariable String clientId,
        @NotNull(message = "没有指定操作状态") Boolean enabled
    ) {
        return this.controlClientRecordProtocol.execute(clientId, enabled);
    }
    
    @Operation(summary = "录像", description = "服务端录像控制")
    @GetMapping("/server/record/{roomId}/{clientId}")
    public Message record(
        @PathVariable String roomId,
        @PathVariable String clientId,
        @NotNull(message = "没有指定操作状态") Boolean enabled
    ) {
        return this.controlServerRecordProtocol.execute(roomId, clientId, enabled);
    }
    
    @Operation(summary = "唤醒终端", description = "唤醒终端")
    @GetMapping("/wakeup/{clientId}")
    public Message wakeup(@PathVariable String clientId) {
        return this.controlWakeupProtocol.execute(clientId);
    }
    
}
