package com.acgist.taoyao.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.boot.config.MediaProperties;
import com.acgist.taoyao.boot.config.RewriteProperties;
import com.acgist.taoyao.boot.config.SocketProperties;
import com.acgist.taoyao.boot.config.WebrtcProperties;
import com.acgist.taoyao.boot.model.Message;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 配置
 * 
 * @author acgist
 */
@Tag(name = "配置", description = "配置管理")
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {
    
    private final MediaProperties   mediaProperties;
    private final FfmpegProperties  ffmpegProperties;
    private final SocketProperties  socketProperties;
    private final WebrtcProperties  webrtcProperties;
    private final RewriteProperties rewriteProperties;
    
    @Operation(summary = "媒体配置", description = "媒体配置")
    @GetMapping("/media")
    @ApiResponse(content = @Content(schema = @Schema(implementation = MediaProperties.class)))
    public Message media() {
        return Message.success(this.mediaProperties);
    }
    
    @Operation(summary = "FFmpeg配置", description = "FFmpeg配置")
    @GetMapping("/ffmpeg")
    @ApiResponse(content = @Content(schema = @Schema(implementation = FfmpegProperties.class)))
    public Message ffmpeg() {
        return Message.success(this.ffmpegProperties);
    }
    
    @Operation(summary = "Socket配置", description = "Socket配置")
    @GetMapping("/socket")
    @ApiResponse(content = @Content(schema = @Schema(implementation = SocketProperties.class)))
    public Message socket() {
        return Message.success(this.socketProperties);
    }
    
    @Operation(summary = "WebRTC配置", description = "WebRTC配置")
    @GetMapping("/webrtc")
    @ApiResponse(content = @Content(schema = @Schema(implementation = WebrtcProperties.class)))
    public Message webrtc() {
        return Message.success(this.webrtcProperties);
    }
    
    @Operation(summary = "地址重写配置", description = "地址重写配置")
    @GetMapping("/rewrite")
    @ApiResponse(content = @Content(schema = @Schema(implementation = WebrtcProperties.class)))
    public Message rewrite() {
        return Message.success(this.rewriteProperties);
    }
    
}
