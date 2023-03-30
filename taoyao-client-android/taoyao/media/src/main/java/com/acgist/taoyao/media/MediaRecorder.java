package com.acgist.taoyao.media;

/**
 * 录像机
 *
 * @author acgist
 */
public final class MediaRecorder {

    /**
     * 是否正在录像
     */
    private boolean active;

    private static final MediaRecorder INSTANCE = new MediaRecorder();

    public static final MediaRecorder getInstance() {
        return INSTANCE;
    }

    /**
     * @return 是否正在录像
     */
    public boolean isActive() {
        return this.active;
    }

}
