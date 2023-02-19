package com.acgist.taoyao.boot.property;

import java.util.LinkedHashMap;
import java.util.Map;

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
	@Schema(title = "宽度", description = "宽度", example = "{ min: 720, ideal: 1280, max: 4096 }")
	private Map<String, Object> width;
	@Schema(title = "高度", description = "高度", example = "{ min: 480, ideal: 720, max: 2160 }")
	private Map<String, Object> height;

	public Map<String, Object> getWidth() {
		if(this.width == null) {
		    final int index = this.resolution.indexOf('*');
		    this.width = new LinkedHashMap<>();
			this.width.put(Constant.MIN, Constant.MIN_WIDTH);
			this.width.put(Constant.IDEAL, Integer.valueOf(this.resolution.substring(0, index).strip()));
			this.width.put(Constant.MAX, Constant.MAX_WIDTH);
		}
		return this.width;
	}
	
	public Map<String, Object> getHeight() {
		if(this.height == null) {
			final int index = this.resolution.indexOf('*');
			this.height = new LinkedHashMap<>();
			this.height.put(Constant.MIN, Constant.MIN_HEIGHT);
			this.height.put(Constant.IDEAL, Integer.valueOf(this.resolution.substring(index + 1).strip()));
			this.height.put(Constant.MAX, Constant.MAX_HEIGHT);
		}
		return this.height;
	}

}
