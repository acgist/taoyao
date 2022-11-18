package com.acgist.taoyao.meeting;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 会议
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "会议", description = "会议")
public class Meeting {
	
	/**
	 * 会议标识
	 */
	@Schema(title = "会议标识", description = "会议标识")
	private String id;
	/**
	 * 终端会话标识列表
	 */
	@Schema(title = "终端会话标识列表", description = "终端会话标识列表")
	private List<String> sns;

}
