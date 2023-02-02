package com.acgist.taoyao.boot.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * URL工具
 * 
 * @author acgist
 */
public final class URLUtils {

	private URLUtils() {
	}

	/**
	 * URL编码
	 * 
	 * @param content 原始内容
	 * 
	 * @return 编码内容
	 */
	public static final String encode(String content) {
		if (StringUtils.isEmpty(content)) {
			return content;
		}
		return URLEncoder
			.encode(content, StandardCharsets.UTF_8)
			// 空格编码变成加号导致加号解码变成空格
			.replace("+", "%20");
	}

	/**
	 * URL解码
	 * 
	 * @param content 编码内容
	 * 
	 * @return 原始内容
	 */
	public static final String decode(String content) {
		if (StringUtils.isEmpty(content)) {
			return content;
		}
		return URLDecoder.decode(content, StandardCharsets.UTF_8);
	}
	
	/**
	 * Map转为URL参数
	 * 
	 * @param map Map
	 * 
	 * @return URL参数
	 */
	public static final String toQuery(Map<String, String> map) {
		if (MapUtils.isEmpty(map)) {
			return null;
		}
		return map.entrySet().stream()
			.filter(entry -> StringUtils.isNotEmpty(entry.getKey()) || StringUtils.isNotEmpty(entry.getValue()))
			.map(entry -> String.join("=", entry.getKey(), URLUtils.encode(entry.getValue())))
			.collect(Collectors.joining("&"));
	}

}
