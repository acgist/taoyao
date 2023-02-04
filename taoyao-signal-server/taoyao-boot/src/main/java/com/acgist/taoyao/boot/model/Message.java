package com.acgist.taoyao.boot.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.utils.JSONUtils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 请求响应消息
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "请求响应消息", description = "请求响应消息")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 响应编码
	 */
	@Schema(title = "响应编码", description = "响应消息标识响应状态")
	private String code;
	/**
	 * 响应描述
	 */
	@Schema(title = "响应描述", description = "响应消息描述响应编码")
	private String message;
	/**
	 * 请求响应头部
	 */
	@Schema(title = "请求响应头部", description = "请求响应头部")
	private Header header;
	/**
	 * 请求响应主体
	 */
	@Schema(title = "请求响应主体", description = "请求响应主体")
	private Object body;
	
	/**
	 * 覆盖
	 * 
	 * @param code 状态编码
	 */
	public void setCode(String code) {
		this.code = code;
	}
	
	/**
	 * @param code 状态编码
	 * 
	 * @return this
	 */
	public Message setCode(MessageCode code) {
		this.code = code.getCode();
		this.message = code.getMessage();
		return this;
	}
	
	/**
	 * @param code 响应编码
	 * @param message 响应描述
	 * 
	 * @return this
	 */
	public Message setCode(MessageCode code, String message) {
		if(StringUtils.isEmpty(message)) {
			message = code.getMessage();
		}
		this.code = code.getCode();
		this.message = message;
		return this;
	}

	/**
	 * @return 成功消息
	 */
	public static final Message success() {
		return success(null);
	}

	/**
	 * @param body 主体
	 * 
	 * @return 成功消息
	 */
	public static final Message success(Object body) {
		final Message message = new Message();
		message.code = MessageCode.CODE_0000.getCode();
		message.message = MessageCode.CODE_0000.getMessage();
		message.body = body;
		return message;
	}

	/**
	 * @return 错误消息
	 */
	public static final Message fail() {
		return fail(null, null, null);
	}

	/**
	 * @param message 主体
	 * 
	 * @return 错误消息
	 */
	public static final Message fail(String message) {
		return fail(null, message, null);
	}

	/**
	 * @param code 响应编码
	 * 
	 * @return 错误消息
	 */
	public static final Message fail(MessageCode code) {
		return fail(code, null, null);
	}

	/**
	 * @param code 响应编码
	 * @param message 响应描述
	 * 
	 * @return 错误消息
	 */
	public static final Message fail(MessageCode code, String message) {
		return fail(code, message, null);
	}

	/**
	 * @param code 响应编码
	 * @param body 主体
	 * 
	 * @return 错误消息
	 */
	public static final Message fail(MessageCode code, Object body) {
		return fail(code, null, body);
	}

	/**
	 * @param code 响应编码
	 * @param message 响应描述
	 * @param body 主体
	 * 
	 * @return 错误消息
	 */
	public static final Message fail(MessageCode code, String message, Object body) {
		if(code == null) {
			code = MessageCode.CODE_9999;
		}
		if (StringUtils.isEmpty(message)) {
			message = code.getMessage();
		}
		final Message failMessage = new Message();
		failMessage.code = code.getCode();
		failMessage.message = message;
		failMessage.body = body;
		return failMessage;
	}

	@Override
	public Message clone() {
		try {
			return (Message) super.clone();
		} catch (CloneNotSupportedException e) {
			return new Message(this.code, this.message, this.header, this.body);
		}
	}
	
	/**
	 * 克隆排除主体
	 * 
	 * @return 请求响应消息
	 */
	public Message cloneWidthoutBody() {
		try {
			final Message message = (Message) super.clone();
			message.setBody(null);
			return message;
		} catch (CloneNotSupportedException e) {
			return new Message(this.code, this.message, this.header, null);
		}
	}
	
	@Override
	public String toString() {
		return JSONUtils.toJSON(this);
	}
	
}
