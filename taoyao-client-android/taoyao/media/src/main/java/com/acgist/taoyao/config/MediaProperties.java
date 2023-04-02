package com.acgist.taoyao.config;

import java.util.Map;

/**
 * 媒体配置
 *
 * @author acgist
 */
public class MediaProperties {

    /**
     * 最小视频宽度
     */
    private Integer minWidth;
    /**
     * 最大视频宽度
     */
    private Integer maxWidth;
    /**
     * 最小视频高度
     */
    private Integer minHeight;
    /**
     * 最大视频高度
     */
    private Integer maxHeight;
    /**
     * 最小视频码率
     */
    private Integer minBitrate;
    /**
     * 最大视频码率
     */
    private Integer maxBitrate;
    /**
     * 最小视频帧率
     */
    private Integer minFrameRate;
    /**
     * 最大视频帧率
     */
    private Integer maxFrameRate;
    /**
     * 最小音频采样数
     */
    private Integer minSampleSize;
    /**
     * 最大音频采样数
     */
    private Integer maxSampleSize;
    /**
     * 最小音频采样率
     */
    private Integer minSampleRate;
    /**
     * 最大音频采样率
     */
    private Integer maxSampleRate;
    /**
     * 音频默认配置
     */
    private MediaAudioProperties audio;
    /**
     * 视频默认配置
     */
    private MediaVideoProperties video;
    /**
     * 音频配置
     */
    private Map<String, MediaAudioProperties> audios;
    /**
     * 视频配置
     */
    private Map<String, MediaVideoProperties> videos;

    public Integer getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(Integer minWidth) {
        this.minWidth = minWidth;
    }

    public Integer getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
    }

    public Integer getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(Integer minHeight) {
        this.minHeight = minHeight;
    }

    public Integer getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(Integer maxHeight) {
        this.maxHeight = maxHeight;
    }

    public Integer getMinBitrate() {
        return minBitrate;
    }

    public void setMinBitrate(Integer minBitrate) {
        this.minBitrate = minBitrate;
    }

    public Integer getMaxBitrate() {
        return maxBitrate;
    }

    public void setMaxBitrate(Integer maxBitrate) {
        this.maxBitrate = maxBitrate;
    }

    public Integer getMinFrameRate() {
        return minFrameRate;
    }

    public void setMinFrameRate(Integer minFrameRate) {
        this.minFrameRate = minFrameRate;
    }

    public Integer getMaxFrameRate() {
        return maxFrameRate;
    }

    public void setMaxFrameRate(Integer maxFrameRate) {
        this.maxFrameRate = maxFrameRate;
    }

    public Integer getMinSampleSize() {
        return minSampleSize;
    }

    public void setMinSampleSize(Integer minSampleSize) {
        this.minSampleSize = minSampleSize;
    }

    public Integer getMaxSampleSize() {
        return maxSampleSize;
    }

    public void setMaxSampleSize(Integer maxSampleSize) {
        this.maxSampleSize = maxSampleSize;
    }

    public Integer getMinSampleRate() {
        return minSampleRate;
    }

    public void setMinSampleRate(Integer minSampleRate) {
        this.minSampleRate = minSampleRate;
    }

    public Integer getMaxSampleRate() {
        return maxSampleRate;
    }

    public void setMaxSampleRate(Integer maxSampleRate) {
        this.maxSampleRate = maxSampleRate;
    }

    public MediaAudioProperties getAudio() {
        return audio;
    }

    public void setAudio(MediaAudioProperties audio) {
        this.audio = audio;
    }

    public MediaVideoProperties getVideo() {
        return video;
    }

    public void setVideo(MediaVideoProperties video) {
        this.video = video;
    }

    public Map<String, MediaAudioProperties> getAudios() {
        return audios;
    }

    public void setAudios(Map<String, MediaAudioProperties> audios) {
        this.audios = audios;
    }

    public Map<String, MediaVideoProperties> getVideos() {
        return videos;
    }

    public void setVideos(Map<String, MediaVideoProperties> videos) {
        this.videos = videos;
    }
}
