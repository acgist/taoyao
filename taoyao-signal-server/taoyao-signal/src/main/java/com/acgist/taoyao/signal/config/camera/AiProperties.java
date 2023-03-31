package com.acgist.taoyao.signal.config.camera;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * AI识别配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "AI识别配置", description = "AI识别配置")
public class AiProperties {
    
    /**
     * 识别类型
     * 
     * @author acgist
     */
    public enum Type {
       
        // 人
        PERSON;
        
    }
    
    @Schema(title = "是否开启", description = "是否开启")
    @NotNull(message = "没有指定操作状态")
    private Boolean enabled;
    @Schema(title = "识别类型", description = "识别类型")
    private Type type;
    
}
