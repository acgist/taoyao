package com.acgist.taoyao.media;

/**
 * 视频来源类型
 *
 * @author acgist
 */
public enum VideoSourceType {

    /**
     * 文件共享：FileVideoCapturer
     */
    FILE,
    /**
     * 后置摄像头：CameraVideoCapturer
     */
    BACK,
    /**
     * 前置摄像头：CameraVideoCapturer
     */
    FRONT,
    /**
     * 屏幕共享：ScreenCapturerAndroid
     */
    SCREEN,
    /**
     * 共享本地：ShareVideoCapturer
     *
     * 注意：这个模式只是用来测试很多功能没有兼容
     */
    SHARE;

    /**
     * @return 是否是摄像头
     */
    public boolean isCamera() {
        return this == BACK || this == FRONT;
    }

}
