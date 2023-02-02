package com.acgist.taoyao.media.meeting;

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
	 * 会议名称
	 */
	@Schema(title = "会议名称", description = "会议名称")
	private String name;
	/**
	 * 会议密码
	 */
	@Schema(title = "会议密码", description = "会议密码")
	private String password;
	/**
	 * 终端会话标识列表
	 */
	@Schema(title = "终端会话标识列表", description = "终端会话标识列表")
	private List<String> sns;
	/**
	 * 创建终端标识
	 */
	@Schema(title = "创建终端标识", description = "创建终端标识")
	private String creator;
	
	/**
	 * 新增终端会话标识
	 * 
	 * @param sn 终端会话标识
	 */
	public void addSn(String sn) {
		synchronized (this.sns) {
			if(this.sns.contains(sn)) {
				return;
			}
			this.sns.add(sn);
		}
	}

}
