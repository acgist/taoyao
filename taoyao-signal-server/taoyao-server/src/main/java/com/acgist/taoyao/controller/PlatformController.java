package com.acgist.taoyao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.protocol.platform.PlatformRebootProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformShutdownProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 平台
 * 
 * @author acgist
 */
@Tag(name = "平台", description = "平台管理")
@Validated
@RestController
@RequestMapping("/platform")
public class PlatformController {
    
    private final PlatformRebootProtocol   platformRebootProtocol;
    private final PlatformShutdownProtocol platformShutdownProtocol;

    public PlatformController(
        @Autowired(required = false) PlatformRebootProtocol   platformRebootProtocol,
        @Autowired(required = false) PlatformShutdownProtocol platformShutdownProtocol
    ) {
        this.platformRebootProtocol   = platformRebootProtocol;
        this.platformShutdownProtocol = platformShutdownProtocol;
    }

    @Operation(summary = "重启平台", description = "重启平台")
    @GetMapping("/reboot")
    public Message platformReboot() {
        if(this.platformRebootProtocol == null) {
            return Message.fail(MessageCode.CODE_3406, "功能没有开启");
        }
        this.platformRebootProtocol.execute();
        return Message.success();
    }
    
    @Operation(summary = "关闭平台", description = "关闭平台")
    @GetMapping("/shutdown")
    public Message platformShutdown() {
        if(this.platformShutdownProtocol == null) {
            return Message.fail(MessageCode.CODE_3406, "功能没有开启");
        }
        this.platformShutdownProtocol.execute();
        return Message.success();
    }
    
}
