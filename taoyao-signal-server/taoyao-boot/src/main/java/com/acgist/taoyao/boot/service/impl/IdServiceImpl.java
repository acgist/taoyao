package com.acgist.taoyao.boot.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import com.acgist.taoyao.boot.config.IdProperties;
import com.acgist.taoyao.boot.service.IdService;

public class IdServiceImpl implements IdService {
    
    public IdServiceImpl(IdProperties idProperties) {
        this.index          = 0;
        this.serverIndex    = idProperties.getServerIndex();
        this.maxIndex       = idProperties.getMaxIndex();
        this.clientIndex    = idProperties.getMinClientIndex();
        this.minClientIndex = idProperties.getMinClientIndex();
        this.maxClientIndex = idProperties.getMaxClientIndex();
    }

    /**
     * 当前索引
     */
    private int index;
    /**
     * 机器序号
     */
    private final int serverIndex;
    /**
     * 最大序号
     */
    private final int maxIndex;
    /**
     * 当前终端索引
     */
    private int clientIndex;
    /**
     * 最小终端序号
     */
    private final int minClientIndex;
    /**
     * 最大终端序号
     */
    private final int maxClientIndex;
    
    @Override
    public long buildId() {
        int index;
        synchronized (this) {
            if (++this.index > this.maxIndex) {
                this.index = 0;
            }
            index = this.index;
        }
        final LocalDateTime time = LocalDateTime.now();
        return
            100000000000000L * time.getDayOfMonth() +
            1000000000000L   * time.getHour()       +
            10000000000L     * time.getMinute()     +
            100000000L       * time.getSecond()     +
            1000000L         * this.serverIndex     +
            index;
    }
    
    @Override
    public long buildClientIndex() {
        int index;
        synchronized (this) {
            if(++this.clientIndex > this.maxClientIndex) {
                this.clientIndex = this.minClientIndex;
            }
            index = this.clientIndex;
        }
        return index;
    }
    
    @Override
    public String buildUuid() {
        return UUID.randomUUID().toString();
    }
    
}
