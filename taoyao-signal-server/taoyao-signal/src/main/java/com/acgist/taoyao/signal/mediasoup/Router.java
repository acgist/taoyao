package com.acgist.taoyao.signal.mediasoup;

import java.io.Closeable;
import java.util.List;

import com.acgist.taoyao.signal.room.Room;

import lombok.extern.slf4j.Slf4j;

/**
 * 路由
 * 
 * @author acgist
 */
@Slf4j
public class Router implements Closeable {
	
	/**
	 * 房间
	 */
	private Room room;
	/**
	 * 传输通道列表
	 */
	private List<Transport> transportList;
	
	@Override
	public void close() {
		log.info("关闭路由：{}", this.room.getId());
		this.transportList.forEach(Transport::close);
	}

}
