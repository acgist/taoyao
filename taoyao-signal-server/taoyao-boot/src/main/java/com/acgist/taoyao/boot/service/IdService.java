package com.acgist.taoyao.boot.service;

/**
 * ID生成器
 * 只能用于生成消息标识
 * 
 * @author acgist
 */
public interface IdService {

	/**
	 * 生成十六位的ID：ddHHmmss(8) + index(2) + xxxxxx(6)
	 * 
	 * @return ID
	 */
	long buildId();
	
	/**
	 * 生成唯一ID
	 * 
	 * @return ID
	 */
	String buildUuid();
	
}
