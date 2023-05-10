package com.acgist.taoyao.boot.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.utils.JSONUtils;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息
 * 接口、信令、媒体信令通用消息模型
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "消息", description = "消息")
@Builder
@JsonIncludeProperties(value = { "code", "message", "header", "body" })
public class Message implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	
    /**
     * 成功标识
     */
    public static final String CODE_0000 = "0000";

	/**
	 * 状态编码
	 */
	@Schema(title = "状态编码", description = "状态编码")
	private String code;
	/**
	 * 状态描述
	 */
	@Schema(title = "状态描述", description = "状态描述")
	private String message;
	/**
	 * 消息头部
	 */
	@Schema(title = "消息头部", description = "消息头部")
	private Header header;
	/**
	 * 消息主体
	 */
	@Schema(title = "消息主体", description = "消息主体")
	private Object body;
	
    public Message() {
        // 默认消息都是成功
        final MessageCode messageCode = MessageCode.CODE_0000;
        this.code    = messageCode.getCode();
        this.message = messageCode.getMessage();
    }

    public Message(String code, String message, Header header, Object body) {
        this.code    = code;
        this.message = message;
        this.header  = header;
        this.body    = body;
    }
	
	/**
	 * @return 成功消息
	 */
	public static final Message success() {
		return Message.success(null);
	}

	/**
	 * @param body 消息主体
	 * 
	 * @return 成功消息
	 */
	public static final Message success(Object body) {
		final Message message = new Message();
		message.setCode(MessageCode.CODE_0000, null);
		message.body = body;
		return message;
	}

	/**
	 * @return 失败消息
	 */
	public static final Message fail() {
		return Message.fail(null, null, null);
	}

	/**
	 * @param messageCode 状态编码
	 * 
	 * @return 失败消息
	 */
	public static final Message fail(MessageCode messageCode) {
		return Message.fail(messageCode, null, null);
	}

	/**
	 * @param messageCode 状态编码
	 * @param body        消息主体
	 * 
	 * @return 失败消息
	 */
	public static final Message fail(MessageCode messageCode, Object body) {
		return Message.fail(messageCode, null, body);
	}
	
	/**
	 * @param message 状态描述
	 * 
	 * @return 失败消息
	 */
	public static final Message fail(String message) {
		return Message.fail(null, message, null);
	}
	
	/**
	 * @param message 状态描述
	 * @param body    消息主体
	 * 
	 * @return 失败消息
	 */
	public static final Message fail(String message, Object body) {
		return Message.fail(null, message, body);
	}
	
	/**
	 * @param messageCode 状态编码
	 * @param message     状态描述
	 * 
	 * @return 失败消息
	 */
	public static final Message fail(MessageCode messageCode, String message) {
		return Message.fail(messageCode, message, null);
	}

	/**
	 * @param messageCode 状态编码
	 * @param message     状态描述
	 * @param body        消息主体
	 * 
	 * @return 失败消息
	 */
	public static final Message fail(MessageCode messageCode, String message, Object body) {
		final Message failMessage = new Message();
		failMessage.setCode(messageCode == null ? MessageCode.CODE_9999 : messageCode, message);
		failMessage.body = body;
		return failMessage;
	}

	@Override
	public Message clone() {
		return new Message(this.code, this.message, this.header.clone(), this.body);
	}
    
    /**
     * @param messageCode 状态编码
     */
    public void setCode(MessageCode messageCode) {
        this.setCode(messageCode, null);
    }
    
    /**
     * @param messageCode 状态编码
     * @param message     状态描述
     * 
     * @return this
     */
    public Message setCode(MessageCode messageCode, String message) {
        this.code    = messageCode.getCode();
        this.message = StringUtils.isEmpty(message) ? messageCode.getMessage() : message;
        return this;
    }
	
	/**
	 * 克隆消息排除消息主体
	 * 
	 * @return 克隆消息
	 */
	public Message cloneWithoutBody() {
	    return new Message(this.code, this.message, this.header.clone(), null);
	}
	
	/**
	 * @return 消息主体
	 */
    @SuppressWarnings("unchecked")
    public <T> T body() {
	    return (T) this.body;
	}
    
    /**
     * 注解不会自动生成
     * 
     * @param code 状态编码
     */
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * @return 是否成功
     */
    public boolean isSuccess() {
        return CODE_0000.equals(this.code);
    }
	
	@Override
	public String toString() {
		return JSONUtils.toJSON(this);
	}

}
