package com.acgist.taoyao.media.config;

/**
 * 配置
 *
 * @author acgist
 */
public class Config {

    /**
     * 屏幕捕获消息
     */
    public static final int WHAT_SCREEN_CAPTURE   = 1000;
    /**
     * 视频录像消息
     */
    public static final int WHAT_RECORD           = 1001;
    /**
     * 新建本地音频消息
     */
    public static final int WHAT_NEW_LOCAL_AUDIO  = 2000;
    /**
     * 新建本地视频消息
     */
    public static final int WHAT_NEW_LOCAL_VIDEO  = 2001;
    /**
     * 新建远程音频消息
     */
    public static final int WHAT_NEW_REMOTE_AUDIO = 2002;
    /**
     * 新建远程视频消息
     */
    public static final int WHAT_NEW_REMOTE_VIDEO = 2003;
    /**
     * 移除远程音频消息
     */
    public static final int WHAT_REMOVE_AUDIO     = 2998;
    /**
     * 移除远程视频消息
     */
    public static final int WHAT_REMOVE_VIDEO     = 2999;
    /**
     * 默认声音大小
     */
    public static final double DEFAULT_VOLUME     = 10.0D;

}
