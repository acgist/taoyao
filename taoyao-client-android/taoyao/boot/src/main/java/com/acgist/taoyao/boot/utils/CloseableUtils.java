package com.acgist.taoyao.boot.utils;

import java.io.Closeable;

/**
 * 关闭资源工具
 * 
 * @author acgist
 */
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
			// TODO：日志
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
			// TODO：日志
		}
	}
	
}
