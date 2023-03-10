package com.acgist.taoyao.signal.config.camera;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 美颜配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "美颜配置", description = "美颜配置")
public class BeautyProperties {

    @Schema(title = "是否开启", description = "是否开启")
    @NotNull(message = "没有指定操作状态")
    private Boolean enabled;
    @Schema(title = "美颜级别", description = "美颜级别")
    private Integer level;
    
}
