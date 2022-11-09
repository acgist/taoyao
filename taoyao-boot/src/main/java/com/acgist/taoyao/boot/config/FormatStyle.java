package com.acgist.taoyao.boot.config;

import java.time.format.DateTimeFormatter;

import lombok.Getter;

/**
 * 格式
 * 
 * @author acgist
 */
public interface FormatStyle {
	
	/**
	 * 默认日期格式
	 */
	public static final String YYYY_MM_DD_HH24_MM_SS = "yyyy-MM-dd HH:mm:ss";

	/**
	 * 日期时间
	 * 
	 * @author acgist
	 */
	@Getter
	public static enum DateTimeStyle {
		
		// YYYY
		YYYYMMDD_HH24_MM("yyyyMMdd HH:mm"),
		YYYY_MM_DD_HH24_MM("yyyy-MM-dd HH:mm"),
		YYYYMMDDHH24MMSS("yyyyMMddHHmmss"),
		YYYYMMDDHH24MMSSSSS("yyyyMMddHHmmssSSS"),
		YYYYMMDD_HH24_MM_SS("yyyyMMdd HH:mm:ss"),
		YYYYMMDD_HH24_MM_SS_SSS("yyyyMMdd HH:mm:ss.SSS"),
		YYYY_MM_DD_HH24_MM_SS("yyyy-MM-dd HH:mm:ss"),
		YYYY_MM_DD_HH24_MM_SS_SSS("yyyy-MM-dd HH:mm:ss.SSS"),
		// YY
		YYMMDD_HH24_MM("yyMMdd HH:mm"),
		YY_MM_DD_HH24_MM("yy-MM-dd HH:mm"),
		YYMMDDHH24MMSS("yyMMddHHmmss"),
		YYMMDDHH24MMSSSSS("yyMMddHHmmssSSS"),
		YYMMDD_HH24_MM_SS("yyMMdd HH:mm:ss"),
		YYMMDD_HH24_MM_SS_SSS("yyMMdd HH:mm:ss.SSS"),
		YY_MM_DD_HH24_MM_SS("yy-MM-dd HH:mm:ss"),
		YY_MM_DD_HH24_MM_SS_SSS("yy-MM-dd HH:mm:ss.SSS"),
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

	}

	/**
	 * 时间
	 * 
	 * @author acgist
	 */
	@Getter
	public static enum TimeStyle {

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

	}

	/**
	 * 日期
	 * 
	 * @author acgist
	 */
	@Getter
	public static enum DateStyle {

		YYMMDD("yyMMdd"),
		YYYYMMDD("yyyyMMdd"),
		YY_MM_DD("yy-MM-dd"),
		YYYY_MM_DD("yyyy-MM-dd");

		/**
		 * 格式
		 */
		private String format;
		/**
		 * 格式工具
		 */
		private final DateTimeFormatter dateTimeFormatter;

		private DateStyle(String format) {
			this.format = format;
			this.dateTimeFormatter = DateTimeFormatter.ofPattern(format);
		}

	}
	
}
