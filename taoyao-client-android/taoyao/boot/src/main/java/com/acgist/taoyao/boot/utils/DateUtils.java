package com.acgist.taoyao.boot.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

/**
 * 日期工具
 *
 * @author acgist
 */
public final class DateUtils {

    /**
     * 日期
     *
     * @author acgist
     */
    public enum DateStyle {

        YYMMDD("yyMMdd"),
        YYYYMMDD("yyyyMMdd"),
        YY_MM_DD("yy-MM-dd"),
        YYYY_MM_DD("yyyy-MM-dd");

        /**
         * 格式
         */
        private final String format;
        /**
         * 格式工具
         */
        private final DateTimeFormatter dateTimeFormatter;

        private DateStyle(String format) {
            this.format = format;
            this.dateTimeFormatter = DateTimeFormatter.ofPattern(format);
        }

        public String getFormat() {
            return this.format;
        }

        public DateTimeFormatter getDateTimeFormatter() {
            return this.dateTimeFormatter;
        }
        
    }

    /**
     * 时间
     *
     * @author acgist
     */
    public enum TimeStyle {

        HH24("HH"),
        HH24MM("HHmm"),
        HH24_MM("HH:mm"),
        HH24MMSS("HHmmss"),
        HH24_MM_SS("HH:mm:ss"),
        HH24MMSSSSS("HHmmssSSS"),
        HH24_MM_SS_SSS("HH:mm:ss.SSS");

        /**
         * 格式
         */
        private final String format;
        /**
         * 格式工具
         */
        private final DateTimeFormatter dateTimeFormatter;

        private TimeStyle(String format) {
            this.format = format;
            this.dateTimeFormatter = DateTimeFormatter.ofPattern(format);
        }

        public String getFormat() {
            return this.format;
        }

        public DateTimeFormatter getDateTimeFormatter() {
            return this.dateTimeFormatter;
        }

    }

    /**
     * 日期时间
     *
     * @author acgist
     */
    public enum DateTimeStyle {

        // YY
        YYMMDD_HH24_MM("yyMMdd HH:mm"),
        YY_MM_DD_HH24_MM("yy-MM-dd HH:mm"),
        YYMMDDHH24MMSS("yyMMddHHmmss"),
        YYMMDDHH24MMSSSSS("yyMMddHHmmssSSS"),
        YYMMDD_HH24_MM_SS("yyMMdd HH:mm:ss"),
        YYMMDD_HH24_MM_SS_SSS("yyMMdd HH:mm:ss.SSS"),
        YY_MM_DD_HH24_MM_SS("yy-MM-dd HH:mm:ss"),
        YY_MM_DD_HH24_MM_SS_SSS("yy-MM-dd HH:mm:ss.SSS"),
        // YYYY
        YYYYMMDD_HH24_MM("yyyyMMdd HH:mm"),
        YYYY_MM_DD_HH24_MM("yyyy-MM-dd HH:mm"),
        YYYYMMDDHH24MMSS("yyyyMMddHHmmss"),
        YYYYMMDDHH24MMSSSSS("yyyyMMddHHmmssSSS"),
        YYYYMMDD_HH24_MM_SS("yyyyMMdd HH:mm:ss"),
        YYYYMMDD_HH24_MM_SS_SSS("yyyyMMdd HH:mm:ss.SSS"),
        YYYY_MM_DD_HH24_MM_SS("yyyy-MM-dd HH:mm:ss"),
        YYYY_MM_DD_HH24_MM_SS_SSS("yyyy-MM-dd HH:mm:ss.SSS"),
        // ISO
        YY_MM_DD_HH24_MM_SS_ISO("yy-MM-dd'T'HH:mm:ss"),
        YY_MM_DD_HH24_MM_SS_SSS_ISO("yy-MM-dd'T'HH:mm:ss.SSS"),
        YYYY_MM_DD_HH24_MM_SS_ISO("yyyy-MM-dd'T'HH:mm:ss"),
        YYYY_MM_DD_HH24_MM_SS_SSS_ISO("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        // UTC
        YY_MM_DD_HH24_MM_SS_UTC("yy-MM-dd'T'HH:mm:ss'Z'"),
        YY_MM_DD_HH24_MM_SS_SSS_UTC("yy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        YYYY_MM_DD_HH24_MM_SS_UTC("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        YYYY_MM_DD_HH24_MM_SS_SSS_UTC("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        /**
         * 格式
         */
        private final String format;
        /**
         * 格式工具
         */
        private final DateTimeFormatter dateTimeFormatter;

        private DateTimeStyle(String format) {
            this.format = format;
            this.dateTimeFormatter = DateTimeFormatter.ofPattern(format);
        }

        public String getFormat() {
            return this.format;
        }

        public DateTimeFormatter getDateTimeFormatter() {
            return this.dateTimeFormatter;
        }

    }
    
    private DateUtils() {
    }

    /**
     * @return 时间戳
     * 
     * @see #buildTime(LocalDateTime)
     */
    public static final String buildTime() {
        return DateUtils.buildTime(LocalDateTime.now());
    }

    /**
     * @param localDateTime 日期时间
     * 
     * @return 时间戳
     */
    public static final String buildTime(LocalDateTime localDateTime) {
        if (Objects.isNull(localDateTime)) {
            return DateUtils.buildTime();
        }
        return DateTimeStyle.YYYYMMDDHH24MMSS.getDateTimeFormatter().format(localDateTime);
    }

    /**
     * 日期转化
     *
     * @param date Date
     * 
     * @return LocalDate
     */
    public static final LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 日期转化
     *
     * @param date Date
     * 
     * @return LocalTime
     */
    public static final LocalTime toLocalTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
    }

    /**
     * 日期转化
     *
     * @param date Date
     * 
     * @return LocalDateTime
     */
    public static final LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 转换毫秒
     *
     * @param localDateTime LocalDateTime
     * 
     * @return 毫秒
     */
    public static final long toMilli(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 格式化日期
     *
     * @param localDate LocalDate
     * @param format    格式
     * 
     * @return 日期字符串
     */
    public static String format(LocalDate localDate, DateStyle format) {
        return localDate != null && format != null ? format.getDateTimeFormatter().format(localDate) : null;
    }

    /**
     * 格式化时间
     *
     * @param localTime LocalTime
     * @param format    格式
     * 
     * @return 时间字符串
     */
    public static String format(LocalTime localTime, TimeStyle format) {
        return localTime != null && format != null ? format.getDateTimeFormatter().format(localTime) : null;
    }

    /**
     * 格式化日期时间
     *
     * @param localDateTime LocalDateTime
     * @param format        格式
     * 
     * @return 日期时间字符串
     */
    public static String format(LocalDateTime localDateTime, DateTimeStyle format) {
        return localDateTime != null && format != null ? format.getDateTimeFormatter().format(localDateTime) : null;
    }

}
