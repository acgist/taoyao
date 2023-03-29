package com.acgist.media;

/**
 * 录像机
 *
 * @author acgist
 */
public final class ClientRecorder {

    /**
     * 是否正在录像
     */
    private boolean active;

    private static final ClientRecorder INSTANCE = new ClientRecorder();

    public static final ClientRecorder getInstance() {
        return INSTANCE;
    }

    /**
     * @return 是否正在录像
     */
    public boolean isActive() {
        return this.active;
    }

}
