package com.acgist.taoyao.media.live;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 直播
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "直播", description = "直播")
public class Live {
	
	/**
	 * 直播标识
	 */
	@Schema(title = "直播标识", description = "直播标识")
	private String id;
	/**
	 * 直播名称
	 */
	@Schema(title = "直播名称", description = "直播名称")
	private String name;
	/**
	 * 直播密码
	 */
	@Schema(title = "直播密码", description = "直播密码")
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
