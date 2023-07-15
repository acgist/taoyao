package com.acgist.taoyao.boot.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 地址重写配置
 * 用于多网卡多子网复杂网络环境自动适配IP地址
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "地址重写", description = "地址重写")
@ConfigurationProperties(prefix = "taoyao.rewrite")
public class RewriteProperties {

    @Schema(title = "是否启用", description = "是否启用")
    private Boolean enabled;
    @Schema(title = "子网掩码", description = "子网掩码：主机号的长度")
    private Integer prefix;
    @Schema(title = "重写规则", description = "重写规则")
    private List<RewriteRuleProperties> rule;

}
