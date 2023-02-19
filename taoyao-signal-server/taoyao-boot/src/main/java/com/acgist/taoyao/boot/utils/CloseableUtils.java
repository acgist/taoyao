package com.acgist.taoyao.boot.utils;

import java.io.Closeable;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭资源工具
 * 
 * @author acgist
 */
@Slf4j
public final class CloseableUtils {

	private CloseableUtils() {
	}
	
	/**
	 * 关闭资源
	 * 
	 * @param closeable 资源
	 */
	public static final void close(Closeable closeable) {
		try {
		    if(closeable != null) {
		        closeable.close();
		    }
		} catch (Exception e) {
			log.error("关闭资源异常", e);
		}
	}
	
	/**
	 * 关闭资源
	 * 
	 * @param closeable 资源
	 */
	public static final void close(AutoCloseable closeable) {
		try {
		    if(closeable != null) {
		        closeable.close();
		    }
		} catch (Exception e) {
			log.error("关闭资源异常", e);
		}
	}
	
}
