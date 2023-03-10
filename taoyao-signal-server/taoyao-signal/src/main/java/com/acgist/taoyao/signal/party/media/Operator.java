package com.acgist.taoyao.signal.party.media;

import java.io.Closeable;

/**
 * 操作接口
 * 所有操作直接发出事件
 * 
 * @author acgist
 */
public interface Operator extends Closeable {

    /**
     * 关闭资源
     */
    @Override
    void close();
    
    /**
     * 移除资源
     */
    void remove();
    
    /**
     * 暂停资源
     */
    void pause();
    
    /**
     * 恢复资源
     */
    void resume();
    
}
