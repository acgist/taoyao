package com.acgist.taoyao.boot.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.config.IdProperties;
import com.acgist.taoyao.boot.service.IdService;

public class IdServiceImpl implements IdService {
	
	/**
	 * 当前索引
	 */
	private int index;

	@Autowired
	private IdProperties idProperties;
	
	@Override
	public long buildId() {
		synchronized (this) {
			if (++this.index > this.idProperties.getMaxIndex()) {
				this.index = 0;
			}
		}
		final LocalDateTime time = LocalDateTime.now();
		return
			100000000000000000L * (time.getYear() % 100) +
			1000000000000000L * time.getMonthValue() +
			10000000000000L * time.getDayOfMonth() +
			100000000000L * time.getHour() +
			1000000000L * time.getMinute() +
			10000000L * time.getSecond() +
			// 机器序号一位
			1000000L * this.idProperties.getIndex() +
			// 每秒并发数量
			this.index;
	}
	
	@Override
	public String buildIdToString() {
		return String.valueOf(this.buildId());
	}
	
}
