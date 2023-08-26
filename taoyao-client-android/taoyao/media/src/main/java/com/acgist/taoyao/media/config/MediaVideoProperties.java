package com.acgist.taoyao.media.config;

/**
 * 视频配置
 *
 * 注意：完全拷贝信令模块`MediaVideoProperties`代码
 *
 * @author acgist
 */
public class MediaVideoProperties {

    /**
     * 视频格式
     *
     * @author acgist
     */
    public enum Format {

        VP8,
        VP9,
        H264,
        H265;

    }

    /**
     * 格式：VP8|VP9|H264|H265
     */
    private Format format;
    /**
     * 码率：400|800|1200|1600
     * 码率影响画质
     */
    private Integer bitrate;
    /**
     * 帧率：15|18|20|24|30|45
     * 帧率影响流畅
     */
    private Integer frameRate;
    /**
     * 分辨率：4096*2160|2560*1440|1920*1080|1280*720|720*480
     * 分辨率影响画面大小
     */
    private String resolution;
    /**
     * 宽度：4096|2560|1920|1280|720
     */
    private Integer width;
    /**
     * 高度：2160|1440|1080|720|480
     */
    private Integer height;

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Integer getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(Integer frameRate) {
        this.frameRate = frameRate;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

}
