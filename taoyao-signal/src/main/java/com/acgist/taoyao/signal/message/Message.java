package com.acgist.taoyao.signal.message;

import java.io.Serializable;

import com.acgist.taoyao.boot.utils.JSONUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 信令消息
 * 
 * @author acgist
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 信令头部
	 */
	private Header header;
	/**
	 * 信令主体
	 */
	private Object body;
	
	@Override
	public String toString() {
		return JSONUtils.toJSON(this);
	}
	
}
