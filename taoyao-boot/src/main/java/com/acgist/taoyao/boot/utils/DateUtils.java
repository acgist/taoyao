package com.acgist.taoyao.boot.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.config.FormatStyle.DateStyle;
import com.acgist.taoyao.boot.config.FormatStyle.DateTimeStyle;
import com.acgist.taoyao.boot.config.FormatStyle.TimeStyle;

import lombok.extern.slf4j.Slf4j;

/**
 * 日期工具
 * 
 * @author acgist
 */
@Slf4j
public final class DateUtils {
	
	private DateUtils() {
	}
	
	/**
	 * 生成时间戳
	 * 
	 * @return 时间戳
	 * 
	 * @see #buildTime(LocalDateTime)
	 */
	public static final String buildTime() {
		return buildTime(LocalDateTime.now());
	}

	/**
	 * 生成时间戳
	 * 
	 * @param localDateTime 时间
	 * 
	 * @return 时间戳
	 */
	public static final String buildTime(LocalDateTime localDateTime) {
		if (Objects.isNull(localDateTime)) {
			return buildTime();
		}
		return DateTimeStyle.YYYYMMDDHH24MMSS.getDateTimeFormatter().format(localDateTime);
	}
	
	/**
	 * 日期字符串转换日期
	 * 
	 * @param value 日期字符串
	 * @param format 格式
	 * 
	 * @return 日期
	 */
	public static final Date parse(String value, String format) {
		if(StringUtils.isEmpty(value) || StringUtils.isEmpty(format)) {
			return null;
		}
		try {
			final SimpleDateFormat formatter = new SimpleDateFormat(format);
			return formatter.parse(value);
		} catch (ParseException e) {
			log.error("字符串转换日期异常：{}-{}", value, format, e);
		}
		return null;
	}

	/**
	 * 日期格式化字符串
	 * 
	 * @param date 日期
	 * @param format 格式
	 * 
	 * @return 日期字符串
	 */
	public static final String format(Date date, String format) {
		if(date == null || StringUtils.isEmpty(format)) {
			return null;
		}
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.format(date);
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
	 * @return LocalDateTime
	 */
	public static final LocalDateTime toLocalDateTime(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	/**
	 * 日期转化
	 * 
	 * @param localDate LocalDate
	 * 
	 * @return Date
	 */
	public static final Date toDate(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * 日期转化
	 * 
	 * @param localDateTime LocalDateTime
	 * 
	 * @return Date
	 */
	public static final Date toDate(LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
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
	 * 格式化时间
	 * 
	 * @param localTime LocalTime
	 * @param format 格式
	 * 
	 * @return 时间字符串
	 */
	public static String format(LocalTime localTime, TimeStyle format) {
		return localTime != null && format != null ? format.getDateTimeFormatter().format(localTime) : null;
	}
	
	/**
	 * 格式化日期
	 * 
	 * @param localDate LocalDate
	 * @param format 格式
	 * 
	 * @return 日期字符串
	 */
	public static String format(LocalDate localDate, DateStyle format) {
		return localDate != null && format != null ? format.getDateTimeFormatter().format(localDate) : null;
	}
	
	/**
	 * 格式化日期
	 * 
	 * @param localDateTime LocalDateTime
	 * @param format 格式
	 * 
	 * @return 日期字符串
	 */
	public static String format(LocalDateTime localDateTime, DateTimeStyle format) {
		return localDateTime != null && format != null ? format.getDateTimeFormatter().format(localDateTime) : null;
	}

}
