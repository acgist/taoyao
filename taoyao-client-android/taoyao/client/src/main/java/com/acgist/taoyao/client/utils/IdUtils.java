package com.acgist.taoyao.client.utils;

import java.time.LocalDateTime;

/**
 * ID工具
 *
 * @author acgist
 */
public final class IdUtils {

    /**
     * 当前索引
     */
    private static int index;
    /**
     * 当前终端索引
     */
    private static int clientIndex = 99999;
    /**
     * 最大索引
     */
    private static final int MAX_INDEX = 999;

    /**
     * @return 消息ID
     */
    public static final long buildId() {
        int index;
        synchronized (IdUtils.class) {
            if (++IdUtils.index > IdUtils.MAX_INDEX) {
                IdUtils.index = 0;
            }
            index = IdUtils.index;
        }
        final LocalDateTime time = LocalDateTime.now();
        return
            100000000000000L * time.getDayOfMonth() +
            1000000000000L   * time.getHour()       +
            10000000000L     * time.getMinute()     +
            100000000L       * time.getSecond()     +
            1000000L         * IdUtils.clientIndex  +
            index;
    }

    /**
     * @param clientIndex 当前终端索引
     */
    public static final void setClientIndex(int clientIndex) {
        IdUtils.clientIndex = clientIndex;
    }

}
