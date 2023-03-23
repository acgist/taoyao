package com.acgist.taoyao.client.media;

/**
 * 录像机
 *
 * @author acgist
 */
public final class Recorder {

    /**
     * 是否正在录像
     */
    private boolean active;

    private static final Recorder INSTANCE = new Recorder();

    public static final Recorder getInstance() {
        return INSTANCE;
    }

    /**
     * @return 是否正在录像
     */
    public boolean isActive() {
        return this.active;
    }

}
