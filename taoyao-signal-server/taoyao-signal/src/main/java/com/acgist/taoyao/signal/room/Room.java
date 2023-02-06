package com.acgist.taoyao.signal.room;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 房间
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "房间", description = "房间")
public class Room {
	
	/**
	 * 房间标识
	 */
	@Schema(title = "房间标识", description = "房间标识")
	private String id;
	/**
	 * 房间名称
	 */
	@Schema(title = "房间名称", description = "房间名称")
	private String name;
	/**
	 * 房间密码
	 */
	@Schema(title = "房间密码", description = "房间密码")
	private String password;
	/**
	 * 终端会话标识列表
	 */
	@Schema(title = "终端会话标识列表", description = "终端会话标识列表")
	private List<String> snList;
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
		synchronized (this.snList) {
			if(this.snList.contains(sn)) {
				return;
			}
			this.snList.add(sn);
		}
	}

}
