package com.acgist.taoyao.signal.media.router;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.acgist.taoyao.signal.media.processor.ProcessorChain;
import com.acgist.taoyao.signal.media.stream.MediaStream;
import com.acgist.taoyao.signal.media.stream.MediaStream.Type;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体流路由器处理器
 * 
 * @author acgist
 */
@Slf4j
public class MediaRouterHandler implements MediaRouter {

	/**
	 * 媒体流处理器责任链
	 */
	private ProcessorChain processorChain;
	/**
	 * 发布者
	 */
	private MediaPublisher mediaPublisher;
	/**
	 * 订阅者
	 */
	private MediaSubscriber mediaSubscriber;
	
	@Override
	public void build() {
		this.mediaPublisher = new MediaPublisher();
		this.mediaSubscriber = new MediaSubscriber();
	}

	@Override
	public MediaPublisher publisher() {
		return this.mediaPublisher;
	}

	@Override
	public MediaSubscriber subscriber() {
		return this.mediaSubscriber;
	}
	
	@Override
	public void processorChain(ProcessorChain processorChain) {
		this.processorChain = processorChain;
	}

	@Override
	public List<MediaStream> streamPublisher() {
		return this.mediaPublisher.getStreams();
	}

	@Override
	public List<MediaStream> streamSubscriber(String ... sns) {
		return this.mediaSubscriber.getStreams().stream()
			.filter(v -> ArrayUtils.contains(sns, v.subscriber()))
			.toList();
	}

	@Override
	public List<MediaStream> streamPublisher(Type type) {
		return this.mediaPublisher.getStreams().stream()
			.filter(v -> v.type() == type)
			.toList();
	}

	@Override
	public List<MediaStream> streamSubscriber(Type type, String... sns) {
		return this.mediaSubscriber.getStreams().stream()
			.filter(v -> v.type() == type)
			.filter(v -> ArrayUtils.contains(sns, v.subscriber()))
			.toList();
	}

	@Override
	public void close() {
		try {
			this.mediaPublisher.close();
		} catch (IOException e) {
			log.error("关闭发布者异常", e);
		}
		try {
			this.mediaSubscriber.close();
		} catch (IOException e) {
			log.error("关闭订阅者异常", e);
		}
	}

}
