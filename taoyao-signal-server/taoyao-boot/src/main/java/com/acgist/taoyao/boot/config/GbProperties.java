package com.acgist.taoyao.boot.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.acgist.taoyao.boot.service.GbURI;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * GB配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "GB配置", description = "GB配置")
@ConfigurationProperties(prefix = "taoyao.gb")
public class GbProperties implements GbURI {

    public static final record Server(String transport, String host, Integer port, String domainId, String deviceId, String username, String password) { }

    @Schema(title = "监听地址", description = "监听地址")
    private String listen = "0.0.0.0";
    @Schema(title = "通信协议", description = "通信协议")
    private String transport = "UDP";
    @Schema(title = "主机地址", description = "主机地址")
    private String host = "192.168.1.100";
    @Schema(title = "监听端口", description = "监听端口")
    private Integer port = 5060;
    @Schema(title = "域名ID", description = "域名ID")
    private String domainId = "4401121300";
    @Schema(title = "设备ID", description = "设备ID")
    private String deviceId = "44011213000000000001";
    @Schema(title = "用户账号", description = "用户账号")
    private String username = "admin";
    @Schema(title = "用户密码", description = "用户密码")
    private String password = "123456";
    @Schema(title = "服务名称", description = "服务名称")
    private String name = "GB28181-sip";
    @Schema(title = "过期时间", description = "过期时间")
    private Integer expires = 3600;
    @Schema(title = "超时时间", description = "超时时间")
    private Integer timeout = 120;
    @Schema(title = "依赖路径", description = "依赖路径")
    private String jniLib = "taoyao-gb-media-server";
    @Schema(title = "上级服务", description = "上级服务")
    private List<Server> server = new ArrayList<>();

}
