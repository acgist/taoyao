package com.acgist.taoyao.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 重写规则
 * 
 * @author acgist
 */
/**
 * WebRTC STUN配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "重写规则", description = "重写规则")
public class IpRewriteRuleProperties {

    @Schema(title = "网络号", description = "网络号：匹配终端IP")
    private String network;
    @Schema(title = "目标地址", description = "目标地址：没有配置等于网络号加上原始主机号")
    private String targetHost;
    
}
