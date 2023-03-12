package com.acgist.taoyao.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.config.MediaAudioProperties;
import com.acgist.taoyao.boot.config.MediaVideoProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.config.camera.AiProperties;
import com.acgist.taoyao.signal.config.camera.BeautyProperties;
import com.acgist.taoyao.signal.config.camera.WatermarkProperties;
import com.acgist.taoyao.signal.model.control.PtzModel;
import com.acgist.taoyao.signal.protocol.control.ControlAiProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlBeautyProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlBellProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlConfigAudioProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlConfigVideoProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlPhotographProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlPtzProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlRecordProtocol;
import com.acgist.taoyao.signal.protocol.control.ControlWatermarkProtocol;

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
    private final ControlBeautyProtocol controlBeautyProtocol;
    private final ControlRecordProtocol controlRecordProtocol;
    private final ControlWatermarkProtocol controlWatermarkProtocol;
    private final ControlPhotographProtocol controlPhotographProtocol;
    private final ControlConfigAudioProtocol controlConfigAudioProtocol;
    private final ControlConfigVideoProtocol controlConfigVideoProtocol;
    
    public ControlController(
        ControlAiProtocol controlAiProtocol,
        ControlPtzProtocol controlPtzProtocol,
        ControlBellProtocol controlBellProtocol,
        ControlBeautyProtocol controlBeautyProtocol,
        ControlRecordProtocol controlRecordProtocol,
        ControlWatermarkProtocol controlWatermarkProtocol,
        ControlPhotographProtocol controlPhotographProtocol,
        ControlConfigAudioProtocol controlConfigAudioProtocol,
        ControlConfigVideoProtocol controlConfigVideoProtocol
    ) {
        this.controlAiProtocol = controlAiProtocol;
        this.controlPtzProtocol = controlPtzProtocol;
        this.controlBellProtocol = controlBellProtocol;
        this.controlBeautyProtocol = controlBeautyProtocol;
        this.controlRecordProtocol = controlRecordProtocol;
        this.controlWatermarkProtocol = controlWatermarkProtocol;
        this.controlPhotographProtocol = controlPhotographProtocol;
        this.controlConfigAudioProtocol = controlConfigAudioProtocol;
        this.controlConfigVideoProtocol = controlConfigVideoProtocol;
    }

    @Operation(summary = "AI识别", description = "AI识别控制")
    @GetMapping("/ai/{clientId}")
    public Message ai(@PathVariable String clientId, @Valid AiProperties aiProperties) {
        return Message.success(this.controlAiProtocol.execute(clientId, aiProperties));
    }
    
    @Operation(summary = "PTZ", description = "PTZ控制")
    @GetMapping("/ptz/{clientId}")
    public Message ptz(@PathVariable String clientId, @Valid PtzModel ptzModel) {
        return Message.success(this.controlPtzProtocol.execute(clientId, ptzModel));
    }
    
    @Operation(summary = "响铃", description = "响铃控制")
    @GetMapping("/bell/{clientId}")
    public Message bell(@PathVariable String clientId, @NotNull(message = "没有指定操作状态") Boolean enabled) {
        return Message.success(this.controlBellProtocol.execute(clientId, enabled));
    }
    
    @Operation(summary = "美颜", description = "美颜控制")
    @GetMapping("/beauty/{clientId}")
    public Message beauty(@PathVariable String clientId, @Valid BeautyProperties beautyProperties) {
        return Message.success(this.controlBeautyProtocol.execute(clientId, beautyProperties));
    }
    
    @Operation(summary = "录像", description = "录像控制")
    @GetMapping("/record/{clientId}")
    public Message record(@PathVariable String clientId, @NotNull(message = "没有指定操作状态") Boolean enabled) {
        return Message.success(this.controlRecordProtocol.execute(clientId, enabled));
    }
    
    @Operation(summary = "水印", description = "水印控制")
    @GetMapping("/watermark/{clientId}")
    public Message watermark(@PathVariable String clientId, @Valid WatermarkProperties watermarkProperties) {
        return Message.success(this.controlWatermarkProtocol.execute(clientId, watermarkProperties));
    }
    
    @Operation(summary = "拍照", description = "拍照控制")
    @GetMapping("/photograph/{clientId}")
    public Message photograph(@PathVariable String clientId) {
        return Message.success(this.controlPhotographProtocol.execute(clientId));
    }
    
    @Operation(summary = "配置音频", description = "配置音频")
    @GetMapping("/config/audio/{clientId}")
    public Message configAudio(@PathVariable String clientId, @Valid MediaAudioProperties mediaAudioProperties) {
        return Message.success(this.controlConfigAudioProtocol.execute(clientId, mediaAudioProperties));
    }
    
    @Operation(summary = "配置视频", description = "配置视频")
    @GetMapping("/config/video/{clientId}")
    public Message configVideo(@PathVariable String clientId, @Valid MediaVideoProperties mediaVideoProperties) {
        return Message.success(this.controlConfigVideoProtocol.execute(clientId, mediaVideoProperties));
    }
    
}
