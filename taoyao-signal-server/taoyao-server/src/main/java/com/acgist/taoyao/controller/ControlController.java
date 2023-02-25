package com.acgist.taoyao.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.protocol.control.ControlBellProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlPhotographProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlPtzProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlRecordProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformRebootProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformShutdownProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemRebootProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemShutdownProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    
    private final ControlPtzProtocol controlPtzProtocol;
    private final ControlBellProtocol controlBellProtocol;
    private final SystemRebootProtocol systemRebootProtocol;
    private final ControlRecordProtocol controlRecordProtocol;
    private final SystemShutdownProtocol systemShutdownProtocol;
    private final PlatformRebootProtocol platformRebootProtocol;
    private final PlatformShutdownProtocol platformShutdownProtocol;
    private final ControlPhotographProtocol controlPhotographProtocol;
    
    public ControlController(
        ControlPtzProtocol controlPtzProtocol, ControlBellProtocol controlBellProtocol,
        SystemRebootProtocol systemRebootProtocol, ControlRecordProtocol controlRecordProtocol,
        SystemShutdownProtocol systemShutdownProtocol, PlatformRebootProtocol platformRebootProtocol,
        PlatformShutdownProtocol platformShutdownProtocol, ControlPhotographProtocol controlPhotographProtocol
    ) {
        this.controlPtzProtocol = controlPtzProtocol;
        this.controlBellProtocol = controlBellProtocol;
        this.systemRebootProtocol = systemRebootProtocol;
        this.controlRecordProtocol = controlRecordProtocol;
        this.systemShutdownProtocol = systemShutdownProtocol;
        this.platformRebootProtocol = platformRebootProtocol;
        this.platformShutdownProtocol = platformShutdownProtocol;
        this.controlPhotographProtocol = controlPhotographProtocol;
    }

    @Operation(summary = "PTZ", description = "PTZ")
    @GetMapping("/ptz/{clientId}")
    public Message ptz(
        @NotNull(message = "PTZ类型不能为空") ControlPtzProtocol.Type type,
        @NotNull(message = "PTZ参数不能为空") Double value,
        @PathVariable String clientId
    ) {
        this.controlPtzProtocol.execute(type, value, clientId);
        return Message.success();
    }
    
    @Operation(summary = "响铃", description = "响铃")
    @GetMapping("/bell/{clientId}")
    public Message bell(@PathVariable String clientId) {
        this.controlBellProtocol.execute(clientId);
        return Message.success();
    }
    
    @Operation(summary = "录像", description = "录像")
    @GetMapping("/record/{clientId}")
    public Message record(@PathVariable String clientId) {
        this.controlRecordProtocol.execute(clientId);
        return Message.success();
    }
    
    @Operation(summary = "拍照", description = "拍照")
    @GetMapping("/photograph/{clientId}")
    public Message photograph(@PathVariable String clientId) {
        this.controlPhotographProtocol.execute(clientId);
        return Message.success();
    }
    
    @Operation(summary = "重启系统", description = "重启系统")
    @GetMapping("/system/reboot")
    public Message systemReboot() {
        this.systemRebootProtocol.execute();
        return Message.success();
    }
    
    @Operation(summary = "关闭系统", description = "关闭系统")
    @GetMapping("/system/shutdown")
    public Message systemShutdown() {
        this.systemShutdownProtocol.execute();
        return Message.success();
    }
    
    @Operation(summary = "重启平台", description = "重启平台")
    @GetMapping("/platform/reboot")
    public Message platformReboot() {
        this.platformRebootProtocol.execute();
        return Message.success();
    }
    
    @Operation(summary = "关闭平台", description = "关闭平台")
    @GetMapping("/platform/shutdown")
    public Message platformShutdown() {
        this.platformShutdownProtocol.execute();
        return Message.success();
    }
    
}
