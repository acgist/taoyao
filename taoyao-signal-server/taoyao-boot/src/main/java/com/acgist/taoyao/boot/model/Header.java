package com.acgist.taoyao.boot.model;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 消息头部
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema( title = "消息头部", description = "消息头部")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * 消息版本
	 */
	@Schema(title = "消息版本", description = "消息版本")
	private String v;
	/**
	 * 消息标识
	 */
	@Schema(title = "消息标识", description = "消息标识")
	private String id;
	/**
	 * 协议标识
	 */
	@Schema(title = "协议标识", description = "协议标识")
	private String signal;
	
	@Override
	public Header clone() {
		return new Header(this.v, this.id, this.signal);
	}
	
}
