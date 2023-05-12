package com.acgist.taoyao.media.config;

/**
 * 音频配置
 * 
 * @author acgist
 */
public class MediaAudioProperties {

        /**
         * 音频格式
         * 
         * @author acgist
         */
        public enum Format {
                
            G722,
            PCMA,
			PCMU,
			OPUS;
                
        }

        /**
         * 格式：G722|PCMA|PCMU|OPUS
         */
        private Format format;
        /**
         * 比特率：96|128|256
         */
        private Integer bitrate;
        /**
         * 采样位数：8|16|32
         */
        private Integer sampleSize;
        /**
         * 采样率：8000|16000|32000|48000
         */
        private Integer sampleRate;

        public Format getFormat() {
                return this.format;
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

        public Integer getSampleSize() {
                return this.sampleSize;
        }

        public void setSampleSize(Integer sampleSize) {
                this.sampleSize = sampleSize;
        }

        public Integer getSampleRate() {
                return this.sampleRate;
        }

        public void setSampleRate(Integer sampleRate) {
                this.sampleRate = sampleRate;
        }
}
