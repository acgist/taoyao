package com.acgist.taoyao.signal.config.camera;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 水印配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "水印配置", description = "水印配置")
public class WatermarkProperties {

    @Schema(title = "是否开启", description = "是否开启")
    private Boolean enabled;
    @Schema(title = "水印内容", description = "水印内容")
    private String text;
    @Schema(title = "X坐标", description = "X坐标")
    private Integer posx;
    @Schema(title = "Y坐标", description = "Y坐标")
    private Integer posy;
    @Schema(title = "透明度", description = "透明度")
    private Double opacity;
    
}
