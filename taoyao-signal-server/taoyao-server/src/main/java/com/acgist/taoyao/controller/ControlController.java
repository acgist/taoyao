package com.acgist.taoyao.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.config.camera.AiProperties;
import com.acgist.taoyao.signal.model.control.PtzControl;
import com.acgist.taoyao.signal.protocol.control.ControlAiProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlBellProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlPhotographProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlPtzProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlRecordProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 控制
 * 
 * @author acgist
 */
@Tag(name = "控制", description = "控制管理")
@Validated
@RestController
@RequestMapping("/control")
public class ControlController {
    
    private final ControlAiProtocol controlAiProtocol;
    private final ControlPtzProtocol controlPtzProtocol;
    private final ControlBellProtocol controlBellProtocol;
    private final ControlRecordProtocol controlRecordProtocol;
    private final ControlPhotographProtocol controlPhotographProtocol;
    
    public ControlController(
        ControlAiProtocol controlAiProtocol,
        ControlPtzProtocol controlPtzProtocol,
        ControlBellProtocol controlBellProtocol,
        ControlRecordProtocol controlRecordProtocol,
        ControlPhotographProtocol controlPhotographProtocol
    ) {
        this.controlAiProtocol = controlAiProtocol;
        this.controlPtzProtocol = controlPtzProtocol;
        this.controlBellProtocol = controlBellProtocol;
        this.controlRecordProtocol = controlRecordProtocol;
        this.controlPhotographProtocol = controlPhotographProtocol;
    }

    @Operation(summary = "AI识别", description = "AI识别")
    @GetMapping("/ai/{clientId}")
    public Message ai(@PathVariable String clientId, @Valid AiProperties aiProperties) {
        return Message.success(this.controlAiProtocol.execute(clientId, aiProperties));
    }
    
    @Operation(summary = "PTZ", description = "PTZ")
    @GetMapping("/ptz/{clientId}")
    public Message ptz(@PathVariable String clientId, @Valid PtzControl ptzControl) {
        return Message.success(this.controlPtzProtocol.execute(clientId, ptzControl));
    }
    
    @Operation(summary = "响铃", description = "响铃")
    @GetMapping("/bell/{clientId}")
    public Message bell(@PathVariable String clientId, @NotNull(message = "没有指定操作") Boolean active) {
        return Message.success(this.controlBellProtocol.execute(clientId, active));
    }
    
    @Operation(summary = "录像", description = "录像")
    @GetMapping("/record/{clientId}")
    public Message record(@PathVariable String clientId, @NotNull(message = "没有指定操作") Boolean active) {
        return Message.success(this.controlRecordProtocol.execute(clientId, active));
    }
    
    @Operation(summary = "拍照", description = "拍照")
    @GetMapping("/photograph/{clientId}")
    public Message photograph(@PathVariable String clientId) {
        return Message.success(this.controlPhotographProtocol.execute(clientId));
    }
    
}
