package com.acgist.taoyao.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 视频配置
 * 原始数据 = 宽 * 高 * 3 / 2 * 8 * 帧率 / 1024 / 1024
 * 视频编码 = 压缩
 * 8     = 颜色位数  
 * 3 / 2 = YUV | RGB
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "视频配置", description = "视频配置")
public class MediaVideoProperties {

	/**
	 * 视频格式
	 * 
	 * @author acgist
	 */
	public enum Format {
		
		VP8,
		VP9,
		H264,
		H265;
		
	}
	
	@Schema(title = "格式", description = "格式", example = "VP8|VP9|H264|H265")
	private Format format;
	@Schema(title = "码率", description = "码率影响画质", example = "600|1200|1500|1800")
	private Integer bitrate;
	@Schema(title = "帧率", description = "帧率影响流畅", example = "15|18|20|24|30|45")
	private Integer frameRate;
	@Schema(title = "分辨率", description = "分辨率影响画面大小", example = "4096*2160|2560*1440|1920*1080|1280*720|720*480")
	private String resolution;
    @Schema(title = "宽度", description = "宽度", example = "4096|2560|1920|1280|720")
    private Integer width;
    @Schema(title = "高度", description = "高度", example = "2160|1440|1080|720|480")
    private Integer height;

    public Integer getWidth() {
        if (this.width == null) {
            final int index = this.resolution.indexOf('*');
            return this.width = Integer.valueOf(this.resolution.substring(0, index).strip());
        }
        return this.width;
    }

    public Integer getHeight() {
        if (this.height == null) {
            final int index = this.resolution.indexOf('*');
            return this.height = Integer.valueOf(this.resolution.substring(index + 1).strip());
        }
        return this.height;
    }

}
