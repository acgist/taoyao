package com.acgist.taoyao.signal.media.stream;

import java.io.IOException;

import com.acgist.taoyao.boot.model.MessageCodeException;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 终端媒体流适配器
 * 
 * @author acgist
 */
@Getter
@Setter
@ToString(of = {"id", "type", "status", "publisher", "subscriber"})
public abstract class MediaStreamAdapter<T> implements MediaStream {

	/**
	 * 标识
	 */
	protected String id;
	/**
	 * 类型
	 */
	protected Type type;
	/**
	 * 状态
	 */
	protected Status status;
	/**
	 * 发布者
	 */
	private String publisher;
	/**
	 * 订阅者
	 */
	private String subscriber;
	/**
	 * 真实流
	 */
	protected T stream;
	
	@Override
	public String id() {
		return this.id;
	}
	
	@Override
	public Type type() {
		return this.type;
	}
	
	@Override
	public Status status() {
		return this.status;
	}
	
	@Override
	public String publisher() {
		return this.publisher;
	}
	
	@Override
	public String subscriber() {
		return this.subscriber;
	}
	
	@Override
	public void open(MediaStream stream) throws IOException {
		throw MessageCodeException.of("禁止套娃");
	}
	
}
