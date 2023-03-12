package com.acgist.taoyao.signal.model.control;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * PTZ控制参数
 * 
 * @author acgist
 */
@Schema(title = "PTZ控制参数", description = "PTZ控制参数")
public class PtzModel {

    /**
     * PTZ类型
     * 
     * @author acgist
     */
    public enum Type {
        
        // 水平
        PAN,
        // 垂直
        TILT,
        // 变焦
        ZOOM;
        
    }
    
    @Schema(title = "PTZ类型", description = "PTZ类型")
    @NotNull(message = "PTZ类型不能为空")
    private Type type;
    @Schema(title = "PTZ参数", description = "PTZ参数")
    @NotNull(message = "PTZ参数不能为空")
    private Double value;
    
}
