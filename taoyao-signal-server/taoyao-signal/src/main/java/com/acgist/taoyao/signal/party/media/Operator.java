package com.acgist.taoyao.signal.party.media;

import java.io.Closeable;

/**
 * 操作接口
 * 
 * @author acgist
 */
public interface Operator extends Closeable {

    /**
     * 关闭资源
     * 推荐使用事件实现
     */
    @Override
    void close();
    
    /**
     * 移除资源
     */
    void remove();
    
    /**
     * 暂停资源
     * 推荐使用事件实现
     */
    void pause();
    
    /**
     * 恢复资源
     * 推荐使用事件实现
     */
    void resume();
    
    /**
     * 记录日志
     */
    void log();
    
}
