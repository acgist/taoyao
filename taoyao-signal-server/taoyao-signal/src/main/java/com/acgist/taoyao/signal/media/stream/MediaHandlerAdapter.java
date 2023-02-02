package com.acgist.taoyao.signal.media.stream;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.media.stream.MediaStream.Type;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 终端媒体处理器适配器
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class MediaHandlerAdapter implements MediaHandler {

	/**
	 * 媒体流集合
	 */
	protected List<MediaStream> streams = new CopyOnWriteArrayList<>();

	@Override
	public void open() throws IOException {
		throw MessageCodeException.of("禁止使用");
	}
	
	@Override
	public void open(MediaStream stream) throws IOException {
		log.debug("打开媒体流：{}", stream);
		this.streams.add(stream);
	}

	@Override
	public void pause() throws IOException {
		this.streams.forEach(v -> {
			try {
				v.pause();
			} catch (IOException e) {
				log.error("暂停媒体流异常：{}", v, e);
			}
		});
	}

	@Override
	public void resume() throws IOException {
		this.streams.forEach(v -> {
			try {
				v.resume();
			} catch (IOException e) {
				log.error("恢复媒体流异常：{}", v, e);
			}
		});
	}

	@Override
	public void close() throws IOException {
		this.streams.forEach(v -> {
			try {
				v.close();
			} catch (IOException e) {
				log.error("关闭媒体流异常：{}", v, e);
			}
		});
	}

	@Override
	public void open(Type type) throws IOException {
		throw MessageCodeException.of("禁止使用");
	}

	@Override
	public void pause(Type type) throws IOException {
		this.streams.stream().filter(v -> v.type() == type).forEach(v -> {
			try {
				v.pause();
			} catch (IOException e) {
				log.error("暂停媒体流异常：{}", v, e);
			}
		});
	}

	@Override
	public void resume(Type type) throws IOException {
		this.streams.stream().filter(v -> v.type() == type).forEach(v -> {
			try {
				v.resume();
			} catch (IOException e) {
				log.error("恢复媒体流异常：{}", v, e);
			}
		});
	}

	@Override
	public void close(Type type) throws IOException {
		this.streams.stream().filter(v -> v.type() == type).forEach(v -> {
			try {
				v.close();
			} catch (IOException e) {
				log.error("关闭媒体流异常：{}", v, e);
			}
		});
	}
	
}
