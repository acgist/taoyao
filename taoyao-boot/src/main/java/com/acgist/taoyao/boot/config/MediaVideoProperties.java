package com.acgist.taoyao.boot.config;

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
		
		/**
		 * VP8
		 */
		VP8,
		/**
		 * VP9
		 */
		VP9,
		/**
		 * H264
		 */
		H264,
		/**
		 * H265
		 */
		H265;
		
	}
	
	/**
	 * 格式
	 */
	@Schema(title = "格式", description = "格式")
	private Format format;
	/**
	 * 码率（画质）
	 */
	@Schema(title = "码率", description = "码率影响画质", example = "600|1200|1500|1800")
	private Integer bitrate;
	/**
	 * 帧率（流畅）
	 */
	@Schema(title = "帧率", description = "帧率影响流程", example = "20|24|30|60")
	private Integer framerate;
	/**
	 * 分辨率（画面大小）
	 */
	@Schema(title = "分辨率", description = "分辨率影响画面大小", example = "1920*1080|1280*720|480*360")
	private String resolution;
	/**
	 * 宽度
	 */
	@Schema(title = "宽度", description = "宽度")
	private Integer width;
	/**
	 * 高度
	 */
	@Schema(title = "高度", description = "高度")
	private Integer height;

	/**
	 * @return 宽度
	 */
	public Integer getWidth() {
		if(this.width == null) {
			final int index = this.resolution.indexOf('*');
			this.width = Integer.valueOf(this.resolution.substring(0, index).strip());
		}
		return this.width;
	}
	
	/**
	 * @return 高度
	 */
	public Integer getHeight() {
		if(this.height == null) {
			final int index = this.resolution.indexOf('*');
			this.height = Integer.valueOf(this.resolution.substring(index + 1).strip());
		}
		return this.height;
	}

}
