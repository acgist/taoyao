package com.acgist.taoyao.boot.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import com.acgist.taoyao.boot.config.IdProperties;
import com.acgist.taoyao.boot.service.IdService;

public class IdServiceImpl implements IdService {
	
	private final IdProperties idProperties;
	
	public IdServiceImpl(IdProperties idProperties) {
        this.idProperties = idProperties;
    }

	/**
	 * 当前索引
	 */
	private int index;
	
    @Override
	public long buildId() {
		synchronized (this) {
			if (++this.index > this.idProperties.getMaxIndex()) {
				this.index = 0;
			}
		}
		final LocalDateTime time = LocalDateTime.now();
		return
			100000000000000L * time.getDayOfMonth() +
			1000000000000L * time.getHour() +
			10000000000L * time.getMinute() +
			100000000L * time.getSecond() +
			1000000L * this.idProperties.getIndex() +
			// 每秒并发数量
			this.index;
	}
    
    @Override
    public String buildUuid() {
        return UUID.randomUUID().toString();
    }
	
}
