package com.acgist.taoyao.signal.client.gb;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GbMediaServer {

    public static final boolean loadJniLib(String libPath) {
        try {
            log.info("加载依赖：{}", libPath);
            System.load(libPath);
            return true;
        } catch (Throwable e) {
            log.error("加载依赖异常：{}", libPath, e);
        }
        return false;
    }

    /**
     * 加载
     */
    public static final native void init();

    /**
     * 销毁
     */
    public static final native void cleanup();

    /**
     * 接收数据
     * 
     * 注意：只实现了UDP模式
     * 
     * @param id   媒体ID
     * @param type 通道模式：UDP|TCP-active|TCP-passive
     * 
     * @return 端口
     */
    public static final native int recv(String id, String type);

    /**
     * 发送数据
     * 
     * 注意：媒体格式PCMA|H264
     * 其他格式需要自己实现转码
     * 
     * @param id         媒体ID
     * @param host       主机
     * @param port       端口
     * @param srcSsrc    原始SSRC
     * @param audio_ssrc 音频SSRC
     * @param video_ssrc 视频SSRC
     * 
     * @return 是否成功
     */
    public static final native int send(String id, String host, int port, long srcSsrc, long audio_ssrc, long video_ssrc);

    /**
     * 转发数据
     * 
     * 注意：媒体格式PCMA|H264
     * 其他格式需要自己实现转码
     * 
     * @param id      媒体ID
     * @param host    主机
     * @param port    端口
     * @param srcSsrc 原始SSRC
     * @param dstSsrc 目标SSRC
     * 
     * @return 是否成功
     */
    public static final native int forward(String id, String host, int port, long srcSsrc, long dstSsrc);

    /**
     * 关闭媒体
     * 
     * @param id 媒体ID
     */
    public static final native void close(String id);

}
