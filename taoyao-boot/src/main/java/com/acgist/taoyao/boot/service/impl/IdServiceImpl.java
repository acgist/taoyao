package com.acgist.taoyao.boot.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;

import com.acgist.taoyao.boot.service.IdService;

public class IdServiceImpl implements IdService {

	/**
	 * 机器序号
	 */
	@Value("${taoyao.sn:0}")
	private int sn = 9;
	/**
	 * 当前索引
	 */
	private int index;
	/**
	 * 最大索引
	 */
	private static final int MAX_INDEX = 999999;
	
	@Override
	public long id() {
		synchronized (this) {
			if (++this.index > MAX_INDEX) {
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
			1000000L * this.sn +
			// 每秒并发数量
			this.index;
	}
	
}
