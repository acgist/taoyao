package com.acgist.taoyao.signal.party.media;

/**
 * 媒体路由类型
 * 
 * @author acgist
 */
public enum RouterType {

    /**
     * 对讲：只有两个人之间的媒体相互路由
     */
    ONE_TO_ONE,
    /**
     * 广播：只有一个人的媒体路由到其他人
     */
    ONE_TO_ALL,
    /**
     * 网播：所有人的媒体相互路由
     */
    ALL_TO_ALL,
    
}
