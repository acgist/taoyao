package com.acgist.taoyao.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 重写规则
 * 配置内网地址直接返回内网地址
 * 没有配置内网地址内网地址等于网络号加上原始主机号
 * 
 * 内网设备IP返回内网地址
 * 外网设备IP返回外网地址
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "重写规则", description = "重写规则")
public class RewriteRuleProperties {

    @Schema(title = "网络号", description = "网络号：匹配终端IP")
    private String network;
    @Schema(title = "内网地址", description = "内网地址")
    private String innerHost;
    @Schema(title = "外网地址", description = "外网地址")
    private String outerHost;

}
