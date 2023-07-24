package com.acgist.taoyao.signal.party.media;

/**
 * 操作接口适配器
 * 
 * @author acgist
 */
public abstract class OperatorAdapter implements Operator {

    /**
     * 是否关闭
     */
    protected volatile boolean close = false;
    
    @Override
    public void close() {
    }
    
    @Override
    public void remove() {
    }
    
    @Override
    public void pause() {
    }
    
    @Override
    public void resume() {
    }
    
    @Override
    public void log() {
    }
    
    /**
     * 标记关闭
     * 
     * @return 是否已经关闭
     */
    protected boolean markClose() {
        final boolean old = this.close;
        this.close = true;
        return old;
    }
    
}
