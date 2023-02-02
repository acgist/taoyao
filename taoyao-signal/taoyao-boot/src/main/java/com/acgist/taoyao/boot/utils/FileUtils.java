package com.acgist.taoyao.boot.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.lang3.ArrayUtils;

/**
 * 文件工具
 * 
 * @author acgist
 */
public final class FileUtils {

	private FileUtils() {
	}
	
	/**
	 * 文件大小单位
	 */
	private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};
	
	/**
	 * @param size 文件大小
	 * 
	 * @return 文件大小
	 */
	public static final String formatSize(Long size) {
		return formatSize(size, "B");
	}
	
	/**
	 * @param size 文件大小
	 * @param unit 当前单位
	 * 
	 * @return 文件大小
	 */
	public static final String formatSize(Long size, String unit) {
		if(size == null || size == 0L) {
			return "0B";
		}
		int index = ArrayUtils.indexOf(UNITS, unit);
		BigDecimal decimal = BigDecimal.valueOf(size);
		final BigDecimal dataScale = BigDecimal.valueOf(1024);
		while(decimal.compareTo(dataScale) >= 0) {
			if(++index >= UNITS.length) {
				index = UNITS.length - 1;
				break;
			}
			decimal = decimal.divide(dataScale);
		}
		return decimal.setScale(2, RoundingMode.HALF_UP) + UNITS[index];
	}
	
}
