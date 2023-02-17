package com.acgist.taoyao.boot.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.DateUtils.DateStyle;
import com.acgist.taoyao.boot.utils.DateUtils.DateTimeStyle;
import com.acgist.taoyao.boot.utils.DateUtils.TimeStyle;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

/**
 * JSON工具
 * 
 * @author acgist
 */
public final class JSONUtils {

	private JSONUtils() {
	}

	/**
	 * Mapper（线程安全）
	 */
	private static final ObjectMapper MAPPER = buildMapper();
	
	/**
	 * Java转JSON
	 * 
	 * @param object Java
	 * 
	 * @return JSON
	 */
	public static final String toJSON(Object object) {
		if (Objects.isNull(object)) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw MessageCodeException.of(e, "Java转JSON失败：" + object);
		}
	}
	
	/**
	 * JSON转Java
	 * 
	 * @param <T> Java类型
	 * 
	 * @param json JSON
	 * 
	 * @return Java
	 */
	public static final <T> T toJava(String json) {
		if (Objects.isNull(json)) {
			return null;
		}
		try {
			return MAPPER.readValue(json, new TypeReference<T>() {
			});
		} catch (IOException e) {
			throw MessageCodeException.of(e, "JSON转Java失败：" + json);
		}
	}

	/**
	 * JSON转Java
	 * 
	 * @param <T> Java类型
	 * 
	 * @param json JSON
	 * @param clazz Java类型
	 * 
	 * @return Java
	 */
	public static final <T> T toJava(String json, Class<T> clazz) {
		if (Objects.isNull(json) || Objects.isNull(clazz)) {
			return null;
		}
		try {
			return MAPPER.readValue(json, clazz);
		} catch (IOException e) {
			throw MessageCodeException.of(e, "JSON转Java失败：" + json);
		}
	}
	
	/**
	 * JSON转Java
	 * 
	 * @param <T> Java类型
	 * 
	 * @param json JSON
	 * @param type Java类型
	 * 
	 * @return Java
	 */
	public static final <T> T toJava(String json, TypeReference<T> type) {
		if (Objects.isNull(json) || Objects.isNull(type)) {
			return null;
		}
		try {
			return MAPPER.readValue(json, type);
		} catch (IOException e) {
			throw MessageCodeException.of(e, "JSON转Java失败：" + json);
		}
	}

	/**
	 * JSON转Map
	 * 
	 * @param <K> K类型
	 * @param <V> V类型
	 * 
	 * @param json JSON
	 * 
	 * @return Map
	 */
	public static final <K, V> Map<K, V> toMap(String json) {
		if (Objects.isNull(json)) {
			return Map.of();
		}
		try {
			return MAPPER.readValue(json, new TypeReference<Map<K, V>>() {
			});
		} catch (IOException e) {
			throw MessageCodeException.of(e, "JSON转Map失败：" + json);
		}
	}

	/**
	 * JSON转List
	 * 
	 * @param <T> 元素类型
	 * 
	 * @param json JSON
	 * 
	 * @return List
	 */
	public static final <T> List<T> toList(String json) {
		if (Objects.isNull(json)) {
			return List.of();
		}
		try {
			return MAPPER.readValue(json, new TypeReference<List<T>>() {
			});
		} catch (IOException e) {
			throw MessageCodeException.of(e, "JSON转List失败：" + json);
		}
	}
	
	/**
	 * JSON转List
	 * 
	 * @param <T> 元素类型
	 * 
	 * @param json JSON
	 * @param clazz 类型
	 * 
	 * @return List
	 */
	public static final <T> List<T> toList(String json, Class<T> clazz) {
		if (Objects.isNull(json)) {
			return List.of();
		}
		try {
			return MAPPER.readValue(json, new TypeReference<List<T>>() {
			});
		} catch (IOException e) {
			throw MessageCodeException.of(e, "JSON转List失败：" + json);
		}
	}
	
	/**
	 * @return Mapper
	 */
	public static final ObjectMapper buildMapper() {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper
			.setTimeZone(TimeZone.getDefault())
			.setDateFormat(new SimpleDateFormat(DateTimeStyle.YYYY_MM_DD_HH24_MM_SS.getFormat()))
			.registerModules(buildCustomModule(), buildJavaTimeModule())
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.setSerializationInclusion(Include.NON_NULL);
	}
	
	/**
	 * @return Java类型转换模块
	 */
	private static final Module buildCustomModule() {
		final SimpleModule customModule = new SimpleModule("CustomModule");
		customModule.addSerializer(Long.class, ToStringSerializer.instance);
		return customModule;
	}
	
	/**
	 * @return Java时间类型模块
	 */
	private static final JavaTimeModule buildJavaTimeModule() {
		final JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(TimeStyle.HH24_MM_SS.getDateTimeFormatter()));
		javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateStyle.YYYY_MM_DD.getDateTimeFormatter()));
		javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeStyle.YYYY_MM_DD_HH24_MM_SS.getDateTimeFormatter()));
		javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TimeStyle.HH24_MM_SS.getDateTimeFormatter()));
		javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateStyle.YYYY_MM_DD.getDateTimeFormatter()));
		javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeStyle.YYYY_MM_DD_HH24_MM_SS.getDateTimeFormatter()));
		return javaTimeModule;
	}
	
}
