package com.acgist.taoyao.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ID配置
 * 
 * 服务端：日期 + 两位机器序号 + 六位索引
 * 范围：00000000~09999999
 * 并发：1000000 / S
 * 
 * 终端：日期 + 五位机器序号 + 三位索引
 * 范围：10000000~99999999
 * 并发：1000 / S
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "ID配置", description = "ID配置")
@ConfigurationProperties(prefix = "taoyao.id")
public class IdProperties {

    @Schema(title = "最大序号", description = "最大序号")
    private Integer maxIndex;
    @Schema(title = "机器序号", description = "机器序号")
    private Integer serverIndex;
    @Schema(title = "最小终端序号", description = "最小终端序号")
    private Integer minClientIndex;
    @Schema(title = "最大终端序号", description = "最大终端序号")
    private Integer maxClientIndex;

}
