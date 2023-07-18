package com.acgist.taoyao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.protocol.system.SystemRebootProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemShutdownProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 系统
 * 
 * @author acgist
 */
@Tag(name = "系统", description = "系统管理")
@Validated
@RestController
@RequestMapping("/system")
public class SystemController {
    
    private final SystemRebootProtocol   systemRebootProtocol;
    private final SystemShutdownProtocol systemShutdownProtocol;
    
    public SystemController(
        @Autowired(required = false) SystemRebootProtocol   systemRebootProtocol,
        @Autowired(required = false) SystemShutdownProtocol systemShutdownProtocol
    ) {
        this.systemRebootProtocol   = systemRebootProtocol;
        this.systemShutdownProtocol = systemShutdownProtocol;
    }
    
    @Operation(summary = "重启系统", description = "重启系统")
    @GetMapping("/reboot")
    public Message systemReboot() {
        if(this.systemRebootProtocol == null) {
            return Message.fail(MessageCode.CODE_3406, "功能没有开启");
        }
        this.systemRebootProtocol.execute();
        return Message.success();
    }
    
    @Operation(summary = "关闭系统", description = "关闭系统")
    @GetMapping("/shutdown")
    public Message systemShutdown() {
        if(this.systemShutdownProtocol == null) {
            return Message.fail(MessageCode.CODE_3406, "功能没有开启");
        }
        this.systemShutdownProtocol.execute();
        return Message.success();
    }

}
