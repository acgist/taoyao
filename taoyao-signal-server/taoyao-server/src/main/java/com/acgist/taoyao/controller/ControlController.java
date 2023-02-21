package com.acgist.taoyao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.protocol.media.MediaRebootProtocol;
import com.acgist.taoyao.signal.protocol.media.MediaShutdownProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformRebootProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformShutdownProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemRebootProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemShutdownProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 控制
 * 
 * @author acgist
 */
@Tag(name = "控制", description = "控制管理")
@RestController
@RequestMapping("/control")
public class ControlController {
    
    @Autowired
    private MediaRebootProtocol mediaRebootProtocol;
    @Autowired
    private MediaShutdownProtocol mediaShutdownProtocol;
    @Autowired
    private SystemRebootProtocol systemRebootProtocol;
    @Autowired
    private SystemShutdownProtocol systemShutdownProtocol;
    @Autowired
    private PlatformRebootProtocol platformRebootProtocol;
    @Autowired
    private PlatformShutdownProtocol platformShutdownProtocol;
    
    @Operation(summary = "重启媒体", description = "重启媒体")
    @GetMapping("/media/reboot/{mediaId}")
    public Message mediaReboot(@PathVariable String mediaId) {
        this.mediaRebootProtocol.execute(mediaId);
        return Message.success();
    }
    
    @Operation(summary = "关闭媒体", description = "关闭媒体")
    @GetMapping("/media/shutdown/{mediaId}")
    public Message mediaShutdown(@PathVariable String mediaId) {
        this.mediaShutdownProtocol.execute(mediaId);
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
