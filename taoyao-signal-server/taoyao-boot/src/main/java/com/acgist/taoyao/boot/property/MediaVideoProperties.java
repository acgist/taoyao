package com.acgist.taoyao.boot.property;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 视频配置
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
	@Schema(title = "帧率", description = "帧率影响流畅", example = "20|24|30|60")
	private Integer frameRate;
	@Schema(title = "分辨率", description = "分辨率影响画面大小", example = "1920*1080|1280*720")
	private String resolution;
	@Schema(title = "宽度", description = "宽度")
	private Integer width;
	@Schema(title = "高度", description = "高度")
	private Integer height;

	public Integer getWidth() {
		if(this.width == null) {
			final int index = this.resolution.indexOf('*');
			this.width = Integer.valueOf(this.resolution.substring(0, index).strip());
		}
		return this.width;
	}
	
	public Integer getHeight() {
		if(this.height == null) {
			final int index = this.resolution.indexOf('*');
			this.height = Integer.valueOf(this.resolution.substring(index + 1).strip());
		}
		return this.height;
	}

}
