package com.acgist.taoyao.boot.property;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 媒体配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "媒体配置", description = "媒体配置")
@ConfigurationProperties(prefix = "taoyao.media")
public class MediaProperties {
	
	/**
	 * 音频配置
	 */
	@Schema(title = "音频配置", description = "音频配置")
	private MediaAudioProperties audio;
	/**
	 * 视频配置
	 */
	@Schema(title = "视频配置", description = "视频配置")
	private MediaVideoProperties video;
	/**
	 * 4K视频
	 */
	@Schema(title = "4K视频", description = "4K视频")
	private MediaVideoProperties udVideo;
	/**
	 * 2K视频
	 */
	@Schema(title = "2K视频", description = "2K视频")
	private MediaVideoProperties qdVideo;
	/**
	 * 超清视频
	 */
	@Schema(title = "超清视频", description = "超清视频")
	private MediaVideoProperties fdVideo;
	/**
	 * 高清视频
	 */
	@Schema(title = "高清视频", description = "高清视频")
	private MediaVideoProperties hdVideo;
	/**
	 * 标清视频
	 */
	@Schema(title = "标清视频", description = "标清视频")
	private MediaVideoProperties sdVideo;
	/**
	 * 媒体服务配置
	 */
	@Schema(title = "媒体服务配置", description = "媒体服务配置")
	private List<MediaServerProperties> mediaServerList;
	/**
	 * 录像存放路径
	 */
	@Schema(title = "录像存放路径", description = "录像存放路径")
	@JsonIgnore
	private String recordStoragePath;

}
